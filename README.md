# Java 25 Post-Quantum Cryptography (PQC) TLS Demo

This demonstration project showcases how to integrate the finalized Post-Quantum Cryptography (PQC) algorithms using BouncyCastle under Java 25.

It provides a client-server TLS 1.3 connection backed by the `bctls-jdk18on` provider (Bouncy Castle version 1.80) capable of negotiating ML-KEM (Kyber) and ML-DSA (Dilithium) standards natively over a standard JSSE socket.

## Requirements

* **Java 25+**
* **Maven 3.8+**

## Structure

* `src/main/java/com/example/pqc/CertGenerator.java`
  * Programmatically generates test `server.jks` and `truststore.jks` utilizing a PQC provider for the core elements (falling back to EC wrapper if strict JCA builder mismatch occurs).
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
java -Djdk.tls.namedGroups=mlkem768 -cp target/java-pqc-tls-demo-1.0-SNAPSHOT.jar com.example.pqc.PqcServer
```

## (Optional) Pure PQC Authentication (ML-DSA)

If you wish to experiment with **Pure PQC Authentication** where the certificate itself is signed using Post-Quantum cryptography (instead of ECDSA):

1. Edit `src/main/java/com/example/pqc/CertGenerator.java`
2. Change the key algorithm: `String keyAlgName = "ML-DSA-65";`
3. Change the signer: `.setProvider(BouncyCastleProvider.PROVIDER_NAME).build(keyPair.getPrivate());` with algorithm `"ML-DSA-65"`
4. Re-run:
```bash
mvn clean compile exec:java -Dexec.mainClass="com.example.pqc.CertGenerator"
```

*Note: Generating a strict ML-DSA certificate will break simple TLS 1.3 compatibilities because standard JSSE clients do not natively advertise `ML-DSA` in their `signature_algorithms` without explicit overrides.*
