package com.example.pqc;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.SSLSocket;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;

public class TomcatClient {

    public static void main(String[] args) {
        try {
            // Because we pass the trustStore and trustStorePassword as -D system properties
            // in the launch script, standard SSLContext.getDefault() will automatically pick it up.
            SSLContext sslContext = SSLContext.getDefault();
            SSLSocketFactory ssf = sslContext.getSocketFactory();
            
            System.out.println("Connecting to Tomcat (Pure Java 25 ML-DSA) on localhost:8444...");
            try (SSLSocket socket = (SSLSocket) ssf.createSocket("localhost", 8444)) {
                
                // Start Handshake explicitly
                socket.startHandshake();
                
                System.out.println("Successfully connected!");
                System.out.println("Protocol: " + socket.getSession().getProtocol());
                System.out.println("Cipher Suite: " + socket.getSession().getCipherSuite());
                System.out.println("Peer Certificate: " + socket.getSession().getPeerCertificates()[0].getType());

                try (PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                     BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                     
                    out.println("GET / HTTP/1.1");
                    out.println("Host: localhost");
                    out.println("Connection: close");
                    out.println();
                    
                    String responseLine;
                    boolean foundHtml = false;
                    while ((responseLine = in.readLine()) != null) {
                        if (responseLine.contains("<html>")) {
                            foundHtml = true;
                        }
                        if (foundHtml) {
                            System.out.println(responseLine);
                            if (responseLine.contains("</html>")) break;
                        }
                    }
                    System.out.println("Data transfer complete. Connection validated natively.");
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to establish PQC connection with Tomcat: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
