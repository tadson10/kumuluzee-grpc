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

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NegotiationType;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContextBuilder;

import javax.net.ssl.SSLException;
import java.io.File;
import java.util.NoSuchElementException;
import java.util.logging.Logger;

/***
 * GrpcClient helper class. It provides channel creation using GrpcChannelConfig class.
 *
 * @author Primoz Hrovat
 * @since 1.0.0
 */
public class GrpcClient {

    private static final Logger logger = Logger.getLogger(GrpcClient.class.getName());

    private GrpcChannelConfig config;
    private ManagedChannel channel;

    public GrpcClient(GrpcChannelConfig config) throws SSLException {
        this.config = config;

        initialize();
    }

    private void initialize() throws SSLException {
        createChannel();
    }

    private SslContext buildTLSContext() throws SSLException {
        SslContextBuilder builder = GrpcSslContexts.forClient();

        if (config.getTrustManager() != null) {
            if (config.getCertFile() != null && config.getKeyFile()!= null) {
                builder.keyManager(new File(config.getCertFile()), new File(config.getKeyFile()));
            }
            builder.trustManager(new File(config.getTrustManager()));
            return builder.build();
        }
        else return null;
    }

    private void createChannel() {
        try {
            SslContext sslContext = buildTLSContext();
            if (sslContext != null) {
                channel = NettyChannelBuilder.forAddress(config.getAddress(), config.getPort())
                        .negotiationType(NegotiationType.TLS)
                        .sslContext(sslContext)
                        .build();
                return;
            }
        } catch (SSLException | NoSuchElementException e) {
            e.printStackTrace();
        }
        channel = ManagedChannelBuilder.forAddress(config.getAddress(), config.getPort())
                .usePlaintext()
                .build();
    }

    public ManagedChannel getChannel() {
        return channel;
    }
}
