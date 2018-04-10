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
import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptors;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyServerBuilder;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;

import javax.net.ssl.SSLException;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/***
 * GrpcServer class
 * Creates and manages grpc server.
 *
 * @author Primoz Hrovat
 * @since 1.0.0
 */
public class GrpcServer {

    private static Logger logger = Logger.getLogger(GrpcServer.class.getName());

    private GrpcServerConf conf;
    private Server server;

    private static GrpcServer instance;

    private GrpcServer() {
    }

    public static GrpcServer getInstance() {
        return instance;
    }

    public static void createServer(GrpcServerConf conf) {
        if (instance != null) {
            return;
        }

        instance = new GrpcServer();
        instance.conf = conf;
        instance.initialize();
    }

    public void initialize() {
        ServerBuilder sb;

        SslContext sslContext = createContext(conf.getCertFile(), conf.getPrivateKeyFile(),
                conf.getChainFile(), conf.getMutualTLS());

        if (sslContext != null) {
            sb = NettyServerBuilder
                    .forPort(conf.getPort())
                    .sslContext(sslContext);
        } else {
            sb = NettyServerBuilder
                    .forPort(conf.getPort());
        }

        bindServices(sb, conf.getServices());

        sb.handshakeTimeout(conf.getTimeout(), TimeUnit.SECONDS);
        server = sb.build();
    }

    /**
     * Bind services annotated with @GrpcService
     */
    private void bindServices(ServerBuilder sb, Set<GrpcServiceDef> grpcServices) {
        for (GrpcServiceDef grpcService : grpcServices) {
            String serviceName = grpcService.getServiceName();
            try {
                Class<?> service = Class.forName(serviceName);
                Constructor<?> serviceConstructor = service.getConstructor();

                if (grpcService.hasInterceptors()) {

                    sb.addService(ServerInterceptors
                            .intercept((BindableService) serviceConstructor.newInstance(),
                                    grpcService.getServiceInterceptors()));
                } else {
                    sb.addService((BindableService) serviceConstructor.newInstance());
                }
            } catch (ClassNotFoundException c) {
                logger.log(Level.WARNING, "Service class not found {0}", serviceName);
            } catch (NoSuchMethodException n){
                logger.log(Level.WARNING, "Constructor for service class {0}", serviceName);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException inv) {
                logger.log(Level.WARNING, "Instantiation of class error {0}: {1}", new String[] {serviceName, inv.toString()});
            }
        }
    }

    private SslContext createContext(File certChainFile, File privateKeyFile, File caCertFile, ClientAuth clientAuth) {
        try {
            if (certChainFile != null && privateKeyFile != null &&
                    certChainFile.canRead() && privateKeyFile.canRead()) {
                SslContextBuilder builder = GrpcSslContexts.forServer(certChainFile, privateKeyFile);
                if (caCertFile != null && clientAuth != null) {
                    builder.trustManager(caCertFile).clientAuth(clientAuth);
                }
                return builder.sslProvider(SslProvider.OPENSSL)
                    .build();
            }

            return null;
        } catch (SSLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void start() throws IOException {
        server.start();

        logger.info("gRPC server started, listening on " + server.getPort());
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            GrpcServer.this.stop();
            System.err.println("gRPC server shut down");
        }));
    }

    public void stop() {
        if (server != null) {
            server.shutdown();
        }
    }

    public void waitForShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    public int getPort() {
        return server.getPort();
    }
}
