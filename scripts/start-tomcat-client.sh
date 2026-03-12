#!/bin/bash
set -e

echo "Starting Tomcat JSSE Native ML-DSA Client test..."

if [ ! -f "tomcat/conf/truststore.jks" ]; then
    echo "Truststore not found at tomcat/conf/truststore.jks. Please run setup-tomcat-pqc.sh first."
    exit 1
fi

export JAVA_HOME="/Users/mb/.sdkman/candidates/java/25.0.2-graalce"
export PATH="$JAVA_HOME/bin:$PATH"

# Re-use the existing PqcClient for the Native test (which operates perfectly without BouncyCastle now)
# But we need to ensure it uses the specific tomcat truststore via args/properties or hardcoded path.
# Rather than hardcoding, let's just make a dedicated TomcatClient in Java dynamically, or pass properties.
# For simplicity, we can pass system properties to PqcClient and modify it slightly to accept property overrides for truststore.
java -Djavax.net.debug=ssl,handshake \
     -Djavax.net.ssl.trustStore=tomcat/conf/truststore.jks \
     -Djavax.net.ssl.trustStorePassword=password \
     -cp target/java-pqc-tls-demo-1.0-SNAPSHOT.jar \
     com.example.pqc.TomcatClient
