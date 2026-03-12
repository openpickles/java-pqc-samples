package com.example.pqc;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.KeyManagerFactory;
import java.security.KeyStore;
import java.io.FileInputStream;

public class NativeTlsServer {
    public static void main(String[] args) throws Exception {
        char[] password = "password".toCharArray();
        KeyStore ks = KeyStore.getInstance("JKS");
        try (FileInputStream fis = new FileInputStream("tomcat/conf/mldsa.jks")) {
            ks.load(fis, password);
        }

        KeyManagerFactory kmf = KeyManagerFactory.getInstance("PKIX");
        kmf.init(ks, password);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), null, null);

        SSLServerSocketFactory ssf = sslContext.getServerSocketFactory();
        try (SSLServerSocket serverSocket = (SSLServerSocket) ssf.createServerSocket(8444)) {
            System.out.println("Native TLS Server listening on port 8444 with ML-DSA cert...");
            while (true) {
                try {
                    javax.net.ssl.SSLSocket socket = (javax.net.ssl.SSLSocket) serverSocket.accept();
                    System.out.println("Client Connected!");
                    socket.startHandshake();
                    System.out.println("Handshake Completed.");
                    socket.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
