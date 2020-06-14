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

import org.iton.jssi.pool.network.event.SendAllRequest;
import org.iton.jssi.pool.request.RequestHandler;
import org.iton.jssi.pool.request.state.Finish;
import org.iton.jssi.pool.request.state.Full;
import org.iton.jssi.pool.request.state.IRequestState;

/**
 *
 * @author ITON Solutions
 */
public class CustomFullRequest implements IRequestEvent {
    
    public String message; // message
    public String reqId; // reqId
    public Long timeout; // timeout
    public String[] nodes; // nodes
    
    public CustomFullRequest(String message, String reqId, Long timeout, String[] nodes){
        this.message = message;
        this.reqId = reqId;
        this.timeout = timeout;
        this.nodes = nodes;
    }
    
    @Override
    public void handleRequest(RequestHandler request) {
        IRequestState current = request.state;
        
        long local_timeout = timeout == null ? timeout : request.extended;
        boolean is_known_nodes = false;

        for (String node : nodes) {
            if (request.verkeys.containsKey(node)) {
                is_known_nodes = true;
                break;
            }
        }

        if (is_known_nodes) {
            LOG.debug(String.format("Send all request: %s", message));
            request.network.handleEvent(new SendAllRequest(message, reqId, local_timeout, nodes));
            request.state = new Full(nodes);
            request.event = null;
        } else {
            request.state = new Finish();
            request.event = null;
        }
        LOG.debug(String.format("Event %s (%s -> %s)", getEvent(), current.getState(), request.state.getState()));
    }
    
    
    @Override
    public Event getEvent() {
        return Event.CUSTOM_FULL_REQUEST;
    }

    public String getMessage() {
        return message;
    }
}
