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
 * 
 * Transitions of request state
 * Start -> Start, Single, Consensus, CatchupSingle, CatchupConsensus, Full, Finish
 * Single -> Single, Finish
 * Consensus -> Consensus, Finish
 * CatchupSingle -> CatchupSingle, Finish
 * CatchupConsensus -> CatchupConsensus, Finish
 * Full -> Full, Finish
 * Finish -> Finish
 */
package org.iton.jssi.pool.request;

import java.util.HashMap;
import java.util.Map;
import org.iton.jssi.pool.request.event.IRequestEvent.Event;
import org.iton.jssi.pool.request.state.IRequestState.State;

/**
 *
 * @author ITON Solutions
 */
public class RequestStates {
    
    Map<State, Map<Event, State[]>> edges = new HashMap<>();
    
    private void init(){
        
        Map<Event, State[]> start = new HashMap<>();
        start.put(Event.LEDGER_STATUS, new State[]{State.CATCHUP_CONSENSUS});
        start.put(Event.CATCHUP_REQUEST, new State[]{State.CATCHUP_SINGLE, State.FINISH});
        start.put(Event.CUSTOM_SINGLE_REQUEST, new State[]{State.SINGLE});
        start.put(Event.CUSTOM_FULL_REQUEST, new State[]{State.FULL, State.FINISH});
        start.put(Event.CUSTOM_CONSENSUS_REQUEST, new State[]{State.CONSENSUS});
        edges.put(State.START, start);
        
        Map<Event, State[]> consensus = new HashMap<>();
        start.put(Event.REPLY, new State[]{State.CONSENSUS, State.FINISH});
        start.put(Event.REQNACK, new State[]{State.CONSENSUS, State.FINISH});
        start.put(Event.REJECT, new State[]{State.CONSENSUS, State.FINISH});
        start.put(Event.REQACK,  new State[]{State.CONSENSUS, State.FINISH});
        start.put(Event.TIMEOUT, new State[]{State.CONSENSUS, State.FINISH});
        start.put(Event.TERMINATE, new State[]{State.FINISH});
        edges.put(State.CONSENSUS, consensus);
        
        Map<Event, State[]> single = new HashMap<>();
        start.put(Event.REPLY, new State[]{State.SINGLE, State.FINISH});
        start.put(Event.REQNACK, new State[]{State.SINGLE, State.FINISH});
        start.put(Event.REJECT, new State[]{State.SINGLE, State.FINISH});
        start.put(Event.REQACK, new State[]{State.SINGLE});
        start.put(Event.TIMEOUT, new State[]{State.SINGLE, State.FINISH});
        start.put(Event.TERMINATE,  new State[]{State.FINISH});
        edges.put(State.SINGLE, single);
        
        Map<Event, State[]> catchup_consensus = new HashMap<>();
        start.put(Event.LEDGER_STATUS, new State[]{State.CATCHUP_CONSENSUS, State.FINISH});
        start.put(Event.CONSISTENCY_PROOF, new State[]{State.CATCHUP_CONSENSUS, State.FINISH});
        start.put(Event.TIMEOUT, new State[]{State.CATCHUP_CONSENSUS, State.FINISH});
        start.put(Event.TERMINATE, new State[]{State.FINISH});
        edges.put(State.CATCHUP_CONSENSUS, catchup_consensus);
        
        Map<Event, State[]> catchup_single = new HashMap<>();
        start.put(Event.CATCHUP_REPLY, new State[]{State.CATCHUP_SINGLE, State.FINISH});
        start.put(Event.TIMEOUT, new State[]{State.CATCHUP_SINGLE});
        start.put(Event.TERMINATE, new State[]{State.FINISH});
        edges.put(State.CATCHUP_SINGLE, catchup_single);
        
        Map<Event, State[]> full = new HashMap<>();
        start.put(Event.REPLY, new State[]{State.FULL, State.FINISH});
        start.put(Event.REQNACK, new State[]{State.FULL, State.FINISH});
        start.put(Event.REJECT, new State[]{State.FULL, State.FINISH});
        start.put(Event.TIMEOUT, new State[]{State.FULL, State.FINISH});
        start.put(Event.TERMINATE,  new State[]{State.FINISH});
        edges.put(State.FULL, full);
        
        Map<Event, State[]> finish = new HashMap<>();
        edges.put(State.FINISH, finish);
    }
}
