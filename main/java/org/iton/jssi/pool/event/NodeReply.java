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
package org.iton.jssi.pool.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.iton.jssi.ledger.merkle.MerkleTree;
import org.iton.jssi.pool.PoolHandler;
import org.iton.jssi.pool.request.event.CatchupRequest;
import org.iton.jssi.pool.request.event.IRequestEvent;
import org.iton.jssi.pool.request.event.LedgerStatus;
import org.iton.jssi.pool.state.IPoolState;
import org.libsodium.jni.SodiumException;

/**
 *
 * @author ITON Solutions
 */
public class NodeReply implements IPoolEvent{
    
    public String alias;
    public String reply;
    
    public NodeReply(){}
    
    public NodeReply(String alias, String reply){
        this.alias = alias;
        this.reply = reply;
    }
    
    @Override
    public Event getEvent() {
        return Event.NODE_REPLY;
    }
    
    @Override
    public IRequestEvent requestEvent() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode event = mapper.readTree(reply);
        String op = event.get("op").asText();
        
        IRequestEvent result = null;
        
        switch(IRequestEvent.Event.valueOf(op)){
            case LEDGER_STATUS:{
                
                result = new LedgerStatus(alias,
                        event.get("txnSeqNo").asInt(),
                        event.get("merkleRoot").asText(),
                        event.get("ledgerId").asInt(),
                        event.get("ppSeqNo").asInt(),
                        event.get("viewNo").asInt(),
                        event.get("protocolVersion").asInt(2));
                break;        
            }
            case CATCHUP_REQUEST:{
                result = new CatchupRequest(new MerkleTree(), 0, new byte[0]);
                break;
            }
            case CATCHUP_REPLY:{
                break;
            }
            case CONSISTENCY_PROOF:{
                break;
            }
            case REPLY:{
                break;
            }
            case REQACK:{
                break;
            }
            case REQNACK:{
                break;
            }
            case REJECT:{
                break;
            }
        }
        return result;
    }

    /**
     *
     * @param pool
     * @throws org.libsodium.jni.SodiumException
     */
    @Override
    public void handleEvent(PoolHandler pool) throws SodiumException, JsonProcessingException {
        IPoolState current = pool.state;
        IRequestEvent event = requestEvent();
        pool.request.handleEvent(event);
        pool.handleEvent(pool.request.getEvent());
        LOG.debug(String.format("Event %s (%s -> %s)", getEvent(), current.getState(), pool.state.getState()));
    }
}
