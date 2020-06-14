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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.iton.jssi.ledger.merkle.MerkleTree;
import org.iton.jssi.pool.event.IPoolEvent.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author ITON Solutions
 * 
 * Create and launch PoolThread, receive commans from inproc socket
 */
public class PoolService {
    
    private static final Logger LOG = LoggerFactory.getLogger(PoolService.class);
    
    private final Map<Integer, ZMQPool> opened  = new HashMap<>();
    private final Map<Integer, ZMQPool> pending = new HashMap<>();
    
    public void create(String name, JsonNode config) throws IOException, PoolAlreadyExistsException, InvalidStructureException {
        
        if(Files.exists(Paths.get(PoolConstants.INDY_CLIENT_DIRECTORY, name))){
            LOG.debug(String.format("Pool ledger config file with name \"%s\" already exists", name));
            throw new PoolAlreadyExistsException(String.format("Pool ledger config file with name \"%s\" already exists", name));
        }
        
        MerkleTree merkle = PoolTreeFactory.create(Paths.get(config.get("genesis_txn").asText()).getFileName().toString());
        if(merkle.getCount() == 0){
            throw new InvalidStructureException("Empty genesis transaction file");
        }
        
        Path path = Paths.get(PoolConstants.INDY_CLIENT_DIRECTORY, name);
        if (!Files.exists(path.getParent())) {
            Files.createDirectories(path.getParent());
        }
        
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(PoolConstants.INDY_CLIENT_DIRECTORY, "config.json"))) {
            writer.write(config.toString());
        }
        
        FileInputStream fis  = new FileInputStream(Paths.get(config.get("genesis_txn").asText()).toFile());
        Files.copy(fis, path, StandardCopyOption.REPLACE_EXISTING);
    }
    
    public void delete(String name){
        
    }
    
    /**
     *
     * @param name pool name
     * @return result
     */
    public int open(String name){

        for(ZMQPool holder : opened.values()) {
            if(holder.pool.getName().equals(name)) {
               LOG.error(String.format("Pool with the same name '%s' is already opened", name));
               return 0;
            }
        }
        
        LOG.debug(String.format("Open pool %s", name));
        
        ZContext context = new ZContext();
        ZMQ.Socket receiver = context.createSocket(SocketType.PAIR);
        ZMQ.Socket sender = context.createSocket(SocketType.PAIR);
        String socketName = String.format("inproc://pool_%s", name);
        receiver.bind(socketName);
        sender.connect(socketName);
        
        int poolId = PoolSequence.getNextId();
        Pool pool = new Pool(
                name, 
                poolId, 
                PoolConstants.POOL_ACK_TIMEOUT,
                PoolConstants.POOL_REPLY_TIMEOUT,
                PoolConstants.POOL_CON_ACTIVE_TO,
                PoolConstants.MAX_REQ_PER_POOL_CON,
                PoolConstants.PREORDERED);
        
        pool.execute(receiver);
        
        int cmdId = CommandSequence.getNextId();
        sendMsg(cmdId, Event.CHECK_CACHE.name(), sender, null, -1);
        
        pending.put(poolId, new ZMQPool(pool, sender));
        return poolId;
    }
    
    public int close(int poolId) {
        int cmdId = CommandSequence.getNextId();

        ZMQPool pool = pending.remove(poolId);
        if(pool == null){
            LOG.error(String.format("No pool with requested handle '%d'", poolId));
            return -1;
        }
        
        sendMsg(cmdId, Event.CLOSE.name(), pool.socket, null, -1);
        return cmdId;
    }
    
    public int add(int poolId) {
        ZMQPool pool = pending.remove(poolId);
        if(pool == null){
            LOG.error(String.format("No pool with requested handle '%d'", poolId));
            return 0;
        }

        opened.put(poolId, pool);
        return poolId;     
    }
    
    private void sendMsg(int cmdId, String message, ZMQ.Socket sender, String nodes, int timeout){
        
        sender.send(message.getBytes(), ZMQ.SNDMORE);
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.order(ByteOrder.LITTLE_ENDIAN).putInt(cmdId);
        sender.send(buffer.array(), ZMQ.SNDMORE);
        
        buffer = ByteBuffer.allocate(4);
        buffer.order(ByteOrder.LITTLE_ENDIAN).putInt(timeout);
        
        if(nodes == null){
            sender.send(buffer.array(), ZMQ.DONTWAIT);
        } else {
            sender.send(buffer.array(), ZMQ.SNDMORE);
            sender.send(nodes.getBytes(), ZMQ.DONTWAIT);
        }
    }
    public String[] list(){
        File file = new File(PoolConstants.INDY_CLIENT_DIRECTORY);
        File[] files = file.listFiles(new FilenameFilter() {
             
            @Override
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".txn");
            }
        });
        
        String[] names = new String[files.length];
        
        for(int i = 0; i < names.length; i++){
            ObjectNode result = JsonNodeFactory.instance.objectNode();
            result.put("pool", files[i].getName());
            names[i] = result.toString();
        }
        return names;
    }
    
    public void version(int version){
        
    }
    
    static class ZMQPool{
        Pool pool;
        ZMQ.Socket socket;
        
        public ZMQPool(Pool pool, ZMQ.Socket socket){
            this.pool = pool;
            this.socket = socket;
        }
    }
}
