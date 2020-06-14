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
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.iton.jssi.pool.network.event.CleanTimeout;
import org.iton.jssi.pool.proof.StateProof;
import org.iton.jssi.pool.proof.StateProofHelper;
import org.iton.jssi.pool.request.RequestHandler;
import org.iton.jssi.pool.request.state.Consensus;
import org.iton.jssi.pool.request.state.Finish;
import org.iton.jssi.pool.request.state.IRequestState;
import org.iton.jssi.pool.request.state.Single;
import org.iton.jssi.ursa.bls.VerKey;

/**
 *
 * @author ITON Solutions
 */
public class Reply implements IRequestEvent {

    public String reply;
    public String message;
    public String alias;
    public String reqId;

    public Reply(String reply, String message, String alias, String reqId){
        this.reply = reply;
        this.message = message;
        this.alias = alias;
        this.reqId = reqId;
    }
    
    @Override
    public Event getEvent() {
        return Event.REPLY;
    }

   
    @Override
    public String getMessage() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void handleRequest(RequestHandler request) throws JsonProcessingException {
         IRequestState current = request.state;
         
         switch (current.getState()) {
            case CONSENSUS: {
                Consensus state = (Consensus) current;
                
                ObjectNode result = request.getResult(message);
                if(result == null){
                    state.denied.add(alias);
                    if(state.denied.size() + state.replies.size() == request.verkeys.size()){
                        request.state = new Finish();
                        request.event = null;
                    } else {
                        request.event = null;
                    }
                    break;
                }
                
                ObjectNode removed = request.removeProof(result);
                Consensus.Key key = new Consensus.Key(removed);
                List<String> list = state.replies.get(key);
                if (list == null) {
                    list = new ArrayList<>();
                    state.replies.put(key, list);
                }
                list.add(alias);
                
                if(list.size() > request.threshold){
                    request.network.handleEvent(new CleanTimeout(reqId, null));
                    request.state = new Finish();
                    request.event = null;
                } else if(state.isConensusReachable(request.threshold, request.verkeys.size())){
                    request.network.handleEvent(new CleanTimeout(reqId, alias));
                    request.event = null;
                } else {
                    request.network.handleEvent(new CleanTimeout(reqId, null));
                    request.state = new Finish();
                    request.event = null;
                }
                break;
            }
            case SINGLE: {
                Single state = (Single) current;
                
                ObjectNode result = request.getResult(message);
                if(result == null){
                    state.denied.add(alias);
                    request.state = state.tryToContinue(request.network, reqId, alias, request.cmdIds, request.verkeys.size(), request.timeout);
                    request.event = null;
                    break;
                }
                
                state.timeouts.remove(alias);
                
                long last = request.getTimestamp(message);
                
                ObjectNode removed = request.removeProof(result);
                Single.Key key = new Single.Key(removed);
                List<Single.NodeResponse> list = state.replies.get(key);
                if (list == null) {
                    list = new ArrayList<>();
                    state.replies.put(key, list);
                }
                list.add(new Single.NodeResponse(message, alias, last));
                int count = list.size();

                if(count > request.threshold || checkStateProof(result, request.threshold, request.verkeys, message, state.sp_key, state.timestamps, last)){
                    request.network.handleEvent(new CleanTimeout(reqId, null));
                    request.state = new Finish();
                    request.event = null;
                } else {
                    request.state = state.tryToContinue(request.network, reqId, alias, request.cmdIds, request.verkeys.size(), request.timeout);
                    request.event = null;
                }
                break;
            }
            case FULL: {
                break;
            }
            default:{
                request.event = null;
            }
        }
        LOG.debug(String.format("Event %s (%s -> %s)", getEvent(), current.getState(), request.state.getState()));
    }

    private boolean checkStateProof(ObjectNode result,
                                    int threshold,
                                    Map<String, VerKey> verkeys,
                                    String message,
                                    byte[] sp_key,
                                    long[] timestamps,
                                    long last) throws JsonProcessingException {
        StateProof[] parsed = StateProofHelper.parse_generic_reply_for_proof_checking(result, message, sp_key);
        return true; //StateProof.verify_parsed_sp(parsed, verkeys, generator);
    }

}
