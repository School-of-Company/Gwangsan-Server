-- MariaDB 읽기 전용 모니터링 계정 템플릿. 운영 적용은 별도 승인 후 DBA 권한 세션에서 실행한다.
-- <APP_HOST_IP>: Alloy가 실행되는 앱 EC2 사설 IP(현재 10.0.2.44). <PASSWORD>: 운영 호스트에서 생성, 저장소에 기록 금지.
-- SELECT *.* 는 부여하지 않는다(업무 데이터 읽기 차단). SHOW GLOBAL STATUS/VARIABLES는 추가 권한이 필요 없다.
CREATE USER IF NOT EXISTS 'gwangsan_monitor'@'<APP_HOST_IP>'
  IDENTIFIED BY '<PASSWORD>'
  WITH MAX_USER_CONNECTIONS 3;
-- PROCESS: information_schema.INNODB_METRICS·PROCESSLIST 집계용(쿼리 본문은 수집 대상 아님).
GRANT PROCESS ON *.* TO 'gwangsan_monitor'@'<APP_HOST_IP>';
GRANT SELECT ON performance_schema.* TO 'gwangsan_monitor'@'<APP_HOST_IP>';

-- 되돌리기:
-- DROP USER 'gwangsan_monitor'@'<APP_HOST_IP>';
