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
package com.kumuluz.ee.grpc.client;

/***
 * GrpcChannelConfig helper class. Parses configuration file. Serves as data holder for {@link GrpcClient}.
 *
 * @author Primoz Hrovat
 * @since 1.0.0
 */
public class GrpcChannelConfig {

    private String name;
    private String address;
    private int port;
    private String certFile;
    private String keyFile;
    private String trustManager;

    private static final Integer GRPC_DEFAULT_SERVER_PORT = 8443;

    public static class Builder {
        private String name;
        private String address;
        private int port;
        private String certFile;
        private String keyFile;
        private String trustManager;

        public void name(String name) {
            this.name = name;
        }

        public void address(String address) {
            this.address = address;
        }

        public void port(Integer port) {
            this.port = port;
        }

        public void certFile(String certFile) {
            this.certFile = certFile;
        }

        public void keyFile(String keyFile) {
            this.keyFile = keyFile;
        }

        public void trustManager(String trustManager) {
            this.trustManager = trustManager;
        }

        public GrpcChannelConfig build() {
            GrpcChannelConfig config = new GrpcChannelConfig();

            config.name = name;
            config.address = address;
            config.port = port;
            config.certFile = certFile;
            config.keyFile = keyFile;
            config.trustManager = trustManager;

            return config;
        }
    }

    private GrpcChannelConfig() {
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    public String getCertFile() {
        return certFile;
    }

    public String getKeyFile() {
        return keyFile;
    }

    public String getTrustManager() {
        return trustManager;
    }
}
