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
package org.iton.jssi.pool;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.iton.jssi.ledger.merkle.Leaf;
import org.iton.jssi.ledger.merkle.MerkleTree;
import static org.iton.jssi.pool.PoolConstants.INDY_CLIENT_DIRECTORY;
import org.iton.jssi.pool.model.NodeTransaction;
import org.msgpack.jackson.dataformat.MessagePackFactory;

/**
 *
 * @author ITON Solutions
 */
public class PoolTreeFactory {
    
    public static MerkleTree create(String genesis) throws IOException{
        return fromGenesis(genesis);
    }
    
    public static Map<String, NodeTransaction> buildNodeState(MerkleTree merkle) throws IOException{
        
        Map<String, NodeTransaction> nodes = new HashMap<>();
        List<Leaf> leaves = merkle.getLeaves();
        ObjectMapper mapper = new ObjectMapper(new MessagePackFactory());
        for(Leaf leaf : leaves){
            TypeReference<NodeTransaction> type = new TypeReference<NodeTransaction>(){};
            NodeTransaction result = mapper.readValue(leaf.data, type);
            nodes.put(result.getTxn().getData().getDest(), result);
        }
        
        return nodes;
    }
    
    public static byte[] fromJson(String json) throws IOException{
        
        TypeReference<Map<String, Object>> type = new TypeReference<Map<String, Object>>(){};
        ObjectMapper mapper = new ObjectMapper(new MessagePackFactory());
        Map<String, Object> map = mapper.readValue(json, type);
        return mapper.writeValueAsBytes(map);
    }
    
    private static MerkleTree fromGenesis(String genesis) throws IOException{
        List<Leaf> leaves = new ArrayList<>();
        
        Path path = Paths.get(INDY_CLIENT_DIRECTORY, genesis);
        List<String> lines = Files.readAllLines(path); 
        for(String line : lines){
            leaves.add(new Leaf(fromJson(line)));
        }
        
        try{
            return new MerkleTree(leaves).build();
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }
}
