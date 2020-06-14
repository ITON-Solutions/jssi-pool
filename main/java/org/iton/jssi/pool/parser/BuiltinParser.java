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

package org.iton.jssi.pool.parser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.iton.jssi.pool.proof.StateProof;
import org.iton.jssi.pool.proof.StateProofDataType;
import org.iton.jssi.pool.proof.StateProofType;
import org.iton.jssi.pool.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static org.iton.jssi.ledger.LedgerConstants.*;

/**
 *
 * @author ITON Solutions
 * 
 * State proof parser
 */
public class BuiltinParser {
    
    private static final Logger LOG = LoggerFactory.getLogger(BuiltinParser.class);

    public static StateProof[] parse(ObjectNode reply, String type, byte[] sp_key) throws JsonProcessingException {
        
        LOG.debug(String.format("Parse reply %s", reply.toString()));

        if(!REQUESTS_FOR_STATE_PROOFS.contains(type)){
            LOG.error(String.format("Contain incorrect state proof type %s", type));
            return null;
        }
        
        String data = null;
        Object parsed = null;
        
        if(!reply.has("data")){
            return new StateProof[0];
        }
        
        Object result = reply.get("data");
        
        if (result instanceof String) {
            data = (String) result;
            ObjectMapper mapper = new ObjectMapper();
            parsed = mapper.readTree(data);
        } else if (result instanceof ObjectNode) {
            data = result.toString();
            parsed = result;
        } else if (result instanceof ArrayNode) {
            data = result.toString();
            parsed = result;
        } else {
            return null;
        }
        
        List<StateProof> sps = new ArrayList<>();
        
        StateProof sp = parse(reply, data, parsed, type, sp_key);
        sps.add(sp);
        
        if(REQUESTS_FOR_MULTI_STATE_PROOFS.contains(type)){
            StateProof multi = parseMulti(reply, data, parsed, type, sp_key);
            sps.add(multi);
        }
        
        return sps.toArray(new StateProof[sps.size()]);
    }

    /**
     * Gets a Revocation Registry Delta (accum values, and delta of issues/revoked indices) for the given time interval (from and to).
     *
     * @param reply
     * @param data
     * @param parsed
     * @param type
     * @param sp_key
     * @return
     */
    private static StateProof parseMulti(ObjectNode reply, String data, Object parsed, String type, byte[] sp_key){
        
        
        return null;
    }
    
    private static StateProof parse(ObjectNode reply, String data, Object parsed_data, String type, byte[] sp_key){

        String proof_nodes;
        String root_hash;
        JsonNode multi_signature;
        int length = 0;


        if(!type.equals(GET_TXN)) {

            JsonNode state_proof = reply.get("state_proof");
            proof_nodes = state_proof.get("proof_nodes").asText();
            root_hash = state_proof.get("root_hash").asText();
            multi_signature = state_proof.get("multi_signature");

            String value = parseValue(reply, data, parsed_data, type, sp_key);

            StateProof<StateProofType.Simple<StateProofDataType.Simple>> sp = new StateProof<>();
            sp.proof_nodes = proof_nodes;
            sp.root_hash = root_hash;
            sp.multi_signature = multi_signature;
            sp.kvs_to_verify = new StateProofType<>(new StateProofType.Simple<>());
            sp.kvs_to_verify.getType().verificationType = new StateProofDataType<>(new StateProofDataType.Simple());
            sp.kvs_to_verify.getType().keyValues.put(Base64.getEncoder().encodeToString(sp_key), value);
            return sp;
        } else {

            JsonNode parsed = (JsonNode) parsed_data;
            proof_nodes = parsed.get("audit_path").toString();
            root_hash = parsed.get("root_hash").asText();
            length = parsed.get("ledger_size").asInt();
            multi_signature = parsed.get("multi_signature");

            String value = parseValue(reply, data, parsed_data, type, sp_key);

            StateProof<StateProofType.Simple<StateProofDataType.Merkle>> sp = new StateProof<>();
            sp.proof_nodes = proof_nodes;
            sp.root_hash = root_hash;
            sp.multi_signature = multi_signature;
            sp.kvs_to_verify = new StateProofType<>(new StateProofType.Simple<>());
            sp.kvs_to_verify.getType().verificationType = new StateProofDataType<>(new StateProofDataType.Merkle());
            sp.kvs_to_verify.getType().verificationType.getType().size = length;
            sp.kvs_to_verify.getType().keyValues.put(Base64.getEncoder().encodeToString(sp_key), value);
            return sp;
        }
    }
    
    private static String parseValue(ObjectNode reply, String data, Object parsed_data, String type, byte[] sp_key){
        
        if(data == null){
            return null;
        }

        ObjectNode value = JsonNodeFactory.instance.objectNode();

        if(type.equals(GET_NYM)){
            value.put("seqNo", reply.has("seqNo") ? reply.get("seqNo").asInt(): null);
            value.put("txnTime",  reply.has("txnTime") ? reply.get("txnTime").asInt(): null);
        } else if(type.equals(GET_AUTH_RULE )){
            // nothing to do
        } else if (!type.equals(GET_TXN_AUTHR_AGRMT) || Utils.indexOf(sp_key, "2:d:".getBytes()) == 0){
            value.put("lsn", reply.has("seqNo") ? reply.get("seqNo").asInt(): null);
            value.put("lut", reply.has("txnTime") ? reply.get("txnTime").asInt(): null);
        }

        if(type.equals(GET_TXN)){
            ObjectNode json_parser_data = (ObjectNode) parsed_data;
            value = (ObjectNode) json_parser_data.get("txn");
        } else if(type.equals(GET_NYM)){
            ObjectNode json_parser_data = (ObjectNode) parsed_data;
            value.set("identifier", json_parser_data.get("identifier"));
            value.set("role", json_parser_data.get("role"));
            value.set("verkey", json_parser_data.get("verkey"));
        } else if(type.equals(GET_ATTR )){
            byte[] hash = Utils.hash256(data.getBytes());
            value.put("val", Utils.toHex(hash));
        } else if(type.equals(GET_CRED_DEF) || type.equals(GET_REVOC_REG_DEF) || type.equals(GET_REVOC_REG) || type.equals(GET_TXN_AUTHR_AGRMT_AML )) {
            //value.put("val", parsed_data);
            LOG.debug("------------------------>" + parsed_data);
        } else if(type.equals(GET_AUTH_RULE)){
            ArrayNode array = (ArrayNode ) parsed_data;
            JsonNode item = array.get(0);
            //value = item.getString("constraint");

        } else if(type.equals(GET_SCHEMA)){
            
        } else if(type.equals(GET_REVOC_REG_DELTA)){
            
        } else if(type.equals(GET_TXN_AUTHR_AGRMT)){
            
        }
        return value.toString();
    }
}
