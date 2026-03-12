#!/bin/bash
set -e

if [ ! -d "tomcat" ]; then
    echo "Tomcat directory not found. Please extract Tomcat 11 to the 'tomcat' folder."
    exit 1
fi

echo "Starting Tomcat 11..."
export JAVA_HOME="/Users/mb/.sdkman/candidates/java/25.0.2-graalce"
./tomcat/bin/catalina.sh run
