package com.kumuluz.ee.grpc.server.auth;

import com.auth0.jwk.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.util.*;

/**
 * Keycloak jwk provider that loads them from a {@link URL}
 * Copy of com.auth0.jwk.UrlJwkProvider, but without url well known domain suffix (well-known/jwks.json)
 */
@SuppressWarnings("WeakerAccess")
public class KumuluzJwkProvider implements JwkProvider {

  private Map<String, Jwk> jwkMap;

  @SuppressWarnings("unchecked")
  public KumuluzJwkProvider(String jwkPayload) throws SigningKeyNotFoundException {

    try {
      Map<String, Object> jwks;
      try {
        //try if base64
        byte[] decoded = java.util.Base64.getDecoder().decode(jwkPayload);
        jwks = new ObjectMapper().readValue(decoded, new TypeReference<Map<String, Object>>() {
        });
      } catch (IllegalArgumentException e) {
        jwks = new ObjectMapper().readValue(jwkPayload, new TypeReference<Map<String, Object>>() {
        });
      }

      this.jwkMap = new HashMap<>();

      List<Map<String, Object>> keys = (List) jwks.get("keys");
      if (keys != null && !keys.isEmpty()) {
        //multiple keys
        try {
          Iterator var3 = keys.iterator();

          while (var3.hasNext()) {
            Map<String, Object> values = (Map) var3.next();
            Jwk jwk = Jwk.fromValues(values);
            jwkMap.put(jwk.getId(), jwk);
          }
        } catch (IllegalArgumentException var5) {
          throw new SigningKeyNotFoundException("Failed to parse jwk from json", var5);
        }
      } else if (jwks.containsKey("n") && jwks.containsKey("e") && jwks.get("n") instanceof String && jwks.get("e") instanceof String) {
        //one key
        Jwk jwk = Jwk.fromValues(jwks);
        jwkMap.put(jwk.getId(), jwk);
      } else {
        throw new SigningKeyNotFoundException("No keys found in payload", null);
      }

    } catch (Exception e) {
      throw new SigningKeyNotFoundException("No keys found in payload", e);
    }
  }

  @Override
  public Jwk get(String keyId) throws JwkException {

    if (jwkMap.containsKey(keyId)) {
      return jwkMap.get(keyId);
    }

    throw new SigningKeyNotFoundException("No key found in Kumuluz JWK provider with kid " + keyId, null);
  }
}