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
package org.iton.jssi.pool.network;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

/**
 *
 * @author ITON Solutions
 */
public class RemoteNode {
    
    private final String name;
    private final byte[] pk;
    private String address;
    private boolean blacklisted;
    
    public RemoteNode(final String name, final byte[] pk, final String address, boolean blacklisted){
        this.name = name;
        this.pk = pk;
        this.address = address;
        this.blacklisted = blacklisted;
    }
    
    public ZMQ.Socket connect(ZContext context, ZMQ.Curve.KeyPair pair){
        
        ZMQ.Socket socket = context.createSocket(SocketType.DEALER);
        socket.setIdentity(pair.publicKey.getBytes());
        socket.setCurveSecretKey(pair.secretKey.getBytes());
        socket.setCurvePublicKey(pair.publicKey.getBytes());
        socket.setCurveServerKey(pk);
        socket.setLinger(0);
        socket.connect(address);
        return socket;
    }

    public String getName() {
        return name;
    }

    public boolean isBlacklisted() {
        return blacklisted;
    }

    public void setBlacklisted(boolean blacklisted) {
        this.blacklisted = blacklisted;
    }
    
    public void setAddress(String address){
        this.address = address;
    }
    @Override
    public boolean equals(Object obj){
        if(!(obj instanceof RemoteNode)){
            return false;
        }
        RemoteNode current = (RemoteNode) obj;
        return this.name.equals(current.getName());
    }
    
}
