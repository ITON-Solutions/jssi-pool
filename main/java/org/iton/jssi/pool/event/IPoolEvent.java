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
import org.iton.jssi.pool.PoolHandler;
import org.iton.jssi.pool.request.event.IRequestEvent;
import org.libsodium.jni.SodiumException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author ITON Solutions
 */
public interface IPoolEvent {
    
    static final Logger LOG = LoggerFactory.getLogger(IPoolEvent.class);
    
    public static enum Event {
        NODE_REPLY,
        REFRESH,
        SEND_REQUEST,
        CATCHUP_RESTART,
        CATCHUP_TARGET_FOUND,
        CATCHUP_TARGET_NOT_FOUND,
        SYNCED,
        CHECK_CACHE,
        TIMEOUT,
        POOL_OUTDATED,
        NODE_BLACKLISTED,
        CLOSE,
        NOP
    }
    
    
    Event getEvent();
    void handleEvent(PoolHandler pool) throws SodiumException, JsonProcessingException;
    IRequestEvent requestEvent() throws JsonProcessingException;
}
