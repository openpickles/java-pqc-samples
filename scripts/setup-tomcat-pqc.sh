#!/bin/bash
set -e

echo "Setting up Tomcat 11 with Pure Java 25 JSSE (Native ML-DSA PQC)..."
echo "======================================================================"

if [ ! -d "tomcat" ]; then
    echo "Tomcat 11 not found in 'tomcat/' directory. Did the download fail during plan phase?"
    echo "Please download Tomcat 11 binary from Apache and extract to 'tomcat' in this directory."
    exit 1
fi

echo "Resolving Java 25 Home..."
export JAVA_HOME="/Users/mb/.sdkman/candidates/java/25.0.2-graalce"
export KEYTOOL="$JAVA_HOME/bin/keytool"

echo "[1] Generating Pure Java 25 ML-DSA Keystore for Tomcat..."
if [ -f "tomcat/conf/mldsa.jks" ]; then
    rm tomcat/conf/mldsa.jks
fi
$KEYTOOL -genkeypair \
    -alias tomcat-pqc \
    -keyalg ML-DSA-65 \
    -sigalg ML-DSA-65 \
    -validity 365 \
    -keystore tomcat/conf/mldsa.jks \
    -storepass password \
    -keypass password \
    -dname "CN=localhost, O=Pure Java 25 Tomcat, C=US" \
    -noprompt

echo "[2] Exporting Server ML-DSA cert for Client Truststore..."
$KEYTOOL -exportcert \
    -alias tomcat-pqc \
    -keystore tomcat/conf/mldsa.jks \
    -storepass password \
    -file tomcat/conf/mldsa.cer

echo "[3] Building Client Truststore..."
if [ -f "tomcat/conf/truststore.jks" ]; then
    rm tomcat/conf/truststore.jks
fi
$KEYTOOL -importcert \
    -alias tomcat-pqc \
    -file tomcat/conf/mldsa.cer \
    -keystore tomcat/conf/truststore.jks \
    -storepass password \
    -noprompt

rm tomcat/conf/mldsa.cer

echo "======================================================================"
echo "Tomcat 11 is ready!"
echo "Server Keystore: tomcat/conf/mldsa.jks"
echo "Client Truststore: tomcat/conf/truststore.jks"
echo "Remember to ensure 'tomcat/conf/server.xml' uses JSSEImplementation and points to mldsa.jks."
