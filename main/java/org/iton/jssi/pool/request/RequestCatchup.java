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

package org.iton.jssi.pool.request;

import java.util.List;
import java.util.Map;
import org.iton.jssi.ledger.merkle.MerkleTree;
import org.iton.jssi.pool.request.state.CatchupConsensus;
import org.iton.jssi.ursa.bls.VerKey;

/**
 *
 * @author ITON Solutions
 */
public class RequestCatchup {
    
    public static enum CatchupProgress{
        IN_PROGRESS,
        NOT_NEEDED,
        RESTART,
        SHOULD_BE_RESTARTED
    }
    
    public static void checkNodesResponses(Map<CatchupConsensus.Key, List<String>> replies, MerkleTree tree, int size, int threshold, Map<String, VerKey> verkeys, String name){
        
    }
}
