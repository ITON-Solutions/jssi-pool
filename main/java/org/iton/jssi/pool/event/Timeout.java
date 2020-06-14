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
public class Timeout implements IPoolEvent{
    public String reqId;
    public String alias;
    
    public Timeout(String reqId, String alias){
        this.reqId = reqId;
        this.alias = alias;
    }

    @Override
    public Event getEvent() {
        return Event.TIMEOUT;
    }

    @Override
    public IRequestEvent requestEvent() {
        return new org.iton.jssi.pool.request.event.Timeout(reqId, alias);
    }

    @Override
    public void handleEvent(PoolHandler pool) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
