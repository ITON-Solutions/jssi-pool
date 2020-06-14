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

import org.iton.jssi.pool.network.event.Resend;
import org.iton.jssi.pool.network.event.SendOneRequest;
import org.iton.jssi.pool.request.RequestHandler;
import org.iton.jssi.pool.request.state.IRequestState;
import org.iton.jssi.pool.request.state.Single;

/**
 *
 * @author ITON Solutions
 */
public class CustomSingleRequest implements IRequestEvent {
    
    public String message;
    public String reqId;
    public byte[] sp_key;     // expected key for State Proof in Reply,
    public long[] timestamps; // expected timestamps for freshness comparison
   
    
    public CustomSingleRequest(String reqId, String message, byte[] sp_key, long[] timestamps) {
        this.reqId = reqId;
        this.message = message;
        this.sp_key = sp_key;
        this.timestamps = timestamps;
    }
    
    @Override
    public void handleRequest(RequestHandler request) {
        
        IRequestState current = request.state;

        LOG.debug(String.format("Send one request: %s reqId %s", message, reqId));
        request.network.handleEvent(new SendOneRequest(message, reqId, request.timeout));

        LOG.debug(String.format("Send resend: %s reqId %s", message, reqId));
        request.network.handleEvent(new Resend(reqId, request.timeout));

        request.state = new Single(sp_key, timestamps);
        request.event = null;
        LOG.debug(String.format("Event %s (%s -> %s)", getEvent(), current.getState(), request.state.getState()));
    }
    
    @Override
    public Event getEvent() {
        return Event.CUSTOM_SINGLE_REQUEST;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
