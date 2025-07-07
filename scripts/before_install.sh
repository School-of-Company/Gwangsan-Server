#!/bin/bash
echo "== Before Install 시작 =="

# 이전 배포 폴더 백업
if [ -d /var/www/myapp ]; then
  mv /var/www/myapp /var/www/myapp_backup_$(date +%Y%m%d%H%M%S)
fi

echo "== Before Install 완료 =="
