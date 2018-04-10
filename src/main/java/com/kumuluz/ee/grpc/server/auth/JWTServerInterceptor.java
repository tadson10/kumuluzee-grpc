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
package com.kumuluz.ee.grpc.server.auth;

import com.auth0.jwt.exceptions.JWTVerificationException;
import io.grpc.*;

import java.util.logging.Logger;

/***
 * JWTServerInterceptor class.
 * Provides security using JWT tokens to grpc services with {@link com.kumuluz.ee.grpc.annotations.GrpcService} secured
 * parameter set to true.
 *
 * @author Primoz Hrovat
 * @since 1.0.0
 */
public class JWTServerInterceptor implements ServerInterceptor {

    private static final ServerCall.Listener NOOP_LISTENER = new ServerCall.Listener() {
    };

    private static final Logger logger = Logger.getLogger(JWTServerInterceptor.class.getName());

    private static final Metadata.Key<String> AUTHORIZATION_HEADER = Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER);

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> serverCall,
                                                                 Metadata metadata,
                                                                 ServerCallHandler<ReqT, RespT> serverCallHandler) {
        logger.info("Authenticating call with JWT token...");

        String authorization = metadata.get(AUTHORIZATION_HEADER);

        if (authorization == null) {
            serverCall.close(Status.UNAUTHENTICATED.withDescription("JWT Token is missing!"), metadata);
            return NOOP_LISTENER;
        }

        if (authorization.startsWith("Bearer")) {
            try {
                String token = authorization.substring(7);
                JWTAuthorization.validateToken(token, JWTContext.getInstance());
            } catch (JWTVerificationException e) {
                serverCall.close(Status.UNAUTHENTICATED.withDescription("JWT token not valid."), metadata);
                return NOOP_LISTENER;
            }
        }

        Context context;
        try {
            context = Context.current();
        } catch (Exception e) {
            e.printStackTrace();
            return NOOP_LISTENER;
        }

        return Contexts.interceptCall(context, serverCall, metadata, serverCallHandler);
    }
}
