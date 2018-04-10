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

import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.common.io.BaseEncoding;
import com.kumuluz.ee.configuration.utils.ConfigurationUtil;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.logging.Logger;

/***
 * JWTContext class
 *
 * @author Primoz Hrovat
 * @since 1.0.0
 */
public class JWTContext {

    private String publicKey;
    private RSAPublicKey decodedPublicKey;

    private String issuer;

    private DecodedJWT token;

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

        return instance;
    }

    private void setDecodedPublicKey() {
        try {
            byte[] bytes = BaseEncoding.base64().decode(instance.publicKey);

            X509EncodedKeySpec encodedKeySpec = new X509EncodedKeySpec(bytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            instance.decodedPublicKey = (RSAPublicKey) kf.generatePublic(encodedKeySpec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            //
        }
    }

    private void setIssuer() {
        confUtil.get("kumuluzee.grpc.server.auth.issuer").ifPresent(s -> this.issuer = s);
    }

    private void setPublicKey() {
        confUtil.get("kumuluzee.grpc.server.auth.public-key").ifPresent(s -> this.publicKey = s);
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
}
