# Gwangsan-Server 헥사고날 아키텍처

기존 20개 업무 패키지를 이름 그대로 유지한 채 계층 우선 구조(domain / application / adapter / infrastructure)로 전환한 결과를 정리한다. 파일별 이전 경로 → 새 경로는 `docs/hexagonal-migration-inventory.md` 가 원본이다. 이 문서는 구조 규칙, 업무 간 연결, 트랜잭션·락 소유, 저장 호환성, 검증 방법, 운영 반영·롤백 기준을 다룬다.

## 1. 계층과 의존 방향

```text
team.startup.gwangsan
├── domain/<업무>          순수 모델·정책·예외 (JDK·lombok 만). domain/common: ActorContext, 공통 예외(ErrorCode, GlobalException)
├── application/<업무>
│   ├── port/in           공개 입력 Port (XxxUseCase / XxxQuery) — 업무 간 공개 표면
│   ├── port/out          업무가 필요로 하는 능력 (XxxPort)
│   ├── data              Command / Query / Result record (Spring 타입 금지)
│   └── service           유스케이스 구현. @Service·@Transactional 허용
├── adapter/<기술>/<업무>   web(controller·HTTP DTO) · persistence(JpaEntity·JpaRepository·QueryDSL·PersistenceAdapter)
│                          integration(타 업무 공개 Port 연결) · event · scheduler · stream · security · redis · s3 · sms · push · http
└── infrastructure         config(Spring 설정·빈 wiring) · security(필터 체인) · web(공통 오류 매핑·요청 로그)
```

의존은 adapter → application → domain 한 방향이다. 업무 간 호출은 소비자 업무의 output Port 를 `adapter/integration/<소비자>` 가 구현하고, 그 안에서 제공 업무의 `application/<제공자>/port/in` 만 호출한다. 다른 업무의 service·JpaEntity·JpaRepository 를 직접 쓰지 않는다. 저장 모델 사이의 JPA association(예: `ProductJpaEntity.member`)은 기존 스키마·cascade 보존을 위한 persistence 내부 예외로만 남긴다.

### 자동 검증 (ArchUnit, `src/test/java/team/startup/gwangsan/architecture`)

`HexagonalArchitectureTest` 가 production 클래스 전체(500+)를 import 해 다음 10개 규칙을 검사하고, 금지 의존 fixture 17종이 실제로 위반으로 보고되는지도 함께 확인한다.

1. 새 클래스는 domain/application/adapter/infrastructure 에만 (global 금지)
2. domain 은 JDK·lombok·domain 타입만
3. application 은 domain·application + `@Service`/`@Transactional`·slf4j 만
4. application port/data 는 Spring 타입 금지
5. domain/application 업무 격리: 자기 업무와 common 만
6. application 업무 간 순환 금지
7. adapter → application service 직접 의존 금지 (input Port 사용)
8. inbound adapter(web/event/scheduler/stream) → persistence·JPA·Spring Data·QueryDSL 금지
9. JPA Entity/Embeddable·Spring Data Repository 는 adapter.persistence 에만
10. Controller 는 adapter.web, ControllerAdvice 는 adapter.web/infrastructure.web

전환 기간의 임시 허용 목록 `src/test/resources/architecture/legacy-classes.txt` 는 최종 상태에서 0줄이며, shared kernel 예외도 0 이다. `HexagonalFinalGateTest` 가 legacy 0·global 패키지 0·`domain/<업무>/{service,repository,entity,presentation}` 0·공통 예외의 `domain.common` 위치를 항상 검사한다.

## 2. 업무별 공개 Port

다른 업무는 아래 입력 Port 와 같은 패키지 아래 `data` 타입만 사용한다. 업무 간 연결은 소비자 output Port 를 구현한 `adapter/integration/<소비자>` 가 담당한다.

| 업무 | 공개 입력 Port (application/<업무>/port/in) | output Port 수 |
| --- | --- | --- |
| auth | `CurrentMemberQuery`, `FindNicknameQuery`, `IssueAccountTokenUseCase`, `PasswordMatchQuery`, `ReissueTokenUseCase`, `ResetPasswordUseCase`, `SignInUseCase`, `SignOutUseCase`, `SignUpUseCase` | 9 |
| member | `AddMemberLightUseCase`, `ChangeGwangsanUseCase`, `ChangePasswordUseCase`, `FindAllUserInfoQuery`, `FindMyInfoQuery`, `FindUserInfoQuery`, `ManageMemberUseCase`, `MemberAccountQuery`, `MemberSummaryQuery`, `RegisterMemberUseCase`, `UpdateMyInfoUseCase`, `WithdrawMemberUseCase` | 6 |
| sms | `PhoneVerificationQuery`, `SendFindNicknameSmsUseCase`, `SendResetPasswordSmsUseCase`, `SendSmsUseCase`, `VerifyCodeUseCase` | 3 |
| dong | `DongQuery` | 1 |
| place | `PlaceQuery` | 1 |
| relatedkeyword | `DeleteAllMemberRelatedKeywordsUseCase`, `DeleteMemberRelatedKeywordUseCase`, `FindMemberRelatedKeywordsQuery`, `FindRelatedKeywordsQuery`, `MemberKeywordCommand` | 1 |
| app | `FindAppVersionQuery` | 1 |
| health | — (기술 adapter 만: adapter/web/health) | 0 |
| notification | `RegisterDeviceTokenUseCase`, `SendPushUseCase` | 3 |
| image | `CleanupOrphanImagesUseCase`, `DeleteImageUseCase`, `DeleteUnusedImagesUseCase`, `ImageQuery`, `UploadImageUseCase` | 7 |
| post | `CancelReservationUseCase`, `CreateProductUseCase`, `DeleteProductUseCase`, `DueReservationReminderQuery`, `MemberProductsQuery`, `PendingReservationUseCase`, `PlaceProductsQuery`, `PostMemberReferenceCleanupUseCase`, `ProductDetailQuery`, `ProductImageQuery`, `ProductSnapshotQuery`, `ProductStatusUseCase`, `ReserveProductUseCase`, `SendReservationReminderUseCase`, `UnlinkProductImageUseCase`, `UpdateProductUseCase`, `ValidateProductContentUseCase` | 10 |
| trade | `ApproveTradeCancelUseCase`, `CompletedTradeQuery`, `RejectTradeCancelUseCase`, `RequestTradeCancelUseCase`, `RequestTradeCompletionUseCase`, `TradeCancelQuery`, `TradeHistoryQuery`, `TradeMemberReferenceCleanupUseCase`, `TradeStateQuery`, `TradeStatisticsQuery`, `TradeSummaryQuery`, `WithdrawTradeCancelUseCase`, `WithdrawTradeCompletionUseCase` | 15 |
| review | `CreateReviewUseCase`, `ReviewMemberReferenceCleanupUseCase`, `ReviewQuery`, `ReviewSummaryQuery` | 6 |
| chat | `ChatMemberCleanupUseCase`, `ChatRoomIdQuery`, `ChatRoomListQuery`, `ChatRoomLookupQuery`, `CreateChatRoomUseCase`, `DeleteChatMessageUseCase`, `HideChatRoomUseCase`, `LoadChatRoomMessagesUseCase`, `ReadChatMessagesUseCase`, `SaveChatMessageUseCase`, `SaveChatSystemMessageUseCase`, `UpdateChatMessageUseCase`, `ValidateChatSendableQuery` | 11 |
| report | `CreateReportUseCase`, `ReassignReportMemberUseCase`, `ReportQuery`, `UnlinkReportImageUseCase` | 5 |
| block | `BlockListQuery`, `BlockMemberUseCase`, `BlockRelationQuery`, `DeleteMemberBlocksUseCase`, `UnblockMemberUseCase` | 3 |
| suspend | `DeleteMemberSuspensionsUseCase`, `ReleaseExpiredSuspensionsUseCase`, `SuspendMemberUseCase`, `SuspensionQuery` | 6 |
| notice | `CreateNoticeUseCase`, `DeleteNoticeUseCase`, `NoticeDetailQuery`, `NoticeListQuery`, `NoticeQuery`, `ReassignNoticeAuthorUseCase`, `UnlinkNoticeImageUseCase`, `UpdateNoticeUseCase` | 5 |
| alert | `AlertMemberCleanupUseCase`, `CreateAlertFromSourceUseCase`, `MyAlertsQuery`, `ReadAlertsUseCase`, `RecordAlertUseCase`, `UnreadAlertQuery` | 6 |
| admin | `AdjustGwangsanUseCase`, `AdminAlertExistenceQuery`, `ApproveTradeCancelUseCase`, `CreateAdminAlertUseCase`, `DeleteAdminAlertUseCase`, `DeleteAdminProductUseCase`, `FindAdminAlertsQuery`, `FindAdminProductsQuery`, `ReassignAdminAlertMemberUseCase`, `RejectAdminAlertUseCase`, `RemoveAdminAlertUseCase`, `SignInAdminUseCase`, `UpdateMemberRoleUseCase`, `UpdateMemberStatusUseCase`, `VerifySignUpUseCase` | 9 |


## 3. 트랜잭션·락·이벤트 소유

| 흐름 | 소유 유스케이스 | 기준 |
| --- | --- | --- |
| 거래 완료 요청·확정 (작성자 즉시 완료 포함, develop #431) | trade `RequestTradeCompletionService` | READ_COMMITTED, 상품 행 잠금 선행(`ProductStatusUseCase.lockActive`), 잔액 이동은 member `ChangeGwangsanUseCase` 가 같은 트랜잭션에 참여 |
| 거래 철회 (상호 동의·관리자 승인) | trade `RequestTradeCancelService`·`ApproveTradeCancelService`, admin `AdminApproveTradeCancelService` | productId 스칼라 조회 → 상품 `FOR UPDATE`(`lockIncludingDeleted`) → TradeCancel 최초 로드 → 인가 → 반영. admin READ_COMMITTED 에 trade 가 참여. `TradeCancelApprovalLockOrderIntegrationTest` 가 실제 SQL 순서로 검증 |
| 사용자 알림함 원자 저장 | trade 유스케이스 → alert `RecordAlertUseCase` | `OTHER_MEMBER_TRADE_COMPLETE`(완료 요청)·`TRADE_CANCEL`(철회 완료) 두 종류만 업무와 같은 트랜잭션, 저장 실패 시 거래·잔액 rollback 과 커밋 후 전달 미발생(`TradeAlertAtomicityIntegrationTest`) |
| 그 밖의 사용자 알림 | 가입·신고 기각·철회 기각·정지·후기·공지 | 기존 AFTER_COMMIT 비동기(`CreateAlertEvent`·`CreateAlertMembersEvent`) 유지 |
| 푸시 | notification | 커밋 후 비동기(`PushRequestedEvent` → `PushRequestedEventListener`), 일시 장애 제한 재시도·영구 실패 미재시도, Expo 요청/응답은 push adapter 내부. 커밋 후 발송 실패에도 업무·알림 유지(`TradePushFailureAfterCommitIntegrationTest`) |
| 거래 상태 실시간 전달 | trade 이벤트 → http adapter | 커밋 후 비동기 HTTP, 서버 재시도 추가 없음 |
| 채팅 Redis Streams | adapter/stream/chat → chat 저장 UseCase | DB 저장 후 ACK, retry/DLQ 기록 성공 후 원본 ACK·실패 시 pending 유지, 같은 messageId·같은 내용 replay / 다른 내용 충돌 거절 |
| 예약 reminder | adapter/scheduler/post | 동기 발송 성공 후 sentAt 기록(현행 유지) |
| 회원 탈퇴 | member 탈퇴 UseCase → 각 업무 탈퇴 정리 command | 한 트랜잭션: 기록 → 재지정(상품 직후 예약자) → 개인 데이터 삭제(키워드는 bulk 삭제) → 회원 삭제. 중간 실패 시 전체 rollback |
| 정지 자동 해제 | adapter/scheduler/suspend → suspend UseCase | 만료 정지 ∧ 현재 SUSPENDED ∧ 진행 중 정지 없음, DISTINCT 회원, 단일 트랜잭션 |

## 4. 외부 계약과 저장 호환

- URL·HTTP method·status·요청/응답 JSON·오류 `{status, message}` 는 바뀌지 않았다. HTTP DTO 는 `adapter/web/<업무>/dto` 에 남아 application Command/Result 와 분리돼 있다.
- DB: 29개 테이블의 이름·컬럼·SQL 타입·enum 값·nullable·PK·FK·unique·index 는 `JpaSchemaCompatibilityTest`(Hibernate 메타데이터 스냅샷, `src/test/resources/compatibility/jpa-schema.snapshot`) 로 고정한다. 엔티티 클래스를 `XxxJpaEntity` 로 옮겨도 테이블 기준 비교라 통과하고, 물리 스키마가 바뀌면 실패한다. 운영 `ddl-auto=validate` 기준을 지킨다.
- Redis: `@RedisHash` RefreshToken·DeviceToken 은 keyspace·TTL·secondary index 를 유지하고, 운영에 이미 저장된 hash 의 `_class`(이전 FQCN)를 읽을 수 있다(`RedisHashCompatibilityTest`). SMS 인증·로그아웃 blacklist 값은 Jackson JSON 바이트(`"123456"`, `2`, `true`, `"BLACKLIST"`)를 유지한다(`RedisValueCompatibilityTest`).
- 오류 코드: `ErrorCode` 86개의 이름·status·메시지를 `error-codes.snapshot` 으로 고정한다(`ErrorContractCompatibilityTest`).
- 알림 발행 범위: `AlertPublisherCompatibilityTest` 가 원자 저장 두 종류의 비동기 재발행 0, 나머지 종류의 비동기 발행 위치 수, `TRADE_COMPLETE` 알림함 미발행을 고정한다.
- 채팅 서버(Nest)·프론트(앱·관리자) 코드는 바꾸지 않았다.

## 5. 검증 명령

```bash
./gradlew clean test                      # 전체 (MariaDB·Redis Testcontainers 포함)
./gradlew test --tests 'team.startup.gwangsan.architecture.*' --tests 'team.startup.gwangsan.compatibility.*'
./gradlew build -x test && git diff --check
```

CI(`.github/workflows/ci.yml`)는 PR 과 develop·main·`refactor/hexagonal` push 에서 `./gradlew test` 를 실행하므로 ArchUnit·호환 checker·최종 gate 가 자동 포함된다. jacoco 제외 경로는 이동 후 경로(`infrastructure/config`, `infrastructure/security/SecurityConfig`, `adapter/sms`, `*JpaEntity`)로 옮겨 기존과 같은 클래스 집합을 제외한다. 테스트 JVM 힙은 1g(테스트 수 증가로 기본 512m 에서 OOM).

호환 스냅샷을 바꾸는 것은 계약 변경이다. 사용자 승인 없이 `COMPAT_SNAPSHOT_UPDATE=true` 로 재생성하지 않는다.

## 6. 운영 반영과 롤백

- 반영 경로: `refactor/hexagonal` → `develop` 통합 PR 하나. develop push 가 CD(ECR·CodeDeploy)를 시작하므로 통합 PR 병합이 곧 운영 배포다. 중간 스택 병합은 refactor/hexagonal 에만 하고 배포를 시작하지 않는다.
- 스키마: 이번 전환은 Flyway migration 을 추가하지 않는다. 운영 `ddl-auto=validate` 가 그대로 통과해야 하며, 스냅샷 테스트가 이를 보장한다.
- Redis: 키·TTL·직렬화 바이트·`_class` 읽기 호환을 유지하므로 배포 전후 세션(refresh token)·디바이스 토큰·SMS 인증 진행 상태가 유지된다.
- 롤백: 이전 이미지(develop 의 직전 배포 SHA)로 재배포한다. 스키마·Redis 형식이 바뀌지 않았으므로 데이터 되돌림은 필요 없다. 새 버전이 쓴 RefreshToken/DeviceToken hash 의 `_class` 는 새 FQCN 이지만, 이전 버전도 선언 타입으로 읽는다(같은 converter 동작 — `RedisHashCompatibilityTest.it_reads_hash_whose_class_no_longer_exists` 와 같은 원리).
- 채팅 Streams: 메시지 payload·필드명·retry/DLQ 키가 같아서 배포 중 pending 메시지를 어느 버전이든 처리할 수 있다.
