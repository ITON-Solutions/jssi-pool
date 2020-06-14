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

import org.iton.jssi.pool.PoolHandler;
import org.iton.jssi.pool.request.event.IRequestEvent;


/**
 *
 * @author ITON Solutions
 */
public class SendRequest implements IPoolEvent{
    
    public int cmdId;
    public String request;
    public Integer timeout;
    public String[] nodes;
    
     public SendRequest(int cmdId, String request, Integer timeout, String[] nodes){
        this.cmdId = cmdId;
        this.request = request;
        this.timeout = timeout;
        this.nodes = nodes;
    }
     
    @Override
    public Event getEvent() {
        return Event.SEND_REQUEST;
    }

    @Override
    public IRequestEvent requestEvent() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void handleEvent(PoolHandler pool) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
