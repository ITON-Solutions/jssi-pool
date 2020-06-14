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
package org.iton.jssi.pool;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.iton.jssi.pool.event.IPoolEvent;
import org.iton.jssi.pool.event.CheckCache;
import org.iton.jssi.pool.event.Close;
import org.iton.jssi.pool.event.Refresh;
import org.iton.jssi.pool.event.SendRequest;
import org.zeromq.ZFrame;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

/**
 *
 * @author ITON Solutions
 * 
 * 
 * Receive commands from inproc socket that will be processed in PoolThread
 */
public class CommandHandler {
    
    ZMQ.Socket receiver;
    
    public CommandHandler(ZMQ.Socket receiver){
        this.receiver = receiver;
    }
    
    public IPoolEvent fetchEvents(){
        ZMsg  msg = ZMsg.recvMsg(receiver, ZMQ.DONTWAIT);
        ZFrame[] parts = new ZFrame[msg.size()];
        msg.toArray(parts);
        
        String message = new String(parts[0].getData());
        ByteBuffer data = ByteBuffer.wrap(parts[1].getData());
        int cmdId = data.order(ByteOrder.LITTLE_ENDIAN).getInt();
        
        switch(IPoolEvent.Event.valueOf(message)){
            case CLOSE:
                return new Close(cmdId);
            case REFRESH:
                return new Refresh(cmdId);
            case CHECK_CACHE:
                return new CheckCache(cmdId);
            default:
                data = ByteBuffer.wrap(parts[2].getData());
                Integer timeout = data.order(ByteOrder.LITTLE_ENDIAN).getInt();
                timeout = timeout == -1 ? null : timeout;
                String[] nodes = new String[0] ;
                if(parts.length > 3){
                    nodes = new String(parts[3].getData()).split(",");
                }
                return new SendRequest(cmdId, message, timeout, nodes);
        }
    }
    
    public ZMQ.PollItem getPollItem(){
        return new ZMQ.PollItem(receiver, ZMQ.Poller.POLLIN);
    }
}
