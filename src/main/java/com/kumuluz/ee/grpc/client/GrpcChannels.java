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

import com.kumuluz.ee.configuration.utils.ConfigurationUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/***
 * GrpcChannels class. Holds different channels configurations.
 *
 * @author Primoz Hrovat
 * @since 1.0
 */
public class GrpcChannels {

    public static class Builder {

        public GrpcChannels build() {

            ConfigurationUtil confUtil = ConfigurationUtil.getInstance();
            Optional<Integer> numClients = confUtil.getListSize("kumuluzee.grpc.clients");

            instance = new GrpcChannels();

            if (numClients.isPresent()) {
                List<GrpcChannelConfig> clients = new ArrayList<>();

                for (int i = 0; i < numClients.get(); i++) {
                    GrpcChannelConfig.Builder gcc = new GrpcChannelConfig.Builder();

                    Optional<String> name = confUtil.get("kumuluzee.grpc.clients[" + i + "].name");
                    Optional<String> address = confUtil.get("kumuluzee.grpc.clients[" + i + "].address");
                    Optional<Integer> keepAlive = confUtil.getInteger("kumuluzee.grpc.clients[" + i + "].keepAlive");
                    Optional<Integer> keepAliveTimeout = confUtil.getInteger("kumuluzee.grpc.clients[" + i + "].keepAliveTimeout");
                    Optional<Boolean> keepAliveWithoutCalls = confUtil.getBoolean("kumuluzee.grpc.clients[" + i + "].keepAliveWithoutCalls");
                    Optional<Integer> port = confUtil.getInteger("kumuluzee.grpc.clients[" + i + "].port");
                    Optional<String> cert = confUtil.get("kumuluzee.grpc.clients[" + i + "].certFile");
                    Optional<String> key = confUtil.get("kumuluzee.grpc.clients[" + i + "].keyFile");
                    Optional<String> trust = confUtil.get("kumuluzee.grpc.clients[" + i + "].trustFile");

                    name.ifPresent(gcc::name);
                    address.ifPresent(gcc::address);
                    keepAlive.ifPresent(gcc::keepAlive);
                    keepAliveTimeout.ifPresent(gcc::keepAliveTimeout);
                    keepAliveWithoutCalls.ifPresent(gcc::keepAliveWithoutCalls);
                    port.ifPresent(gcc::port);
                    cert.ifPresent(gcc::certFile);
                    key.ifPresent(gcc::keyFile);
                    trust.ifPresent(gcc::trustManager);

                    clients.add(gcc.build());
                }
                instance.grpcChannelConfigs = clients;
            }

            return instance;
        }
    }

    private List<GrpcChannelConfig> grpcChannelConfigs;

    private static GrpcChannels instance;

    public static GrpcChannels getInstance() {
        return instance;
    }

    public List<GrpcChannelConfig> getGrpcChannelConfigs() {
        return grpcChannelConfigs;
    }

    public GrpcChannelConfig getGrpcClientConfig(String name) {
        for (GrpcChannelConfig config : grpcChannelConfigs) {
            if (config.getName().equals(name))
                return config;
        }

        return null;
    }
}
