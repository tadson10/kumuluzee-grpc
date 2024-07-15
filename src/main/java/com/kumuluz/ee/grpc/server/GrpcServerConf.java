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
import io.grpc.netty.shaded.io.netty.handler.ssl.ClientAuth;

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

    private boolean httpsEnabled;
    private File certFile;
    private File privateKeyFile;
    private File chainFile;
    private ClientAuth mutualTLS;

    private Long timeout;
    private Long permitKeepAliveTime;
    private boolean permitKeepAliveWithoutCalls;
    private Long keepAliveTimeout;
    private Long keepAliveTime;
    private Long maxConnectionIdle;
    private Long maxConnectionAge;
    private Long maxConnectionAgeGrace;

    public GrpcServerConf(int port, Long timeout, Long permitKeepAliveTime, boolean permitKeepAliveWithoutCalls,
                          Long keepAliveTimeout, Long keepAliveTime, Long maxConnectionIdle, Long maxConnectionAge, Long maxConnectionAgeGrace) {
        this(port, false, null, null, null, null, timeout,
            permitKeepAliveTime, permitKeepAliveWithoutCalls, keepAliveTimeout, keepAliveTime, maxConnectionIdle, maxConnectionAge, maxConnectionAgeGrace);
    }

    public GrpcServerConf(int port, boolean httpsEnabled, File certFile,
                          File privateKeyFile, File chainFile, ClientAuth mutualTLS, Long timeout, Long permitKeepAliveTime,
                          boolean permitKeepAliveWithoutCalls, Long keepAliveTimeout, Long keepAliveTime, Long maxConnectionIdle, Long maxConnectionAge, Long maxConnectionAgeGrace) {
        this.port = port;
        this.httpsEnabled = httpsEnabled;
        this.certFile = certFile;
        this.privateKeyFile = privateKeyFile;
        this.chainFile = chainFile;
        this.mutualTLS = mutualTLS;
        this.timeout = timeout;
        this.permitKeepAliveTime = permitKeepAliveTime;
        this.permitKeepAliveWithoutCalls = permitKeepAliveWithoutCalls;
        this.keepAliveTimeout = keepAliveTimeout;
        this.keepAliveTime = keepAliveTime;
        this.maxConnectionIdle = maxConnectionIdle;
        this.maxConnectionAge = maxConnectionAge;
        this.maxConnectionAgeGrace = maxConnectionAgeGrace;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public boolean isHttpsEnabled() {
        return httpsEnabled;
    }

    public void setHttpsEnabled(boolean httpsEnabled) {
        this.httpsEnabled = httpsEnabled;
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

    public Long getPermitKeepAliveTime() {
        return permitKeepAliveTime;
    }

    public void setPermitKeepAliveTime(Long permitKeepAliveTime) {
        this.permitKeepAliveTime = permitKeepAliveTime;
    }

    public boolean getPermitKeepAliveWithoutCalls() {
        return permitKeepAliveWithoutCalls;
    }

    public void setPermitKeepAliveWithoutCalls(boolean permitKeepAliveWithoutCalls) {
        this.permitKeepAliveWithoutCalls = permitKeepAliveWithoutCalls;
    }

    public Long getKeepAliveTimeout() {
        return keepAliveTimeout;
    }

    public void setKeepAliveTimeout(Long keepAliveTimeout) {
        this.keepAliveTimeout = keepAliveTimeout;
    }

    public Long getKeepAliveTime() {
        return keepAliveTime;
    }

    public void setKeepAliveTime(Long keepAliveTime) {
        this.keepAliveTime = keepAliveTime;
    }

    public Long getMaxConnectionIdle() {
        return maxConnectionIdle;
    }

    public void setMaxConnectionIdle(Long maxConnectionIdle) {
        this.maxConnectionIdle = maxConnectionIdle;
    }

    public Long getMaxConnectionAge() {
        return maxConnectionAge;
    }

    public void setMaxConnectionAge(Long maxConnectionAge) {
        this.maxConnectionAge = maxConnectionAge;
    }

    public Long getMaxConnectionAgeGrace() {
        return maxConnectionAgeGrace;
    }

    public void setMaxConnectionAgeGrace(Long maxConnectionAgeGrace) {
        this.maxConnectionAgeGrace = maxConnectionAgeGrace;
    }
}
