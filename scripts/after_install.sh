#!/bin/bash
echo "== After Install 시작 =="

chmod +x /var/www/myapp/start.sh

# 혹시 .jar 파일 권한도 맞춰주기
chmod +x /var/www/myapp/build/libs/app.jar || true

echo "== After Install 완료 =="
