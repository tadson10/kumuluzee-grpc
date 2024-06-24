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
import com.auth0.jwt.interfaces.DecodedJWT;
import com.kumuluz.ee.configuration.utils.ConfigurationUtil;
import org.apache.commons.codec.binary.Base64;

import java.lang.reflect.Method;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/***
 * JWTContext class
 *
 * @author Primoz Hrovat
 * @since 1.0.0
 */
public class JWTContext {
    private static final String DEFAULT_LEEWAY_MILLISECONDS = "0";
    private String publicKey;
    private RSAPublicKey decodedPublicKey;
    private String jwksUri;
    private String keycloakJwksUri;
    private JwkProvider jwkProvider;
    private Integer maximumLeeway;
    private String issuer;
    private DecodedJWT token;
    private String resourceName;
    private Map<String, Map<String, Method>> methods = new HashMap<>();

    private static JWTContext instance;
    private static final ConfigurationUtil confUtil = ConfigurationUtil.getInstance();
    private static final Logger logger = Logger.getLogger(JWTContext.class.getName());

    private JWTContext() {}

    public static JWTContext getInstance() {
        if (instance != null) {
            return instance;
        }

        instance = new JWTContext();
        instance.setIssuer();
        instance.setPublicKey();
        instance.setDecodedPublicKey();
        instance.setKeycloakJwksUri();
        instance.setJwksUri();
        instance.setJwkProvider();
        instance.setMaximumLeeway();
        instance.setResourceName();

        return instance;
    }

    private void setDecodedPublicKey() {
        try {
            if (instance.publicKey != null) {
                // Public key must be in PKCS#8 PEM format
                // If it is not, public key will be processed in setJwkProvider method

                // Remove BEGIN and END lines and new lines
                String key = instance.publicKey.replace("-----BEGIN PUBLIC KEY-----", "")
                    .replaceAll(System.lineSeparator(), "")
                    .replace("-----END PUBLIC KEY-----", "");

                byte[] bytes = Base64.decodeBase64(key);

                X509EncodedKeySpec encodedKeySpec = new X509EncodedKeySpec(bytes);
                KeyFactory kf = KeyFactory.getInstance("RSA");
                instance.decodedPublicKey = (RSAPublicKey) kf.generatePublic(encodedKeySpec);
            }
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | IllegalArgumentException e) {
            logger.warning("Problem decoding public key: " + e.getMessage());
        }
    }

    private void setIssuer() {
        confUtil.get("kumuluzee.grpc.server.auth.issuer").ifPresent(s -> this.issuer = s);
    }

    private void setPublicKey() {
        confUtil.get("kumuluzee.grpc.server.auth.public-key").ifPresent(s -> this.publicKey = s);
    }

    public void setJwksUri() {
        confUtil.get("kumuluzee.grpc.server.auth.jwks-uri").ifPresent(s -> this.jwksUri = s);
    }

    public void setKeycloakJwksUri() {
        confUtil.get("kumuluzee.grpc.server.auth.keycloak-jwks-uri").ifPresent(s -> this.keycloakJwksUri = s);
    }

    public void setResourceName() {
        confUtil.get("kumuluzee.grpc.server.auth.resource-name").ifPresent(s -> this.resourceName = s);
    }

    public void setJwkProvider() {
        if (instance.jwksUri != null) {
            // JWKS URL was provided
            instance.jwkProvider = new UrlJwkProvider(jwksUri);
        } else if (instance.keycloakJwksUri != null) {
            // Keycloak JWKS URL was provided
            instance.jwkProvider = new KeycloakUrlJwkProvider(keycloakJwksUri);
        } else if (instance.publicKey != null) {
            // JWKS URL was not provided, but public key was provided in JWK/JWKS format
            // We check if the provided public key is in JWK/JWKS format (Base64 or text)
            try {
                instance.jwkProvider = new KumuluzJwkProvider(instance.publicKey);
            } catch (SigningKeyNotFoundException e) {
                logger.severe("Exception: " + e.getMessage());
            }
        }
    }

    public void setMaximumLeeway() {
        instance.maximumLeeway = Integer.parseInt(confUtil.get("kumuluzee.grpc.server.auth.maximum-leeway").orElse(DEFAULT_LEEWAY_MILLISECONDS));
    }

    public String getIssuer() {
        if (instance != null) {
            return instance.issuer;
        }
        return null;
    }

    public String getPublicKey() {
        if (instance != null) {
            return instance.publicKey;
        }
        return null;
    }

    public RSAPublicKey getDecodedPublicKey() {
        if (instance != null) {
            return instance.decodedPublicKey;
        }
         return null;
    }

    protected void setToken(DecodedJWT decodedJWT) {
        this.token = decodedJWT;
    }

    public DecodedJWT getToken() {
        return token;
    }

    public String getJwksUri() {
        if (instance != null) {
            return instance.jwksUri;
        }
        return null;
    }

    public JwkProvider getJwkProvider() {
        if (instance != null) {
            return instance.jwkProvider;
        }
        return null;
    }

    public Integer getMaximumLeeway() {
        return maximumLeeway;
    }

    public String getKeycloakJwksUri() {
        return keycloakJwksUri;
    }

    public String getResourceName() {
        return resourceName;
    }

    public Map<String, Map<String, Method>> getMethods() {
        return methods;
    }

    public void setMethods(Map<String, Map<String, Method>> methods) {
        this.methods = methods;
    }
}
