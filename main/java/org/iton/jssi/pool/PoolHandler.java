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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.bitcoinj.core.Base58;
import org.iton.jssi.ursa.bls.VerKey;
import org.iton.jssi.ursa.pair.CryptoException;
import org.iton.jssi.ledger.merkle.MerkleTree;
import org.iton.jssi.pool.event.IPoolEvent;
import org.iton.jssi.pool.network.RemoteNode;
import org.iton.jssi.pool.model.NodeTransaction;
import org.iton.jssi.pool.model.TxnData;
import org.iton.jssi.pool.network.event.NodesStateUpdated;
import org.iton.jssi.pool.request.event.LedgerStatus;
import org.iton.jssi.pool.request.IRequestHandler;
import org.iton.jssi.pool.request.RequestHandler;
import org.iton.jssi.pool.state.Initialization;
import org.iton.jssi.pool.state.IPoolState;
import org.libsodium.api.Crypto_sign_ed25519;
import org.libsodium.jni.SodiumException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.iton.jssi.pool.network.INetworkHandler;

/**
 *
 * @author ITON Solutions
 * 
 * Receive events from service that will be processed in PoolThread
 * 
 * Transitions of pool state
 * Initialization -> GettingCatchupTarget, Terminated, Closed
 * GettingCatchupTarget -> SyncCatchup, Active, Terminated, Closed
 * Active -> GettingCatchupTarget, Terminated, Closed
 * SyncCatchup -> Active, Terminated, Closed
 * Terminated -> GettingCatchupTarget, Closed
 * Closed -> Closed
 */
public class PoolHandler implements IPoolHandler {
    
    private static final Logger LOG = LoggerFactory.getLogger(PoolHandler.class);
    
    public RemoteNode[] remotes = new RemoteNode[0];
    public Map<String, NodeTransaction> nodes = new HashMap<>();
    public Map<String, VerKey> verkeys = new HashMap<>();
    
    public INetworkHandler network;
    public IPoolHandler pool;
    public IRequestHandler request;
    public IPoolState state;
    
    public String name;
    private int id;
    private long timeout;
    private long extended;
    
    public PoolHandler(){}
    
    public PoolHandler(INetworkHandler network, String name, int id, long timeout, long extended){
        this.name = name;
        this.id = id;
        this.timeout = timeout;
        this.extended = extended;
        this.network = network;
        state = new Initialization();
    }
    
    @Override
    public void handleEvent(IPoolEvent event) throws SodiumException, JsonProcessingException {
        LOG.debug(String.format("Handle event %s", event == null ? "NONE" : event.getEvent()));
        if(event == null){
            return;
        }
        event.handleEvent(this);
    }
    
    @Override
    public boolean isTerminal() {
        return state.getState() == IPoolState.State.TERMINATED;
    }
    
    private int threshold(int size){
        if(size < 4){
            return 0;
        }
        return (size - 1) / 3;
    }

    public IRequestHandler getRequestHandler(String poolName, INetworkHandler network) throws IOException, CryptoException, SodiumException {
        MerkleTree tree = PoolTreeFactory.create(poolName);
        verkeys = getVerkeys(tree);

        network.handleEvent(new NodesStateUpdated(remotes));
        IRequestHandler handler = new RequestHandler(network, threshold(nodes.size()), new int[0], verkeys, poolName, timeout, extended);
        LedgerStatus event = new LedgerStatus(null, tree);
        handler.handleEvent(event);
        return handler;
    }
            
    public Map<String, VerKey> getVerkeys(MerkleTree tree) throws IOException, CryptoException, SodiumException{
        
        nodes = PoolTreeFactory.buildNodeState(tree);
        remotes = new RemoteNode[nodes.size()];
        
        NodeTransaction[] values = nodes.values().toArray(new NodeTransaction[remotes.length]);
        
        for(int i = 0; i < remotes.length; i++){
            TxnData data = values[i].getTxn().getData();
            String alias   = data.getData().getAlias();
            String address = String.format("tcp://%s:%d", data.getData().getClientIP(), data.getData().getClientPort());
            
            byte[] pk = Base58.decode(data.getDest());
            byte[] public_key = Crypto_sign_ed25519.pk_to_curve25519(pk);
            remotes[i] = new RemoteNode(alias, public_key, address, false);
            
            byte[] bytes = Base58.decode(data.getData().getBlskey());
            verkeys.put(alias, new VerKey().build(bytes));
        }
        return verkeys;
    }
}
