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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.bitcoinj.core.Base58;
import org.iton.jssi.pool.request.state.IRequestState;
import org.iton.jssi.pool.request.event.IRequestEvent;
import org.iton.jssi.pool.request.state.Start;
import org.iton.jssi.pool.event.IPoolEvent;
import org.iton.jssi.ursa.bls.VerKey;
import org.iton.jssi.pool.network.INetworkHandler;
import org.iton.jssi.ursa.bls.Generator;
import org.iton.jssi.ursa.pair.CryptoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author ITON Solutions
 * 
 * 
 * Transitions of request state
 * 
 * Start -> Start, Single, Consensus, CatchupSingle, CatchupConsensus, Full, Finish
 * Single -> Single, Finish
 * Consensus -> Consensus, Finish
 * CatchupSingle -> CatchupSingle, Finish
 * CatchupConsensus -> CatchupConsensus, Finish
 * Full -> Full, Finish
 * Finish -> Finish
 */
public class RequestHandler implements IRequestHandler {
    
    private static final Logger LOG = LoggerFactory.getLogger(RequestHandler.class);
    
    public INetworkHandler network;
    public IRequestState state;
    public IPoolEvent event;
    
    public int[] cmdIds;
    public Map<String, VerKey> verkeys = new HashMap<>();
    public Generator generator;
    private String name;
    public long timeout;
    public long extended;
    public int threshold;
    
     public RequestHandler(INetworkHandler network,
            int threshold,
            int[] cmdIds,
            Map<String, VerKey> verkeys,
            String name,
            long timeout,
            long extended) throws CryptoException{
        
        this.threshold = threshold;
        this.network = network;
        this.cmdIds = cmdIds;
        this.verkeys = verkeys;
        this.generator = new Generator().fromBytes(Base58.decode("3LHpUjiyFC2q2hD7MnwwNmVXiuaFbQx2XkAFJWzswCjgN1utjsCeLzHsKk1nJvFEaS4fcrUmVAkdhtPCYbrVyATZcmzwJReTcJqwqBCPTmTQ9uWPwz6rEncKb2pYYYFcdHa8N17HzVyTqKfgPi4X9pMetfT3A5xCHq54R2pDNYWVLDX"));
        this.name = name;
        this.timeout = timeout;
        this.extended = extended;
        this.state = new Start();
     }
    
    @Override
    public void handleEvent(IRequestEvent event) throws JsonProcessingException {
        LOG.debug(String.format("Handle event %s", event == null ? "NONE" : event.getEvent()));
        if(event == null){
            return;
        }
        event.handleRequest(this);
    }
    
    @Override
    public boolean isTerminal() {
        return state.getState() == IRequestState.State.FINISH;
    }

    @Override
    public IPoolEvent getEvent() {
        return event;
    }
    
    public ObjectNode getResult(String message){
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode data = mapper.readTree(message);
            JsonNode result = data.get("result");
            return (ObjectNode) result;
        } catch (IOException e) {
            return null;
        }
    }
    
    public ObjectNode removeProof(ObjectNode result) {
        
        result.remove("state_proof");
        if (result.has("data")) {
            ((ObjectNode)result.get("data")).remove("stateProofFrom");
        }
        return result;

    }
    public long getTimestamp(String message){
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode data = mapper.readTree(message);
            String op = data.get("op").asText();
            if(op.equals(IRequestEvent.Event.REJECT.name()) || op.equals(IRequestEvent.Event.REQNACK.name())){
                return 0L;
            }

            JsonNode result = data.get("result");
            
            if(result.get("ver").asInt() != 1){
                return 0L;
            }
            
            long timestamp = result.get("multiSignature")
                    .get("signedState")
                    .get("stateMetadata")
                    .get("timestamp").asLong();
            
            return timestamp;
        } catch (JsonProcessingException e) {
            return 0L;
        }
    }
}
