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
package org.iton.jssi.pool.network;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import org.iton.jssi.pool.network.event.Resend;
import org.iton.jssi.pool.network.event.SendAllRequest;
import org.iton.jssi.pool.network.event.SendOneRequest;
import org.iton.jssi.pool.event.NodeReply;
import org.iton.jssi.pool.event.IPoolEvent;
import org.iton.jssi.pool.PoolConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.iton.jssi.pool.network.event.INetworkEvent;

/**
 *
 * @author ITON Solutions
 */
public class PoolConnection {
    
    private static final Logger LOG = LoggerFactory.getLogger(PoolConnection.class);
    
    private final ZContext context = new ZContext();
    private Date created = new Date();
    
    private final Map<String, Holder> resends = new HashMap<>();
    private final Map<Key, Long> timeouts = new HashMap<>();
    
    private int requestCount = 0;
    
    private final RemoteNode[] nodes;
    private final ZMQ.Socket[] sockets;
    private final long active;
    private final ZMQ.Curve.KeyPair pair = ZMQ.Curve.generateKeyPair();
    
    
    public PoolConnection(RemoteNode[] nodes, long active, String[] preordered){
        this.nodes = shuffle(nodes, preordered);
        this.sockets = new ZMQ.Socket[nodes.length];
        this.active = active;
    }
    
    public List<ZMQ.PollItem> getPollItems(){
        
        List<ZMQ.PollItem> result = new ArrayList<>();
        
        ZMQ.Poller items = context.createPoller(sockets.length);
        for(ZMQ.Socket socket : sockets){
            int index = items.register(socket, ZMQ.Poller.POLLIN);
            result.add(items.getItem(index));
        }
        return result;
    }
    
    private ZMQ.Socket getSocket(int index) {
        if (sockets[index] == null) {
            ZMQ.Socket socket = nodes[index].connect(context, pair);
            sockets[index] = socket;
        }
        return sockets[index];
    }
    
    public boolean isActive(){
        return (new Date().getTime() - created.getTime()) < active;
    }
    
    public boolean hasActiveRequests(){
        return !timeouts.isEmpty();
    }
    
    public boolean isOrphaned(){
        return !(isActive() || hasActiveRequests());
    }
    
    public void sendRequest(INetworkEvent event){
        
        if(event == null){
            return;
        }
        
        LOG.debug(String.format("Send request to ledger %s", event.getEvent().name()));
        
        switch (event.getEvent()) {
            case SEND_ONE_REQUEST: {
                
                SendOneRequest request = (SendOneRequest) event;
                requestCount++;
                sendMessageToOneNode(0, request.reqId, request.message, request.timeout);
                resends.put(request.reqId, new Holder(0, request.message));
                break;
            }
            case SEND_ALL_REQUEST: {
                
                SendAllRequest request = (SendAllRequest) event;
                requestCount++;
                // send to all nodes
                if(request.nodes == null){
                    for (int index = 0; index < nodes.length; index++) {
                        sendMessageToOneNode(index, request.reqId, request.message, request.timeout);
                    }
                    break;
                }
                // send to set of nodes
                for (int index = 0; index < request.nodes.length; index++) {
                    for (RemoteNode node : nodes) {
                        if (node.getName().equals(request.nodes[index])) {
                            sendMessageToOneNode(index, request.reqId, request.message, request.timeout);
                        }
                    }
                }
                break;
            }
            case RESEND: {
                
                Resend request = (Resend) event;
                Holder resend = resends.get(request.reqId);
                if (resend != null) {
                    int count = resend.count;
                    count++;
                    sendMessageToOneNode(count % nodes.length, request.reqId, resend.message, request.timeout);
                }
                break;
            }
        }
    }
    
    private void sendMessageToOneNode(int index, String reqId, String message, long timeout){
        ZMQ.Socket socket = getSocket(index);
        socket.send(message, ZMQ.DONTWAIT);
        timeouts.put(new Key(reqId, nodes[index].getName()), new Date().getTime() + timeout);
    }
    
    public List<IPoolEvent> fetchEvents(ZMQ.PollItem[] pollItems) {

        List<IPoolEvent> events = new ArrayList<>();
        
        int index = 0;
        
        for(int i = 0; i < nodes.length; i++) {
            ZMQ.Socket socket = sockets[i];
            RemoteNode node = nodes[i];
            if(pollItems[index].isReadable()){
                String message = socket.recvStr(ZMQ.DONTWAIT);
                events.add(new NodeReply(node.getName(), message));
            }
            index++;
        }
        return events;
    }
    
    public Timeout getTimeout(){
        Long timeoutMin = Long.MAX_VALUE;
        Key keyMin = null;
        
        for(Key key : timeouts.keySet()){
            Long timeout = timeouts.get(key) - new Date().getTime();
            
            if(timeout < timeoutMin){
                timeoutMin = timeout;
                keyMin = key;
            }
        }
        
        if(keyMin != null){
           return new Timeout(keyMin, timeoutMin); 
        } else {
           long fromStart = new Date().getTime() - created.getTime();
           return new Timeout(new Key("", ""), PoolConstants.POOL_CON_ACTIVE_TO - fromStart);
        }
    }
    
    public void extendTimeout(String reqId, String name, long extended){
        timeouts.replace(new Key(reqId, name), new Date().getTime() + extended);
    }
    
    public void cleanTimeout(String reqId, String alias){
        if(alias != null){
            Key key = new Key(reqId, alias);
            timeouts.remove(key);
        } else {
            List<Key> removes = new ArrayList<>();
            
            for(Key item : timeouts.keySet()){
                if(item.reqId.equals(reqId)){
                    removes.add(item);
                }
            }
            
            for(Key remove : removes){
                timeouts.remove(remove);
            }
        }
    }
    
    private RemoteNode[] shuffle(RemoteNode[] nodes, String[] preordered){
        
        Random rgen = new Random();  // Random number generator			

        for (int i = 0; i < nodes.length; i++) {
            int pos = rgen.nextInt(nodes.length);
            RemoteNode tmp = nodes[i];
            nodes[i] = nodes[pos];
            nodes[pos] = tmp;
        }
        
        if(preordered != null && preordered.length > 0){
            
            List<RemoteNode> pos = new LinkedList<>(Arrays.asList(nodes));
            List<RemoteNode> pre = new LinkedList<>();
            
            for (String name : preordered) {
                for (RemoteNode node : nodes) {
                    if (node.getName().equals(name)) {
                        pre.add(node);
                        pos.remove(node);
                    }
                }
            }
            
            pre.addAll(pos);
            nodes = pre.toArray(new RemoteNode[pre.size()]);
        }
        
        return nodes;
    }
        
    public ZMQ.Socket[] getSockets() {
        return sockets;
    }

    public int getRequestCount() {
        return requestCount;
    }

    public RemoteNode[] getNodes() {
        return nodes;
    }

    public void setTimeCreated(Date created) {
        this.created = created;
    }

    public ZMQ.Curve.KeyPair getPair() {
        return pair;
    }
    
    public static class Timeout {

        public Key key = new Key("", "");
        public long timeout;

        public Timeout() {
        }

        public Timeout(Key key, long timeout) {
            this.key = key;
            this.timeout = timeout;
        }

        public Timeout(String reqId, String alias, long timeout) {
            this.key = new Key(reqId, alias);
            this.timeout = timeout;
        }
    }
    
    public static class Key {

        public String reqId;
        public String alias;

        public Key(String reqId, String alias) {
            this.reqId = reqId;
            this.alias = alias;
        }

        @Override
        public boolean equals(Object object) {
            if (!(object instanceof Key)) {
                return false;
            }

            Key key = (Key) object;
            return reqId.equals(key.reqId) && alias.equals(key.alias);
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 59 * hash + Objects.hashCode(this.reqId);
            hash = 59 * hash + Objects.hashCode(this.alias);
            return hash;
        }
    }
    
    private class Holder {

        public int count;
        public String message;

        public Holder(int count, String message) {
            this.count = count;
            this.message = message;
        }

        @Override
        public boolean equals(Object object) {
            if (!(object instanceof Holder)) {
                return false;
            }

            Holder item = (Holder) object;

            return count == item.count && message.equals(item.message);
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 31 * hash + this.count;
            hash = 31 * hash + Objects.hashCode(this.message);
            return hash;
        }

    }
}
