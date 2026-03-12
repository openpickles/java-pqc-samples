#!/usr/bin/env bash

cd "$(dirname "$0")/.."

# Build project if not already built (ensure jar exists)
if [ ! -f "target/java-pqc-tls-demo-1.0-SNAPSHOT.jar" ]; then
    mvn clean package
fi

# Execute the PQC Client
echo "Starting PQC Client targeting Java 25 from generated JAR..."
java -Djavax.net.debug=ssl,handshake -cp target/java-pqc-tls-demo-1.0-SNAPSHOT.jar com.example.pqc.PqcClient
