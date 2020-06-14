/*
 *
 *  The MIT License
 *
 *  Copyright 2019 ITON Solutions.
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */

package org.iton.jssi.pool.proof;

import org.iton.jssi.ursa.rlp.DecodeException;
import org.iton.jssi.ursa.rlp.RLPItem;
import org.iton.jssi.ursa.rlp.RLPList;
import org.iton.jssi.ursa.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.iton.jssi.ursa.rlp.RLPDecoder.RLP_STRICT;

public class NodeHelper {

    private static final Logger LOG = LoggerFactory.getLogger(NodeHelper.class);

    public static boolean verifyProof(byte[] rlp, byte[] root, byte[] key, String expected) {

        try {
            RLPItem item = RLP_STRICT.collectAll(rlp).get(0);
            RLPList list = (RLPList) item;
            List<RLPItem> elements = list.elements(RLP_STRICT);
            RLPNodeAdapter adapter = new RLPNodeAdapter();
            List<Node> nodes = new ArrayList<>();
            for (RLPItem element : elements) {
                Node node = adapter.decode(element);
                nodes.add(node);
            }

            Map<String, Node> trie = new HashMap<>();
            for (Node node : nodes) {
                trie.put(node.getHash(), node);
            }

            Node node = trie.get(Bytes.toHex(root));
            String result = node.getStringValue(trie, key);

            return result.equals(expected);
        } catch (DecodeException e){
            LOG.error(String.format("Verify exception %s", e.getMessage()));
            return false;
        }
    }

    public static boolean verifyProofRange(byte[] rlp, byte[] root, String prefix, Long from, Long to, Map<String, String> range) throws DecodeException {

        RLPItem item = RLP_STRICT.collectAll(rlp).get(0);
        RLPList list = (RLPList) item;
        List<RLPItem> elements = list.elements(RLP_STRICT);
        RLPNodeAdapter adapter = new RLPNodeAdapter();
        List<Node> nodes = new ArrayList<>();
        for (RLPItem element : elements) {
            Node node = adapter.decode(element);
            nodes.add(node);
        }

        Map<String, Node> trie = new HashMap<>();
        for (Node node : nodes) {
            trie.put(node.getHash(), node);
        }

        Node node = trie.get(Bytes.toHex(root));

        Map<String, String> values = node.getAllValues(trie, prefix.getBytes());
        if(values == null){
            LOG.error("Some errors happened while collecting values from state proof");
        }

        // Preparation of data for verification
        // Fetch numerical suffixes
        Map<String, String> result = new HashMap<>();
        for(String key : values.keySet()){
            if(key.startsWith(prefix)){
                String suffix = key.substring(prefix.length());
                Long index = Long.parseLong(suffix);

                to = to == null ? Long.MAX_VALUE : to;
                from = from == null ? Long.MIN_VALUE : from;

                if(index >= from && index < to){
                    result.put(key, values.get(key));
                }
            }
        }
        if(range == null){
            return !result.isEmpty();
        }
        return result.equals(range);
    }
}
