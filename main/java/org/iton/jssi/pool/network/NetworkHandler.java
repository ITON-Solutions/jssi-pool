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
package org.iton.jssi.pool.network;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.iton.jssi.pool.event.IPoolEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;
import org.iton.jssi.pool.network.event.INetworkEvent;

/**
 *
 * @author ITON Solutions
 */
public class NetworkHandler implements INetworkHandler {
    
    private static final Logger LOG = LoggerFactory.getLogger(NetworkHandler.class);
    
    public Map<Integer, PoolConnection> pools = new HashMap<>();
    public Map<String, Integer> reqIds = new HashMap<>();
    public RemoteNode[] nodes = new RemoteNode[0];
    String[] preordered;
    private final int limit;
    private final long active;
    
    public NetworkHandler(long active, int limit, String[] preordered){
        this.active = active;
        this.limit= limit;
        this.preordered = preordered;
    }
    
    @Override
    public List<IPoolEvent> fetchEvents(ZMQ.PollItem[] pollItems) {
        
        List<IPoolEvent> result = new ArrayList<>();
        int end = 0;
        
        for(PoolConnection pool : pools.values()){
            int init = end;
            end += pool.getSockets().length;
            ZMQ.PollItem[] items = Arrays.copyOfRange(pollItems, init, end);
            result.addAll(pool.fetchEvents(items));
        }

        return result;
    }
    
    @Override
    public PoolConnection.Timeout getTimeout() {
        
        Long min = Long.MAX_VALUE;
        PoolConnection.Timeout timeout = null;
        
        for(PoolConnection pool : pools.values()){
            PoolConnection.Timeout current = pool.getTimeout();
            if(current.timeout < min){
                min = current.timeout;
                timeout = current;
            }
        }
        
        if(timeout != null){
            return timeout;
        } else {
            return new PoolConnection.Timeout(new PoolConnection.Key("", ""), min);
        }
    }
    
    @Override
    public List<ZMQ.PollItem> getPollItems() {
        
        List<ZMQ.PollItem> result = new ArrayList<>();
         
        for(PoolConnection pool : pools.values()){
            result.addAll(pool.getPollItems());
        }

        return result;
    }
    
    public void sendRequest(String reqId, INetworkEvent event){
        
        Integer index = reqIds.get(reqId);
        
        if(index != null){
            PoolConnection pool = pools.get(index);
            pool.sendRequest(event);
            reqIds.put(reqId, index);
            return;
        }
        
        for(Integer key : pools.keySet()){
            PoolConnection pool = pools.get(key);
            if (pool != null 
                    && pool.isActive() 
                    && pool.getRequestCount() < limit
                    && Arrays.equals(nodes, pool.getNodes())) {
                
                LOG.debug(String.format("Reuse pool connection id=%d", key));
                pool.sendRequest(event);
                reqIds.put(reqId, key);
                return;
            }
        }
       
        index = Sequence.getNextId();
        PoolConnection pool = new PoolConnection(nodes, active, preordered);
        LOG.debug(String.format("Create pool connection id=%d", index));
        pools.put(index, pool);
        pool.sendRequest(event);
        reqIds.put(reqId, index);
    }
    
    @Override
    public void handleEvent(INetworkEvent event) {
        
        if(event == null){
            return;
        }
        LOG.debug(String.format("Handle network event %s", event.getEvent().name()));
        event.handleRequest(this);
    }

    public Map<Integer, PoolConnection> getPools() {
        return pools;
    }
}
