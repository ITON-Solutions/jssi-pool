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

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.iton.jssi.pool.network.event.CleanTimeout;
import org.iton.jssi.pool.request.RequestHandler;
import org.iton.jssi.pool.request.state.Consensus;
import org.iton.jssi.pool.request.state.Finish;
import org.iton.jssi.pool.request.state.IRequestState;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author ITON Solutions
 */
public class Reject implements IRequestEvent {

    
    public String reqId;
    public String message;
    public String alias;
    
    public Reject(String message, String alias, String reqId){
        this.reqId = reqId;
        this.message = message;
        this.alias = alias;
    }
    
    @Override
    public Event getEvent() {
        return Event.REJECT;
    }

   
    @Override
    public String getMessage() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void handleRequest(RequestHandler request) {
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
                    list.add(alias);
                } else if (!list.contains(alias)){
                    list.add(alias);
                }
                
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

}
