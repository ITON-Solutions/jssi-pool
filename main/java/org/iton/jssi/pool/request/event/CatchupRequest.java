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

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.iton.jssi.ledger.merkle.MerkleTree;
import org.iton.jssi.pool.event.Synced;
import org.iton.jssi.pool.network.event.SendOneRequest;
import org.iton.jssi.pool.request.RequestHandler;
import org.iton.jssi.pool.request.state.CatchupSingle;
import org.iton.jssi.pool.request.state.Finish;
import org.iton.jssi.pool.request.state.IRequestState;

/**
 *
 * @author ITON Solutions
 */
public class CatchupRequest implements IRequestEvent{
    
    public MerkleTree tree;
    public int size;
    public byte[] root;
    public String reqId;
    
    public CatchupRequest(MerkleTree tree, int size, byte[] root){
        this.tree = tree;
        this.size = size;
        this.root = root;
    }
    
    @Override
    public void handleRequest(RequestHandler request) {
        IRequestState current = request.state;

        String message = getMessage();

        if (message == null) {
            LOG.debug("No transactions to catch up!");
            request.state = new Finish();
            request.event = new Synced(tree);
            return;
        }

        request.network.handleEvent(new SendOneRequest(message, reqId, request.timeout));
        request.state = new CatchupSingle(size, root, reqId, tree);
        request.event = null;
        LOG.debug(String.format("Event %s (%s -> %s)", getEvent(), current.getState(), request.state.getState()));
    }
    
    @Override
    public Event getEvent() {
        return Event.CATCHUP_REQUEST;
    }

    @Override
    public String getMessage(){

        ObjectNode message = JsonNodeFactory.instance.objectNode();
        ObjectNode event = buildRequest(tree, size);
        if(event == null){
            return null;
        }
        message.put(getEvent().name(), event);
        return message.toString();
    }
    
    private ObjectNode buildRequest(MerkleTree tree, int size){
        if(tree.getCount() >= size){
            return null;
        }
        
        int seqNoStart = tree.getCount() + 1;
        int seqNoEnd = size;
        
        reqId = String.format("%d%d", seqNoStart, seqNoEnd);

        ObjectNode event = JsonNodeFactory.instance.objectNode();
        event.put("ledgerId", 0);
        event.put("seqNoStart", seqNoStart);
        event.put("seqNoEnd", seqNoEnd);
        event.put("catchupTill", size);
        
        return event;
    }
}
