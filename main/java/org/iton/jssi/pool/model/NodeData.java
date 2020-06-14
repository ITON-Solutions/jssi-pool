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
import java.util.List;

/**
 *
 * @author ITON Solutions
 */
public class NodeData {
    @JsonProperty("alias")
    private String alias;
    @JsonProperty("client_ip")
    private String client_ip;
    @JsonProperty("client_port")
    private int client_port;
    @JsonProperty("node_ip")
    private String node_ip;
    @JsonProperty("node_port")
    private int node_port;
    @JsonProperty("services")
    private List<String> services;
    @JsonProperty("blskey")
    private String blskey;
    @JsonProperty("blskey_pop")
    private String blskey_pop;

    public String getAlias() {
        return alias;
    }

    public String getClientIP() {
        return client_ip;
    }

    public int getClientPort() {
        return client_port;
    }

    public String getNodeIP() {
        return node_ip;
    }

    public int getNodePort() {
        return node_port;
    }

    public List<String> getServices() {
        return services;
    }

    public String getBlskey() {
        return blskey;
    }

    public String getBlskeyPop() {
        return blskey_pop;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public void setClientIP(String client_ip) {
        this.client_ip = client_ip;
    }

    public void setClientPort(int client_port) {
        this.client_port = client_port;
    }

    public void setNodeIP(String node_ip) {
        this.node_ip = node_ip;
    }

    public void setNodePort(int node_port) {
        this.node_port = node_port;
    }

    public void setServices(List<String> services) {
        this.services = services;
    }

    public void setBlskey(String blskey) {
        this.blskey = blskey;
    }

    public void setBlskeyPop(String blskey_pop) {
        this.blskey_pop = blskey_pop;
    }
    
}
