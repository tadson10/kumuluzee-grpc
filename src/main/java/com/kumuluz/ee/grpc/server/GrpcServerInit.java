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

import com.kumuluz.ee.configuration.utils.ConfigurationUtil;
import com.kumuluz.ee.grpc.annotations.GrpcInterceptor;
import com.kumuluz.ee.grpc.annotations.GrpcService;
import com.kumuluz.ee.grpc.server.auth.JWTServerInterceptor;
import com.kumuluz.ee.grpc.utils.GrpcServiceDef;
import io.grpc.BindableService;
import io.grpc.ServerInterceptor;
import io.grpc.netty.shaded.io.netty.handler.ssl.ClientAuth;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/***
 * GrpcServerInit class
 * Parses configuration file and creates {@link GrpcServer}
 *
 * @author Primoz Hrovat
 * @since 1.0.0
 */
public class GrpcServerInit {

    private final static Logger logger = Logger.getLogger(GrpcServerInit.class.getName());

    private final static Integer GRPC_DEFAULT_PORT = 8443;
    private final static Long GRPC_DEFAULT_TIMEOUT = 120L;

    private boolean withCallCredentials;

    public void initialize() {
        logger.info("gRPC Server initialization");

        ConfigurationUtil confUtil = ConfigurationUtil.getInstance();
        GrpcServerConf grpcServerConf;

        File chainFile;
        File keyFile;
        File caFile;

        int port = setPort(confUtil);
        long timeout = setTimeout(confUtil);
        long permitKeepAliveTime = confUtil.getLong("kumuluzee.grpc.server.conf.permitKeepAliveTime").orElse(0L);
        long permitKeepAliveWithoutCalls = confUtil.getLong("kumuluzee.grpc.server.conf.permitKeepAliveWithoutCalls").orElse(0L);
        long keepAliveTimeout = confUtil.getLong("kumuluzee.grpc.server.conf.keepAliveTimeout").orElse(0L);
        long keepAliveTime = confUtil.getLong("kumuluzee.grpc.server.conf.keepAliveTime").orElse(0L);
        long maxConnectionIdle = confUtil.getLong("kumuluzee.grpc.server.conf.maxConnectionIdle").orElse(0L);
        long maxConnectionAge = confUtil.getLong("kumuluzee.grpc.server.conf.maxConnectionAge").orElse(0L);
        long maxConnectionAgeGrace = confUtil.getLong("kumuluzee.grpc.server.conf.maxConnectionAgeGrace").orElse(0L);

        if (confUtil.getBoolean("kumuluzee.grpc.server.https.enable").orElse(false)) {
            Optional<String> certChainFile = confUtil.get("kumuluzee.grpc.server.https.certFile");
            Optional<String> privateKeyFile = confUtil.get("kumuluzee.grpc.server.https.keyFile");
            Optional<String> caCertFile = confUtil.get("kumuluzee.grpc.server.https.chainFile");
            chainFile = openFile(certChainFile);
            keyFile = openFile(privateKeyFile);
            caFile = openFile(caCertFile);
            grpcServerConf = new GrpcServerConf(port, true, chainFile, keyFile, caFile,
                setClientAuth(confUtil), timeout, permitKeepAliveTime, permitKeepAliveWithoutCalls, keepAliveTimeout,
                keepAliveTime, maxConnectionIdle, maxConnectionAge, maxConnectionAgeGrace);
        } else {
            grpcServerConf = new GrpcServerConf(port, timeout, permitKeepAliveTime, permitKeepAliveWithoutCalls,
                keepAliveTimeout, keepAliveTime, maxConnectionIdle, maxConnectionAge, maxConnectionAgeGrace);
        }

        Set<GrpcServiceDef> services = new HashSet<>();

        ServiceLoader.load(BindableService.class).forEach(service ->
            services.add(loadInterceptorsForService(service)));

        grpcServerConf.setServices(services);
        GrpcServer.createServer(grpcServerConf);

        GrpcServer grpcServer = GrpcServer.getInstance();
        try {
            grpcServer.start();
        } catch (IOException e) {
            logger.log(Level.WARNING, "Error instantiating gRPC server");
        }

        logger.info("End of gRPC server initialization");
    }

    private int setPort(ConfigurationUtil confUtil) {
        Integer httpPort = confUtil.getInteger("kumuluzee.grpc.server.http.port").orElse(0);
        Integer httpsPort = confUtil.getInteger("kumuluzee.grpc.server.https.port").orElse(0);

        if (confUtil.getBoolean("kumuluzee.grpc.server.https.enable").orElse(false)) {
            return httpsPort;
        } else {
            if (httpPort.equals(0)) {
                return GRPC_DEFAULT_PORT;
            } else {
                return httpPort;
            }
        }
    }

    private File openFile(Optional<String> fileName) {
        try {
            String fName = fileName.get();
            File file = new File(fName);
            return file;
        } catch (NoSuchElementException | NullPointerException e) {
//            logger.info("Couldn't find provided file");
            return null;
        }
    }

    private Long setTimeout(ConfigurationUtil confUtil) {
        return confUtil.getLong("kumuluzee.grpc.server.timeout").orElse(GRPC_DEFAULT_TIMEOUT);
    }

    private GrpcServiceDef loadInterceptorsForService(BindableService service) {
        String serviceName = service.getClass().getName();
        logger.info("Searching interceptors for service " + service.toString());
        List<ServerInterceptor> serviceInterceptors = new ArrayList<>();

        GrpcInterceptor[] interceptors;
        interceptors = service.getClass().getAnnotation(GrpcService.class).interceptors();

        /* secure service with JWT token if specified */
        if (service.getClass().getAnnotation(GrpcService.class).secured()) {
            logger.info("Securing service " + serviceName);
            serviceInterceptors.add(new JWTServerInterceptor());
        }

        for (GrpcInterceptor interceptor : interceptors) {
            try {
                serviceInterceptors.add((ServerInterceptor) Class.forName(interceptor.name()).newInstance());
                logger.info("Initialized interceptor " + interceptor.name() + " for service " + service);

            } catch (ClassNotFoundException e) {
                logger.warning("gRPC interceptor with provided name doesn't exists. " + e.getMessage());
            } catch (IllegalAccessException e) {
                logger.warning("Illegal Access exception: " + e.getMessage());
            } catch (InstantiationException e) {
                logger.warning("Couldn't instantiate interceptor.");
            }
        }
        return new GrpcServiceDef()
                .setServiceName(serviceName)
                .setServiceInterceptors(serviceInterceptors);
    }

    private ClientAuth setClientAuth(ConfigurationUtil confUtil) {
        Optional<String> mutualTLS = confUtil.get("kumuluzee.grpc.server.https.mutualTLS");

        if (mutualTLS.isPresent()) {
            switch (mutualTLS.get().toLowerCase()) {
                case "optional":
                    return ClientAuth.OPTIONAL;
                case "require":
                    return ClientAuth.REQUIRE;
                default:
                    return ClientAuth.NONE;
            }
        }
        return null;
    }
}
