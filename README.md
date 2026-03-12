# Java 25 Post-Quantum Cryptography (PQC) TLS Demo

This demonstration project showcases how to integrate the finalized Post-Quantum Cryptography (PQC) algorithms using BouncyCastle under Java 25.

It provides a client-server TLS 1.3 connection backed by the `bctls-jdk18on` provider (Bouncy Castle version 1.80) capable of negotiating ML-KEM (Kyber) and ML-DSA (Dilithium) standards natively over a standard JSSE socket.

## Requirements

* **Java 25+**
* **Maven 3.8+**

## Structure

* `src/main/java/com/example/pqc/CertGenerator.java`
  * Programmatically generates test `server.jks` and `truststore.jks` utilizing a PQC provider for the core elements (falling back to EC wrapper if strict JCA builder 
  mismatch occurs).
* `src/main/java/com/example/pqc/PqcServer.java`
  * Implements `SSLServerSocket` mapped strictly to `BouncyCastleJsseProvider`. This provider enables hybrid / explicit PQC key encapsulations (like `x25519+kyber768` or `ML-KEM`).
* `src/main/java/com/example/pqc/PqcClient.java`
  * Standard Java SSL client validating against the generated truststore connecting over TLS 1.3 `TLS_CHACHA20_POLY1305_SHA256` wrapped with PQC negotiations.

## How it Works (KEM vs Authentication)

In TLS 1.3, the cryptographic handshake is split into two distinct parts:

1. **Authentication (Signatures):** Proves the server is who it claims to be. This is what the certificate in `server.jks` provides. In the current demo, `CertGenerator.java` generates a standard **ECDSA** certificate to guarantee client compatibility.
2. **Key Exchange (KEM):** Derives the symmetric encryption keys (e.g., for `CHACHA20_POLY1305`) used for the actual data transfer.

**The Hybrid Approach:**
Even though `server.jks` looks like a standard EC keystore, **TLS 1.3 uses a Post-Quantum Key Encapsulation Mechanism (like ML-KEM or Kyber)** for the *Key Exchange*. BouncyCastle JSSE implicitly prefers hybrid PQC key shares out of the box when PQC providers are loaded. This secures the connection data against "harvest now, decrypt later" quantum attacks while maintaining standard authentication compatibility.

### Why BouncyCastle in Java 25?
Although Java 25 intrinsically supplies Post-Quantum mathematical primitives (`ML-KEM` and `ML-DSA`) via the `SunJCE` provider natively, the standard Java Secure Socket Extension (`SunJSSE`) does not yet bridge these algorithms into standard TLS 1.3 `NamedGroups` for the `key_share` extension. Therefore, `BouncyCastleJsseProvider` is strictly required to negotiate Post-Quantum sockets until native standard integration is complete.

## Quickstart

The keystores are statically provided inside the `certs/` directory for immediate testing. 

1. **Start the server:**
```bash
./scripts/start-server.sh
```

2. **In a separate terminal, start the client:**
```bash
./scripts/start-client.sh
```

When connected, the output will log the handshake protocols and confirm the secure PQC JSSE configuration in the background!

### Verifying PQC Key Exchange

To explicitly observe the PQC Key Exchange being selected, the `start-server.sh` and `start-client.sh` scripts are configured to log the raw Java Secure Socket Extension (JSSE) debug data for the handshake (`-Djavax.net.debug=ssl,handshake`). 

If you want to **strictly force** the Java socket to accept nothing but PQC key exchanges (proving it works!), pass the `jdk.tls.namedGroups` JVM property configured to a PQC encapsulation mechanism (like `mlkem768`):

```bash
## (Optional) Pure PQC Authentication (ML-DSA) with Tomcat 11

Java 25 intrinsically supports generating fully Post-Quantum ML-DSA certificates natively via the `SunJCE` provider, meaning we can configure a secure Tomcat 11 container relying strictly on Java 25 primitives (without BouncyCastle or OpenSSL libraries!).

However, **OpenJDK 25's `SunJSSE` TLS 1.3 implementation does not natively map ML-DSA certificates into the handshake Cipher Suites yet.** Therefore, connecting pure Java clients to this server will gracefully result in standard handshake rejections (`handshake_failure`) demonstrating exactly the missing JSSE mapping capability!

You can execute this fascinating Pure JSSE test setup seamlessly with the provided Tomcat distribution:

### 1. The Pre-Packaged Tomcat 11 Directory
This repository already contains a cleanly extracted Tomcat 11 container inside the `tomcat/` folder. It has been stripped of the OpenSSL (APR) listener bridging inside `tomcat/conf/server.xml` to guarantee it only processes security algorithms using pure native Java mechanisms:

```xml
<Connector port="8443" protocol="org.apache.coyote.http11.Http11NioProtocol"
           maxThreads="150" SSLEnabled="true"
           sslImplementationName="org.apache.tomcat.util.net.jsse.JSSEImplementation">
    <SSLHostConfig>
        <Certificate certificateKeystoreFile="conf/mldsa.jks" certificateKeystorePassword="password" />
    </SSLHostConfig>
</Connector>
```

### 2. Generate ML-DSA Keystores and Start the Test
Run the prepared scripts which utilize Native Java 25 `keytool` capabilities (No BouncyCastle) to construct true PQC keystores and boot the Tomcat Server!
```bash
./scripts/setup-tomcat-pqc.sh
./scripts/start-tomcat-server.sh
```

### 3. Validate ML-DSA Rejection (Browser / Client)
Because Tomcat successfully serves a 100% pure Post-Quantum authentication certificate, modern clients will fail to negotiate the cipher blocks until standards evolve.

**Test via Browser:** 
Open your web browser and navigate to `https://localhost:8443`.
You will receive an immediate `ERR_SSL_PROTOCOL_ERROR` or `ERR_CONNECTION_CLOSED` because standard browsers (Chrome, Firefox, Safari) do not yet understand ML-DSA signatures natively.

**Test via Pure Java:** 
Try negotiating the Pure PQC connection against Tomcat from the client script:
```bash
./scripts/start-tomcat-client.sh
```
You will securely observe Java 25 `SunJSSE` dropping the peer connection due to TLS 1.3 `handshake_failure` proving that standard `NamedGroup` specifications are eagerly awaiting future JDK updates!
