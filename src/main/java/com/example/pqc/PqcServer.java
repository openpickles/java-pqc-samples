package com.example.pqc;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jsse.BCExtendedSSLSession;
import org.bouncycastle.jsse.provider.BouncyCastleJsseProvider;
import org.bouncycastle.pqc.jcajce.provider.BouncyCastlePQCProvider;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.security.KeyStore;
import java.security.Security;

public class PqcServer {

    public static void main(String[] args) {
        try {
            Security.addProvider(new BouncyCastleProvider());
            Security.addProvider(new BouncyCastlePQCProvider());
            Security.addProvider(new BouncyCastleJsseProvider());

            char[] ksPass = "password".toCharArray();
            KeyStore ks = KeyStore.getInstance("JKS");
            try (FileInputStream fis = new FileInputStream("certs/server.jks")) {
                ks.load(fis, ksPass);
            }

            KeyManagerFactory kmf = KeyManagerFactory.getInstance("PKIX", "BCJSSE");
            kmf.init(ks, ksPass);

            SSLContext sslContext = SSLContext.getInstance("TLS", "BCJSSE");
            sslContext.init(kmf.getKeyManagers(), null, new java.security.SecureRandom());

            SSLServerSocketFactory ssf = sslContext.getServerSocketFactory();
            SSLServerSocket serverSocket = (SSLServerSocket) ssf.createServerSocket(8443);
            
            System.out.println("----------------------------------------------");
            System.out.println("PQC TLS Server started on port 8443");
            System.out.println("----------------------------------------------");

            while (true) {
                System.out.println("Waiting for secure connection...");
                try (SSLSocket socket = (SSLSocket) serverSocket.accept()) {
                    System.out.println("Incoming connection from: " + socket.getInetAddress());

                    socket.startHandshake();

                    System.out.println("Successfully negotiated TLS handshake.");
                    System.out.println("Protocol: " + socket.getSession().getProtocol());
                    System.out.println("Cipher Suite: " + socket.getSession().getCipherSuite());
                    
                    if (socket.getSession() instanceof org.bouncycastle.jsse.BCExtendedSSLSession) {
                        try {
                            org.bouncycastle.jsse.BCExtendedSSLSession bcSession = (org.bouncycastle.jsse.BCExtendedSSLSession) socket.getSession();
                            System.out.println("Local Supported Signature Algorithms: " + java.util.Arrays.toString(bcSession.getLocalSupportedSignatureAlgorithms()));
                            System.out.println("Peer Supported Signature Algorithms: " + java.util.Arrays.toString(bcSession.getPeerSupportedSignatureAlgorithms()));
                        } catch (Exception x) {
                            // Ignored
                        }
                    }
                    
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                    String line = in.readLine();
                    System.out.println("Received: " + line);

                    out.println("Hello from the PQC TLS Java 25 server!");
                    
                } catch (Exception se) {
                    System.err.println("Connection failed: " + se.getMessage());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
