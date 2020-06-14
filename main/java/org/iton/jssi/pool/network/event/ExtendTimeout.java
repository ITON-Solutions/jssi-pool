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
package org.iton.jssi.pool.network.event;

import org.iton.jssi.pool.network.NetworkHandler;
import org.iton.jssi.pool.network.PoolConnection;

/**
 *
 * @author ITON Solutions
 */
public class ExtendTimeout implements INetworkEvent{
    public String reqId;
    public String alias;
    public long extended;
    
    public ExtendTimeout(String reqId, String alias, long extended){
        this.reqId = reqId;
        this.alias = alias;
        this.extended = extended;
    }
    
    @Override
    public Event getEvent() {
        return Event.EXTEND_TIMEOUT;
    }

    @Override
    public void handleRequest(NetworkHandler network) {
        int index = network.reqIds.get(reqId);
        PoolConnection pool = network.pools.get(index);
        pool.extendTimeout(reqId, alias, extended);
    }
}
