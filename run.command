#!/bin/bash
cd "$(dirname "$0")"
mvn clean package -DskipTests -q
java -jar target/my-app-0.0.1-SNAPSHOT.jar
