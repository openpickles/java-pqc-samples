package com.example.pqc;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.io.FileOutputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Date;

public class CertGenerator {

    public static void main(String[] args) {
        try {
            // Using standard BC Provider to generate an acceptable TLS certificate (e.g. EC)
            Security.addProvider(new BouncyCastleProvider());

            String keyAlgName = "EC";
            System.out.println("Generating key pair using: " + keyAlgName);
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance(keyAlgName, BouncyCastleProvider.PROVIDER_NAME);
            keyGen.initialize(256);
            
            KeyPair keyPair = keyGen.generateKeyPair();
            System.out.println("Key Pair generated successfully.");

            X500Name issuer = new X500Name("CN=PQC-TLS-CA");
            X500Name subject = new X500Name("CN=localhost");
            BigInteger serial = BigInteger.valueOf(System.currentTimeMillis());
            Date notBefore = new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000L); // 1 day ago
            Date notAfter = new Date(System.currentTimeMillis() + 365L * 24 * 60 * 60 * 1000L); // 1 year from now

            JcaX509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                    issuer, serial, notBefore, notAfter, subject, keyPair.getPublic()
            );

            System.out.println("Signing certificate using SHA256withECDSA");
            
            ContentSigner signer = new JcaContentSignerBuilder("SHA256withECDSA")
                    .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                    .build(keyPair.getPrivate());

            X509CertificateHolder certHolder = certBuilder.build(signer);

            X509Certificate cert = new JcaX509CertificateConverter()
                    .setProvider(BouncyCastleProvider.PROVIDER_NAME) 
                    .getCertificate(certHolder);
                    
            System.out.println("Certificate generated: " + cert.getSubjectX500Principal());

            char[] password = "password".toCharArray();

            KeyStore serverKs = KeyStore.getInstance("JKS");
            serverKs.load(null, null);
            serverKs.setKeyEntry("server-tls", keyPair.getPrivate(), password, new Certificate[]{cert});
            try (FileOutputStream fos = new FileOutputStream("certs/server.jks")) {
                serverKs.store(fos, password);
            }
            System.out.println("Saved server keystore to certs/server.jks");

            KeyStore trustKs = KeyStore.getInstance("JKS");
            trustKs.load(null, null);
            trustKs.setCertificateEntry("server-tls", cert);
            try (FileOutputStream fos = new FileOutputStream("certs/truststore.jks")) {
                trustKs.store(fos, password);
            }
            System.out.println("Saved truststore to certs/truststore.jks");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
