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
package org.iton.jssi.pool.request.state;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.iton.jssi.pool.network.INetworkHandler;
import org.iton.jssi.pool.network.event.CleanTimeout;
import org.iton.jssi.pool.network.event.Resend;


/**
 *
 * @author ITON Solutions
 */
public class Single implements IRequestState {
       
    public List<String> denied = new ArrayList<>();
    public Map<Key, List<NodeResponse>> replies = new HashMap<>();
    public List<String> timeouts = new ArrayList<>();
    
    public byte[] sp_key;     // optional expected key for State Proof in Reply,
    public long[] timestamps; // optional
    
  
    public Single(byte[] sp_key, long[] timestamps){
        this.sp_key = sp_key;
        this.timestamps = timestamps;
    }
    
    public Single(Map<Key, List<NodeResponse>> replies, List<String> denied, List<String> timeouts, byte[] sp_key, long[] timestamps){
        this.replies = replies;
        this.denied = denied;
        this.timeouts = timeouts;
        
        this.sp_key = sp_key;
        this.timestamps = timestamps;
    }
       
    @Override
    public State getState() {
        return State.SINGLE;
    }
    
    public boolean isConensusReachable(int total){
        int sum = 0;
        for(List<NodeResponse> reply : replies.values()){
            sum += reply.size();
        }
        return timeouts.size() + denied.size() + sum < total;
    }
    
    public IRequestState tryToContinue(INetworkHandler network, String reqId, String alias, int[] cmdIds, int total, long timeout) {
        if (isConensusReachable(total)) {
            network.handleEvent(new Resend(reqId, timeout));
            network.handleEvent(new CleanTimeout(reqId, alias));
            return this;
        } else {
            //TODO: maybe we should change the error, but it was made to escape changing of ErrorCode returned to client
            network.handleEvent(new CleanTimeout(reqId, null));
            return new Finish();
        }
    }
    
    public static class Key {

        public ObjectNode inner;

        public Key(ObjectNode inner) {
            this.inner = inner;
        }

        @Override
        public boolean equals(Object object) {
            if (!(object instanceof Key)) {
                return false;
            }

            try {
                Key current = (Key) object;
                ObjectMapper mapper = new ObjectMapper();
                return mapper.readTree(current.inner.toString()).equals(mapper.readTree(inner.toString()));
            } catch (IOException e) {
                return false;
            }

        }

        @Override
        public int hashCode() {
            try {
                ObjectMapper mapper = new ObjectMapper();
                return mapper.readTree(inner.toString()).hashCode();
            } catch (IOException e) {
                return Objects.hash(inner);
            }
        }
    }
    
    public static class NodeResponse{
        public String message;
        public String alias;
        public long timestamp;
        
        public NodeResponse(String message, String alias, long timestamp){
            this.message = message;
            this.alias = alias;
            this.timestamp = timestamp;
        }
        
        @Override
        public boolean equals(Object object){
            if(!(object instanceof NodeResponse)){
                return false;
            }
            NodeResponse current = (NodeResponse) object;
            return alias.equals(current.alias);
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 97 * hash + Objects.hashCode(this.alias);
            return hash;
        }
    }
    
}
