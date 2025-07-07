#!/bin/bash
echo "== After Install 시작 =="

# 실행 권한 부여
chmod +x /var/www/myapp/gradlew
chmod +x /var/www/myapp/start.sh

# 디렉터리 이동
cd /var/www/myapp

# Gradle Wrapper로 빌드 실행
./gradlew clean build -x test

echo "== After Install 완료 =="
