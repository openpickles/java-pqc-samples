package com.example.pqc;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jsse.provider.BouncyCastleJsseProvider;
import org.bouncycastle.pqc.jcajce.provider.BouncyCastlePQCProvider;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManagerFactory;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.security.KeyStore;
import java.security.Security;

public class PqcClient {

    public static void main(String[] args) {
        try {
            // Register providers
            Security.addProvider(new BouncyCastleProvider());
            Security.addProvider(new BouncyCastlePQCProvider());
            Security.addProvider(new BouncyCastleJsseProvider());

            char[] trPass = "password".toCharArray();
            KeyStore ks = KeyStore.getInstance("JKS");
            try (FileInputStream fis = new FileInputStream("certs/truststore.jks")) {
                ks.load(fis, trPass);
            }

            TrustManagerFactory tmf = TrustManagerFactory.getInstance("PKIX", "BCJSSE");
            tmf.init(ks);

            SSLContext sslContext = SSLContext.getInstance("TLS", "BCJSSE");
            sslContext.init(null, tmf.getTrustManagers(), new java.security.SecureRandom());

            SSLSocketFactory ssf = sslContext.getSocketFactory();
            
            System.out.println("Connecting to PQC server on localhost:8443...");
            try (SSLSocket socket = (SSLSocket) ssf.createSocket("localhost", 8443)) {
                
                // Force handshake
                socket.startHandshake();

                System.out.println("----------------------------------------------");
                System.out.println("Connected to secure server");
                System.out.println("Protocol in use: " + socket.getSession().getProtocol());
                System.out.println("Cipher Suite in use: " + socket.getSession().getCipherSuite());
                System.out.println("----------------------------------------------");

                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                System.out.println("Sending greeting to server...");
                out.println("Hello from the PQC Client!");

                String response = in.readLine();
                System.out.println("Server replied: " + response);

            } catch (Exception se) {
                System.err.println("Connection failed: " + se.getMessage());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
