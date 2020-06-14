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


import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.iton.jssi.ledger.merkle.MerkleTree;


/**
 *
 * @author ITON Solutions
 */
public class CatchupConsensus implements IRequestState{

    public MerkleTree tree = null;
    public Map<Key, List<String>> replies = new HashMap<>();
    
    public CatchupConsensus(MerkleTree tree){
        this.tree = tree;
    }
    
    @Override
    public State getState() {
         return State.CATCHUP_CONSENSUS;
    }
    
    public static class Key{
        public String root;
        public int txnSeqNo;
        public String[] hashes;
        
        public Key(String root, int txnSeqNo, String[] hashes){
            this.root = root;
            this.txnSeqNo = txnSeqNo;
            this.hashes = hashes;
        }
        
        @Override
        public boolean equals(Object object){
            if(!(object instanceof Key)){
                return false;
            }
            
            Key other = (Key) object;
            return (root == null ? other.root == null : this.root.equals(other.root)) 
                    && txnSeqNo == other.txnSeqNo 
                    && Arrays.equals(other.hashes, hashes);
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 43 * hash + Objects.hashCode(this.root);
            hash = 43 * hash + this.txnSeqNo;
            hash = 43 * hash + Arrays.deepHashCode(this.hashes);
            return hash;
        }
    }

}
