# 앱 버전 조회 및 운영 절차

## API

`GET /api/app/version?platform=ios|android`는 인증 없이 호출한다. Authorization 헤더는 보내지 않는다.
응답은 `{"latestVersion":"1.1.5","minimumVersion":"1.0.0"}` 형태다(예시이며 운영 버전이 아님).
로그인 없이 사용하는 **GET 한 경로만** 공개하며 쓰기 API는 제공하지 않는다.

- 플랫폼 누락·빈 값·대문자·지원하지 않는 값: 400.
- 플랫폼 설정이 없거나 유효하지 않은 버전 설정: 503, 기존 `{status,message}` 오류 형식.
- 버전은 선행 0 없는 `major.minor.patch`, 각 숫자는 0~999999999. minimumVersion <= latestVersion.
- 앱은 숫자 세 부분을 비교한다. 문자열 사전순 비교는 금지(1.10.0 > 1.9.0).
- 현재 클라 연계 #715는 latestVersion만 권장 업데이트에 사용한다. minimumVersion은 향후 강제 업데이트용이다.
- API 실패/설정 전에는 버전을 추측하여 업데이트를 강제하지 않는다. 기존 앱 진입을 유지하도록 클라이언트와 공유한다.

## 저장 및 배포

플랫폼을 PK로 갖는 MariaDB `tbl_app_version`에 저장한다. 응답은 두 문자열과 PK 단건 조회뿐이다.
별도 Redis/인메모리 캐시 없이 매 요청 조회하고 `Cache-Control: no-store`를 사용한다.
운영 수정/롤백을 즉시 반영하며, 캐시가 필요해질 만큼 부하가 관측되면 TTL·무효화 정책을 별도로 정한다.

기존 설정은 Flyway가 비활성화되어 있으므로 자동 마이그레이션을 가정하지 않는다.
DB 관리자가 대상 DB를 확인하고 `src/main/resources/db/migration/V10__create_app_version.sql`을 적용한다.
이미 Hibernate ddl-auto=update로 동일 테이블이 생성되었다면 CREATE IF NOT EXISTS는 이를 유지한다.
기존 테이블의 세 컬럼/PK 정의가 일치하는지도 확인한다. 이 작업에서 전체 Flyway를 활성화하지 않는다.

초기 데이터는 임의로 넣지 않는다. 앱 전환 전에 **각 스토어에서 실제 배포가 확인된 최신 버전**과
운영자가 승인한 최소 지원 버전을 각각 입력하고 두 플랫폼 API의 200을 확인한다. 설정 전에는 503이다.

## 재배포 없는 값 변경/롤백

1. App Store/Play Store의 대상 사용자에게 실제 업데이트가 제공되는지 확인한다. 단계적 출시 중에는 전체 사용자 공개 전에 값을 올리지 않는다.
2. 기존 값을 기록한다. 최소 버전 상향은 강제 업데이트 영향에 대한 별도 승인 후 수행한다.
3. 권한 있는 DB 운영자가 아래 예시의 값을 **확인한 실제 버전으로 바꿔서** 트랜잭션으로 적용한다.

```sql
START TRANSACTION;
SELECT * FROM tbl_app_version WHERE platform = 'ios' FOR UPDATE;
-- 아래 버전은 예시다. 운영 확인 없이 실행하지 않는다.
INSERT INTO tbl_app_version (platform, latest_version, minimum_version)
VALUES ('ios', '1.1.5', '1.0.0')
ON DUPLICATE KEY UPDATE latest_version = VALUES(latest_version), minimum_version = VALUES(minimum_version);
SELECT * FROM tbl_app_version WHERE platform = 'ios';
COMMIT;
```

4. ios와 android는 독립적으로 관리한다. 입력 형식과 minimum <= latest를 확인한 후 커밋한다.
5. 무인증 GET으로 변경 반영을 확인한다. 잘못된 설정은 503이므로 즉시 이전 두 값을 같은 방식으로 복원한다.
6. 롤백도 같은 upsert로 기록한 이전 값을 복원하고 GET 결과를 확인한다. 서버 재시작/배포는 필요 없다.

운영 DB 변경은 본 PR 작업에서 수행하지 않는다. DB 접근 권한은 기존 운영 권한 체계를 따른다.
