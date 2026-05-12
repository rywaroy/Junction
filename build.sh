#!/bin/bash
# 构建 fat jar，跳过测试
set -e
cd "$(dirname "$0")"
./mvnw clean package -DskipTests
echo "Build done. Jar: target/junction-0.0.1-SNAPSHOT.jar"
