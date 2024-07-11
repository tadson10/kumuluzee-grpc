# KumuluzEE gRPC
[![Build Status](https://img.shields.io/travis/kumuluz/kumuluzee-grpc/master.svg?style=flat)](https://travis-ci.org/kumuluz/kumuluzee-grpc)
> KumuluzEE gRPC project for enabling gRPC protocol for KumuluzEE microservices.

## Usage

You can enable the KumuluzEE gRPC extension by adding the following dependency:
```xml
<dependency>
    <groupId>com.kumuluz.ee.grpc</groupId>
    <artifactId>kumuluzee-grpc</artifactId>
    <version>${kumuluz-grpc.version}</version>
</dependency>
```

## Server
**@GrpcService annotation**

To enable gRPC communication gRPC services must be implemented and annotated with @GrpcService annotation.
Additionally for each service one or many interceptor(s) can be added. To bind interceptor to service
 **@GrpcInterceptor** annotation should be used as @GrpcService "interceptors" parameter e.g.
```java
@GrpcService(interceptors = {
        @GrpcInterceptor(name = "full.class.path.name"),
        @GrpcInterceptor(name = "full.class.path.name2",
        secured = true,
        resourceName = "user-grpc")
})
public class GrpcServiceImpl extends ServiceGrpc.ServiceGrpcBaseImpl {
    // service implementation
}
```

Each service can be secured using JWT tokens. To enable call security, `security` parameter of @GrpcService annotation must be set to
**true** (**false** by default). If we are using secured service with Keycloak generated JWT, we need to provide `resourceName`.
Keycloak will add client roles in `resource_access` for each resource separately. Public key and issuer must be provided in configuration file as follows:

```yaml
grpc:
  server:
    auth:
      public-key: MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAnOTgnGBISzm3pKuG8QXMVm6eEuTZx8Wqc8D9gy7vArzyE5QC/bVJNFwlz...
      issuer: http://example.org/auth
```

When implementing secured service, we need to add `@RolesAllowed`, `@PermitAll` or `@DenyAll` annotations
to each service method.

More about gRPC protocol can be found on [webpage](https://grpc.io).
gRPC implementation, tutorials and samples can be found [here](https://github.com/grpc/grpc-java).

## Client
Helper class is provided to simplify client configuration. GrpcClient class provides
seamless channel creation. Client configuration is done with GrpcClientConf which automatically 
reads data from configuration file. To override port and address, overloaded constructor can be used.

*default configuration*
```java
try {
    GrpcClientConf clientConf = new GrpcClientConf("localhost", 8080);
    GrpcClient client = new GrpcClient(clientConf);
    stub = UserGrpc.newStub(client.getChannel());
} catch (ConfigurationException | SSLException e) {
    logger.warning(e.getMessage());
}
```

When calling services secured with JWT tokens, token must be provided and applied using JWTCallCredentials class.

```java
stub = UserGrpc.newStub(client.getChannel()).withCallCredentials(new JWTCallCredentials(JWT_TOKEN));
```

## gRPC configuration
Specific server configuration such as port number, 
server address, tls options... can be configured in config source (config.yml).
When using GrpcClientConf helper class, configuration for client must also be provided.

Example:
```yaml
kumuluzee:
  name: gRPC sample
  grpc:
    server:
      base-url: http://localhost:8080
      http:
        port: 8081
      https:
        enable: true
        port: 8443
        certFile: /path/to/cert/file
        keyFile: /path/to/key/file
        chainFile: /path/to/chain/file
        mutualTLS: optional
      conf:
        permitKeepAliveTime: 60000
        permitKeepAliveWithoutCalls: 10000
        keepAliveTimeout: 60000
        keepAliveTime: 60000
        maxConnectionIdle: 10000
        maxConnectionAge: 20000
        maxConnectionAgeGrace: 10000
      health:
        healthCheckEnabled: true        
      auth:
        maximum-leeway: 100
        public-key: MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAnOTgnGBISzm3pKuG8QXMVm6eEuTZx8Wqc8D9gy7vArzyE5QC/bVJNFwlz...
        issuer: http://example.org/auth
    clients:
    - name: client1
      port: 8081
      address: localhost
      keepAlive: 1000 
      keepAliveTimeout: 1000
      certFile: /path/to/cert/file
      keyFile: /path/to/key/file
      chainFile: /path/to/chain/file
```
The supplied public key can be in any of the following formats:
- PKCS#8 PEM
- JWK
- JWKS
- Base64 URL encoded JWK
- Base64 URL encoded JWKS

We can also use JWKS server to get public keys for JWT verification. In this case we need to provide JWKS server URL instead of public key.
```yaml
grpc:
    server:
        auth:
            jwks-uri: http://localhost:8080/jwks
            issuer: http://localhost
```

We can use even Keycloak server to get public keys for JWT verification. In this case we need to provide Keycloak server URL instead of public key.
```yaml
grpc:
    server:
        auth:
            keycloak-jwks-uri: http://localhost:8090/realms/master/protocol/openid-connect/certs
            issuer: http://localhost
```

Example shows all available options for extension. Required fields are:
* server
    * http
        * port
* clients (required if using GrpcClientConf helper class)
    * client[i].name
    * client[i].port
    * client[i].address
 
When using secure connection via TLS, "certFile" and "keyFile" must be present
in the server section and "chainFile" in client section. If mutual TLS connection 
is required "chainFile" path must be set. Default value for "mutualTLS" parameter is "optional".
Additionally "keyFile" and "chainFile" must be set on client side.

Extension can be disabled by setting `enabled` property to `false` (defaults to `true`).

```yaml
kumuluzee:
  grpc:
    enabled: false
```


### Generate Java classes from Protobuf files

For protobuf code generation with Maven you can use protobuf-maven-plugin. 
It will locate .proto files in /src/main/proto and generated Java classes.

```xml
<build>
  <extensions>
    <extension>
      <groupId>kr.motd.maven</groupId>
      <artifactId>os-maven-plugin</artifactId>
      <version>1.5.0.Final</version>
    </extension>
  </extensions>
  <plugins>
    <plugin>
      <groupId>org.xolstice.maven.plugins</groupId>
      <artifactId>protobuf-maven-plugin</artifactId>
      <version>0.5.1</version>
      <configuration>
        <protocArtifact>com.google.protobuf:protoc:3.5.1-1:exe:${os.detected.classifier}</protocArtifact>
        <pluginId>grpc-java</pluginId>
        <pluginArtifact>io.grpc:protoc-gen-grpc-java:1.15.1:exe:${os.detected.classifier}</pluginArtifact>
      </configuration>
      <executions>
        <execution>
          <goals>
            <goal>compile</goal>
            <goal>compile-custom</goal>
          </goals>
        </execution>
      </executions>
    </plugin>
  </plugins>
</build>
```

# Issues

* CDI injection does not work on Grpc service implementation *(when implementing service on server side 
final ServerServiceDefinition class is used)*. CDI lookup must be explicitly called programmatically using
CDI lookup:

```java
UserBean bean = CDI.current().select(UserBean.class).get();
``` 




