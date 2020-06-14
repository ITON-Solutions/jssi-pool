/*
 * The MIT License
 *
 * Copyright 2019 ITON Solutions.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.iton.jssi.pool.request.event;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.bitcoinj.core.Base58;
import org.iton.jssi.ledger.merkle.MerkleTree;
import org.iton.jssi.pool.event.Synced;
import org.iton.jssi.pool.network.event.SendAllRequest;
import org.iton.jssi.pool.request.RequestHandler;
import org.iton.jssi.pool.request.state.CatchupConsensus;
import org.iton.jssi.pool.request.state.Finish;
import org.iton.jssi.pool.request.state.IRequestState;

/**
 *
 * @author ITON Solutions
 */
public class LedgerStatus implements IRequestEvent {
    
    public String root;
    public Integer txnSeqNo;
    public Integer ledgerId;
    public Integer ppSeqNo;
    public Integer viewNo;
    public Integer protocolVersion;
    
    public String alias;
    public MerkleTree tree;
    
    public LedgerStatus(String alias, MerkleTree tree){

        this.txnSeqNo = tree.getCount();
        this.root = Base58.encode(tree.getHash());
        this.ledgerId = 0;
        this.ppSeqNo = null;
        this.viewNo = null;
        this.protocolVersion = 2;
        
        this.alias = alias;
        this.tree = tree;
    }
    
    public LedgerStatus(String alias, int txnSeqNo, String root, Integer ledgerId, Integer ppSeqNo, Integer viewNo, Integer protocolVersion){
        
        this.alias = alias;
        this.txnSeqNo = txnSeqNo;
        this.root = root;
        this.ledgerId = ledgerId;
        this.ppSeqNo = ppSeqNo; // optional
        this.viewNo = viewNo;   // optional
        this.protocolVersion = protocolVersion; // optional
    }
    
    @Override
    public void handleRequest(RequestHandler request) {

        IRequestState current = request.state;
        switch(current.getState()){
            case START:{
                String message = getMessage();
                request.network.handleEvent(new SendAllRequest(message, root, request.timeout, null));
                request.state = new CatchupConsensus(tree);
                request.event = null;
                break;
            }
            case CATCHUP_CONSENSUS:{ // TODO
                CatchupConsensus state = (CatchupConsensus) request.state;
                CatchupConsensus.Key key = new CatchupConsensus.Key(root, txnSeqNo, new String[0]);
                List<String> values = state.replies.get(key);

                if (values == null) {
                    values = new ArrayList<>();
                    state.replies.put(key, values);
                }
                values.add(alias);
                request.event = new Synced(state.tree);
                request.state = new Finish();
            }
        }
        LOG.debug(String.format("Event %s (%s -> %s)", getEvent(), current.getState(), request.state.getState()));
    }

    @Override
    public Event getEvent() {
        return Event.LEDGER_STATUS;
    }
    
    @Override
    public String getMessage(){
        ObjectNode event = JsonNodeFactory.instance.objectNode();
        event.put("txnSeqNo", txnSeqNo);
        event.put("merkleRoot", root);
        event.put("ledgerId", ledgerId);
        event.put("ppSeqNo", ppSeqNo == null ? null : ppSeqNo);
        event.put("viewNo", viewNo == null ? null : viewNo);
        event.put("protocolVersion", protocolVersion);
        event.put("op", getEvent().name());
        return event.toString();
    }

}
