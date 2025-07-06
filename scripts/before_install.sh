#!/bin/bash
echo "== Before Install 시작 =="

sudo systemctl stop myapp.service || true

if [ -d /var/www/myapp ]; then
  mv /var/www/myapp /var/www/myapp_backup_$(date +%Y%m%d%H%M%S)
fi

# 새 배포파일을 복사 (예시)
cp -r /tmp/myapp /var/www/

echo "== Before Install 완료 =="
