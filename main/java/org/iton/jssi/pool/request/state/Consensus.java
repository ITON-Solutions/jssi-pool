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

/**
 *
 * @author ITON Solutions
 */
public class Consensus implements IRequestState{
    
    public List<String> denied = new ArrayList<>();
    public Map<Key, List<String>> replies = new HashMap<>();
    public List<String> timeouts = new ArrayList<>();
    
    
    public Consensus(){}
    
    public Consensus(Map<Key, List<String>> replies, List<String> denied, List<String> timeouts){
        this.replies = replies;
        this.denied = denied;
        this.timeouts = timeouts;
    }
    
    @Override
    public State getState() {
        return State.CONSENSUS;
    }
    
    public boolean isConensusReachable(int threshold, int total){
        int sum = 0;
        int max = 0;
        for(List<String> reply : replies.values()){
            sum += reply.size();
            max = Math.max(max, reply.size());
        }
        return total + max - sum - timeouts.size() - denied.size() > threshold;
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
}
