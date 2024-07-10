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

import com.auth0.jwt.JWT;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.kumuluz.ee.grpc.server.GrpcServer;
import io.grpc.*;

import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
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
            JWTContext jwtContext = JWTContext.getInstance();

            try {
                String token = authorization.substring(7);
                try {
                    JWTAuthorization.validateToken(token, JWTContext.getInstance());
                } catch (JWTVerificationException e) {
                    serverCall.close(Status.UNAUTHENTICATED.withDescription("JWT token not valid."), metadata);
                    return NOOP_LISTENER;
                }

                try {
                    DecodedJWT jwt = JWT.decode(token);
                    // Check if security annotations are required and
                    // method is annotated with security annotations and if user has required roles in token
                    if (!checkClientRolesForMethod(serverCall.getMethodDescriptor(), jwt)) {
                        serverCall.close(Status.PERMISSION_DENIED.withDescription("Client has insufficient permissions."), metadata);
                        return NOOP_LISTENER;
                    }
                } catch (Exception e) {
                    logger.log(java.util.logging.Level.SEVERE, e.getMessage());
                    serverCall.close(Status.PERMISSION_DENIED.withDescription("Insufficient permissions."), metadata);
                    return NOOP_LISTENER;
                }
            } catch (Exception e) {
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

    /**
     * Check if method is annotated with security annotations and if user has required roles in token.
     *
     * @param methodDescriptor MethodDescriptor
     * @param jwt              DecodedJWT
     * @return boolean
     */
    private static boolean checkClientRolesForMethod(MethodDescriptor<?, ?> methodDescriptor, DecodedJWT jwt) {
        JWTContext context = JWTContext.getInstance();
        String fullMethodName = methodDescriptor.getFullMethodName();
        String serviceName = MethodDescriptor.extractFullServiceName(fullMethodName);
        String methodName = fullMethodName.substring(serviceName.length() + 1);

        String resourceName = context.getResourceNames().get(serviceName);
        Map<String, Claim> claims = jwt.getClaims();
        Map<String, Object> resourceAccess = null;
        Claim roles = null;
        if (claims != null) {
            if (claims.containsKey("resource_access")) {
                resourceAccess = claims.get("resource_access").asMap();
            }
            roles = claims.get("roles");
        }

        // If service doesn't have secure=true, then context.getMethods() will be empty for that service
        Map<String, Map<String, Method>> serviceMethods = GrpcServer.getInstance().getServiceMethods();
        if (!serviceMethods.containsKey(serviceName)) {
            // If there is no method map for the service, permit access
            // This means that security annotations are not required
            return true;
        } else {
            Map<String, Method> methodMap = serviceMethods.get(serviceName);
            if (methodMap.containsKey(methodName)) {
                Method method = methodMap.get(methodName);
                // If there is @DenyAll annotation on the method, deny access
                if (method.isAnnotationPresent(DenyAll.class)) {
                    return false;
                }
                // If there is @PermitAll annotation on the method, permit access
                if (method.isAnnotationPresent(PermitAll.class)) {
                    return true;
                }
                // If there is @RolesAllowed annotation on the method, check if user has required roles
                if (method.isAnnotationPresent(RolesAllowed.class)) {
                    List<?> resourceRoles = null;
                    if (resourceAccess != null) {
                        // Token has roles in resource_access
                        // This is Keycloak token
                        if (resourceAccess.containsKey(resourceName)) {
                            // resource_access contains the resource name from config.yml
                            Map<?, ?> resource = (Map<?, ?>) resourceAccess.get(resourceName);
                            resourceRoles = (List<?>) resource.get("roles");
                        }
                    }

                    for (String methodRole : method.getAnnotation(RolesAllowed.class).value()) {
                        // Token has roles in resource_access
                        if (resourceRoles != null && resourceRoles.contains(methodRole)) {
                            return true;
                        } else if (resourceRoles == null &&
                            roles != null &&
                            roles.asList(String.class).contains(methodRole)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}
