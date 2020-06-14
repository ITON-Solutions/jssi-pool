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
package org.iton.jssi.pool.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

/**
 *
 * @author ITON Solutions
 */
public class NodeTransaction {
    @JsonProperty("ver")
    String ver;
    @JsonProperty("txn")
    Txn txn;
    @JsonProperty("txnMetadata") 
    TxnMetadata txnMetadata;
    @JsonProperty("reqSignature") 
    ReqSignature reqSignature;

    public String getVer() {
        return ver;
    }

    public void setVer(String ver) {
        this.ver = ver;
    }

    public Txn getTxn() {
        return txn;
    }

    public void setTxn(Txn txn) {
        this.txn = txn;
    }

    public TxnMetadata getTxnMetadata() {
        return txnMetadata;
    }

    public void setTxnMetadata(TxnMetadata txnMetadata) {
        this.txnMetadata = txnMetadata;
    }

    public ReqSignature getReqSignature() {
        return reqSignature;
    }

    public void setReqSignature(ReqSignature reqSignature) {
        this.reqSignature = reqSignature;
    }

    
    
    
}
