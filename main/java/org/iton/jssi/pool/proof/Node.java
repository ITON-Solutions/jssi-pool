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
import org.iton.jssi.ursa.rlp.UnrecoverableDecodeException;
import org.iton.jssi.ursa.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.jcajce.provider.digest.SHA3;

import java.util.*;

import static org.iton.jssi.ursa.rlp.RLPDecoder.RLP_STRICT;

public abstract class Node {

    private static final Logger LOG = LoggerFactory.getLogger(Node.class);

    static final byte IS_LEAF_MASK = 0x20;
    static final byte IS_PATH_ODD_MASK = 0x10;

    public enum Type{
        FULL,
        EXTENSION,
        LEAF,
        HASH,
        BLANK
    }

    public abstract Type getType();

    public String getHash() throws DecodeException {
        RLPNodeAdapter adapter = new RLPNodeAdapter();
        RLPItem item = adapter.encode(this);
        SHA3.DigestSHA3 digest = new SHA3.Digest256();
        return Bytes.toHex(digest.digest(item.bytes()));
    }

    public String getStringValue(Map<String, Node> trie, byte[] path) throws DecodeException {
        byte[] value = getValue(trie, path);
        return value == null ? null : new String(value);
    }

    public byte[] getValue(Map<String, Node> trie, byte[] path) throws DecodeException {
        byte[] nibble = pathToNibbles(path);
        byte[] value = _getValue(trie, nibble);

        if(value != null){
            RLPItem item = RLP_STRICT.collectAll(value).get(0);
            if(item.isList()) {
                List<RLPItem> elements = ((RLPList) item).elements(RLP_STRICT);
                RLPItem element = elements.remove(elements.size() - 1);
                return element.data();
            }
        }
        return null;
    }

    private byte[] _getValue(Map<String, Node> trie, byte[] path) throws DecodeException {

        Node node = _getNode(trie, path, new byte[0]);

        if(node != null){
            switch(node.getType()) {
                case FULL: {
                    Full full = (Full) node;
                    return full.value;
                }
                case LEAF:{
                    Leaf leaf = (Leaf) node;
                    return leaf.value;
                }
                case HASH:
                case EXTENSION:
                case BLANK:
                default:
                    return null;
            }
        }
        return null;
    }

    public Map<String, String> getAllValues(Map<String, Node> trie, byte[] prefix) throws DecodeException {

        Node node = getNode(trie, prefix);
        if(node == null){
            node = this;
            prefix = new byte[0];
        }
        Map<byte[], String> values= node._getAllValues(trie, prefix);
        Map<String, String> result = new HashMap<>();
        for(byte[] key : values.keySet()){
            result.put(nibbleToString(key), values.get(key));
        }

        return result;

    }

    public Map<byte[], String> _getAllValues(Map<String, Node> trie, byte[] prefix) throws DecodeException {

        switch(this.getType()){
            case FULL:{
                Full full = (Full) this;
                Map<byte[], String> result = new HashMap<>();

                for(int i = 0; i < 0x0F; i++){
                    Node node = full.nodes[i];
                    byte[] nibble = Bytes.concat(prefix, new byte[]{(byte) i});
                    Map<byte[], String> values = node._getAllValues(trie, nibble);
                    result.putAll(values);
                }
                if(full.value != null){
                    RLPItem item = RLP_STRICT.collectAll(full.value).get(0);
                    if(item.isList()){
                        List<RLPItem> elements = ((RLPList) item).elements(RLP_STRICT);
                        RLPItem element = elements.remove(elements.size() - 1);
                        result.put(prefix, new String(element.data()));
                    }
                }
                return result;
            }
            case EXTENSION:{
                Extension extension = (Extension) this;

                boolean isLeaf = (extension.path[0] & IS_LEAF_MASK) == IS_LEAF_MASK;
                if(isLeaf){
                    throw new UnrecoverableDecodeException("Incorrect Patricia Merkle Trie: node marked as extension but path contains leaf flag");
                }

                byte[] pairPath = parsePath(extension.path);
                prefix = Bytes.concat(prefix, pairPath);
                return extension.next._getAllValues(trie, prefix);
            }
            case LEAF:{
                Leaf leaf = (Leaf) this;

                boolean isLeaf = (leaf.path[0] & IS_LEAF_MASK) == IS_LEAF_MASK;
                if(!isLeaf){
                    throw new UnrecoverableDecodeException("Incorrect Patricia Merkle Trie: node marked as leaf but path contains extension flag");
                }

                byte[] pairPath = parsePath(leaf.path);
                RLPItem item = RLP_STRICT.collectAll(leaf.value).get(0);
                if(item.isList()){
                    List<RLPItem> elements = ((RLPList) item).elements(RLP_STRICT);
                    RLPItem element = elements.remove(elements.size() - 1);
                    Map<byte[], String> result = new HashMap<>();
                    result.put(Bytes.concat(prefix, pairPath), new String(element.data()));
                    return result;
                }
                throw new UnrecoverableDecodeException("Unexpected data format of value in Patricia Merkle Trie");
            }
            case HASH:{
                Hash hash = (Hash) this;
                String key = Bytes.toHex(hash.hash);
                Node next = trie.get(key);
                if(next != null){
                    return next._getAllValues(trie, prefix);
                }
                throw new UnrecoverableDecodeException("Incorrect Patricia Merkle Trie: empty hash node when it should not be empty");
            }
            case BLANK:
            default:
                return new HashMap<>();
        }
    }

    public Node getNode(Map<String, Node> trie, byte[] path) throws DecodeException {
        byte[] nibble = pathToNibbles(path);
        return _getNode(trie, nibble, new byte[0]);
    }

    private Node _getNode(Map<String, Node> trie, byte[] path, byte[] seen) throws DecodeException {

        switch(this.getType()){
            case FULL:{
                if(path.length == 0){
                    return this;
                }
                Full full = (Full) this;
                Node next = full.nodes[path[0]];
                if(next != null) {
                    seen = Bytes.concat(seen, new byte[]{path[0]});
                    return next._getNode(trie, Arrays.copyOfRange(path, 1, path.length), seen);
                }
                return null;
            }
            case HASH:{
                Hash hash = (Hash) this;
                String key = Bytes.toHex(hash.hash);
                Node next = trie.get(key);
                if(next != null){
                    return next._getNode(trie, path, seen);
                }
                throw new UnrecoverableDecodeException("Incomplete key-value DB for Patricia Merkle Trie to get value by the key");
            }
            case LEAF:{
                Leaf leaf = (Leaf) this;

                boolean isLeaf = (leaf.path[0] & IS_LEAF_MASK) == IS_LEAF_MASK;
                if(!isLeaf){
                    throw new UnrecoverableDecodeException("Incorrect Patricia Merkle Trie: node marked as leaf but path contains extension flag");
                }

                byte[] pairPath = parsePath(leaf.path);
                // pair start with path
                if(Bytes.indexOf(pairPath, path) == 0){
                    return leaf;
                } else {
                    return null;
                }
            }
            case EXTENSION:{
                Extension extension = (Extension) this;

                boolean isLeaf = (extension.path[0] & IS_LEAF_MASK) == IS_LEAF_MASK;
                if(isLeaf){
                    throw new UnrecoverableDecodeException("Incorrect Patricia Merkle Trie: node marked as extension but path contains leaf flag");
                }

                byte[] pairPath = parsePath(extension.path);

                if(Bytes.indexOf(path, pairPath) == 0) {
                    // path start with pair
                    seen = Bytes.concat(seen, pairPath);
                    return extension.next._getNode(trie, Arrays.copyOfRange(path, pairPath.length, path.length), seen);
                } else if(Bytes.indexOf(pairPath, path) == 0){
                    // pair start with path
                    return extension;
                } else {
                    return null;
                }
            }
            case BLANK:
            default:{
                return null;
            }
        }
    }

    private byte[] parsePath(byte[] path){
        byte[] nibbles = pathToNibbles(Arrays.copyOfRange(path, 1, path.length));
        if((path[0] & IS_PATH_ODD_MASK) == IS_PATH_ODD_MASK) {
            nibbles = Bytes.put(nibbles, (byte) (path[0] & 0x0F), 0);
        }
        return nibbles;
    }

    private String nibbleToString(byte[] nibble){
        byte[] result = new byte[nibble.length / 2];
        for(int i = 0; i < result.length; i++){
            result[i] = (byte) ((nibble[2 * i] << 4) | nibble[2 * i + 1] & 0x0F);
        }
        return new String(result);
    }

    private byte[] pathToNibbles(byte[] path){
        byte[] nibbles = new byte[2 * path.length];
        for(int i = 0; i < path.length; i++) {
            nibbles[2 * i] = (byte) (path[i] >> 4);
            nibbles[2 * i + 1] = (byte) (path[i] & 0x0F);
        }
        return nibbles;
    }

}
