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

import com.auth0.jwk.*;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import java.security.interfaces.RSAPublicKey;
import java.util.logging.Logger;


/***
 * JWTAuthorization class
 *
 * @author Primoz Hrovat
 * @since 1.0.0
 */
public class JWTAuthorization {
    private static final Logger logger = Logger.getLogger(JWTAuthorization.class.getName());

    /**
     * Validates JWT token.
     *
     * @param token            JWT token
     * @param context          JWT context
     * @throws JWTVerificationException if token is not valid
     */
    public static void validateToken(String token, JWTContext context) throws JWTVerificationException {
        try {
            Algorithm algorithm = null;
            DecodedJWT jwt = JWT.decode(token);
            if (context.getJwkProvider() != null) {
                // Jwks URI was provided OR
                // the provided public key is in JWK/JWKS format
                algorithm = getJwksAlgorithm(jwt, context);
            } else if(context.getDecodedPublicKey() != null) {
                // Public key was provided in PEM format
                algorithm = Algorithm.RSA256(context.getDecodedPublicKey(), null);
            }

            if (algorithm != null) {
                JWTVerifier verifier = JWT.require(algorithm)
                    .withIssuer(context.getIssuer())
                    .acceptLeeway(context.getMaximumLeeway())
                    .build();

                try{
                    verifier.verify(token);
                } catch (JWTVerificationException e) {
                    logger.severe("Exception: " + e.getMessage());
                    throw e;
                }

            } else {
                logger.severe("Neither kumuluzee.grpc.server.auth.jwks-uri nor kumuluzee.grpc.server.auth.public-key were configured.");
                throw new IllegalStateException("Neither kumuluzee.grpc.server.auth.jwks-uri nor kumuluzee.grpc.server.auth.public-key were configured.");
            }

            context.setToken(jwt);

        } catch (JwkException e) {
            logger.severe("Exception: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * Get JWK algorithm.
     *
     * @param jwt              JWT token
     * @param context          JWT context
     * @return Algorithm
     * @throws JwkException if JWK is not found
     */
    public static Algorithm getJwksAlgorithm(DecodedJWT jwt, JWTContext context) throws JwkException {
        try {
            Jwk jwk = context.getJwkProvider().get(jwt.getKeyId());
            return Algorithm.RSA256((RSAPublicKey) jwk.getPublicKey(), null);
        } catch (Exception e) {
            logger.severe("Exception: " + e.getMessage());
            throw e;
        }
    }
}
