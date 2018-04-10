/*
 *  Copyright (c) 2014-2018 Kumuluz and/or its affiliates
 *  and other contributors as indicated by the @author tags and
 *  the contributor list.
 *
 *  Licensed under the MIT License (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  https://opensource.org/licenses/MIT
 *
 *  The software is provided "AS IS", WITHOUT WARRANTY OF ANY KIND, express or
 *  implied, including but not limited to the warranties of merchantability,
 *  fitness for a particular purpose and noninfringement. in no event shall the
 *  authors or copyright holders be liable for any claim, damages or other
 *  liability, whether in an action of contract, tort or otherwise, arising from,
 *  out of or in connection with the software or the use or other dealings in the
 *  software. See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.kumuluz.ee.grpc.server;

import com.kumuluz.ee.grpc.utils.GrpcServiceDef;
import io.grpc.Server;
import io.netty.handler.ssl.ClientAuth;

import java.io.File;
import java.util.Set;

/***
 * GrpcServerConf class
 * Configuration for {@link GrpcServer}.
 *
 * @author Primoz Hrovat
 * @since 1.0.0
 */
public class GrpcServerConf {

    private int port;

    private Set<GrpcServiceDef> services;

    private File certFile;
    private File privateKeyFile;
    private File chainFile;
    private ClientAuth mutualTLS;

    private Long timeout;

    public GrpcServerConf(int port, File certFile,
                          File privateKeyFile, File chainFile, ClientAuth mutualTLS, Long timeout) {
        this.port = port;
        this.certFile = certFile;
        this.privateKeyFile = privateKeyFile;
        this.chainFile = chainFile;
        this.mutualTLS = mutualTLS;
        this.timeout = timeout;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public Set<GrpcServiceDef> getServices() {
        return services;
    }

    public void setServices(Set<GrpcServiceDef> services) {
        this.services = services;
    }

    public File getCertFile() {
        return certFile;
    }

    public void setCertFile(File certFile) {
        this.certFile = certFile;
    }

    public File getPrivateKeyFile() {
        return privateKeyFile;
    }

    public void setPrivateKeyFile(File privateKeyFile) {
        this.privateKeyFile = privateKeyFile;
    }

    public File getChainFile() {
        return chainFile;
    }

    public void setChainFile(File chainFile) {
        this.chainFile = chainFile;
    }

    public ClientAuth getMutualTLS() {
        return mutualTLS;
    }

    public void setMutualTLS(ClientAuth mutualTLS) {
        this.mutualTLS = mutualTLS;
    }

    public Long getTimeout() {
        return timeout;
    }

    public void setTimeout(Long timeout) {
        this.timeout = timeout;
    }
}
