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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.bitcoinj.core.Base58;
import org.iton.jssi.pool.parser.BuiltinParser;
import org.iton.jssi.pool.util.Utils;
import org.iton.jssi.ursa.bls.BLS;
import org.iton.jssi.ursa.bls.Generator;
import org.iton.jssi.ursa.bls.MultiSignature;
import org.iton.jssi.ursa.bls.VerKey;
import org.iton.jssi.ursa.pair.CryptoException;
import org.msgpack.jackson.dataformat.MessagePackFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.iton.jssi.ledger.LedgerConstants.*;

/**
 *
 * @author ITON Solutions
 * 
 * State Proof main class
 */
public class StateProofHelper {
    
    private static final Logger LOG = LoggerFactory.getLogger(StateProofHelper.class);

    public static StateProof[] parse_generic_reply_for_proof_checking(ObjectNode result, String message, byte[] sp_key) throws JsonProcessingException {

        if(!result.has("type")){
            return null;
        }
        String type = result.get("type").asText();
        if(REQUESTS_FOR_STATE_PROOFS.contains(type)){
            if(sp_key != null){
                return BuiltinParser.parse(result, type, sp_key);
            } else {
                LOG.warn("Can't get key in state proof for built-in type");
                return new StateProof[0];
            }
        }

        // TODO Add plugable parser
        
        return new StateProof[0];
    }
    
    
    public static byte[] parse_key_from_request_for_builtin_sp(ObjectNode message){
        String type = message.get("operation").get("type").asText();
        JsonNode operation = message.get("operation");
        
        LOG.debug(String.format("Parse key from %s", type));
        String key = "";
        if(type.equals(GET_ATTR )){
            
            String attr_name = null;
            if(operation.has("raw")){
                attr_name = operation.get("raw").asText();
            } else if(operation.has("enc")){
                attr_name = operation.get("enc").asText();
            } else if(operation.has("hash")){
                attr_name = operation.get("hash").asText();
            } else {
                return null;
            }
            key = String.format(":%s:%s", "1", Utils.toHex(Utils.hash256(attr_name.getBytes())));
        } else if(type.equals(GET_CRED_DEF)){
            if(!operation.has("signature_type") || !operation.has("ref")){
                return null;
            }
            
            String signature_type = operation.get("signature_type").asText();
            long ref = operation.get("ref").asLong();
            
            String tag = operation.has("tag") ? String.format(":%s", operation.get("tag").asText()) : "";
            key = String.format(":%s:%s:%d%s", "3", signature_type, ref, tag);
        } else if(type.equals(GET_NYM) || type.equals(GET_REVOC_REG_DEF)){
            
        } else if(type.equals(GET_SCHEMA)){
            if(!operation.get("data").has("name") || !operation.get("data").has("version")){
                return null;
            }
            
            String name = operation.get("data").get("signature_type").asText();
            String version = operation.get("data").get("version").asText();
            key = String.format(":%s:%s:%s", "1", name, version);
        } else if(type.equals(GET_REVOC_REG)){
            
            if(!operation.has("revocRegDefId")){
                return null;
            }
            
            String revocRegDefId = operation.get("revocRegDefId").asText();
            key = String.format("%s:%s", "6", revocRegDefId);
        } else if(type.equals(GET_AUTH_RULE)){
            if(!operation.has("auth_type") || !operation.has("auth_action") || !operation.has("field")){
                return null;
            }
            
            String auth_type = operation.get("auth_type").asText();
            String auth_action = operation.get("auth_action").asText();
            String field = operation.get("field").asText();
            
            String new_value = "";
            if(operation.has("new_value")){
                new_value = operation.get("new_value").asText();
            }
            
            String old_value = auth_action.equals("ADD") ? "*" : "";
            if(operation.has("old_value")){
                old_value = operation.get("old_value").asText();
            }
            
            key = String.format("1:%s--%s--%s--%s--%s",  auth_type, auth_action, field, old_value, new_value);
            
        } else if(type.equals(GET_REVOC_REG_DELTA) && !operation.has("from")){
            if(!operation.has("revocRegDefId")){
                return null;
            }
            
            String revocRegDefId = operation.get("revocRegDefId").asText();
            key = String.format("%s:%s", "5", revocRegDefId);
        } else if(type.equals(GET_REVOC_REG_DELTA) && operation.has("from")){
            if(!operation.has("revocRegDefId")){
                return null;
            }
            
            String revocRegDefId = operation.get("revocRegDefId").asText();
            key = String.format("%s:%s", "6", revocRegDefId);
        } else if(type.equals(GET_TXN_AUTHR_AGRMT)){
            String version = null;
            if(operation.has("version")){
                version = operation.get("version").asText();
            }
            
            String digest = null;
            if(operation.has("digest")){
                digest = operation.get("digest").asText();
            }
            
            Long timestamp = null;
            if(operation.has("timestamp")){
                timestamp = operation.get("timestamp").asLong();
            }
            
            if(version == null && digest == null && timestamp != null){
                key = "2:latest";
            } else if(version == null && digest != null && timestamp == null){
                key = String.format("2:d:%s", digest);
            } else if(version != null && digest == null && timestamp == null){
                key = String.format("2:v:%s", version);
            } else {
                return null;
            }
        } else if (type.equals(GET_TXN_AUTHR_AGRMT_AML)) {
            key = "3:latest";

            if (operation.has("version")) {
                key = String.format("3:v:%s", operation.get("version").asText());
            }
        } else {
            return null;
        }

        String dest = null;

        if (operation.has("dest")) {
            dest = operation.get("dest").asText();
        } else if (operation.has("origin")) {
            dest = operation.get("origin").asText();
        } else {
            return null;
        }
        
        byte[] prefix = new byte[0];
        
        if(type.equals(GET_NYM)){
            prefix = Utils.hash256(dest.getBytes());
        } else if (type.equals(GET_REVOC_REG) 
                || type.equals(GET_REVOC_REG_DELTA)
                || type.equals(GET_TXN_AUTHR_AGRMT)
                || type.equals(GET_TXN_AUTHR_AGRMT_AML)
                || type.equals(GET_AUTH_RULE)){
            
        } else if(type.equals(GET_REVOC_REG_DEF)){

            if(operation.has("id")){
                prefix =  operation.get("id").asText().getBytes();
            }
            
        } else {
            prefix = dest.getBytes();
        }

        byte[] suffix = key.getBytes();
        byte[] sp_key = new byte[prefix.length + suffix.length];
        System.arraycopy(prefix, 0, sp_key, 0, prefix.length);
        System.arraycopy(suffix, 0, sp_key, prefix.length, suffix.length);
        return sp_key;
    }
    
    public static boolean verify_parsed_sp(List<StateProof> parsed_sps, Map<String, VerKey> nodes, int threshold, Generator generator){
        
        for(StateProof parsed_sp : parsed_sps){
            if(!parsed_sp.multi_signature.get("value").get("state_root_hash").asText().equals(parsed_sp.root_hash)){
                return false;
            }
        }
        
        return false;
    }
    
    public static void parse_reply_for_proof_signature_checking(ObjectNode message){
        String signature = message.get("signature").asText();
        ArrayNode array = (ArrayNode) message.get("participants");
        
        List<String> participants = new ArrayList<>();
        for(JsonNode node : array){
            participants.add(node.asText());
        }
       
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        try {
            // read json
            ObjectMapper mapper = new ObjectMapper(new JsonFactory());
            JsonNode node = mapper.readTree(message.get("value").toString());
            // write message pack
            MessagePackFactory factory = new MessagePackFactory();
            JsonGenerator generator = factory.createGenerator(bos);
            mapper.writeTree(generator, node);
        } catch (IOException e) {
            LOG.error("Exception converting message pack to JSON", e);
        }
        
        byte[] value = bos.toByteArray();
    }
    
    public static boolean verify_proof_signature(String signature, List<String> participants, byte[] value, Map<String, VerKey> nodes, int threshold, Generator generator){
        
        List<VerKey> ver_keys = new ArrayList<>();
        
        for(String node : nodes.keySet()){
            if(participants.contains(node));
            ver_keys.add(nodes.get(node));
        }
        
        if(ver_keys.size() < nodes.size() - threshold){
            return false;
        }
        
        try {
            MultiSignature multi_signature = new MultiSignature().fromBytes(Base58.decode(signature));
            return BLS.verifyMultiSignature(multi_signature, value, ver_keys.toArray(new VerKey[ver_keys.size()]), generator);
        } catch (CryptoException e) {
            return false;
        }
    }
}
