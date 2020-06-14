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

import org.iton.jssi.ursa.rlp.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.iton.jssi.ursa.rlp.RLPDecoder.RLP_STRICT;

public class RLPNodeAdapter implements RLPAdapter<Node> {

    private static final Logger LOG = LoggerFactory.getLogger(RLPNodeAdapter.class);

    public static final int RADIX = 0x10;
    public static final int FULL_SIZE = RADIX + 1;
    public static final int PAIR_SIZE = 0x02;
    public static final int HASH_SIZE = 0x20;
    public static final int EMPTY_SIZE = 0x00;
    public static final byte EMPTY_DATA = (byte) 0x80;

    @Override
    public Node decode(RLPItem item) throws DecodeException {
        if (item.isList()){
            RLPList list = (RLPList) item;
            List<RLPItem> elements = list.elements(RLP_STRICT);
            switch(elements.size()){
                case FULL_SIZE:{
                    Node[] nodes = new Node[RADIX];

                    for(int i = 0; i < RADIX; i ++){
                        RLPItem element = elements.get(i);
                        if(Arrays.equals(element.data(), new byte[]{0x00})){
                            continue;
                        }
                        nodes[i] = decode(element);
                    }
                    byte[] value = elements.get(RADIX).data().length == 0 ? null :elements.get(RADIX).data();
                    return new Full(nodes, value);
                }
                case PAIR_SIZE:{
                    byte[] path = elements.get(0).data();
                    if((path[0] & Node.IS_LEAF_MASK) == Node.IS_LEAF_MASK){
                        byte[] value = elements.get(1).data();
                        return new Leaf(path, value);
                    } else if((path[0] & Node.IS_LEAF_MASK) == 0x00){
                        Node next = decode(elements.get(1));
                        return new Extension(path, next);
                    } else{
                        throw new UnrecoverableDecodeException("Decoding error");
                    }
                }
            }
        } else {
            byte[] data = item.data();
            switch(data.length){
                case EMPTY_SIZE:{
                    return new Blank();
                }
                case HASH_SIZE:{
                    return new Hash(data);
                }
            }
        }
        throw new UnrecoverableDecodeException("Decoding error");
    }

    RLPList result = null;

    @Override
    public RLPItem encode(Node node) throws DecodeException{

        switch(node.getType()){
            case FULL:{
                Full full = (Full) node;
                List<RLPItem> list = new ArrayList<>();
                for(Node item : full.nodes){
                    if(item == null){
                        list.add(RLPDecoder.RLP_STRICT.wrap(EMPTY_DATA));
                    } else {
                        list.add(encode(item));
                    }
                }
                if(full.value == null){
                    list.add(RLPDecoder.RLP_STRICT.wrap(EMPTY_DATA));
                } else {
                    list.add(RLPDecoder.RLP_STRICT.wrap(RLPEncoder.encode(full.value)));
                }
                return RLPEncoder.toList(list);
            }
            case EXTENSION:{ // TODO test this case
                Extension extension = (Extension) node;
                List<byte[]> list = new ArrayList<>();
                list.add(extension.path);
                list.add(encode(extension.next).data());
                return RLPDecoder.RLP_STRICT.wrap(RLPEncoder.encodeAsList(list));
            }
            case LEAF:{
                Leaf leaf = (Leaf) node;
                List<byte[]> list = new ArrayList<>();
                list.add(leaf.path);
                list.add(leaf.value);
                return RLPDecoder.RLP_STRICT.wrap(RLPEncoder.encodeAsList(list));
            }
            case HASH:{
                Hash hash = (Hash) node;
                return RLPDecoder.RLP_STRICT.wrap(RLPEncoder.encode(hash.hash));
            }
            case BLANK:{
                return RLPDecoder.RLP_STRICT.wrap(EMPTY_DATA);
            }
            default:
                return null;
        }
    }
}
