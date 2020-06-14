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
package org.iton.jssi.pool.state;

import java.util.HashMap;
import java.util.Map;
import org.iton.jssi.pool.request.IRequestHandler;
import org.iton.jssi.ursa.bls.VerKey;
import org.iton.jssi.pool.request.event.Terminate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.iton.jssi.pool.network.INetworkHandler;

/**
 *
 * @author ITON Solutions
 * 
 * SyncCatchup -> Active, Terminated, Closed
 * 
 */
public class SyncCatchup implements IPoolState {
    
    private static final Logger LOG = LoggerFactory.getLogger(SyncCatchup.class);
    
    public final INetworkHandler networker;
    public IRequestHandler handler;
    public final int cmdId;
    public final boolean refresh;
    
    public SyncCatchup(INetworkHandler networker, IRequestHandler handler, int cmdId, boolean refresh) {
        this.networker = networker;
        this.handler = handler;
        this.cmdId = cmdId;
        this.refresh = refresh;
    }

    @Override
    public State getState() {
        return State.SYNC_CATCHUP;
    }
    
}
