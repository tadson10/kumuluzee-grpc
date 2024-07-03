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

import com.kumuluz.ee.grpc.annotations.GrpcService;
import com.kumuluz.ee.grpc.server.auth.JWTContext;
import com.kumuluz.ee.grpc.utils.GrpcServiceDef;
import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptors;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.ClientAuth;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContextBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslProvider;
import io.grpc.protobuf.services.HealthStatusManager;
import com.kumuluz.ee.configuration.utils.ConfigurationUtil;

import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.net.ssl.SSLException;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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
    private HealthStatusManager healthStatusManager;
    private Map<String, Map<String, Method>> serviceMethods = new HashMap<>();
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
        instance.healthStatusManager = new HealthStatusManager();
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

        if (!conf.getPermitKeepAliveTime().equals(0L)) {
            sb.permitKeepAliveTime(conf.getPermitKeepAliveTime(), TimeUnit.MILLISECONDS);
        }

        if (!conf.getPermitKeepAliveWithoutCalls().equals(0L)) {
            sb.permitKeepAliveWithoutCalls(true);
        }

        if (!conf.getKeepAliveTimeout().equals(0L)) {
            sb.keepAliveTimeout(conf.getKeepAliveTimeout(), TimeUnit.MILLISECONDS);
        }

        if (!conf.getKeepAliveTime().equals(0L)) {
            sb.keepAliveTime(conf.getKeepAliveTime(), TimeUnit.MILLISECONDS);
        }

        if (!conf.getMaxConnectionIdle().equals(0L)) {
            sb.maxConnectionIdle(conf.getMaxConnectionIdle(), TimeUnit.MILLISECONDS);
        }

        if (!conf.getMaxConnectionAge().equals(0L)) {
            sb.maxConnectionAge(conf.getMaxConnectionAge(), TimeUnit.MILLISECONDS);
        }

        if (!conf.getMaxConnectionAgeGrace().equals(0L)) {
            sb.maxConnectionAgeGrace(conf.getMaxConnectionAgeGrace(), TimeUnit.MILLISECONDS);
        }


        bindServices(sb, conf.getServices());

        sb.handshakeTimeout(conf.getTimeout(), TimeUnit.SECONDS);
        server = sb.build();

        checkSecurityAnnotations();
    }

    /**
     * Check if security annotations are required and present on service implementation class
     * Each method should have at least one security annotation present (DenyAll, PermitAll, RolesAllowed)
     * If not, throw an exception
     *
     */
    private void checkSecurityAnnotations() {
        // List of all methods for each service
        Map<String, Map<String, Method>> methods = new HashMap<>();
        conf.getServices().forEach(service -> {
            logger.info("Checking security annotations for service: " + service.getServiceName());
            String serviceClassName = service.getServiceName();
            try {
                // Find service implementation class
                Class<?> serviceImplClass = Class.forName(serviceClassName);
                // Name of service as defined in proto file
                final String protoServiceName = serviceImplClass.getSuperclass().getEnclosingClass().getField("SERVICE_NAME").get(null).toString();
                boolean isServiceSecured = serviceImplClass.getAnnotation(GrpcService.class).secured();

                // If service has secured=true, check if methods have security annotations
                if (isServiceSecured) {
                    server.getServices().stream()
                        .filter(s -> s.getServiceDescriptor().getName().equals(protoServiceName))
                        .findFirst()
                        .ifPresent(protoService -> {
                            logger.info("Proto service: " + serviceClassName);
                            methods.put(protoServiceName, new HashMap<>());

                            // transform protoService.getMethods() to list of method names
                            List<String> methodNames = protoService.getMethods().stream()
                                .map(m -> m.getMethodDescriptor().getFullMethodName().split("/")[1])
                                .collect(Collectors.toList());

                            Map<String, Method> serviceMethods = new HashMap<>();
                            // Check if security annotations are present on methods of service implementation class
                            // Each method should have at least one security annotation present (DenyAll, PermitAll, RolesAllowed)
                            // If not, throw an exception
                            for (Method method : serviceImplClass.getDeclaredMethods()) {
                                logger.info("Checking method: " + method.getName());
                                // Check if method belongs to service implementation class - based on proto file
                                if (methodNames.contains(method.getName())) {
                                    serviceMethods.put(method.getName(), method);
                                    methods.put(protoServiceName, serviceMethods);
                                    if (!method.isAnnotationPresent(DenyAll.class) && !method.isAnnotationPresent(PermitAll.class) && !method.isAnnotationPresent(RolesAllowed.class)) {
                                        throw new IllegalStateException("No security annotations (DenyAll, PermitAll, RolesAllowed) found on service implementation class. Service: " + serviceClassName + ", method: " + method.getName());
                                    }
                                }
                            }
                        });
                }
            } catch (IllegalAccessException | NoSuchFieldException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        });

        // Save service methods to context
        JWTContext.getInstance().setMethods(methods);
        this.serviceMethods = methods;
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

        // Health check service
        ConfigurationUtil confUtil = ConfigurationUtil.getInstance();
        boolean healthCheckEnabled = confUtil.getBoolean("kumuluzee.grpc.server.health.healthCheckEnabled").orElse(false);

        if (healthCheckEnabled) {
            sb.addService(healthStatusManager.getHealthService());
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

    public HealthStatusManager getHealthStatusManager() {
        return healthStatusManager;
    }

    public Map<String, Map<String, Method>> getServiceMethods() {
        return serviceMethods;
    }
}
