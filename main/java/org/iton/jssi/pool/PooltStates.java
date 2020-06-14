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

/**
 * Receive events from network that will be processed in PoolThread
 * 
 * Transitions of pool state
 * Initialization -> GettingCatchupTarget, Terminated, Closed
 * GettingCatchupTarget -> SyncCatchup, Active, Terminated, Closed
 * Active -> GettingCatchupTarget, Terminated, Closed
 * SyncCatchup -> Active, Terminated, Closed
 * Terminated -> GettingCatchupTarget, Closed
 * Closed -> Closed
 */

package org.iton.jssi.pool;

import java.util.HashMap;
import java.util.Map;
import org.iton.jssi.pool.event.IPoolEvent.Event;
import org.iton.jssi.pool.state.IPoolState.State;

/**
 *
 * @author ITON Solutions
 */
public class PooltStates {
    
    Map<State, Map<Event, State[]>> edges = new HashMap<>();
    
    private void init(){
        
        Map<Event, State[]> initialization = new HashMap<>();
        initialization.put(Event.CHECK_CACHE, new State[]{State.GETTING_CATCHUP_TARGET, State.TERMINATED});
        initialization.put(Event.CLOSE, new State[]{State.CLOSED});
        edges.put(State.INITIALIZATION, initialization);
        
        Map<Event, State[]> gettingCatchupTarget = new HashMap<>();
        gettingCatchupTarget.put(Event.CLOSE, new State[]{State.CLOSED});
        gettingCatchupTarget.put(Event.CATCHUP_TARGET_NOT_FOUND, new State[]{State.TERMINATED});
        gettingCatchupTarget.put(Event.CATCHUP_RESTART, new State[]{State.GETTING_CATCHUP_TARGET, State.TERMINATED});
        gettingCatchupTarget.put(Event.CATCHUP_TARGET_FOUND,  new State[]{State.SYNC_CATCHUP, State.TERMINATED});
        gettingCatchupTarget.put(Event.SYNCED, new State[]{State.ACTIVE, State.TERMINATED});
        edges.put(State.GETTING_CATCHUP_TARGET, gettingCatchupTarget);
        
        Map<Event, State[]> terminated = new HashMap<>();
        terminated.put(Event.CLOSE, new State[]{State.CLOSED});
        terminated.put(Event.REFRESH, new State[]{State.GETTING_CATCHUP_TARGET, State.TERMINATED});
        terminated.put(Event.TIMEOUT, new State[]{State.TERMINATED});
        edges.put(State.TERMINATED, terminated);
        
        Map<Event, State[]> active = new HashMap<>();
        active.put(Event.POOL_OUTDATED, new State[]{State.TERMINATED});
        active.put(Event.CLOSE, new State[]{State.CLOSED});
        active.put(Event.REFRESH, new State[]{State.GETTING_CATCHUP_TARGET, State.TERMINATED});
        active.put(Event.SEND_REQUEST, new State[]{State.ACTIVE});
        active.put(Event.NODE_REPLY, new State[]{State.ACTIVE});
        active.put(Event.TIMEOUT, new State[]{State.ACTIVE});
        edges.put(State.ACTIVE, active);
        
        Map<Event, State[]> sync_catchup = new HashMap<>();
        sync_catchup.put(Event.CLOSE, new State[]{State.CLOSED});
        sync_catchup.put(Event.NODE_BLACKLISTED, new State[]{State.TERMINATED});
        sync_catchup.put(Event.SYNCED, new State[]{State.ACTIVE, State.TERMINATED});
        edges.put(State.SYNC_CATCHUP, sync_catchup);
        
        Map<Event, State[]> finish = new HashMap<>();
        edges.put(State.CLOSED, finish);
    }
}
