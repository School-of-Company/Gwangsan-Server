# 헥사고날 전환 inventory

기준 commit `474a2cde24025e828e6c96fd5f29865bfa2a3fd0` (develop). 생성: PR01 inventory 생성기(명령은 `.omo/evidence/hex-pr01.md`). 이 문서는 PR별 전환의 소유·bridge 제거 책임 기준이다.

## 사용 규칙

- 각 PR은 자기 행의 `현재 경로`·`상태`를 갱신하고, 옮긴/삭제한 클래스를 `src/test/resources/architecture/legacy-classes.txt`에서 지운다. `HexagonalArchitectureTest`가 남은 항목(stale)과 새 클래스의 경계 위반을 실패로 보고한다.
- `목표 배치`는 기본 제안이다. 실제 위치는 담당 PR이 계획 v1 컨벤션에 맞춰 확정하고 이 표를 고친다. 빈 model/policy를 억지로 만들지 않는다.
- 담당 PR이 아닌 파일은 import 갱신 외에 수정하지 않는다. 선행 업무가 미전환이면 output Adapter 안 bridge로 연결하고 아래 bridge 표에 제거 PR을 적는다.
- 표의 API status/트랜잭션/락은 정규식 추출 결과다. `?`는 코드에서 직접 확인해야 하는 항목이다.

## 1. 요약

- 기준 main Java 파일: **539** (inventory 행 수 539, 중복 0 — 생성기가 기준 목록과 1:1 생성)
- PR01에서 이동 15 · 삭제 1 · legacy 유지 522 (+ GwangsanApplication)
- 담당 PR별 파일 수: PR01 17, PR02 102, PR03 29, PR04 34, PR05 29, PR06 54, PR07 84, PR08 68, PR09 44, PR10 24, PR11 46, PR12 8
- 업무별 파일 수: - 1, admin 46, alert 24, app 7, auth 49, block 19, chat 68, common 5, dong 2, health 1, image 29, infrastructure 13, member 34, notice 24, notification 10, place 5, post 60, relatedkeyword 14, report 19, review 25, sms 25, suspend 8, trade 51

## 2. 파일 inventory (기준 commit의 모든 main Java 파일, 경로는 `team/startup/gwangsan/` 기준)

| # | 기준 경로 | 현재 경로 | 업무 | 역할 | 목표 배치 | 담당 PR | 상태 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `GwangsanApplication.java` | = | - | bootstrap | 유지 (team.startup.gwangsan) | PR01 | 유지 |
| 2 | `domain/admin/entity/AdminAlert.java` | = | admin | jpa entity | adapter/persistence/admin (JpaEntity) + domain/admin/model (순수 모델, 규칙 있을 때) | PR11 | legacy |
| 3 | `domain/admin/entity/constant/AlertType.java` | = | admin | enum | domain/admin/model | PR11 | legacy |
| 4 | `domain/admin/exception/NotFoundAdminAlertException.java` | = | admin | exception | domain/admin/exception | PR11 | legacy |
| 5 | `domain/admin/exception/NotFoundAlertTypeException.java` | = | admin | exception | domain/admin/exception | PR11 | legacy |
| 6 | `domain/admin/exception/NotFoundPendingMemberException.java` | = | admin | exception | domain/admin/exception | PR11 | legacy |
| 7 | `domain/admin/presentation/AdminController.java` | = | admin | controller | adapter/web/admin/controller | PR11 | legacy |
| 8 | `domain/admin/presentation/AdminProductController.java` | = | admin | controller | adapter/web/admin/controller | PR11 | legacy |
| 9 | `domain/admin/presentation/dto/request/AdjustGwangsanRequest.java` | = | admin | web dto | adapter/web/admin/dto | PR11 | legacy |
| 10 | `domain/admin/presentation/dto/request/AdminSignInRequest.java` | = | admin | web dto | adapter/web/admin/dto | PR11 | legacy |
| 11 | `domain/admin/presentation/dto/request/UpdateMemberRoleRequest.java` | = | admin | web dto | adapter/web/admin/dto | PR11 | legacy |
| 12 | `domain/admin/presentation/dto/request/UpdateMemberStatusRequest.java` | = | admin | web dto | adapter/web/admin/dto | PR11 | legacy |
| 13 | `domain/admin/presentation/dto/response/GetAdminAlertResponse.java` | = | admin | web dto | adapter/web/admin/dto | PR11 | legacy |
| 14 | `domain/admin/presentation/dto/response/GetReportAlertResponse.java` | = | admin | web dto | adapter/web/admin/dto | PR11 | legacy |
| 15 | `domain/admin/presentation/dto/response/GetSignUpAlertResponse.java` | = | admin | web dto | adapter/web/admin/dto | PR11 | legacy |
| 16 | `domain/admin/presentation/dto/response/GetTradeCancelAlertResponse.java` | = | admin | web dto | adapter/web/admin/dto | PR11 | legacy |
| 17 | `domain/admin/presentation/dto/response/SignInAdminResponse.java` | = | admin | web dto | adapter/web/admin/dto | PR11 | legacy |
| 18 | `domain/admin/repository/AdminAlertRepository.java` | = | admin | repository | adapter/persistence/admin (JpaRepository) + application/admin/port/out | PR11 | legacy |
| 19 | `domain/admin/repository/custom/AdminAlertCustomRepository.java` | = | admin | querydsl | adapter/persistence/admin (QueryPort 구현) | PR11 | legacy |
| 20 | `domain/admin/repository/custom/impl/AdminAlertCustomRepositoryImpl.java` | = | admin | querydsl | adapter/persistence/admin (QueryPort 구현) | PR11 | legacy |
| 21 | `domain/admin/service/AdjustGwangsanService.java` | = | admin | service interface | application/admin/port/in (UseCase/Query) | PR11 | legacy |
| 22 | `domain/admin/service/ApproveTradeCancelService.java` | = | admin | service interface | application/admin/port/in (UseCase/Query) | PR11 | legacy |
| 23 | `domain/admin/service/CreateAdminAlertService.java` | = | admin | service interface | application/admin/port/in (UseCase/Query) | PR11 | legacy |
| 24 | `domain/admin/service/DeleteAdminAlertService.java` | = | admin | service interface | application/admin/port/in (UseCase/Query) | PR11 | legacy |
| 25 | `domain/admin/service/DeleteAdminProductService.java` | = | admin | service interface | application/admin/port/in (UseCase/Query) | PR11 | legacy |
| 26 | `domain/admin/service/FindAdminProductsService.java` | = | admin | service interface | application/admin/port/in (UseCase/Query) | PR11 | legacy |
| 27 | `domain/admin/service/FindAlertByAlertTypeAndPlaceService.java` | = | admin | service interface | application/admin/port/in (UseCase/Query) | PR11 | legacy |
| 28 | `domain/admin/service/RejectAdminAlertService.java` | = | admin | service interface | application/admin/port/in (UseCase/Query) | PR11 | legacy |
| 29 | `domain/admin/service/SignInAdminService.java` | = | admin | service interface | application/admin/port/in (UseCase/Query) | PR11 | legacy |
| 30 | `domain/admin/service/UpdateMemberRoleService.java` | = | admin | service interface | application/admin/port/in (UseCase/Query) | PR11 | legacy |
| 31 | `domain/admin/service/UpdateMemberStatusService.java` | = | admin | service interface | application/admin/port/in (UseCase/Query) | PR11 | legacy |
| 32 | `domain/admin/service/VerificationSignUpService.java` | = | admin | service interface | application/admin/port/in (UseCase/Query) | PR11 | legacy |
| 33 | `domain/admin/service/impl/AdjustGwangsanServiceImpl.java` | = | admin | service impl | application/admin/service (+ 필요한 output Port) | PR11 | legacy |
| 34 | `domain/admin/service/impl/ApproveTradeCancelServiceImpl.java` | = | admin | service impl | application/admin/service (+ 필요한 output Port) | PR11 | legacy |
| 35 | `domain/admin/service/impl/CreateAdminAlertServiceImpl.java` | = | admin | service impl | application/admin/service (+ 필요한 output Port) | PR11 | legacy |
| 36 | `domain/admin/service/impl/DeleteAdminAlertServiceImpl.java` | = | admin | service impl | application/admin/service (+ 필요한 output Port) | PR11 | legacy |
| 37 | `domain/admin/service/impl/DeleteAdminProductServiceImpl.java` | = | admin | service impl | application/admin/service (+ 필요한 output Port) | PR11 | legacy |
| 38 | `domain/admin/service/impl/FindAdminProductsServiceImpl.java` | = | admin | service impl | application/admin/service (+ 필요한 output Port) | PR11 | legacy |
| 39 | `domain/admin/service/impl/FindAlertByAlertTypeAndPlaceServiceImpl.java` | = | admin | service impl | application/admin/service (+ 필요한 output Port) | PR11 | legacy |
| 40 | `domain/admin/service/impl/RejectAdminAlertServiceImpl.java` | = | admin | service impl | application/admin/service (+ 필요한 output Port) | PR11 | legacy |
| 41 | `domain/admin/service/impl/SignInAdminServiceImpl.java` | = | admin | service impl | application/admin/service (+ 필요한 output Port) | PR11 | legacy |
| 42 | `domain/admin/service/impl/UpdateMemberRoleServiceImpl.java` | = | admin | service impl | application/admin/service (+ 필요한 output Port) | PR11 | legacy |
| 43 | `domain/admin/service/impl/UpdateMemberStatusServiceImpl.java` | = | admin | service impl | application/admin/service (+ 필요한 output Port) | PR11 | legacy |
| 44 | `domain/admin/service/impl/VerificationSignUpServiceImpl.java` | = | admin | service impl | application/admin/service (+ 필요한 output Port) | PR11 | legacy |
| 45 | `domain/admin/util/ValidatePlaceUtil.java` | = | admin | util | domain/admin/policy (순수 인가 정책) | PR11 | legacy |
| 46 | `domain/alert/entity/Alert.java` | `adapter/persistence/alert/AlertJpaEntity.java` + `domain/alert/model/Alert.java` | alert | jpa entity | adapter/persistence/alert (JpaEntity) + domain/alert/model (순수 모델, 규칙 있을 때) | PR04 | PR04 완료 |
| 47 | `domain/alert/entity/AlertReceipt.java` | `adapter/persistence/alert/AlertReceiptJpaEntity.java` | alert | jpa entity | adapter/persistence/alert (JpaEntity) + domain/alert/model (순수 모델, 규칙 있을 때) | PR04 | PR04 완료 |
| 48 | `domain/alert/entity/constant/AlertType.java` | `domain/alert/model/AlertType.java` | alert | enum | domain/alert/model | PR04 | PR04 완료 |
| 49 | `domain/alert/presentation/AlertController.java` | `adapter/web/alert/controller/AlertController.java` | alert | controller | adapter/web/alert/controller | PR04 | PR04 완료 |
| 50 | `domain/alert/presentation/dto/response/ExistsAlertResponse.java` | `adapter/web/alert/dto/ExistsAlertResponse.java` | alert | web dto | adapter/web/alert/dto | PR04 | PR04 완료 |
| 51 | `domain/alert/presentation/dto/response/GetAlertResponse.java` | `adapter/web/alert/dto/GetAlertResponse.java` + `AlertImageResponse.java` | alert | web dto | adapter/web/alert/dto | PR04 | PR04 완료 |
| 52 | `domain/alert/repository/AlertReceiptRepository.java` | `adapter/persistence/alert/AlertReceiptJpaRepository.java` | alert | repository | adapter/persistence/alert (JpaRepository) + application/alert/port/out | PR04 | PR04 완료 |
| 53 | `domain/alert/repository/AlertRepository.java` | `adapter/persistence/alert/AlertJpaRepository.java` | alert | repository | adapter/persistence/alert (JpaRepository) + application/alert/port/out | PR04 | PR04 완료 |
| 54 | `domain/alert/repository/custom/AlertReceiptCustomRepository.java` | `adapter/persistence/alert/AlertPersistenceAdapter.java` | alert | querydsl | adapter/persistence/alert (QueryPort 구현) | PR04 | PR04 완료 (삭제·Adapter 흡수) |
| 55 | `domain/alert/repository/custom/impl/AlertReceiptCustomRepositoryImpl.java` | `adapter/persistence/alert/AlertPersistenceAdapter.java` | alert | querydsl | adapter/persistence/alert (QueryPort 구현) | PR04 | PR04 완료 (삭제·Adapter 흡수) |
| 56 | `domain/alert/service/CreateAlertMembersService.java` | `application/alert/port/in/CreateAlertFromSourceUseCase.java` | alert | service interface | application/alert/port/in (UseCase/Query) | PR04 | PR04 완료 |
| 57 | `domain/alert/service/CreateAlertService.java` | `application/alert/port/in/RecordAlertUseCase.java` + `CreateAlertFromSourceUseCase.java` | alert | service interface | application/alert/port/in (UseCase/Query) | PR04 | PR04 완료 |
| 58 | `domain/alert/service/ExistsUnreadAlertService.java` | `application/alert/port/in/UnreadAlertQuery.java` | alert | service interface | application/alert/port/in (UseCase/Query) | PR04 | PR04 완료 |
| 59 | `domain/alert/service/FindAlertByCurrentService.java` | `application/alert/port/in/MyAlertsQuery.java` | alert | service interface | application/alert/port/in (UseCase/Query) | PR04 | PR04 완료 |
| 60 | `domain/alert/service/ReadAlertService.java` | `application/alert/port/in/ReadAlertsUseCase.java` | alert | service interface | application/alert/port/in (UseCase/Query) | PR04 | PR04 완료 |
| 61 | `domain/alert/service/impl/CreateAlertMembersServiceImpl.java` | `application/alert/service/CreateAlertService.java` | alert | service impl | application/alert/service (+ 필요한 output Port) | PR04 | PR04 완료 |
| 62 | `domain/alert/service/impl/CreateAlertServiceImpl.java` | `application/alert/service/CreateAlertService.java` + `adapter/integration/alert/LegacyAlertSourceAdapter.java` | alert | service impl | application/alert/service (+ 필요한 output Port) | PR04 | PR04 완료 (원천 조회 bridge 는 각 업무 공개 조회로 교체 PR02/06/07/09/10, 최종 PR12) |
| 63 | `domain/alert/service/impl/ExistsUnreadAlertServiceImpl.java` | `application/alert/service/AlertQueryService.java` | alert | service impl | application/alert/service (+ 필요한 output Port) | PR04 | PR04 완료 |
| 64 | `domain/alert/service/impl/FindAlertByCurrentServiceImpl.java` | `application/alert/service/AlertQueryService.java` + `adapter/integration/alert/LegacyAlertImageAdapter.java` | alert | service impl | application/alert/service (+ 필요한 output Port) | PR04 | PR04 완료 (이미지 조회 bridge 제거 PR06/07/09/10) |
| 65 | `domain/alert/service/impl/ReadAlertServiceImpl.java` | `application/alert/service/ReadAlertsService.java` | alert | service impl | application/alert/service (+ 필요한 output Port) | PR04 | PR04 완료 |
| 66 | `domain/app/entity/AppVersion.java` | `adapter/persistence/app/AppVersionJpaEntity.java (+ domain/app/model/AppVersion.java, adapter/persistence/app/AppVersionPersistenceAdapter.java)` | app | jpa entity | adapter/persistence/app (JpaEntity) + domain/app/model (순수 모델, 규칙 있을 때) | PR03 | PR03 완료 |
| 67 | `domain/app/exception/AppVersionUnavailableException.java` | = (위치 유지) | app | exception | domain/app/exception | PR03 | PR03 완료 |
| 68 | `domain/app/presentation/AppVersionController.java` | `adapter/web/app/controller/AppVersionController.java` | app | controller | adapter/web/app/controller | PR03 | PR03 완료 |
| 69 | `domain/app/presentation/dto/response/AppVersionResponse.java` | `adapter/web/app/dto/AppVersionResponse.java` | app | web dto | adapter/web/app/dto | PR03 | PR03 완료 |
| 70 | `domain/app/repository/AppVersionRepository.java` | `adapter/persistence/app/AppVersionJpaRepository.java (+ application/app/port/out/LoadAppVersionPort.java)` | app | repository | adapter/persistence/app (JpaRepository) + application/app/port/out | PR03 | PR03 완료 |
| 71 | `domain/app/service/FindAppVersionService.java` | `application/app/port/in/FindAppVersionQuery.java` | app | service interface | application/app/port/in (UseCase/Query) | PR03 | PR03 완료 |
| 72 | `domain/app/service/impl/FindAppVersionServiceImpl.java` | `application/app/service/FindAppVersionService.java` | app | service impl | application/app/service (+ 필요한 output Port) | PR03 | PR03 완료 |
| 73 | `domain/auth/entity/RefreshToken.java` | `adapter/persistence/auth/RefreshToken.java` | auth | jpa entity | adapter/persistence/auth (JpaEntity) + domain/auth/model (순수 모델, 규칙 있을 때) | PR02 | 전환 (Redis hash, 구 _class 호환 테스트) |
| 74 | `domain/auth/entity/constant/OsType.java` | 삭제 | auth | enum | domain/auth/model | PR02 | 삭제 (참조 0건, 실제 사용은 notification OsType) |
| 75 | `domain/auth/exception/BannedPhoneNumberException.java` | = | auth | exception | domain/auth/exception | PR02 | 전환 (순수 domain 예외, 위치 유지) |
| 76 | `domain/auth/exception/DongNotFoundException.java` | = | auth | exception | domain/auth/exception | PR02 | 전환 (순수 domain 예외, 위치 유지) |
| 77 | `domain/auth/exception/DuplicateNicknameException.java` | = | auth | exception | domain/auth/exception | PR02 | 전환 (순수 domain 예외, 위치 유지) |
| 78 | `domain/auth/exception/DuplicatePhoneNumberException.java` | = | auth | exception | domain/auth/exception | PR02 | 전환 (순수 domain 예외, 위치 유지) |
| 79 | `domain/auth/exception/ForbiddenException.java` | = | auth | exception | domain/auth/exception | PR02 | 전환 (순수 domain 예외, 위치 유지) |
| 80 | `domain/auth/exception/InvalidVerificationCodeException.java` | = | auth | exception | domain/auth/exception | PR02 | 전환 (순수 domain 예외, 위치 유지) |
| 81 | `domain/auth/exception/NotFoundUserException.java` | = | auth | exception | domain/auth/exception | PR02 | 전환 (순수 domain 예외, 위치 유지) |
| 82 | `domain/auth/exception/PendingApprovalException.java` | = | auth | exception | domain/auth/exception | PR02 | 전환 (순수 domain 예외, 위치 유지) |
| 83 | `domain/auth/exception/PlaceNotFoundException.java` | = | auth | exception | domain/auth/exception | PR02 | 전환 (순수 domain 예외, 위치 유지) |
| 84 | `domain/auth/exception/RecommenderNotFoundException.java` | = | auth | exception | domain/auth/exception | PR02 | 전환 (순수 domain 예외, 위치 유지) |
| 85 | `domain/auth/exception/SmsAuthNotCompletedException.java` | = | auth | exception | domain/auth/exception | PR02 | 전환 (순수 domain 예외, 위치 유지) |
| 86 | `domain/auth/exception/SmsAuthNotFoundException.java` | = | auth | exception | domain/auth/exception | PR02 | 전환 (순수 domain 예외, 위치 유지) |
| 87 | `domain/auth/exception/UnauthorizedException.java` | = | auth | exception | domain/auth/exception | PR02 | 전환 (순수 domain 예외, 위치 유지) |
| 88 | `adapter/web/auth/controller/AuthController.java` | `adapter/web/auth/controller/AuthController.java` | auth | controller | adapter/web/auth/controller | PR02 | 전환 |
| 89 | `domain/auth/presentation/dto/request/FindNicknameRequest.java` | `adapter/web/auth/dto/request/FindNicknameRequest.java` | auth | web dto | adapter/web/auth/dto | PR02 | 전환 |
| 90 | `domain/auth/presentation/dto/request/ResetPasswordRequest.java` | `adapter/web/auth/dto/request/ResetPasswordRequest.java` | auth | web dto | adapter/web/auth/dto | PR02 | 전환 |
| 91 | `domain/auth/presentation/dto/request/SignInRequest.java` | `adapter/web/auth/dto/request/SignInRequest.java` | auth | web dto | adapter/web/auth/dto | PR02 | 전환 |
| 92 | `domain/auth/presentation/dto/request/SignUpRequest.java` | `adapter/web/auth/dto/request/SignUpRequest.java` | auth | web dto | adapter/web/auth/dto | PR02 | 전환 |
| 93 | `domain/auth/presentation/dto/response/FindNicknameResponse.java` | `adapter/web/auth/dto/response/FindNicknameResponse.java` | auth | web dto | adapter/web/auth/dto | PR02 | 전환 |
| 94 | `domain/auth/presentation/dto/response/MemberInfoResponse.java` | `adapter/web/auth/dto/response/MemberInfoResponse.java` | auth | web dto | adapter/web/auth/dto | PR02 | 전환 |
| 95 | `domain/auth/presentation/dto/response/TokenResponse.java` | `adapter/web/auth/dto/response/TokenResponse.java` | auth | web dto | adapter/web/auth/dto | PR02 | 전환 |
| 96 | `domain/auth/repository/RefreshTokenRepository.java` | `adapter/persistence/auth/RefreshTokenRepository.java` | auth | repository | adapter/persistence/auth (JpaRepository) + application/auth/port/out | PR02 | 전환 |
| 97 | `domain/auth/service/FindNicknameService.java` | `application/auth/port/in/FindNicknameQuery.java` | auth | service interface | application/auth/port/in (UseCase/Query) | PR02 | 전환 (input Port) |
| 98 | `domain/auth/service/ReissueTokenService.java` | `application/auth/port/in/ReissueTokenUseCase.java` | auth | service interface | application/auth/port/in (UseCase/Query) | PR02 | 전환 (input Port) |
| 99 | `domain/auth/service/ResetPasswordService.java` | `application/auth/port/in/ResetPasswordUseCase.java` | auth | service interface | application/auth/port/in (UseCase/Query) | PR02 | 전환 (input Port) |
| 100 | `domain/auth/service/SignInService.java` | `application/auth/port/in/SignInUseCase.java` | auth | service interface | application/auth/port/in (UseCase/Query) | PR02 | 전환 (input Port) |
| 101 | `domain/auth/service/SignOutService.java` | `application/auth/port/in/SignOutUseCase.java` | auth | service interface | application/auth/port/in (UseCase/Query) | PR02 | 전환 (input Port) |
| 102 | `domain/auth/service/SignUpService.java` | `application/auth/port/in/SignUpUseCase.java` | auth | service interface | application/auth/port/in (UseCase/Query) | PR02 | 전환 (input Port) |
| 103 | `domain/auth/service/TokenAuthenticationService.java` | `application/auth/port/in/CurrentMemberQuery.java` | auth | service interface | application/auth/port/in (UseCase/Query) | PR02 | 전환 (input Port) |
| 104 | `domain/auth/service/impl/FindNicknameServiceImpl.java` | `application/auth/service/FindNicknameService.java` | auth | service impl | application/auth/service (+ 필요한 output Port) | PR02 | 전환 |
| 105 | `domain/auth/service/impl/ReissueTokenServiceImpl.java` | `application/auth/service/ReissueTokenService.java` | auth | service impl | application/auth/service (+ 필요한 output Port) | PR02 | 전환 |
| 106 | `domain/auth/service/impl/ResetPasswordServiceImpl.java` | `application/auth/service/ResetPasswordService.java` | auth | service impl | application/auth/service (+ 필요한 output Port) | PR02 | 전환 |
| 107 | `domain/auth/service/impl/SignInServiceImpl.java` | `application/auth/service/SignInService.java` | auth | service impl | application/auth/service (+ 필요한 output Port) | PR02 | 전환 |
| 108 | `domain/auth/service/impl/SignOutServiceImpl.java` | `application/auth/service/SignOutService.java` | auth | service impl | application/auth/service (+ 필요한 output Port) | PR02 | 전환 |
| 109 | `domain/auth/service/impl/SignUpServiceImpl.java` | `application/auth/service/SignUpService.java` | auth | service impl | application/auth/service (+ 필요한 output Port) | PR02 | 전환 |
| 110 | `domain/auth/service/impl/TokenAuthenticationServiceImpl.java` | `application/auth/service/CurrentMemberService.java` | auth | service impl | application/auth/service (+ 필요한 output Port) | PR02 | 전환 |
| 111 | `domain/block/entity/MemberBlock.java` | = | block | jpa entity | adapter/persistence/block (JpaEntity) + domain/block/model (순수 모델, 규칙 있을 때) | PR09 | legacy |
| 112 | `domain/block/exception/AlreadyBlockedException.java` | = | block | exception | domain/block/exception | PR09 | legacy |
| 113 | `domain/block/exception/BlockedMemberException.java` | = | block | exception | domain/block/exception | PR09 | legacy |
| 114 | `domain/block/exception/NotFoundBlockException.java` | = | block | exception | domain/block/exception | PR09 | legacy |
| 115 | `domain/block/exception/SelfBlockNotAllowedException.java` | = | block | exception | domain/block/exception | PR09 | legacy |
| 116 | `domain/block/presentation/BlockController.java` | = | block | controller | adapter/web/block/controller | PR09 | legacy |
| 117 | `domain/block/presentation/dto/response/GetBlockedMemberResponse.java` | = | block | web dto | adapter/web/block/dto | PR09 | legacy |
| 118 | `domain/block/repository/MemberBlockRepository.java` | = | block | repository | adapter/persistence/block (JpaRepository) + application/block/port/out | PR09 | legacy |
| 119 | `domain/block/repository/custom/MemberBlockCustomRepository.java` | = | block | querydsl | adapter/persistence/block (QueryPort 구현) | PR09 | legacy |
| 120 | `domain/block/repository/custom/impl/MemberBlockCustomRepositoryImpl.java` | = | block | querydsl | adapter/persistence/block (QueryPort 구현) | PR09 | legacy |
| 121 | `domain/block/service/BlockMemberService.java` | = | block | service interface | application/block/port/in (UseCase/Query) | PR09 | legacy |
| 122 | `domain/block/service/GetBlockListService.java` | = | block | service interface | application/block/port/in (UseCase/Query) | PR09 | legacy |
| 123 | `domain/block/service/UnblockMemberService.java` | = | block | service interface | application/block/port/in (UseCase/Query) | PR09 | legacy |
| 124 | `domain/block/service/impl/BlockMemberServiceImpl.java` | = | block | service impl | application/block/service (+ 필요한 output Port) | PR09 | legacy |
| 125 | `domain/block/service/impl/GetBlockListServiceImpl.java` | = | block | service impl | application/block/service (+ 필요한 output Port) | PR09 | legacy |
| 126 | `domain/block/service/impl/UnblockMemberServiceImpl.java` | = | block | service impl | application/block/service (+ 필요한 output Port) | PR09 | legacy |
| 127 | `domain/chat/entity/ChatMessage.java` | = | chat | jpa entity | adapter/persistence/chat (JpaEntity) + domain/chat/model (순수 모델, 규칙 있을 때) | PR08 | legacy |
| 128 | `domain/chat/entity/ChatMessageImage.java` | = | chat | jpa entity | adapter/persistence/chat (JpaEntity) + domain/chat/model (순수 모델, 규칙 있을 때) | PR08 | legacy |
| 129 | `domain/chat/entity/ChatRoom.java` | = | chat | jpa entity | adapter/persistence/chat (JpaEntity) + domain/chat/model (순수 모델, 규칙 있을 때) | PR08 | legacy |
| 130 | `domain/chat/entity/constant/MessageType.java` | = | chat | enum | domain/chat/model | PR08 | legacy |
| 131 | `domain/chat/exception/ChatMessageIdConflictException.java` | = | chat | exception | domain/chat/exception | PR08 | legacy |
| 132 | `domain/chat/exception/InvalidChatStreamPayloadException.java` | = | chat | exception | domain/chat/exception | PR08 | legacy |
| 133 | `domain/chat/exception/NotFoundChatMessageException.java` | = | chat | exception | domain/chat/exception | PR08 | legacy |
| 134 | `domain/chat/exception/NotFoundChatRoomException.java` | = | chat | exception | domain/chat/exception | PR08 | legacy |
| 135 | `domain/chat/presentation/ChatController.java` | = | chat | controller | adapter/web/chat/controller | PR08 | legacy |
| 136 | `domain/chat/presentation/dto/GetChatMessageDto.java` | = | chat | web dto | adapter/web/chat/dto | PR08 | legacy |
| 137 | `domain/chat/presentation/dto/GetChatProductDto.java` | = | chat | web dto | adapter/web/chat/dto | PR08 | legacy |
| 138 | `domain/chat/presentation/dto/GetRoomProductDto.java` | = | chat | web dto | adapter/web/chat/dto | PR08 | legacy |
| 139 | `domain/chat/presentation/dto/GetRoomsDto.java` | = | chat | web dto | adapter/web/chat/dto | PR08 | legacy |
| 140 | `domain/chat/presentation/dto/request/UpdateChatMessageRequest.java` | = | chat | web dto | adapter/web/chat/dto | PR08 | legacy |
| 141 | `domain/chat/presentation/dto/request/UpdateMessageCheckedRequest.java` | = | chat | web dto | adapter/web/chat/dto | PR08 | legacy |
| 142 | `domain/chat/presentation/dto/response/CreateChatRoomResponse.java` | = | chat | web dto | adapter/web/chat/dto | PR08 | legacy |
| 143 | `domain/chat/presentation/dto/response/GetChatMessageResponse.java` | = | chat | web dto | adapter/web/chat/dto | PR08 | legacy |
| 144 | `domain/chat/presentation/dto/response/GetChatMessagesResponse.java` | = | chat | web dto | adapter/web/chat/dto | PR08 | legacy |
| 145 | `domain/chat/presentation/dto/response/GetRoomIdResponse.java` | = | chat | web dto | adapter/web/chat/dto | PR08 | legacy |
| 146 | `domain/chat/presentation/dto/response/GetRoomMemberResponse.java` | = | chat | web dto | adapter/web/chat/dto | PR08 | legacy |
| 147 | `domain/chat/presentation/dto/response/GetRoomsResponse.java` | = | chat | web dto | adapter/web/chat/dto | PR08 | legacy |
| 148 | `domain/chat/presentation/dto/response/SaveChatMessageResponse.java` | = | chat | web dto | adapter/web/chat/dto | PR08 | legacy |
| 149 | `domain/chat/presentation/dto/response/UpdateChatMessageResponse.java` | = | chat | web dto | adapter/web/chat/dto | PR08 | legacy |
| 150 | `domain/chat/presentation/dto/response/ValidateChatSendableResponse.java` | = | chat | web dto | adapter/web/chat/dto | PR08 | legacy |
| 151 | `domain/chat/repository/ChatMessageImageRepository.java` | = | chat | repository | adapter/persistence/chat (JpaRepository) + application/chat/port/out | PR08 | legacy |
| 152 | `domain/chat/repository/ChatMessageRepository.java` | = | chat | repository | adapter/persistence/chat (JpaRepository) + application/chat/port/out | PR08 | legacy |
| 153 | `domain/chat/repository/ChatRoomRepository.java` | = | chat | repository | adapter/persistence/chat (JpaRepository) + application/chat/port/out | PR08 | legacy |
| 154 | `domain/chat/repository/custom/ChatMessageCustomRepository.java` | = | chat | querydsl | adapter/persistence/chat (QueryPort 구현) | PR08 | legacy |
| 155 | `domain/chat/repository/custom/ChatMessageImageCustomRepository.java` | = | chat | querydsl | adapter/persistence/chat (QueryPort 구현) | PR08 | legacy |
| 156 | `domain/chat/repository/custom/ChatRoomCustomRepository.java` | = | chat | querydsl | adapter/persistence/chat (QueryPort 구현) | PR08 | legacy |
| 157 | `domain/chat/repository/custom/impl/ChatMessageCustomRepositoryImpl.java` | = | chat | querydsl | adapter/persistence/chat (QueryPort 구현) | PR08 | legacy |
| 158 | `domain/chat/repository/custom/impl/ChatMessageImageCustomRepositoryImpl.java` | = | chat | querydsl | adapter/persistence/chat (QueryPort 구현) | PR08 | legacy |
| 159 | `domain/chat/repository/custom/impl/ChatRoomCustomRepositoryImpl.java` | = | chat | querydsl | adapter/persistence/chat (QueryPort 구현) | PR08 | legacy |
| 160 | `domain/chat/repository/projection/ChatRoomDto.java` | = | chat | repository | adapter/persistence/chat (JpaRepository) + application/chat/port/out | PR08 | legacy |
| 161 | `domain/chat/repository/projection/LatestMessageDto.java` | = | chat | repository | adapter/persistence/chat (JpaRepository) + application/chat/port/out | PR08 | legacy |
| 162 | `domain/chat/repository/projection/UnreadCountDto.java` | = | chat | repository | adapter/persistence/chat (JpaRepository) + application/chat/port/out | PR08 | legacy |
| 163 | `domain/chat/service/CreateChatRoomService.java` | = | chat | service interface | application/chat/port/in (UseCase/Query) | PR08 | legacy |
| 164 | `domain/chat/service/DeleteChatRoomService.java` | = | chat | service interface | application/chat/port/in (UseCase/Query) | PR08 | legacy |
| 165 | `domain/chat/service/FindChatMessageByRoomIdService.java` | = | chat | service interface | application/chat/port/in (UseCase/Query) | PR08 | legacy |
| 166 | `domain/chat/service/FindRoomIdByProductIdService.java` | = | chat | service interface | application/chat/port/in (UseCase/Query) | PR08 | legacy |
| 167 | `domain/chat/service/FindRoomsByCurrentUserService.java` | = | chat | service interface | application/chat/port/in (UseCase/Query) | PR08 | legacy |
| 168 | `domain/chat/service/ReadChatMessageService.java` | = | chat | service interface | application/chat/port/in (UseCase/Query) | PR08 | legacy |
| 169 | `domain/chat/service/SaveChatMessageService.java` | = | chat | service interface | application/chat/port/in (UseCase/Query) | PR08 | legacy |
| 170 | `domain/chat/service/ValidateChatSendableService.java` | = | chat | service interface | application/chat/port/in (UseCase/Query) | PR08 | legacy |
| 171 | `domain/chat/service/impl/ChatMessageMutationService.java` | = | chat | service impl | application/chat/service (+ 필요한 output Port) | PR08 | legacy |
| 172 | `domain/chat/service/impl/CreateChatRoomServiceImpl.java` | = | chat | service impl | application/chat/service (+ 필요한 output Port) | PR08 | legacy |
| 173 | `domain/chat/service/impl/DeleteChatRoomServiceImpl.java` | = | chat | service impl | application/chat/service (+ 필요한 output Port) | PR08 | legacy |
| 174 | `domain/chat/service/impl/FindChatMessageByRoomIdServiceImpl.java` | = | chat | service impl | application/chat/service (+ 필요한 output Port) | PR08 | legacy |
| 175 | `domain/chat/service/impl/FindRoomIdByProductIdServiceImpl.java` | = | chat | service impl | application/chat/service (+ 필요한 output Port) | PR08 | legacy |
| 176 | `domain/chat/service/impl/FindRoomsByCurrentUserServiceImpl.java` | = | chat | service impl | application/chat/service (+ 필요한 output Port) | PR08 | legacy |
| 177 | `domain/chat/service/impl/ReadChatMessageServiceImpl.java` | = | chat | service impl | application/chat/service (+ 필요한 output Port) | PR08 | legacy |
| 178 | `domain/chat/service/impl/SaveChatMessageServiceImpl.java` | = | chat | service impl | application/chat/service (+ 필요한 output Port) | PR08 | legacy |
| 179 | `domain/chat/service/impl/ValidateChatSendableServiceImpl.java` | = | chat | service impl | application/chat/service (+ 필요한 output Port) | PR08 | legacy |
| 180 | `domain/dong/entity/Dong.java` | `adapter/persistence/dong/DongJpaEntity.java` | dong | jpa entity | adapter/persistence/dong (JpaEntity) + domain/dong/model (순수 모델, 규칙 있을 때) | PR03 | PR03 완료 |
| 181 | `domain/dong/repository/DongRepository.java` | `adapter/persistence/dong/DongJpaRepository.java` (+ application/dong/port/out/LoadDongPort.java) | dong | repository | adapter/persistence/dong (JpaRepository) + application/dong/port/out | PR03 | PR03 완료 |
| 182 | `domain/health/presentation/HealthController.java` | `adapter/web/health/controller/HealthController.java` | health | controller | adapter/web/health/controller | PR03 | PR03 완료 |
| 183 | `domain/image/entity/Image.java` | = | image | jpa entity | adapter/persistence/image (JpaEntity) + domain/image/model (순수 모델, 규칙 있을 때) | PR05 | legacy 유지(bridge→PR12): 5개 업무 Entity association·legacy 서비스 사용. 순수 모델은 domain/image/model/Image |
| 184 | `domain/image/entity/dto/DeleteResult.java` | `application/image/data/DeleteResult.java` | image | jpa entity | adapter/persistence/image (JpaEntity) + domain/image/model (순수 모델, 규칙 있을 때) | PR05 | 이동 |
| 185 | `domain/image/exception/ImageDeleteFailedException.java` | = (순수 domain 예외 확인, shared kernel 만 의존) | image | exception | domain/image/exception | PR05 | 전환 |
| 186 | `domain/image/exception/ImageNotFoundException.java` | = (순수 domain 예외 확인, shared kernel 만 의존) | image | exception | domain/image/exception | PR05 | 전환 |
| 187 | `domain/image/exception/ImageUploadFailedException.java` | = (순수 domain 예외 확인, shared kernel 만 의존) | image | exception | domain/image/exception | PR05 | 전환 |
| 188 | `domain/image/exception/InappropriateImageException.java` | = (순수 domain 예외 확인, shared kernel 만 의존) | image | exception | domain/image/exception | PR05 | 전환 |
| 189 | `domain/image/exception/InvalidImageIdsException.java` | = (순수 domain 예외 확인, shared kernel 만 의존) | image | exception | domain/image/exception | PR05 | 전환 |
| 190 | `domain/image/exception/NotFoundImageIdsException.java` | = (순수 domain 예외 확인, shared kernel 만 의존) | image | exception | domain/image/exception | PR05 | 전환 |
| 191 | `domain/image/presentation/ImageController.java` | `adapter/web/image/controller/ImageController.java` | image | controller | adapter/web/image/controller | PR05 | 이동 |
| 192 | `domain/image/presentation/dto/request/UploadImageRequest.java` | (삭제: 미사용) | image | web dto | adapter/web/image/dto | PR05 | 삭제 |
| 193 | `domain/image/presentation/dto/response/GetImageResponse.java` | = | image | web dto | adapter/web/image/dto | PR05 | legacy 유지(bridge→각 소비 업무 PR/PR12): 타 업무 web DTO 약 40곳 공유 |
| 194 | `domain/image/presentation/dto/response/UploadImageResponse.java` | `adapter/web/image/dto/UploadImageResponse.java` | image | web dto | adapter/web/image/dto | PR05 | 이동 |
| 195 | `domain/image/repository/ImageRepository.java` | = | image | repository | adapter/persistence/image (JpaRepository) + application/image/port/out | PR05 | legacy 유지(bridge→PR12): ImagePersistenceAdapter 와 타 업무 legacy 서비스가 사용. custom 상속 제거 |
| 196 | `domain/image/repository/custom/ImageCustomRepository.java` | (삭제) → application/image/port/out/LoadOrphanImageCandidatesPort.java | image | querydsl | adapter/persistence/image (QueryPort 구현) | PR05 | 삭제 |
| 197 | `domain/image/repository/custom/impl/ImageCustomRepositoryImpl.java` | `adapter/persistence/image/OrphanImageQueryAdapter.java` | image | querydsl | adapter/persistence/image (QueryPort 구현) | PR05 | 이동 |
| 198 | `domain/image/service/DeleteImageService.java` | `application/image/port/in/DeleteImageUseCase.java` | image | service interface | application/image/port/in (UseCase/Query) | PR05 | 전환 |
| 199 | `domain/image/service/UploadImageService.java` | `application/image/port/in/UploadImageUseCase.java` | image | service interface | application/image/port/in (UseCase/Query) | PR05 | 전환 |
| 200 | `domain/image/service/impl/DeleteImageServiceImpl.java` | `application/image/service/DeleteImageService.java` | image | service impl | application/image/service (+ 필요한 output Port) | PR05 | 전환 |
| 201 | `domain/image/service/impl/UploadImageServiceImpl.java` | `application/image/service/UploadImageService.java` | image | service impl | application/image/service (+ 필요한 output Port) | PR05 | 전환 |
| 202 | `domain/member/entity/Member.java` | `adapter/persistence/member/MemberJpaEntity.java + domain/member/model/Member.java` | member | jpa entity | adapter/persistence/member (JpaEntity) + domain/member/model (순수 모델, 규칙 있을 때) | PR02 | 전환 (@Entity(name) 유지) |
| 203 | `domain/member/entity/MemberDetail.java` | `adapter/persistence/member/MemberDetailJpaEntity.java + domain/member/model/MemberDetail.java` | member | jpa entity | adapter/persistence/member (JpaEntity) + domain/member/model (순수 모델, 규칙 있을 때) | PR02 | 전환 (@Entity(name) 유지) |
| 204 | `domain/member/entity/WithdrawalRecord.java` | `adapter/persistence/member/WithdrawalRecordJpaEntity.java` | member | jpa entity | adapter/persistence/member (JpaEntity) + domain/member/model (순수 모델, 규칙 있을 때) | PR02 | 전환 (@Entity(name) 유지) |
| 205 | `domain/member/entity/constant/MemberRole.java` | `domain/member/model/MemberRole.java` | member | enum | domain/member/model | PR02 | 전환 |
| 206 | `domain/member/entity/constant/MemberStatus.java` | `domain/member/model/MemberStatus.java` | member | enum | domain/member/model | PR02 | 전환 |
| 207 | `domain/member/exception/DummyMemberDeletionNotAllowedException.java` | = | member | exception | domain/member/exception | PR02 | 전환 (순수 domain 예외, 위치 유지) |
| 208 | `domain/member/exception/NotAllowedUserAccessException.java` | = | member | exception | domain/member/exception | PR02 | 전환 (순수 domain 예외, 위치 유지) |
| 209 | `domain/member/exception/NotFoundDummyMemberException.java` | = | member | exception | domain/member/exception | PR02 | 전환 (순수 domain 예외, 위치 유지) |
| 210 | `domain/member/exception/NotFoundMemberDetailException.java` | = | member | exception | domain/member/exception | PR02 | 전환 (순수 domain 예외, 위치 유지) |
| 211 | `domain/member/exception/NotFoundMemberException.java` | = | member | exception | domain/member/exception | PR02 | 전환 (순수 domain 예외, 위치 유지) |
| 212 | `adapter/web/member/controller/MemberController.java` | `adapter/web/member/controller/MemberController.java` | member | controller | adapter/web/member/controller | PR02 | 전환 |
| 213 | `domain/member/peresentation/dto/request/UpdateMyInfoRequest.java` | `adapter/web/member/dto/request/UpdateMyInfoRequest.java` | member | web dto | adapter/web/member/dto | PR02 | 전환 |
| 214 | `domain/member/peresentation/dto/response/FindAllUserInfoResponse.java` | `adapter/web/member/dto/response/FindAllUserInfoResponse.java` | member | web dto | adapter/web/member/dto | PR02 | 전환 |
| 215 | `domain/member/peresentation/dto/response/FindMyInfoResponse.java` | `adapter/web/member/dto/response/FindMyInfoResponse.java` | member | web dto | adapter/web/member/dto | PR02 | 전환 |
| 216 | `domain/member/peresentation/dto/response/FindUserInfoResponse.java` | `adapter/web/member/dto/response/FindUserInfoResponse.java` | member | web dto | adapter/web/member/dto | PR02 | 전환 |
| 217 | `domain/member/repository/MemberDetailRepository.java` | `adapter/persistence/member/MemberDetailJpaRepository.java` | member | repository | adapter/persistence/member (JpaRepository) + application/member/port/out | PR02 | 전환 |
| 218 | `domain/member/repository/MemberRepository.java` | `adapter/persistence/member/MemberJpaRepository.java` | member | repository | adapter/persistence/member (JpaRepository) + application/member/port/out | PR02 | 전환 |
| 219 | `domain/member/repository/WithdrawalRecordRepository.java` | `adapter/persistence/member/WithdrawalRecordJpaRepository.java` | member | repository | adapter/persistence/member (JpaRepository) + application/member/port/out | PR02 | 전환 |
| 220 | `domain/member/repository/custom/MemberCustomRepository.java` | `adapter/persistence/member/MemberCustomRepository.java` | member | querydsl | adapter/persistence/member (QueryPort 구현) | PR02 | 전환 |
| 221 | `domain/member/repository/custom/MemberDetailCustomRepository.java` | `adapter/persistence/member/MemberDetailCustomRepository.java` | member | querydsl | adapter/persistence/member (QueryPort 구현) | PR02 | 전환 |
| 222 | `domain/member/repository/custom/impl/MemberCustomRepositoryImpl.java` | `adapter/persistence/member/MemberCustomRepositoryImpl.java` | member | querydsl | adapter/persistence/member (QueryPort 구현) | PR02 | 전환 |
| 223 | `domain/member/repository/custom/impl/MemberDetailCustomRepositoryImpl.java` | `adapter/persistence/member/MemberDetailCustomRepositoryImpl.java` | member | querydsl | adapter/persistence/member (QueryPort 구현) | PR02 | 전환 |
| 224 | `domain/member/service/FindAllUserInfoService.java` | `application/member/port/in/FindAllUserInfoQuery.java` | member | service interface | application/member/port/in (UseCase/Query) | PR02 | 전환 (input Port) |
| 225 | `domain/member/service/FindMyInfoService.java` | `application/member/port/in/FindMyInfoQuery.java` | member | service interface | application/member/port/in (UseCase/Query) | PR02 | 전환 (input Port) |
| 226 | `domain/member/service/FindUserInfoService.java` | `application/member/port/in/FindUserInfoQuery.java` | member | service interface | application/member/port/in (UseCase/Query) | PR02 | 전환 (input Port) |
| 227 | `domain/member/service/MemberDeletionService.java` | = | member | service interface | application/member/port/in (UseCase/Query) | PR02 | legacy bridge (탈퇴·삭제, PR11 교체) |
| 228 | `domain/member/service/MemberWithdrawalService.java` | = | member | service interface | application/member/port/in (UseCase/Query) | PR02 | legacy bridge (탈퇴·삭제, PR11 교체) |
| 229 | `domain/member/service/UpdateMyInfoService.java` | `application/member/port/in/UpdateMyInfoUseCase.java` | member | service interface | application/member/port/in (UseCase/Query) | PR02 | 전환 (input Port) |
| 230 | `domain/member/service/impl/FindAllUserInfoServiceImpl.java` | `application/member/service/FindAllUserInfoService.java` | member | service impl | application/member/service (+ 필요한 output Port) | PR02 | 전환 |
| 231 | `domain/member/service/impl/FindMyInfoServiceImpl.java` | `application/member/service/FindMyInfoService.java` | member | service impl | application/member/service (+ 필요한 output Port) | PR02 | 전환 |
| 232 | `domain/member/service/impl/FindUserInfoServiceImpl.java` | `application/member/service/FindUserInfoService.java` | member | service impl | application/member/service (+ 필요한 output Port) | PR02 | 전환 |
| 233 | `domain/member/service/impl/MemberDeletionServiceImpl.java` | = | member | service impl | application/member/service (+ 필요한 output Port) | PR02 | legacy bridge (탈퇴·삭제, PR11 교체) |
| 234 | `domain/member/service/impl/MemberWithdrawalServiceImpl.java` | = | member | service impl | application/member/service (+ 필요한 output Port) | PR02 | legacy bridge (탈퇴·삭제, PR11 교체) |
| 235 | `domain/member/service/impl/UpdateMyInfoServiceImpl.java` | `application/member/service/UpdateMyInfoService.java` | member | service impl | application/member/service (+ 필요한 output Port) | PR02 | 전환 |
| 236 | `domain/notice/entity/Notice.java` | = | notice | jpa entity | adapter/persistence/notice (JpaEntity) + domain/notice/model (순수 모델, 규칙 있을 때) | PR10 | legacy |
| 237 | `domain/notice/entity/NoticeImage.java` | = | notice | jpa entity | adapter/persistence/notice (JpaEntity) + domain/notice/model (순수 모델, 규칙 있을 때) | PR10 | legacy |
| 238 | `domain/notice/exception/NoticeForbiddenException.java` | = | notice | exception | domain/notice/exception | PR10 | legacy |
| 239 | `domain/notice/exception/NoticeNotFoundException.java` | = | notice | exception | domain/notice/exception | PR10 | legacy |
| 240 | `domain/notice/exception/UnauthorizedNoticeAccessException.java` | = | notice | exception | domain/notice/exception | PR10 | legacy |
| 241 | `domain/notice/presentation/NoticeController.java` | = | notice | controller | adapter/web/notice/controller | PR10 | legacy |
| 242 | `domain/notice/presentation/dto/request/CreateNoticeRequest.java` | = | notice | web dto | adapter/web/notice/dto | PR10 | legacy |
| 243 | `domain/notice/presentation/dto/request/UpdateNoticeRequest.java` | = | notice | web dto | adapter/web/notice/dto | PR10 | legacy |
| 244 | `domain/notice/presentation/dto/response/FindAllNoticeResponse.java` | = | notice | web dto | adapter/web/notice/dto | PR10 | legacy |
| 245 | `domain/notice/presentation/dto/response/FindNoticeResponse.java` | = | notice | web dto | adapter/web/notice/dto | PR10 | legacy |
| 246 | `domain/notice/repository/NoticeImageRepository.java` | = | notice | repository | adapter/persistence/notice (JpaRepository) + application/notice/port/out | PR10 | legacy |
| 247 | `domain/notice/repository/NoticeRepository.java` | = | notice | repository | adapter/persistence/notice (JpaRepository) + application/notice/port/out | PR10 | legacy |
| 248 | `domain/notice/repository/custom/NoticeImageCustomRepository.java` | = | notice | querydsl | adapter/persistence/notice (QueryPort 구현) | PR10 | legacy |
| 249 | `domain/notice/repository/custom/impl/NoticeImageCustomRepositoryImpl.java` | = | notice | querydsl | adapter/persistence/notice (QueryPort 구현) | PR10 | legacy |
| 250 | `domain/notice/service/CreateNoticeService.java` | = | notice | service interface | application/notice/port/in (UseCase/Query) | PR10 | legacy |
| 251 | `domain/notice/service/DeleteNoticeService.java` | = | notice | service interface | application/notice/port/in (UseCase/Query) | PR10 | legacy |
| 252 | `domain/notice/service/FindAllNoticeService.java` | = | notice | service interface | application/notice/port/in (UseCase/Query) | PR10 | legacy |
| 253 | `domain/notice/service/FindNoticeService.java` | = | notice | service interface | application/notice/port/in (UseCase/Query) | PR10 | legacy |
| 254 | `domain/notice/service/UpdateNoticeService.java` | = | notice | service interface | application/notice/port/in (UseCase/Query) | PR10 | legacy |
| 255 | `domain/notice/service/impl/CreateNoticeServiceImpl.java` | = | notice | service impl | application/notice/service (+ 필요한 output Port) | PR10 | legacy |
| 256 | `domain/notice/service/impl/DeleteNoticeServiceImpl.java` | = | notice | service impl | application/notice/service (+ 필요한 output Port) | PR10 | legacy |
| 257 | `domain/notice/service/impl/FindAllNoticeServiceImpl.java` | = | notice | service impl | application/notice/service (+ 필요한 output Port) | PR10 | legacy |
| 258 | `domain/notice/service/impl/FindNoticeServiceImpl.java` | = | notice | service impl | application/notice/service (+ 필요한 output Port) | PR10 | legacy |
| 259 | `domain/notice/service/impl/UpdateNoticeServiceImpl.java` | = | notice | service impl | application/notice/service (+ 필요한 output Port) | PR10 | legacy |
| 260 | `domain/notification/NotificationPort.java` | `(삭제)` | notification | port | application/notification/port/out | PR04 | PR06 완료: 예약 알림이 SendPushUseCase 직접 호출, LegacyNotificationPortBridge 와 함께 삭제 |
| 261 | `domain/notification/entity/DeviceToken.java` | `adapter/persistence/notification/DeviceToken.java` | notification | jpa entity | adapter/persistence/notification (JpaEntity) + domain/notification/model (순수 모델, 규칙 있을 때) | PR04 | PR04 완료 (@TypeAlias 로 _class 유지) |
| 262 | `domain/notification/entity/constant/NotificationType.java` | `domain/notification/model/NotificationType.java` | notification | enum | domain/notification/model | PR04 | PR04 완료 |
| 263 | `domain/notification/entity/constant/OsType.java` | `domain/notification/model/OsType.java` | notification | enum | domain/notification/model | PR04 | PR04 완료 |
| 264 | `domain/notification/repository/DeviceTokenRepository.java` | `adapter/persistence/notification/DeviceTokenRepository.java` | notification | repository | adapter/persistence/notification (JpaRepository) + application/notification/port/out | PR04 | PR04 완료 (legacy 호출자 조회 직접 사용 PR06/07/08/10) |
| 265 | `domain/notification/service/SendNotificationService.java` | `application/notification/port/in/SendPushUseCase.java` | notification | service interface | application/notification/port/in (UseCase/Query) | PR04 | PR04 완료 |
| 266 | `domain/notification/service/impl/SendNotificationServiceImpl.java` | `application/notification/service/SendPushService.java` | notification | service impl | application/notification/service (+ 필요한 output Port) | PR04 | PR04 완료 |
| 267 | `domain/place/entity/Head.java` | = | place | jpa entity | adapter/persistence/place (JpaEntity) + domain/place/model (순수 모델, 규칙 있을 때) | PR12 | legacy 유지 — Place association·QueryDSL 호출자(PR02·06·07·09·10·11) 전환 후 PR12 최종 단계에서 PR03 이 이동(legacy 태그 PR12) |
| 268 | `domain/place/entity/Place.java` | = | place | jpa entity | adapter/persistence/place (JpaEntity) + domain/place/model (순수 모델, 규칙 있을 때) | PR12 | legacy 유지 — 위와 같음, PlaceQuery/PlacePersistenceAdapter 가 내부 사용(legacy 태그 PR12) |
| 269 | `domain/place/exception/PlaceMismatchException.java` | = (위치 유지) | place | exception | domain/place/exception | PR03 | PR03 완료 |
| 270 | `domain/place/presentation/dto/PlaceDto.java` | (삭제) → adapter/web/trade/dto/TradePlaceResponse.java (같은 JSON id·name) | place | web dto | adapter/web/place/dto | PR07 | PR07 완료 |
| 271 | `domain/place/repository/PlaceRepository.java` | = | place | repository | adapter/persistence/place (JpaRepository) + application/place/port/out | PR12 | legacy 유지 — PlacePersistenceAdapter 내부 사용(+findAllByHeadId), 호출자(PR07·10·11) 전환 후 PR12 최종 단계에서 이동(legacy 태그 PR12) |
| 272 | `domain/post/entity/Product.java` | `adapter/persistence/post/Product.java` | post | jpa entity | adapter/persistence/post/ (저장 모델, 이름 유지) + domain/post/model/Product (순수 모델) | PR06 | PR06 완료 (타 업무 legacy 호출자는 import만 갱신) |
| 273 | `domain/post/entity/ProductImage.java` | `adapter/persistence/post/ProductImage.java` | post | jpa entity | adapter/persistence/post/ (저장 모델) | PR06 | PR06 완료 (타 업무 legacy 호출자는 import만 갱신) |
| 274 | `domain/post/entity/ProductReservation.java` | `adapter/persistence/post/ProductReservation.java` | post | jpa entity | adapter/persistence/post/ (저장 모델) + domain/post/model/ProductReservation (순수 모델) | PR06 | PR06 완료 (타 업무 legacy 호출자는 import만 갱신) |
| 275 | `domain/post/entity/constant/Mode.java` | `domain/post/model/Mode.java` | post | enum | domain/post/model | PR06 | PR06 완료 |
| 276 | `domain/post/entity/constant/ProductStatus.java` | `domain/post/model/ProductStatus.java` | post | enum | domain/post/model | PR06 | PR06 완료 |
| 277 | `domain/post/entity/constant/ReservationStatus.java` | `domain/post/model/ReservationStatus.java` | post | enum | domain/post/model | PR06 | PR06 완료 |
| 278 | `domain/post/entity/constant/Type.java` | `domain/post/model/Type.java` | post | enum | domain/post/model | PR06 | PR06 완료 |
| 279 | `domain/post/exception/ForbiddenProductException.java` | = | post | exception | domain/post/exception | PR06 | PR06 완료 (목표 위치와 같음) |
| 280 | `domain/post/exception/InappropriateContentException.java` | = | post | exception | domain/post/exception | PR06 | PR06 완료 (목표 위치와 같음) |
| 281 | `domain/post/exception/NotFoundProductException.java` | = | post | exception | domain/post/exception | PR06 | PR06 완료 (목표 위치와 같음) |
| 282 | `domain/post/exception/ObjectRequiredImageException.java` | = | post | exception | domain/post/exception | PR06 | PR06 완료 (목표 위치와 같음) |
| 283 | `domain/post/exception/ProductAlreadyReservationException.java` | = | post | exception | domain/post/exception | PR06 | PR06 완료 (목표 위치와 같음) |
| 284 | `domain/post/exception/ProductNotOngoingException.java` | = | post | exception | domain/post/exception | PR06 | PR06 완료 (목표 위치와 같음) |
| 285 | `domain/post/exception/ProductNotReservationException.java` | = | post | exception | domain/post/exception | PR06 | PR06 완료 (목표 위치와 같음) |
| 286 | `domain/post/exception/ReservationParticipantOnlyException.java` | = | post | exception | domain/post/exception | PR06 | PR06 완료 (목표 위치와 같음) |
| 287 | `domain/post/exception/ReservedProductDeletionException.java` | = | post | exception | domain/post/exception | PR06 | PR06 완료 (목표 위치와 같음) |
| 288 | `domain/post/presentation/PostController.java` | `adapter/web/post/controller/PostController.java` | post | controller | adapter/web/post/controller | PR06 | PR06 완료 (`/trade` 2개는 PR07 bridge) |
| 289 | `domain/post/presentation/dto/request/CreateProductRequest.java` | `adapter/web/post/dto/CreateProductRequest.java` | post | web dto | adapter/web/post/dto | PR06 | PR06 완료 |
| 290 | `domain/post/presentation/dto/request/PatchProductRequest.java` | `adapter/web/post/dto/PatchProductRequest.java` | post | web dto | adapter/web/post/dto | PR06 | PR06 완료 |
| 291 | `domain/post/presentation/dto/request/RequestTradeCompleteRequest.java` | `adapter/web/trade/dto/RequestTradeCompleteRequest.java` | post | web dto | adapter/web/post/dto | PR07 | PR07 완료 |
| 292 | `domain/post/presentation/dto/request/ReservationRequest.java` | `adapter/web/post/dto/ReservationRequest.java` | post | web dto | adapter/web/post/dto | PR06 | PR06 완료 |
| 293 | `domain/post/presentation/dto/request/WithdrawTradeCompleteRequest.java` | `adapter/web/trade/dto/WithdrawTradeCompleteRequest.java` | post | web dto | adapter/web/post/dto | PR07 | PR07 완료 |
| 294 | `domain/post/presentation/dto/response/GetProductByIdResponse.java` | `adapter/web/post/dto/GetProductByIdResponse.java` | post | web dto | adapter/web/post/dto | PR06 | PR06 완료 |
| 295 | `domain/post/presentation/dto/response/GetProductMemberResponse.java` | `adapter/web/post/dto/GetProductMemberResponse.java` | post | web dto | adapter/web/post/dto (legacy 사본 삭제, member QueryDSL projection·admin 호출자 import 갱신) | PR06 | PR06 완료 |
| 296 | `domain/post/presentation/dto/response/GetProductResponse.java` | `adapter/web/post/dto/GetProductResponse.java` | post | web dto | adapter/web/post/dto (legacy 사본 삭제, admin 호출자는 이미지 원소 타입만 ProductImageResponse 로 갱신) | PR06 | PR06 완료 |
| 297 | `domain/post/repository/ProductImageRepository.java` | `adapter/persistence/post/ProductImageRepository.java` | post | repository | adapter/persistence/post/ + application/post/port/out/ProductImagePort | PR06 | PR06 완료 |
| 298 | `domain/post/repository/ProductRepository.java` | `adapter/persistence/post/ProductRepository.java` | post | repository | adapter/persistence/post/ + application/post/port/out/ProductPort | PR06 | PR06 완료 |
| 299 | `domain/post/repository/ProductReservationRepository.java` | `adapter/persistence/post/ProductReservationRepository.java` | post | repository | adapter/persistence/post/ + application/post/port/out/ReservationPort (예약자 dummy 재지정 추가) | PR06 | PR06 완료 |
| 300 | `domain/post/repository/custom/ProductCustomRepository.java` | `adapter/persistence/post/ProductCustomRepository.java` | post | querydsl | adapter/persistence/post/ | PR06 | PR06 완료 (`findRoomProductsWithImagesByIds` 의 chat DTO 반환은 PR08 bridge 교체 시 삭제) |
| 301 | `domain/post/repository/custom/ProductImageCustomRepository.java` | `adapter/persistence/post/ProductImageCustomRepository.java` | post | querydsl | adapter/persistence/post/ | PR06 | PR06 완료 |
| 302 | `domain/post/repository/custom/impl/ProductCustomRepositoryImpl.java` | `adapter/persistence/post/ProductCustomRepositoryImpl.java` | post | querydsl | adapter/persistence/post/ | PR06 | PR06 완료 |
| 303 | `domain/post/repository/custom/impl/ProductImageCustomRepositoryImpl.java` | `adapter/persistence/post/ProductImageCustomRepositoryImpl.java` | post | querydsl | adapter/persistence/post/ | PR06 | PR06 완료 |
| 304 | `domain/post/service/CreateProductService.java` | `(삭제)` | post | service interface | application/post/port/in/CreateProductUseCase | PR06 | PR06 완료 |
| 305 | `domain/post/service/DeleteProductByIdService.java` | `(삭제)` | post | service interface | application/post/port/in/DeleteProductUseCase | PR06 | PR06 완료 |
| 306 | `domain/post/service/DeleteReservationProductService.java` | `(삭제)` | post | service interface | application/post/port/in/CancelReservationUseCase | PR06 | PR06 완료 |
| 307 | `domain/post/service/FindProductByCurrentUserAndTypeAndModeService.java` | `(삭제)` | post | service interface | application/post/port/in/MemberProductsQuery | PR06 | PR06 완료 |
| 308 | `domain/post/service/FindProductByIdService.java` | `(삭제)` | post | service interface | application/post/port/in/ProductDetailQuery | PR06 | PR06 완료 |
| 309 | `domain/post/service/FindProductsByMemberIdService.java` | `(삭제)` | post | service interface | application/post/port/in/MemberProductsQuery | PR06 | PR06 완료 |
| 310 | `domain/post/service/FindProductsByTypeAndModeService.java` | `(삭제)` | post | service interface | application/post/port/in/PlaceProductsQuery | PR06 | PR06 완료 |
| 311 | `domain/post/service/ProductContentValidationService.java` | `(삭제)` | post | service interface | application/post/port/in/ValidateProductContentUseCase | PR06 | PR06 완료 |
| 312 | `domain/post/service/RequestTradeCompleteService.java` | (삭제) → application/trade/port/in/RequestTradeCompletionUseCase.java | post | service interface | application/post/port/in (UseCase/Query) | PR07 | PR07 완료 |
| 313 | `domain/post/service/ReservationProductService.java` | `(삭제)` | post | service interface | application/post/port/in/ReserveProductUseCase | PR06 | PR06 완료 |
| 314 | `domain/post/service/UpdateProductService.java` | `(삭제)` | post | service interface | application/post/port/in/UpdateProductUseCase | PR06 | PR06 완료 |
| 315 | `domain/post/service/WithdrawTradeCompleteService.java` | (삭제) → application/trade/port/in/WithdrawTradeCompletionUseCase.java | post | service interface | application/post/port/in (UseCase/Query) | PR07 | PR07 완료 |
| 316 | `domain/post/service/impl/CreateProductServiceImpl.java` | `(삭제)` | post | service impl | application/post/service/CreateProductService | PR06 | PR06 완료 |
| 317 | `domain/post/service/impl/DeleteProductByIdServiceImpl.java` | `(삭제)` | post | service impl | application/post/service/DeleteProductService | PR06 | PR06 완료 |
| 318 | `domain/post/service/impl/DeleteReservationProductServiceImpl.java` | `(삭제)` | post | service impl | application/post/service/CancelReservationService | PR06 | PR06 완료 |
| 319 | `domain/post/service/impl/FindProductByCurrentUserAndTypeAndModeServiceImpl.java` | `(삭제)` | post | service impl | application/post/service/ProductListQueryService | PR06 | PR06 완료 |
| 320 | `domain/post/service/impl/FindProductByIdServiceImpl.java` | `(삭제)` | post | service impl | application/post/service/ProductDetailQueryService | PR06 | PR06 완료 |
| 321 | `domain/post/service/impl/FindProductsByMemberIdServiceImpl.java` | `(삭제)` | post | service impl | application/post/service/ProductListQueryService | PR06 | PR06 완료 |
| 322 | `domain/post/service/impl/FindProductsByTypeAndModeServiceImpl.java` | `(삭제)` | post | service impl | application/post/service/ProductListQueryService | PR06 | PR06 완료 |
| 323 | `domain/post/service/impl/ProductContentValidationServiceImpl.java` | `(삭제)` | post | service impl | application/post/service/ValidateProductContentService | PR06 | PR06 완료 |
| 324 | `domain/post/service/impl/RequestTradeCompleteServiceImpl.java` | (삭제) → application/trade/service/RequestTradeCompletionService.java | post | service impl | application/post/service (+ 필요한 output Port) | PR07 | PR07 완료 |
| 325 | `domain/post/service/impl/ReservationProductServiceImpl.java` | `(삭제)` | post | service impl | application/post/service/ReserveProductService | PR06 | PR06 완료 |
| 326 | `domain/post/service/impl/ReservationReminderSender.java` | `(삭제)` | post | service impl | application/post/service/ReservationReminderService | PR06 | PR06 완료 |
| 327 | `domain/post/service/impl/UpdateProductServiceImpl.java` | `(삭제)` | post | service impl | application/post/service/UpdateProductService | PR06 | PR06 완료 |
| 328 | `domain/post/service/impl/WithdrawTradeCompleteServiceImpl.java` | (삭제) → application/trade/service/WithdrawTradeCompletionService.java | post | service impl | application/post/service (+ 필요한 output Port) | PR07 | PR07 완료 |
| 329 | `domain/relatedkeyword/entity/MemberRelatedKeyword.java` | `adapter/persistence/relatedkeyword/MemberRelatedKeywordJpaEntity.java` | relatedkeyword | jpa entity | adapter/persistence/relatedkeyword (JpaEntity) + domain/relatedkeyword/model (순수 모델, 규칙 있을 때) | PR03 | PR03 완료 |
| 330 | `domain/relatedkeyword/entity/RelatedKeyword.java` | `adapter/persistence/relatedkeyword/RelatedKeywordJpaEntity.java` | relatedkeyword | jpa entity | adapter/persistence/relatedkeyword (JpaEntity) + domain/relatedkeyword/model (순수 모델, 규칙 있을 때) | PR03 | PR03 완료 |
| 331 | `domain/relatedkeyword/exception/MemberRelatedKeywordForbiddenException.java` | = (위치 유지) | relatedkeyword | exception | domain/relatedkeyword/exception | PR03 | PR03 완료 |
| 332 | `domain/relatedkeyword/exception/MemberRelatedKeywordNotFoundException.java` | = (위치 유지) | relatedkeyword | exception | domain/relatedkeyword/exception | PR03 | PR03 완료 |
| 333 | `adapter/web/relatedkeyword/controller/RelatedKeywordController.java` | `adapter/web/relatedkeyword/controller/RelatedKeywordController.java` | relatedkeyword | controller | adapter/web/relatedkeyword/controller | PR03 | PR03 완료 |
| 334 | `domain/relatedkeyword/presentation/dto/response/RelatedKeywordResponse.java` | `adapter/web/relatedkeyword/dto/RelatedKeywordResponse.java` | relatedkeyword | web dto | adapter/web/relatedkeyword/dto | PR03 | PR03 완료 |
| 335 | `domain/relatedkeyword/repository/MemberRelatedKeywordRepository.java` | `adapter/persistence/relatedkeyword/MemberRelatedKeywordJpaRepository.java` (+ application/relatedkeyword/port/out/RelatedKeywordPort.java) | relatedkeyword | repository | adapter/persistence/relatedkeyword (JpaRepository) + application/relatedkeyword/port/out | PR03 | PR03 완료 |
| 336 | `domain/relatedkeyword/repository/RelatedKeywordRepository.java` | `adapter/persistence/relatedkeyword/RelatedKeywordJpaRepository.java` | relatedkeyword | repository | adapter/persistence/relatedkeyword (JpaRepository) + application/relatedkeyword/port/out | PR03 | PR03 완료 |
| 337 | `domain/relatedkeyword/service/DeleteMemberRelatedKeywordService.java` | `application/relatedkeyword/port/in/DeleteMemberRelatedKeywordUseCase.java` | relatedkeyword | service interface | application/relatedkeyword/port/in (UseCase/Query) | PR03 | PR03 완료 |
| 338 | `domain/relatedkeyword/service/FindMyRelatedKeywordService.java` | `application/relatedkeyword/port/in/FindMemberRelatedKeywordsQuery.java` | relatedkeyword | service interface | application/relatedkeyword/port/in (UseCase/Query) | PR03 | PR03 완료 |
| 339 | `domain/relatedkeyword/service/FindRelatedKeywordService.java` | `application/relatedkeyword/port/in/FindRelatedKeywordsQuery.java` | relatedkeyword | service interface | application/relatedkeyword/port/in (UseCase/Query) | PR03 | PR03 완료 |
| 340 | `domain/relatedkeyword/service/impl/DeleteMemberRelatedKeywordServiceImpl.java` | `application/relatedkeyword/service/DeleteMemberRelatedKeywordService.java (+ domain/relatedkeyword/model/MemberKeyword.java)` | relatedkeyword | service impl | application/relatedkeyword/service (+ 필요한 output Port) | PR03 | PR03 완료 |
| 341 | `domain/relatedkeyword/service/impl/FindMyRelatedKeywordServiceImpl.java` | `application/relatedkeyword/service/FindMemberRelatedKeywordsService.java` | relatedkeyword | service impl | application/relatedkeyword/service (+ 필요한 output Port) | PR03 | PR03 완료 |
| 342 | `domain/relatedkeyword/service/impl/FindRelatedKeywordServiceImpl.java` | `application/relatedkeyword/service/FindRelatedKeywordsService.java (+ adapter/persistence/relatedkeyword/RelatedKeywordPersistenceAdapter.java)` | relatedkeyword | service impl | application/relatedkeyword/service (+ 필요한 output Port) | PR03 | PR03 완료 |
| 343 | `domain/report/entity/Report.java` | = | report | jpa entity | adapter/persistence/report (JpaEntity) + domain/report/model (순수 모델, 규칙 있을 때) | PR09 | legacy |
| 344 | `domain/report/entity/ReportImage.java` | = | report | jpa entity | adapter/persistence/report (JpaEntity) + domain/report/model (순수 모델, 규칙 있을 때) | PR09 | legacy |
| 345 | `domain/report/entity/constant/ReportTargetType.java` | = | report | enum | domain/report/model | PR09 | legacy |
| 346 | `domain/report/entity/constant/ReportType.java` | = | report | enum | domain/report/model | PR09 | legacy |
| 347 | `domain/report/exception/AlreadyReportedException.java` | = | report | exception | domain/report/exception | PR09 | legacy |
| 348 | `domain/report/exception/InvalidReportTypeException.java` | = | report | exception | domain/report/exception | PR09 | legacy |
| 349 | `domain/report/exception/NotFoundReportException.java` | = | report | exception | domain/report/exception | PR09 | legacy |
| 350 | `domain/report/exception/SelfReportNotAllowedException.java` | = | report | exception | domain/report/exception | PR09 | legacy |
| 351 | `domain/report/presentation/ReportController.java` | = | report | controller | adapter/web/report/controller | PR09 | legacy |
| 352 | `domain/report/presentation/dto/request/CreateReportRequest.java` | = | report | web dto | adapter/web/report/dto | PR09 | legacy |
| 353 | `domain/report/presentation/dto/response/GetReportResponse.java` | = | report | web dto | adapter/web/report/dto | PR09 | legacy |
| 354 | `domain/report/repository/ReportImageRepository.java` | = | report | repository | adapter/persistence/report (JpaRepository) + application/report/port/out | PR09 | legacy |
| 355 | `domain/report/repository/ReportRepository.java` | = | report | repository | adapter/persistence/report (JpaRepository) + application/report/port/out | PR09 | legacy |
| 356 | `domain/report/repository/custom/ReportCustomRepository.java` | = | report | querydsl | adapter/persistence/report (QueryPort 구현) | PR09 | legacy |
| 357 | `domain/report/repository/custom/ReportImageCustomRepository.java` | = | report | querydsl | adapter/persistence/report (QueryPort 구현) | PR09 | legacy |
| 358 | `domain/report/repository/custom/impl/ReportCustomRepositoryImpl.java` | = | report | querydsl | adapter/persistence/report (QueryPort 구현) | PR09 | legacy |
| 359 | `domain/report/repository/custom/impl/ReportImageCustomRepositoryImpl.java` | = | report | querydsl | adapter/persistence/report (QueryPort 구현) | PR09 | legacy |
| 360 | `domain/report/service/CreateReportService.java` | = | report | service interface | application/report/port/in (UseCase/Query) | PR09 | legacy |
| 361 | `domain/report/service/impl/CreateReportServiceImpl.java` | = | report | service impl | application/report/service (+ 필요한 output Port) | PR09 | legacy |
| 362 | `domain/review/entity/Review.java` | `adapter/persistence/review/ReviewJpaEntity.java` + `domain/review/model/Review.java` | review | jpa entity | adapter/persistence/review (JpaEntity) + domain/review/model (순수 모델, 규칙 있을 때) | PR07 | PR07 완료 |
| 363 | `domain/review/exception/AlreadyReviewedException.java` | = | review | exception | domain/review/exception | PR07 | PR07 완료 |
| 364 | `domain/review/exception/CannotReviewBeforeTradeException.java` | = | review | exception | domain/review/exception | PR07 | PR07 완료 |
| 365 | `domain/review/exception/CannotReviewSelfException.java` | = | review | exception | domain/review/exception | PR07 | PR07 완료 |
| 366 | `domain/review/exception/NotFoundReviewException.java` | = | review | exception | domain/review/exception | PR07 | PR07 완료 |
| 367 | `domain/review/exception/NotTradeParticipantException.java` | = | review | exception | domain/review/exception | PR07 | PR07 완료 |
| 368 | `domain/review/presentation/ReviewController.java` | `adapter/web/review/controller/ReviewController.java` | review | controller | adapter/web/review/controller | PR07 | PR07 완료 |
| 369 | `domain/review/presentation/dto/request/CreateReviewRequest.java` | `adapter/web/review/dto/CreateReviewRequest.java` | review | web dto | adapter/web/review/dto | PR07 | PR07 완료 |
| 370 | `domain/review/presentation/dto/response/ReviewDetailResponse.java` | `adapter/web/review/dto/ReviewDetailResponse.java` | review | web dto | adapter/web/review/dto | PR07 | PR07 완료 |
| 371 | `domain/review/presentation/dto/response/ReviewResponse.java` | `adapter/web/review/dto/ReviewResponse.java` | review | web dto | adapter/web/review/dto | PR07 | PR07 완료 |
| 372 | `domain/review/repository/ReviewRepository.java` | `adapter/persistence/review/ReviewJpaRepository.java` | review | repository | adapter/persistence/review (JpaRepository) + application/review/port/out | PR07 | PR07 완료 |
| 373 | `domain/review/repository/custom/ReviewCustomRepository.java` | `adapter/persistence/review/ReviewCustomRepository.java` | review | querydsl | adapter/persistence/review (QueryPort 구현) | PR07 | PR07 완료 |
| 374 | `domain/review/repository/custom/impl/ReviewCustomRepositoryImpl.java` | `adapter/persistence/review/ReviewCustomRepositoryImpl.java` | review | querydsl | adapter/persistence/review (QueryPort 구현) | PR07 | PR07 완료 |
| 375 | `domain/review/repository/projection/MyReviewDto.java` | `adapter/persistence/review/MyReviewDto.java` | review | repository | adapter/persistence/review (JpaRepository) + application/review/port/out | PR07 | PR07 완료 |
| 376 | `domain/review/repository/projection/ReceivedReviewDto.java` | `adapter/persistence/review/ReceivedReviewDto.java` | review | repository | adapter/persistence/review (JpaRepository) + application/review/port/out | PR07 | PR07 완료 |
| 377 | `domain/review/service/CreateReviewService.java` | (삭제) → application/review/port/in/CreateReviewUseCase.java | review | service interface | application/review/port/in (UseCase/Query) | PR07 | PR07 완료 |
| 378 | `domain/review/service/GetMyReviewListService.java` | (삭제) → application/review/port/in/ReviewQuery.java | review | service interface | application/review/port/in (UseCase/Query) | PR07 | PR07 완료 |
| 379 | `domain/review/service/GetReceivedReviewListService.java` | (삭제) → application/review/port/in/ReviewQuery.java | review | service interface | application/review/port/in (UseCase/Query) | PR07 | PR07 완료 |
| 380 | `domain/review/service/GetReviewByMemberService.java` | (삭제) → application/review/port/in/ReviewQuery.java | review | service interface | application/review/port/in (UseCase/Query) | PR07 | PR07 완료 |
| 381 | `domain/review/service/GetReviewDetailService.java` | (삭제) → application/review/port/in/ReviewQuery.java | review | service interface | application/review/port/in (UseCase/Query) | PR07 | PR07 완료 |
| 382 | `domain/review/service/impl/CreateReviewServiceImpl.java` | (삭제) → application/review/service/CreateReviewService.java | review | service impl | application/review/service (+ 필요한 output Port) | PR07 | PR07 완료 |
| 383 | `domain/review/service/impl/GetMyReviewListServiceImpl.java` | (삭제) → application/review/service/ReviewQueryService.java | review | service impl | application/review/service (+ 필요한 output Port) | PR07 | PR07 완료 |
| 384 | `domain/review/service/impl/GetReceivedReviewListServiceImpl.java` | (삭제) → application/review/service/ReviewQueryService.java | review | service impl | application/review/service (+ 필요한 output Port) | PR07 | PR07 완료 |
| 385 | `domain/review/service/impl/GetReviewByMemberServiceImpl.java` | (삭제) → application/review/service/ReviewQueryService.java | review | service impl | application/review/service (+ 필요한 output Port) | PR07 | PR07 완료 |
| 386 | `domain/review/service/impl/GetReviewDetailServiceImpl.java` | (삭제) → application/review/service/ReviewQueryService.java | review | service impl | application/review/service (+ 필요한 output Port) | PR07 | PR07 완료 |
| 387 | `domain/sms/exception/AlreadyRegisteredPhoneNumberException.java` | = | sms | exception | domain/sms/exception | PR02 | 전환 (순수 domain 예외, 위치 유지) |
| 388 | `domain/sms/exception/AuthCodeGenerationException.java` | = | sms | exception | domain/sms/exception | PR02 | 전환 (순수 domain 예외, 위치 유지) |
| 389 | `domain/sms/exception/NotMatchRandomCodeException.java` | = | sms | exception | domain/sms/exception | PR02 | 전환 (순수 domain 예외, 위치 유지) |
| 390 | `domain/sms/exception/NotRegisteredPhoneNumberException.java` | = | sms | exception | domain/sms/exception | PR02 | 전환 (순수 domain 예외, 위치 유지) |
| 391 | `domain/sms/exception/SmsSendFailedException.java` | = | sms | exception | domain/sms/exception | PR02 | 전환 (순수 domain 예외, 위치 유지) |
| 392 | `domain/sms/exception/TooManyRequestAuthCodeException.java` | = | sms | exception | domain/sms/exception | PR02 | 전환 (순수 domain 예외, 위치 유지) |
| 393 | `adapter/web/sms/controller/SmsController.java` | `adapter/web/sms/controller/SmsController.java` | sms | controller | adapter/web/sms/controller | PR02 | 전환 |
| 394 | `domain/sms/presentation/dto/SendSmsRequest.java` | `adapter/web/sms/dto/SendSmsRequest.java` | sms | web dto | adapter/web/sms/dto | PR02 | 전환 |
| 395 | `domain/sms/presentation/dto/VerifyCodeRequest.java` | `adapter/web/sms/dto/VerifyCodeRequest.java` | sms | web dto | adapter/web/sms/dto | PR02 | 전환 |
| 396 | `domain/sms/service/SendFindNicknameSmsService.java` | `application/sms/port/in/SendFindNicknameSmsUseCase.java` | sms | service interface | application/sms/port/in (UseCase/Query) | PR02 | 전환 (input Port) |
| 397 | `domain/sms/service/SendResetPasswordSmsService.java` | `application/sms/port/in/SendResetPasswordSmsUseCase.java` | sms | service interface | application/sms/port/in (UseCase/Query) | PR02 | 전환 (input Port) |
| 398 | `domain/sms/service/SendSmsService.java` | `application/sms/port/in/SendSmsUseCase.java` | sms | service interface | application/sms/port/in (UseCase/Query) | PR02 | 전환 (input Port) |
| 399 | `domain/sms/service/VerifyCodeService.java` | `application/sms/port/in/VerifyCodeUseCase.java` | sms | service interface | application/sms/port/in (UseCase/Query) | PR02 | 전환 (input Port) |
| 400 | `domain/sms/service/VerifyFindNicknameCodeService.java` | `application/sms/port/in/VerifyCodeUseCase.java` | sms | service interface | application/sms/port/in (UseCase/Query) | PR02 | 전환 (input Port) |
| 401 | `domain/sms/service/VerifyResetPasswordCodeService.java` | `application/sms/port/in/VerifyCodeUseCase.java` | sms | service interface | application/sms/port/in (UseCase/Query) | PR02 | 전환 (input Port) |
| 402 | `domain/sms/service/impl/SendFindNicknameSmsServiceImpl.java` | `application/sms/service/SendFindNicknameSmsService.java` | sms | service impl | application/sms/service (+ 필요한 output Port) | PR02 | 전환 |
| 403 | `domain/sms/service/impl/SendResetPasswordSmsServiceImpl.java` | `application/sms/service/SendResetPasswordSmsService.java` | sms | service impl | application/sms/service (+ 필요한 output Port) | PR02 | 전환 |
| 404 | `domain/sms/service/impl/SendSmsServiceImpl.java` | `application/sms/service/SendSmsService.java` | sms | service impl | application/sms/service (+ 필요한 output Port) | PR02 | 전환 |
| 405 | `domain/sms/service/impl/VerifyCodeServiceImpl.java` | `application/sms/service/VerifyCodeService.java` | sms | service impl | application/sms/service (+ 필요한 output Port) | PR02 | 전환 (3개 확인 서비스 통합) |
| 406 | `domain/sms/service/impl/VerifyFindNicknameCodeServiceImpl.java` | `application/sms/service/VerifyCodeService.java` | sms | service impl | application/sms/service (+ 필요한 output Port) | PR02 | 전환 (3개 확인 서비스 통합) |
| 407 | `domain/sms/service/impl/VerifyResetPasswordCodeServiceImpl.java` | `application/sms/service/VerifyCodeService.java` | sms | service impl | application/sms/service (+ 필요한 output Port) | PR02 | 전환 (3개 확인 서비스 통합) |
| 408 | `domain/suspend/entity/Suspend.java` | = | suspend | jpa entity | adapter/persistence/suspend (JpaEntity) + domain/suspend/model (순수 모델, 규칙 있을 때) | PR09 | legacy |
| 409 | `domain/suspend/exception/NotFoundSuspendException.java` | = | suspend | exception | domain/suspend/exception | PR09 | legacy |
| 410 | `domain/suspend/presentation/SuspendController.java` | = | suspend | controller | adapter/web/suspend/controller | PR09 | legacy |
| 411 | `domain/suspend/presentation/dto/request/SuspendMemberRequest.java` | = | suspend | web dto | adapter/web/suspend/dto | PR09 | legacy |
| 412 | `domain/suspend/repository/SuspendRepository.java` | = | suspend | repository | adapter/persistence/suspend (JpaRepository) + application/suspend/port/out | PR09 | legacy |
| 413 | `domain/suspend/service/SuspendMemberService.java` | = | suspend | service interface | application/suspend/port/in (UseCase/Query) | PR09 | legacy |
| 414 | `domain/suspend/service/impl/SuspendMemberServiceImpl.java` | = | suspend | service impl | application/suspend/service (+ 필요한 output Port) | PR09 | legacy |
| 415 | `domain/trade/entity/TradeCancel.java` | `adapter/persistence/trade/TradeCancelJpaEntity.java` + `domain/trade/model/TradeCancel.java` | trade | jpa entity | adapter/persistence/trade (JpaEntity) + domain/trade/model (순수 모델, 규칙 있을 때) | PR07 | PR07 완료 |
| 416 | `domain/trade/entity/TradeCancelImage.java` | `adapter/persistence/trade/TradeCancelImageJpaEntity.java` | trade | jpa entity | adapter/persistence/trade (JpaEntity) + domain/trade/model (순수 모델, 규칙 있을 때) | PR07 | PR07 완료 |
| 417 | `domain/trade/entity/TradeComplete.java` | `adapter/persistence/trade/TradeCompleteJpaEntity.java` + `domain/trade/model/TradeComplete.java` | trade | jpa entity | adapter/persistence/trade (JpaEntity) + domain/trade/model (순수 모델, 규칙 있을 때) | PR07 | PR07 완료 |
| 418 | `domain/trade/entity/constant/TradeCancelStatus.java` | `domain/trade/model/TradeCancelStatus.java` | trade | enum | domain/trade/model | PR07 | PR07 완료 |
| 419 | `domain/trade/entity/constant/TradeStatus.java` | `domain/trade/model/TradeStatus.java` | trade | enum | domain/trade/model | PR07 | PR07 완료 |
| 420 | `domain/trade/exception/AlreadyTradeCancelRequestException.java` | = | trade | exception | domain/trade/exception | PR07 | PR07 완료 |
| 421 | `domain/trade/exception/CannotPendingTradeCancelException.java` | = | trade | exception | domain/trade/exception | PR07 | PR07 완료 |
| 422 | `domain/trade/exception/CannotSelectSelfException.java` | = | trade | exception | domain/trade/exception | PR07 | PR07 완료 |
| 423 | `domain/trade/exception/InvalidTradeStatisticsPeriodException.java` | = | trade | exception | domain/trade/exception | PR07 | PR07 완료 |
| 424 | `domain/trade/exception/NotFoundTradeCancelException.java` | = | trade | exception | domain/trade/exception | PR07 | PR07 완료 |
| 425 | `domain/trade/exception/NotFoundTradeCompleteException.java` | = | trade | exception | domain/trade/exception | PR07 | PR07 완료 |
| 426 | `domain/trade/exception/NotTradeCancelRequesterException.java` | = | trade | exception | domain/trade/exception | PR07 | PR07 완료 |
| 427 | `domain/trade/exception/NotTradeCompleteRequesterException.java` | = | trade | exception | domain/trade/exception | PR07 | PR07 완료 |
| 428 | `domain/trade/exception/TradeAlreadyCompleteException.java` | = | trade | exception | domain/trade/exception | PR07 | PR07 완료 |
| 429 | `domain/trade/exception/TradeAlreadyCompleteRequestException.java` | = | trade | exception | domain/trade/exception | PR07 | PR07 완료 |
| 430 | `domain/trade/exception/TradeCompleteWithoutChattingException.java` | = | trade | exception | domain/trade/exception | PR07 | PR07 완료 |
| 431 | `domain/trade/exception/TradeParticipantOnlyException.java` | = | trade | exception | domain/trade/exception | PR07 | PR07 완료 |
| 432 | `domain/trade/presentation/TradeController.java` | `adapter/web/trade/controller/TradeController.java` | trade | controller | adapter/web/trade/controller | PR07 | PR07 완료 |
| 433 | `domain/trade/presentation/dto/request/TradeCancelRequest.java` | `adapter/web/trade/dto/TradeCancelRequest.java` | trade | web dto | adapter/web/trade/dto | PR07 | PR07 완료 |
| 434 | `domain/trade/presentation/dto/request/constant/Period.java` | `adapter/web/trade/dto/Period.java` | trade | web dto | adapter/web/trade/dto | PR07 | PR07 완료 |
| 435 | `domain/trade/presentation/dto/response/GetTradeHistoryResponse.java` | `adapter/web/trade/dto/GetTradeHistoryResponse.java` | trade | web dto | adapter/web/trade/dto | PR07 | PR07 완료 |
| 436 | `domain/trade/presentation/dto/response/GetTradeMemberResponse.java` | `adapter/web/trade/dto/GetTradeMemberResponse.java` | trade | web dto | adapter/web/trade/dto | PR07 | PR07 완료 |
| 437 | `domain/trade/presentation/dto/response/GetTradeProductResponse.java` | `adapter/web/trade/dto/GetTradeProductResponse.java` | trade | web dto | adapter/web/trade/dto | PR07 | PR07 완료 |
| 438 | `domain/trade/presentation/dto/response/HeadTradeHistoryResponse.java` | `adapter/web/trade/dto/HeadTradeHistoryResponse.java` | trade | web dto | adapter/web/trade/dto | PR07 | PR07 완료 |
| 439 | `domain/trade/presentation/dto/response/PlaceTradeHistoryResponse.java` | `adapter/web/trade/dto/PlaceTradeHistoryResponse.java` | trade | web dto | adapter/web/trade/dto | PR07 | PR07 완료 |
| 440 | `domain/trade/presentation/dto/response/TradeCancelResponse.java` | `adapter/web/trade/dto/TradeCancelResponse.java` | trade | web dto | adapter/web/trade/dto | PR07 | PR07 완료 |
| 441 | `domain/trade/presentation/dto/response/constant/TradeRole.java` | `adapter/web/trade/dto/TradeRole.java` | trade | web dto | adapter/web/trade/dto | PR07 | PR07 완료 |
| 442 | `domain/trade/repository/TradeCancelImageRepository.java` | `adapter/persistence/trade/TradeCancelImageJpaRepository.java` | trade | repository | adapter/persistence/trade (JpaRepository) + application/trade/port/out | PR07 | PR07 완료 |
| 443 | `domain/trade/repository/TradeCancelRepository.java` | `adapter/persistence/trade/TradeCancelJpaRepository.java` | trade | repository | adapter/persistence/trade (JpaRepository) + application/trade/port/out | PR07 | PR07 완료 |
| 444 | `domain/trade/repository/TradeCompleteRepository.java` | `adapter/persistence/trade/TradeCompleteJpaRepository.java` | trade | repository | adapter/persistence/trade (JpaRepository) + application/trade/port/out | PR07 | PR07 완료 |
| 445 | `domain/trade/repository/custom/TradeCancelCustomRepository.java` | `adapter/persistence/trade/TradeCancelCustomRepository.java` | trade | querydsl | adapter/persistence/trade (QueryPort 구현) | PR07 | PR07 완료 |
| 446 | `domain/trade/repository/custom/TradeCancelImageCustomRepository.java` | `adapter/persistence/trade/TradeCancelImageCustomRepository.java` | trade | querydsl | adapter/persistence/trade (QueryPort 구현) | PR07 | PR07 완료 |
| 447 | `domain/trade/repository/custom/TradeCompleteCustomRepository.java` | `adapter/persistence/trade/TradeCompleteCustomRepository.java` | trade | querydsl | adapter/persistence/trade (QueryPort 구현) | PR07 | PR07 완료 |
| 448 | `domain/trade/repository/custom/impl/TradeCancelCustomRepositoryImpl.java` | `adapter/persistence/trade/TradeCancelCustomRepositoryImpl.java` | trade | querydsl | adapter/persistence/trade (QueryPort 구현) | PR07 | PR07 완료 |
| 449 | `domain/trade/repository/custom/impl/TradeCancelImageCustomRepositoryImpl.java` | `adapter/persistence/trade/TradeCancelImageCustomRepositoryImpl.java` | trade | querydsl | adapter/persistence/trade (QueryPort 구현) | PR07 | PR07 완료 |
| 450 | `domain/trade/repository/custom/impl/TradeCompleteCustomRepositoryImpl.java` | `adapter/persistence/trade/TradeCompleteCustomRepositoryImpl.java` | trade | querydsl | adapter/persistence/trade (QueryPort 구현) | PR07 | PR07 완료 |
| 451 | `domain/trade/service/FindMyTradeHistoryService.java` | (삭제) → application/trade/port/in/TradeHistoryQuery.java | trade | service interface | application/trade/port/in (UseCase/Query) | PR07 | PR07 완료 |
| 452 | `domain/trade/service/TradeCancelApplier.java` | `application/trade/service/TradeCancelApplier.java` | trade | service interface | application/trade/port/in (UseCase/Query) | PR07 | PR07 완료 |
| 453 | `domain/trade/service/TradeCancelService.java` | (삭제) → application/trade/port/in/RequestTradeCancelUseCase.java | trade | service interface | application/trade/port/in (UseCase/Query) | PR07 | PR07 완료 |
| 454 | `domain/trade/service/TradeCancelWithdrawService.java` | (삭제) → application/trade/port/in/WithdrawTradeCancelUseCase.java | trade | service interface | application/trade/port/in (UseCase/Query) | PR07 | PR07 완료 |
| 455 | `domain/trade/service/TradeHistoryByHeadService.java` | (삭제) → application/trade/port/in/TradeStatisticsQuery.java | trade | service interface | application/trade/port/in (UseCase/Query) | PR07 | PR07 완료 |
| 456 | `domain/trade/service/TradeHistoryByPlaceService.java` | (삭제) → application/trade/port/in/TradeStatisticsQuery.java | trade | service interface | application/trade/port/in (UseCase/Query) | PR07 | PR07 완료 |
| 457 | `domain/trade/service/TradeStateReader.java` | (삭제) → application/trade/port/in/TradeStateQuery.java + service/TradeStateService.java | trade | service interface | application/trade/port/in (UseCase/Query) | PR07 | PR07 완료 |
| 458 | `domain/trade/service/TradeStateSnapshot.java` | `application/trade/data/TradeStateSnapshot.java` (+ 규칙 `domain/trade/model/TradeState.java`) | trade | service interface | application/trade/port/in (UseCase/Query) | PR07 | PR07 완료 |
| 459 | `domain/trade/service/impl/FindMyTradeHistoryServiceImpl.java` | (삭제) → application/trade/service/TradeHistoryService.java | trade | service impl | application/trade/service (+ 필요한 output Port) | PR07 | PR07 완료 |
| 460 | `domain/trade/service/impl/TradeCancelServiceImpl.java` | (삭제) → application/trade/service/RequestTradeCancelService.java | trade | service impl | application/trade/service (+ 필요한 output Port) | PR07 | PR07 완료 |
| 461 | `domain/trade/service/impl/TradeCancelWithdrawServiceImpl.java` | (삭제) → application/trade/service/WithdrawTradeCancelService.java | trade | service impl | application/trade/service (+ 필요한 output Port) | PR07 | PR07 완료 |
| 462 | `domain/trade/service/impl/TradeHistoryByHeadServiceImpl.java` | (삭제) → application/trade/service/TradeStatisticsService.java | trade | service impl | application/trade/service (+ 필요한 output Port) | PR07 | PR07 완료 |
| 463 | `domain/trade/service/impl/TradeHistoryByPlaceServiceImpl.java` | (삭제) → application/trade/service/TradeStatisticsService.java | trade | service impl | application/trade/service (+ 필요한 output Port) | PR07 | PR07 완료 |
| 464 | `global/aop/BlockAspect.java` | (삭제) → application/trade/port/out/TradeBlockPort.java + adapter/integration/trade/TradeBlockBridge.java | block | global/aop | @CheckBlocked 유일 사용처(RequestTradeComplete) 전환 시 consumer output Port(차단 조회)로 대체 후 삭제 | PR07 | PR07 완료 |
| 465 | `global/aop/CheckBlocked.java` | (삭제) (유일 사용처가 거래 완료 요청이었음) | block | global/aop | @CheckBlocked 유일 사용처(RequestTradeComplete) 전환 시 consumer output Port(차단 조회)로 대체 후 삭제 | PR07 | PR07 완료 |
| 466 | `global/auth/MemberDetails.java` | `adapter/security/auth/MemberDetails.java` | auth | global/auth | adapter/security/auth | PR01 | PR01 완료 |
| 467 | `global/auth/MemberDetailsService.java` | `adapter/security/auth/MemberDetailsService.java` | auth | global/auth | adapter/security/auth | PR01 | PR01 완료 |
| 468 | `global/chat/notification/ChattingServerMessageMutationNotifier.java` | = | chat | global/chat | adapter/http/chat | PR08 | legacy |
| 469 | `global/chat/notification/ChattingServerProperties.java` | = | chat | global/chat | adapter/http/chat | PR08 | legacy |
| 470 | `global/chat/notification/ChattingServerSystemMessageNotifier.java` | = | chat | global/chat | adapter/http/chat | PR08 | legacy |
| 471 | `global/chat/notification/ChattingServerTradeStatusNotifier.java` | = | chat | global/chat | adapter/http/chat | PR08 | legacy |
| 472 | `global/chat/notification/ChattingServerTradeStatusNotifierImpl.java` | = | chat | global/chat | adapter/http/chat | PR08 | legacy |
| 473 | `global/chat/stream/ChatStreamConsumerWorker.java` | = | chat | global/chat | adapter/stream/chat | PR08 | legacy |
| 474 | `global/chat/stream/ChatStreamField.java` | = | chat | global/chat | adapter/stream/chat | PR08 | legacy |
| 475 | `global/chat/stream/ChatStreamHandler.java` | = | chat | global/chat | adapter/stream/chat | PR08 | legacy |
| 476 | `global/chat/stream/ChatStreamMessage.java` | = | chat | global/chat | adapter/stream/chat | PR08 | legacy |
| 477 | `global/chat/stream/ChatStreamMessageProcessor.java` | = | chat | global/chat | adapter/stream/chat | PR08 | legacy |
| 478 | `global/chat/stream/ChatStreamProperties.java` | = | chat | global/chat | adapter/stream/chat | PR08 | legacy |
| 479 | `global/chat/stream/ChatStreamRedisAdapter.java` | = | chat | global/chat | adapter/stream/chat | PR08 | legacy |
| 480 | `global/chat/stream/SaveChatMessageHandler.java` | = | chat | global/chat | adapter/stream/chat | PR08 | legacy |
| 481 | `global/config/AsyncConfig.java` | `infrastructure/config/AsyncConfig.java` | infrastructure | global/config | infrastructure/config | PR01 | PR01 완료 |
| 482 | `global/config/GsonConfig.java` | `infrastructure/config/GsonConfig.java` | infrastructure | global/config | infrastructure/config | PR01 | PR01 완료 |
| 483 | `global/config/RetryConfig.java` | `infrastructure/config/RetryConfig.java` | infrastructure | global/config | infrastructure/config | PR01 | PR01 완료 |
| 484 | `global/config/SchedulerConfig.java` | `infrastructure/config/SchedulerConfig.java` | infrastructure | global/config | infrastructure/config | PR01 | PR01 완료 |
| 485 | `global/event/ChatMessageMutationEvent.java` | = | chat | global/event | application/chat (기존 이벤트 경계 정리) | PR08 | legacy |
| 486 | `global/event/CreateAdminAlertEvent.java` | = | admin | global/event | application/admin (관리자 알림) | PR11 | legacy |
| 487 | `global/event/CreateAlertEvent.java` | `adapter/event/alert/CreateAlertEvent.java` | alert | global/event | alert 공개 input Port (중요 알림은 동기 저장으로 대체) | PR04 | PR04 완료 (비동기 경로 유지. OTHER_MEMBER_TRADE_COMPLETE·TRADE_CANCEL 발행만 PR07 이 동기로 이동) |
| 488 | `global/event/CreateAlertMembersEvent.java` | `adapter/event/alert/CreateAlertMembersEvent.java` | alert | global/event | alert 공개 input Port (중요 알림은 동기 저장으로 대체) | PR04 | PR04 완료 (공지 비동기 경로 유지) |
| 489 | `global/event/DeleteNotUsedImageEvent.java` | `adapter/event/image/DeleteNotUsedImageEvent.java` | image | global/event | application/image (기존 이벤트 경계 정리) | PR05 | PR06 완료: image listener 옆으로 이동, 소유 타입은 ImageOwnerType (AFTER_COMMIT 비동기 유지) |
| 490 | `global/event/ReservationCancelledEvent.java` | `adapter/integration/post/ReservationCancelledEvent.java` | post | global/event | adapter/integration/post (커밋 후 알림 bridge 내부 이벤트) | PR06 | PR06 완료 |
| 491 | `global/event/SendNotificationEvent.java` | = | notification | global/event | application/notification 발송 계약 | PR04 | PR04 bridge (소비 LegacySendNotificationEventBridge, 제거 PR10) |
| 492 | `global/event/TradeStatusChangedEvent.java` | `adapter/event/trade/TradeStatusChangedEvent.java` | trade | global/event | application/trade (기존 이벤트 경계 정리) | PR07 | PR07 완료 |
| 493 | `global/event/constant/ImageType.java` | `(삭제)` | image | global/event | domain/image/model | PR05 | PR06 완료: ImageOwnerType 으로 대체 |
| 494 | `global/event/handler/ChatMessageMutationEventListener.java` | = | chat | global/event | adapter/event/chat | PR08 | legacy |
| 495 | `global/event/handler/CreateAdminAlertEventListener.java` | = | admin | global/event | adapter/event/admin | PR11 | legacy |
| 496 | `global/event/handler/CreateAlertEventListener.java` | `adapter/event/alert/AlertEventListener.java` | alert | global/event | adapter/event/alert (중요 알림 발행자 전환은 PR06/07/09/10/11) | PR04 | PR04 완료 (비동기 경로 유지) |
| 497 | `global/event/handler/CreateAlertMembersEventListener.java` | `adapter/event/alert/AlertEventListener.java` | alert | global/event | adapter/event/alert (중요 알림 발행자 전환은 PR06/07/09/10/11) | PR04 | PR04 완료 (공지 비동기 경로 유지) |
| 498 | `global/event/handler/DeleteNotUsedImageEventListener.java` | `adapter/event/image/DeleteNotUsedImageEventListener.java (+ application/image/service/DeleteUnusedImagesService)` | image | global/event | adapter/event/image | PR05 | 전환 |
| 499 | `global/event/handler/ReservationCancelledEventListener.java` | `adapter/integration/post/ReservationCancelledEventListener.java` | post | global/event | adapter/integration/post (커밋 후 비동기 푸시·채팅 HTTP. notification PR04·채팅 HTTP PR08 bridge) | PR06 | PR06 완료 |
| 500 | `global/event/handler/SendNotificationEventListener.java` | `adapter/integration/notification/LegacySendNotificationEventBridge.java` | notification | global/event | adapter/event/notification | PR04 | PR04 완료 (bridge, 제거 PR10) |
| 501 | `global/event/handler/TradeStatusChangedEventListener.java` | `adapter/event/trade/TradeStatusChangedEventListener.java` | trade | global/event | adapter/event/trade → adapter/http/chat | PR07 | PR07 완료 |
| 502 | `global/exception/ErrorCode.java` | = | common | global/exception | domain/common(GlobalException·ErrorCode) + infrastructure/web(Handler·ErrorResponse) | PR12 | legacy |
| 503 | `global/exception/ErrorResponse.java` | = | common | global/exception | domain/common(GlobalException·ErrorCode) + infrastructure/web(Handler·ErrorResponse) | PR12 | legacy |
| 504 | `global/exception/GlobalException.java` | = | common | global/exception | domain/common(GlobalException·ErrorCode) + infrastructure/web(Handler·ErrorResponse) | PR12 | legacy |
| 505 | `global/exception/GlobalExceptionHandler.java` | = | common | global/exception | domain/common(GlobalException·ErrorCode) + infrastructure/web(Handler·ErrorResponse) | PR12 | legacy |
| 506 | `global/filter/ExceptionFilter.java` | `infrastructure/web/ExceptionFilter.java` | infrastructure | global/filter | infrastructure/web | PR01 | PR01 완료 |
| 507 | `global/filter/JwtAuthenticationFilter.java` | `adapter/security/auth/JwtAuthenticationFilter.java` | auth | global/filter | adapter/security/auth | PR01 | PR01 완료 |
| 508 | `global/filter/JwtFilter.java` | `adapter/security/auth/JwtFilter.java` | auth | global/filter | adapter/security/auth | PR01 | PR01 완료 |
| 509 | `global/filter/RequestLogFilter.java` | `infrastructure/web/RequestLogFilter.java` | infrastructure | global/filter | infrastructure/web | PR01 | PR01 완료 |
| 510 | `global/querydsl/QueryDslConfig.java` | = | infrastructure | global/querydsl | infrastructure/config | PR12 | legacy |
| 511 | `global/redis/RedisConfig.java` | = | infrastructure | global/redis | infrastructure/config | PR12 | legacy |
| 512 | `global/redis/RedisUtil.java` | = | common | global/redis | 사용처별 좁은 output Port 뒤 adapter/redis/<업무> (각 업무 PR에서 호출 교체) | PR12 | legacy |
| 513 | `global/scheduler/DeleteOrphanImageScheduler.java` | `adapter/scheduler/image/DeleteOrphanImageScheduler.java (+ application/image/service/CleanupOrphanImagesService)` | image | global/scheduler | adapter/scheduler/image | PR05 | 전환 |
| 514 | `global/scheduler/ReservationReminderScheduler.java` | `adapter/scheduler/post/ReservationReminderScheduler.java` | post | global/scheduler | adapter/scheduler/post | PR06 | PR06 완료 |
| 515 | `global/scheduler/SuspendReleaseScheduler.java` | = | suspend | global/scheduler | adapter/scheduler/suspend | PR09 | legacy |
| 516 | `global/security/config/JwtSecurityConfig.java` | `(삭제)` | infrastructure | global/security | 삭제 (어디서도 적용되지 않는 dead code) | PR01 | PR01 완료 |
| 517 | `global/security/config/SecurityConfig.java` | `infrastructure/security/SecurityConfig.java` | infrastructure | global/security | infrastructure/security · infrastructure/config | PR01 | PR01 완료 |
| 518 | `global/security/config/WebMvcConfig.java` | `infrastructure/config/WebMvcConfig.java` | infrastructure | global/security | infrastructure/security · infrastructure/config | PR01 | PR01 완료 |
| 519 | `global/security/exception/ExpiredTokenException.java` | `adapter/security/auth/exception/ExpiredTokenException.java` | auth | global/security | domain/auth/exception | PR02 | 전환 |
| 520 | `global/security/exception/InvalidMemberPrincipalException.java` | `adapter/security/auth/exception/InvalidMemberPrincipalException.java` | auth | global/security | domain/auth/exception | PR02 | 전환 |
| 521 | `global/security/exception/InvalidTokenException.java` | `adapter/security/auth/exception/InvalidTokenException.java` | auth | global/security | domain/auth/exception | PR02 | 전환 |
| 522 | `global/security/handler/JwtAccessDeniedHandler.java` | `infrastructure/security/JwtAccessDeniedHandler.java` | infrastructure | global/security | infrastructure/security | PR01 | PR01 완료 |
| 523 | `global/security/handler/JwtAuthenticationEntryPoint.java` | `infrastructure/security/JwtAuthenticationEntryPoint.java` | infrastructure | global/security | infrastructure/security | PR01 | PR01 완료 |
| 524 | `global/security/jwt/JwtProperties.java` | `adapter/security/auth/JwtProperties.java` | auth | global/security | adapter/security/auth (토큰 발급 output Adapter) | PR02 | 전환 |
| 525 | `global/security/jwt/JwtProvider.java` | `adapter/security/auth/JwtProvider.java` | auth | global/security | adapter/security/auth (토큰 발급 output Adapter) | PR02 | 전환 |
| 526 | `global/security/jwt/TokenParser.java` | `adapter/security/auth/TokenParser.java` | auth | global/security | adapter/security/auth | PR01 | PR01 완료 |
| 527 | `global/sms/SmsConfig.java` | `adapter/sms/SmsConfig.java` | sms | global/sms | adapter/sms | PR02 | 전환 |
| 528 | `global/sms/SmsDemoAccount.java` | `domain/sms/policy/SmsDemoAccount.java` | sms | global/sms | adapter/sms | PR02 | 전환 |
| 529 | `global/sms/SmsProperties.java` | `adapter/sms/SmsProperties.java` | sms | global/sms | adapter/sms | PR02 | 전환 |
| 530 | `global/sms/SmsSendHelper.java` | `adapter/sms/CoolSmsSenderAdapter.java` | sms | global/sms | adapter/sms | PR02 | 전환 (SmsSenderPort 구현) |
| 531 | `global/thirdparty/ai/AiModerationClient.java` | `adapter/http/ai/AiModerationClient.java` | image | global/thirdparty | adapter/http/image (AI 응답 변환=Adapter 내부 ACL) | PR05 | PR06 완료: post 텍스트 검사·AiImageModerationAdapter 공유 |
| 532 | `global/thirdparty/ai/AiModerationProperties.java` | `adapter/http/ai/AiModerationProperties.java` | image | global/thirdparty | adapter/http/image (AI 응답 변환=Adapter 내부 ACL) | PR05 | PR06 완료 |
| 533 | `global/thirdparty/aws/s3/config/S3Config.java` | `infrastructure/config/S3Config.java` | image | global/thirdparty | adapter/s3/image | PR05 | 이동 |
| 534 | `global/thirdparty/aws/s3/service/S3DeleteService.java` | `adapter/s3/image/S3ImageStorageAdapter.java (병합)` | image | global/thirdparty | adapter/s3/image | PR05 | 전환 |
| 535 | `global/thirdparty/aws/s3/service/S3UploadService.java` | `adapter/s3/image/S3ImageStorageAdapter.java` | image | global/thirdparty | adapter/s3/image | PR05 | 전환 |
| 536 | `global/thirdparty/expo/ExpoPushAdapter.java` | `adapter/push/notification/ExpoPushAdapter.java` | notification | global/thirdparty | adapter/push/notification | PR04 | PR04 완료 |
| 537 | `global/util/BlockValidator.java` | = | block | global/util | block 공개 query + consumer output Port (호출처 교체는 각 업무 PR) | PR09 | legacy |
| 538 | `global/util/ImageValidateUtil.java` | = | image | global/util | image 공개 계약 (호출처 교체는 각 업무 PR) | PR05 | legacy 유지(bridge→PR08): post 는 ImageQuery.getAllByIds 로 교체 완료(PR06), 남은 호출자 chat ValidateChatSendable |
| 539 | `global/util/MemberUtil.java` | = | auth | global/util | ActorContext 로 대체. 호출처 교체는 각 업무 PR, 클래스 삭제는 PR12 | PR12 | legacy |

## 3. HTTP API (Controller 매핑)

URL·method·body 계약은 전환 중 변경하지 않는다. 내부 업무 이름 변경을 URL에 반영하지 않는다.

| method | URL | controller | handler | 성공 status |
| --- | --- | --- | --- | --- |
| GET | `/api/admin/alert` | `domain/admin/presentation/AdminController.java` | getAdminAlert | 200 |
| DELETE | `/api/admin/alert/{alert_id}` | `domain/admin/presentation/AdminController.java` | rejectAlert | 204 |
| PATCH | `/api/admin/gwangsan/{member_id}` | `domain/admin/presentation/AdminController.java` | adjustGwangsan | 200 |
| GET | `/api/admin/product` | `domain/admin/presentation/AdminProductController.java` | findProducts | 200 / 400(분기) |
| DELETE | `/api/admin/product/{product_id}` | `domain/admin/presentation/AdminProductController.java` | deleteProduct | 204 / 400(분기) |
| PATCH | `/api/admin/role/{member_id}` | `domain/admin/presentation/AdminController.java` | updateMemberRole | 204 |
| POST | `/api/admin/signin` | `domain/admin/presentation/AdminController.java` | signIn | 200 |
| PATCH | `/api/admin/status/{member_id}` | `domain/admin/presentation/AdminController.java` | updateMemberStatus | 204 |
| PATCH | `/api/admin/trade/{alert_id}` | `domain/admin/presentation/AdminController.java` | approveTradeCancel | 200 |
| PATCH | `/api/admin/verify/signup/{alert_id}` | `domain/admin/presentation/AdminController.java` | verifySignUp | 204 |
| DELETE | `/api/admin/{alert_id}` | `domain/admin/presentation/AdminController.java` | deleteAlert | 204 |
| GET | `/api/alert` | `adapter/web/alert/controller/AlertController.java` | getAlerts | 200 |
| PATCH | `/api/alert/read/{alert_id}` | `adapter/web/alert/controller/AlertController.java` | readAlerts | 204 |
| GET | `/api/alert/unread` | `adapter/web/alert/controller/AlertController.java` | getUnreadAlerts | 200 |
| GET | `/api/app/version` | `adapter/web/app/controller/AppVersionController.java` | findVersion | 200 / 400(분기) |
| GET | `/api/auth` | `adapter/web/auth/controller/AuthController.java` | getMemberInfo | 200 |
| POST | `/api/auth/nickname` | `adapter/web/auth/controller/AuthController.java` | findNickname | 200 |
| PATCH | `/api/auth/password` | `adapter/web/auth/controller/AuthController.java` | resetPassword | 200 |
| PATCH | `/api/auth/reissue` | `adapter/web/auth/controller/AuthController.java` | reissueToken | 200 |
| POST | `/api/auth/signin` | `adapter/web/auth/controller/AuthController.java` | signIn | 200 |
| DELETE | `/api/auth/signout` | `adapter/web/auth/controller/AuthController.java` | signout | 204 |
| POST | `/api/auth/signup` | `adapter/web/auth/controller/AuthController.java` | signUp | 201 |
| GET | `/api/block` | `domain/block/presentation/BlockController.java` | getBlockList | 200 |
| DELETE | `/api/block/{targetMemberId}` | `domain/block/presentation/BlockController.java` | unblock | 204 |
| POST | `/api/block/{targetMemberId}` | `domain/block/presentation/BlockController.java` | block | 201 |
| DELETE | `/api/chat/message/{message_id}` | `domain/chat/presentation/ChatController.java` | deleteMessage | 204 |
| PATCH | `/api/chat/message/{message_id}` | `domain/chat/presentation/ChatController.java` | updateMessage | 200 |
| PATCH | `/api/chat/read` | `domain/chat/presentation/ChatController.java` | readMessages | 200 |
| GET | `/api/chat/room/{product_id}` | `domain/chat/presentation/ChatController.java` | getRoom | 200 |
| POST | `/api/chat/room/{product_id}` | `domain/chat/presentation/ChatController.java` | createChatRoom | 201 |
| DELETE | `/api/chat/room/{room_id}` | `domain/chat/presentation/ChatController.java` | deleteChatRoom | 204 |
| GET | `/api/chat/room/{room_id}/sendable` | `domain/chat/presentation/ChatController.java` | validateSendable | 200 |
| GET | `/api/chat/rooms` | `domain/chat/presentation/ChatController.java` | getRooms | 200 |
| GET | `/api/chat/{room_id}` | `domain/chat/presentation/ChatController.java` | getChatMessage | 200 |
| GET | `/api/health` | `domain/health/presentation/HealthController.java` | health | 200 |
| POST | `/api/image` | `adapter/web/image/controller/ImageController.java` | upload | 200 |
| DELETE | `/api/image/{image_id}` | `adapter/web/image/controller/ImageController.java` | delete | 204 |
| DELETE | `/api/member` | `adapter/web/member/controller/MemberController.java` | deleteMember | 204 |
| GET | `/api/member` | `adapter/web/member/controller/MemberController.java` | findMyInfo | 200 |
| PATCH | `/api/member` | `adapter/web/member/controller/MemberController.java` | updateMyInfo | 200 |
| GET | `/api/member/all` | `adapter/web/member/controller/MemberController.java` | findAllUserInfo | 200 |
| GET | `/api/member/{id}` | `adapter/web/member/controller/MemberController.java` | findUserInfo | 200 |
| GET | `/api/notice` | `domain/notice/presentation/NoticeController.java` | findAllNotices | 200 |
| POST | `/api/notice` | `domain/notice/presentation/NoticeController.java` | createNotice | 201 |
| DELETE | `/api/notice/{noticeId}` | `domain/notice/presentation/NoticeController.java` | deleteNotice | 204 |
| GET | `/api/notice/{noticeId}` | `domain/notice/presentation/NoticeController.java` | findNotice | 200 |
| PATCH | `/api/notice/{noticeId}` | `domain/notice/presentation/NoticeController.java` | updateNotice | 200 |
| GET | `/api/related-keyword` | `adapter/web/relatedkeyword/controller/RelatedKeywordController.java` | findAllKeywords | 200 |
| GET | `/api/related-keyword/current` | `adapter/web/relatedkeyword/controller/RelatedKeywordController.java` | findMyKeywords | 200 |
| DELETE | `/api/related-keyword/{memberRelatedKeywordId}` | `adapter/web/relatedkeyword/controller/RelatedKeywordController.java` | deleteMyKeyword | 204 |
| POST | `/api/report` | `domain/report/presentation/ReportController.java` | createReport | 201 |
| GET | `/api/review` | `adapter/web/review/controller/ReviewController.java` | getMyReviews | 200 |
| POST | `/api/review` | `adapter/web/review/controller/ReviewController.java` | createReview | 201 |
| GET | `/api/review/current` | `adapter/web/review/controller/ReviewController.java` | getMyReceivedReviews | 200 / 400(분기) |
| GET | `/api/review/detail/{reviewId}` | `adapter/web/review/controller/ReviewController.java` | getReviewDetail | 200 |
| GET | `/api/review/{memberId}` | `adapter/web/review/controller/ReviewController.java` | getReviewByMember | 200 / 400(분기) |
| POST | `/api/sms` | `adapter/web/sms/controller/SmsController.java` | sendSms | 200 |
| POST | `/api/sms/nickname` | `adapter/web/sms/controller/SmsController.java` | sendFindNicknameSms | 200 |
| POST | `/api/sms/nickname/verify` | `adapter/web/sms/controller/SmsController.java` | verifyFindNicknameSms | 200 |
| POST | `/api/sms/password` | `adapter/web/sms/controller/SmsController.java` | sendResetPasswordSms | 200 |
| POST | `/api/sms/password/verify` | `adapter/web/sms/controller/SmsController.java` | verifyResetPasswordSms | 200 |
| POST | `/api/sms/verify` | `adapter/web/sms/controller/SmsController.java` | verifySms | 200 |
| PATCH | `/api/suspend` | `domain/suspend/presentation/SuspendController.java` | suspend | 204 |
| DELETE | `/api/trade/cancel/{product_id}` | `adapter/web/trade/controller/TradeController.java` | cancelWithdraw | 204 |
| POST | `/api/trade/cancel/{product_id}` | `adapter/web/trade/controller/TradeController.java` | cancel | 200 |
| GET | `/api/trade/graph/head` | `adapter/web/trade/controller/TradeController.java` | getHeadHistory | 200 |
| GET | `/api/trade/graph/place` | `adapter/web/trade/controller/TradeController.java` | getPlaceHistory | 200 |
| GET | `/api/trade/history` | `adapter/web/trade/controller/TradeController.java` | getMyTradeHistory | 200 |
| GET | `/api/trade/statistics/head` | `adapter/web/trade/controller/TradeController.java` | getHeadStatistics | 200 |
| GET | `/api/trade/statistics/place` | `adapter/web/trade/controller/TradeController.java` | getPlaceStatistics | 200 |
| GET | `api/post` | `domain/post/presentation/PostController.java` | findProductByTypeAndMode | 200 |
| POST | `api/post` | `domain/post/presentation/PostController.java` | createProduct | 201 |
| GET | `api/post/current` | `domain/post/presentation/PostController.java` | findCurrentProduct | 200 / 400(분기) |
| GET | `api/post/member/{member_id}` | `domain/post/presentation/PostController.java` | findByMemberID | 200 / 400(분기) |
| DELETE | `api/post/reservation/{product_id}` | `domain/post/presentation/PostController.java` | deleteReservation | 204 |
| PATCH | `api/post/reservation/{product_id}` | `domain/post/presentation/PostController.java` | reservation | 200 |
| DELETE | `api/post/trade` | `adapter/web/trade/controller/TradeCompletionController.java` | tradeWithdraw | 204 |
| POST | `api/post/trade` | `adapter/web/trade/controller/TradeCompletionController.java` | tradeRequest | 204 |
| DELETE | `api/post/{post_id}` | `domain/post/presentation/PostController.java` | deleteProduct | 204 |
| GET | `api/post/{post_id}` | `domain/post/presentation/PostController.java` | findProductById | 200 |
| PATCH | `api/post/{post_id}` | `domain/post/presentation/PostController.java` | updateProduct | 204 |

총 81개 엔드포인트.

## 4. 보안·인가 규칙 (infrastructure/security/SecurityConfig, 원문)

```java
.requestMatchers(CorsUtils::isPreFlightRequest).permitAll()
// auth
.requestMatchers(HttpMethod.POST, "/api/auth/signup").permitAll()
.requestMatchers(HttpMethod.POST, "/api/auth/signin").permitAll()
.requestMatchers(HttpMethod.PATCH, "/api/auth/reissue").permitAll()
.requestMatchers(HttpMethod.DELETE, "/api/auth/signout").authenticated()
.requestMatchers(HttpMethod.PATCH, "/api/auth/password").permitAll()
.requestMatchers(HttpMethod.POST, "/api/auth/nickname").permitAll()
// sms
.requestMatchers(HttpMethod.POST, "/api/sms").permitAll()
.requestMatchers(HttpMethod.POST, "/api/sms/verify").permitAll()
.requestMatchers(HttpMethod.POST, "/api/sms/password").permitAll()
.requestMatchers(HttpMethod.POST, "/api/sms/password/verify").permitAll()
.requestMatchers(HttpMethod.POST, "/api/sms/nickname").permitAll()
.requestMatchers(HttpMethod.POST, "/api/sms/nickname/verify").permitAll()
// post
.requestMatchers(HttpMethod.POST, "/api/post").authenticated()
.requestMatchers(HttpMethod.GET, "/api/post").authenticated()
.requestMatchers(HttpMethod.PATCH, "/api/post/{post_id}").authenticated()
.requestMatchers(HttpMethod.GET, "/api/post/{post_id}").authenticated()
.requestMatchers(HttpMethod.DELETE, "/api/post/{post_id}").authenticated()
.requestMatchers(HttpMethod.GET, "/api/post/current").authenticated()
.requestMatchers(HttpMethod.GET, "/api/post/member/{member_id}").authenticated()
.requestMatchers(HttpMethod.POST, "/api/post/trade").authenticated()
.requestMatchers(HttpMethod.PATCH, "/api/post/reservation/{post_id}").authenticated()
.requestMatchers(HttpMethod.DELETE, "/api/post/reservation/{post_id}").authenticated()
// notice
.requestMatchers(HttpMethod.GET, "/api/notice").authenticated()
.requestMatchers(HttpMethod.GET, "/api/notice/{id}").authenticated()
.requestMatchers(HttpMethod.POST, "/api/notice")
.hasAnyAuthority(MemberRole.ROLE_PLACE_ADMIN.name(), MemberRole.ROLE_HEAD_ADMIN.name())
.requestMatchers(HttpMethod.PATCH, "/api/notice/{id}").authenticated()
.requestMatchers(HttpMethod.DELETE, "/api/notice/{id}").authenticated()
// related-keyword
.requestMatchers(HttpMethod.GET, "/api/related-keyword").permitAll()
.requestMatchers(HttpMethod.GET, "/api/related-keyword/current").authenticated()
.requestMatchers(HttpMethod.DELETE, "/api/related-keyword/{memberRelatedKeywordId}").authenticated()
// admin
.requestMatchers(HttpMethod.POST, "/api/admin/signin").permitAll()
.requestMatchers("/api/admin/**")
.hasAnyAuthority(MemberRole.ROLE_PLACE_ADMIN.name(), MemberRole.ROLE_HEAD_ADMIN.name())
// review
.requestMatchers(HttpMethod.POST, "/api/review").authenticated()
.requestMatchers(HttpMethod.GET, "/api/review").authenticated()
.requestMatchers(HttpMethod.GET, "/api/review/current").authenticated()
.requestMatchers(HttpMethod.GET, "/api/review/{memberId}").authenticated()
.requestMatchers(HttpMethod.POST, "/api/review/detail/{reviewId}").authenticated()
// member
.requestMatchers(HttpMethod.GET, "/api/member").authenticated()
.requestMatchers(HttpMethod.PATCH, "/api/member").authenticated()
.requestMatchers(HttpMethod.GET, "/api/member/{memberId}").authenticated()
.requestMatchers(HttpMethod.GET, "/api/member/all")
.hasAnyAuthority(MemberRole.ROLE_PLACE_ADMIN.name(), MemberRole.ROLE_HEAD_ADMIN.name())
// report
.requestMatchers(HttpMethod.POST, "/api/report").authenticated()
// health
.requestMatchers(HttpMethod.GET, "/api/health").permitAll()
.requestMatchers(HttpMethod.GET, "/api/app/version").permitAll()
// suspend
.requestMatchers(HttpMethod.PATCH, "/api/suspend")
.hasAnyAuthority(MemberRole.ROLE_PLACE_ADMIN.name(), MemberRole.ROLE_HEAD_ADMIN.name())
// trade
.requestMatchers(HttpMethod.GET, "/api/trade/graph/head").hasAnyAuthority(MemberRole.ROLE_HEAD_ADMIN.name())
.requestMatchers(HttpMethod.GET, "/api/trade/graph/place").hasAnyAuthority(MemberRole.ROLE_HEAD_ADMIN.name(), MemberRole.ROLE_PLACE_ADMIN.name())
.requestMatchers(HttpMethod.GET, "/api/trade/statistics/head").hasAnyAuthority(MemberRole.ROLE_HEAD_ADMIN.name())
.requestMatchers(HttpMethod.GET, "/api/trade/statistics/place").hasAnyAuthority(MemberRole.ROLE_HEAD_ADMIN.name(), MemberRole.ROLE_PLACE_ADMIN.name())
.anyRequest().hasAnyAuthority(
```
필터 순서(기존 유지): RequestLogFilter → ExceptionFilter → JwtFilter → JwtAuthenticationFilter (모두 UsernamePasswordAuthenticationFilter 앞). JwtFilter와 JwtAuthenticationFilter가 둘 다 회원을 조회해 SecurityContext를 설정한다(기준선 동작, 요청당 회원 조회 2회). JwtAuthenticationFilter는 `/api/auth/**`, `/api/sms/**`, `/api/health`, OPTIONS를 건너뛴다.

## 5. 실행자 식별·차단 검사 사용처

새 유스케이스는 `domain.common.ActorContext`를 Command로 받는다. Controller는 `ActorContext` 파라미터를 선언하면 `adapter.security.auth.ActorContextArgumentResolver`가 인증 principal에서만 채운다(요청 body/query 회원 ID 미사용). 아래 `MemberUtil.getCurrentMember()` 호출은 각 담당 PR이 ActorContext로 교체하고, MemberUtil 클래스 삭제는 PR12가 한다.

| 파일 | 호출 수 | 교체 PR |
| --- | --- | --- |
| `domain/admin/service/impl/AdjustGwangsanServiceImpl.java` | 1 | PR11 |
| `domain/admin/service/impl/ApproveTradeCancelServiceImpl.java` | 1 | PR11 |
| `domain/admin/service/impl/DeleteAdminAlertServiceImpl.java` | 1 | PR11 |
| `domain/admin/service/impl/FindAlertByAlertTypeAndPlaceServiceImpl.java` | 1 | PR11 |
| `domain/admin/service/impl/RejectAdminAlertServiceImpl.java` | 1 | PR11 |
| `domain/admin/service/impl/UpdateMemberRoleServiceImpl.java` | 1 | PR11 |
| `domain/admin/service/impl/UpdateMemberStatusServiceImpl.java` | 1 | PR11 |
| `domain/admin/service/impl/VerificationSignUpServiceImpl.java` | 1 | PR11 |
| `domain/alert/service/impl/ExistsUnreadAlertServiceImpl.java` | 1 | PR04 |
| `domain/alert/service/impl/FindAlertByCurrentServiceImpl.java` | 1 | PR04 |
| `domain/alert/service/impl/ReadAlertServiceImpl.java` | 1 | PR04 |
| `domain/auth/service/impl/SignOutServiceImpl.java` | 1 | PR02 (완료: ActorContext, `application/auth/service/SignOutService.java`) |
| `domain/auth/service/impl/TokenAuthenticationServiceImpl.java` | 1 | PR02 (완료: ActorContext, `application/auth/service/CurrentMemberService.java`) |
| `domain/block/service/impl/BlockMemberServiceImpl.java` | 1 | PR09 |
| `domain/block/service/impl/GetBlockListServiceImpl.java` | 1 | PR09 |
| `domain/block/service/impl/UnblockMemberServiceImpl.java` | 1 | PR09 |
| `domain/chat/service/impl/ChatMessageMutationService.java` | 1 | PR08 |
| `domain/chat/service/impl/CreateChatRoomServiceImpl.java` | 1 | PR08 |
| `domain/chat/service/impl/DeleteChatRoomServiceImpl.java` | 1 | PR08 |
| `domain/chat/service/impl/FindChatMessageByRoomIdServiceImpl.java` | 1 | PR08 |
| `domain/chat/service/impl/FindRoomIdByProductIdServiceImpl.java` | 1 | PR08 |
| `domain/chat/service/impl/FindRoomsByCurrentUserServiceImpl.java` | 1 | PR08 |
| `domain/chat/service/impl/ReadChatMessageServiceImpl.java` | 1 | PR08 |
| `domain/chat/service/impl/ValidateChatSendableServiceImpl.java` | 1 | PR08 |
| `domain/member/service/impl/FindAllUserInfoServiceImpl.java` | 1 | PR02 (완료: ActorContext, `application/member/service/FindAllUserInfoService.java`) |
| `domain/member/service/impl/FindMyInfoServiceImpl.java` | 1 | PR02 (완료: ActorContext, `application/member/service/FindMyInfoService.java`) |
| `domain/member/service/impl/MemberWithdrawalServiceImpl.java` | 1 | PR11 (탈퇴 bridge, PR02 에서 유지) |
| `domain/member/service/impl/UpdateMyInfoServiceImpl.java` | 1 | PR02 (완료: ActorContext, `application/member/service/UpdateMyInfoService.java`) |
| `domain/notice/service/impl/CreateNoticeServiceImpl.java` | 1 | PR10 |
| `domain/notice/service/impl/DeleteNoticeServiceImpl.java` | 1 | PR10 |
| `domain/notice/service/impl/FindAllNoticeServiceImpl.java` | 1 | PR10 |
| `domain/notice/service/impl/FindNoticeServiceImpl.java` | 1 | PR10 |
| `domain/notice/service/impl/UpdateNoticeServiceImpl.java` | 1 | PR10 |
| `domain/post/service/impl/CreateProductServiceImpl.java` | 1 | PR06 |
| `domain/post/service/impl/DeleteProductByIdServiceImpl.java` | 1 | PR06 |
| `domain/post/service/impl/DeleteReservationProductServiceImpl.java` | 1 | PR06 |
| `domain/post/service/impl/FindProductByCurrentUserAndTypeAndModeServiceImpl.java` | 1 | PR06 |
| `domain/post/service/impl/FindProductByIdServiceImpl.java` | 1 | PR06 |
| `domain/post/service/impl/FindProductsByTypeAndModeServiceImpl.java` | 1 | PR06 |
| `domain/post/service/impl/ReservationProductServiceImpl.java` | 1 | PR06 |
| `domain/post/service/impl/UpdateProductServiceImpl.java` | 1 | PR06 |
| `domain/relatedkeyword/service/impl/DeleteMemberRelatedKeywordServiceImpl.java` | 0 (PR03 완료: ActorContext) | PR03 |
| `domain/relatedkeyword/service/impl/FindMyRelatedKeywordServiceImpl.java` | 0 (PR03 완료: ActorContext) | PR03 |
| `domain/report/service/impl/CreateReportServiceImpl.java` | 1 | PR09 |
| `domain/review/service/impl/CreateReviewServiceImpl.java` | 1 | PR07 완료 (파일 삭제, ActorContext 사용) |
| `domain/review/service/impl/GetMyReviewListServiceImpl.java` | 1 | PR07 완료 (파일 삭제, ActorContext 사용) |
| `domain/review/service/impl/GetReceivedReviewListServiceImpl.java` | 1 | PR07 완료 (파일 삭제, ActorContext 사용) |
| `domain/trade/service/impl/FindMyTradeHistoryServiceImpl.java` | 1 | PR07 완료 (파일 삭제, ActorContext 사용) |
| `domain/trade/service/impl/TradeCancelServiceImpl.java` | 1 | PR07 완료 (파일 삭제, ActorContext 사용) |
| `domain/trade/service/impl/TradeCancelWithdrawServiceImpl.java` | 1 | PR07 완료 (파일 삭제, ActorContext 사용) |
| `global/aop/BlockAspect.java` | 1 | PR07 완료 (파일 삭제, ActorContext 사용) |

총 51회 / 51파일.

차단 검사(@CheckBlocked·BlockValidator) 사용처 — block 공개 query(PR09) + 각 consumer output Port로 교체, 기존 적용 범위·실행 순서 유지:

- `domain/chat/service/impl/CreateChatRoomServiceImpl.java` → PR08
- `domain/chat/service/impl/SaveChatMessageServiceImpl.java` → PR08
- `domain/chat/service/impl/ValidateChatSendableServiceImpl.java` → PR08
- `domain/post/service/impl/FindProductByIdServiceImpl.java` → PR06
- `domain/post/service/impl/RequestTradeCompleteServiceImpl.java` → PR07 완료: `RequestTradeCompletionService` 첫 단계 `TradeBlockPort` (기존과 같이 다른 검증·잠금보다 먼저)
- `domain/review/service/impl/CreateReviewServiceImpl.java` → PR07 완료: `CreateReviewService` 의 `ReviewBlockPort` (대상 회원 확인 뒤, 기존 순서)
- `global/aop/BlockAspect.java` → PR07 완료: 삭제

## 6. 비 HTTP 진입점 (scheduler·event listener·async·stream)

| 파일 | 진입 annotation |
| --- | --- |
| `global/chat/stream/ChatStreamConsumerWorker.java` | `@Scheduled(fixedDelay = 60000)` |
| `global/chat/stream/ChatStreamConsumerWorker.java` | `@Scheduled(fixedDelayString = "${chat.stream.blockMs:2000}")` |
| `global/event/handler/ChatMessageMutationEventListener.java` | `@Async("asyncExecutor")` |
| `global/event/handler/ChatMessageMutationEventListener.java` | `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` |
| `global/event/handler/CreateAdminAlertEventListener.java` | `@Async("asyncExecutor")` |
| `global/event/handler/CreateAdminAlertEventListener.java` | `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` |
| `global/event/handler/CreateAlertEventListener.java` | `@Async("asyncExecutor")` |
| `global/event/handler/CreateAlertEventListener.java` | `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` |
| `global/event/handler/CreateAlertMembersEventListener.java` | `@Async("asyncExecutor")` |
| `global/event/handler/CreateAlertMembersEventListener.java` | `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` |
| `adapter/event/image/DeleteNotUsedImageEventListener.java` (PR05 이동) | `@Async("asyncExecutor")` |
| `adapter/event/image/DeleteNotUsedImageEventListener.java` (PR05 이동) | `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` |
| `global/event/handler/ReservationCancelledEventListener.java` | `@Async("asyncExecutor")` |
| `global/event/handler/ReservationCancelledEventListener.java` | `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` |
| `global/event/handler/SendNotificationEventListener.java` | `@Async("asyncExecutor")` |
| `global/event/handler/SendNotificationEventListener.java` | `@TransactionalEventListener(value = SendNotificationEvent.class, phase = TransactionPhase.AFTER_COMMIT)` |
| `global/event/handler/TradeStatusChangedEventListener.java` | `@Async("asyncExecutor")` |
| `global/event/handler/TradeStatusChangedEventListener.java` | `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` |
| `adapter/scheduler/image/DeleteOrphanImageScheduler.java` (PR05 이동) | `@Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")` |
| `global/scheduler/ReservationReminderScheduler.java` | `@Scheduled(fixedDelay = 30000)` |
| `global/scheduler/SuspendReleaseScheduler.java` | `@Scheduled(cron = "0 0 * * * *")` |
| `global/sms/SmsSendHelper.java` | `@Async("asyncExecutor")` |
| ~~`global/thirdparty/aws/s3/service/S3DeleteService.java`~~ | ~~`@Async`~~ PR05 제거: 단건 삭제는 SDK 비동기 호출로 대기 없이 반환, deleteAll 은 self-call 이라 원래 미적용 |
| ~~`global/thirdparty/aws/s3/service/S3UploadService.java`~~ | ~~`@Async`~~ PR05 제거: 호출자가 결과를 동기 대기했음. S3ImageStorageAdapter.upload 가 직접 대기 |

이벤트 발행처(`publishEvent`):

| 발행 파일 | 이벤트 |
| --- | --- |
| `domain/admin/service/impl/RejectAdminAlertServiceImpl.java` | CreateAlertEvent |
| `domain/admin/service/impl/VerificationSignUpServiceImpl.java` | CreateAlertEvent |
| `domain/auth/service/impl/SignUpServiceImpl.java` | CreateAdminAlertEvent |
| `domain/auth/service/impl/SignUpServiceImpl.java` | CreateAlertEvent |
| `domain/chat/service/impl/ChatMessageMutationService.java` | ChatMessageMutationEvent |
| `domain/chat/service/impl/SaveChatMessageServiceImpl.java` | SendNotificationEvent |
| `domain/notice/service/impl/CreateNoticeServiceImpl.java` | CreateAlertMembersEvent |
| `domain/notice/service/impl/CreateNoticeServiceImpl.java` | SendNotificationEvent |
| `domain/post/service/impl/DeleteReservationProductServiceImpl.java` | ReservationCancelledEvent |
| `domain/post/service/impl/DeleteReservationProductServiceImpl.java` | TradeStatusChangedEvent |
| `domain/post/service/impl/RequestTradeCompleteServiceImpl.java` | CreateAlertEvent |
| `domain/post/service/impl/RequestTradeCompleteServiceImpl.java` | SendNotificationEvent |
| `domain/post/service/impl/RequestTradeCompleteServiceImpl.java` | TradeStatusChangedEvent |
| `domain/post/service/impl/ReservationProductServiceImpl.java` | TradeStatusChangedEvent |
| `domain/post/service/impl/UpdateProductServiceImpl.java` | DeleteNotUsedImageEvent |
| `domain/post/service/impl/WithdrawTradeCompleteServiceImpl.java` | TradeStatusChangedEvent |
| `domain/report/service/impl/CreateReportServiceImpl.java` | CreateAdminAlertEvent |
| `domain/review/service/impl/CreateReviewServiceImpl.java` | CreateAlertEvent |
| `domain/suspend/service/impl/SuspendMemberServiceImpl.java` | CreateAlertEvent |
| `domain/trade/service/TradeCancelApplier.java` | CreateAlertEvent |
| `domain/trade/service/TradeCancelApplier.java` | TradeStatusChangedEvent |
| `domain/trade/service/impl/TradeCancelServiceImpl.java` | CreateAdminAlertEvent |

## 7. 트랜잭션·락

application 공개 실행 메서드가 트랜잭션을 소유한다. 아래 readOnly/isolation/propagation/timeout과 락 순서를 보존한다.

| 파일:행 | 대상 | 속성 |
| --- | --- | --- |
| `domain/admin/service/impl/AdjustGwangsanServiceImpl.java:24` | execute | `(기본)` |
| `domain/admin/service/impl/ApproveTradeCancelServiceImpl.java:44` | execute | `(isolation = Isolation.READ_COMMITTED)` |
| `domain/admin/service/impl/CreateAdminAlertServiceImpl.java:42` | execute | `(기본)` |
| `domain/admin/service/impl/DeleteAdminProductServiceImpl.java:19` | execute | `(기본)` |
| `domain/admin/service/impl/FindAdminProductsServiceImpl.java:33` | execute | `(readOnly = true)` |
| `domain/admin/service/impl/FindAlertByAlertTypeAndPlaceServiceImpl.java:65` | execute | `(기본)` |
| `domain/admin/service/impl/RejectAdminAlertServiceImpl.java:46` | execute | `(isolation = Isolation.READ_COMMITTED)` |
| `domain/admin/service/impl/SignInAdminServiceImpl.java:34` | execute | `(기본)` |
| `domain/admin/service/impl/UpdateMemberRoleServiceImpl.java:31` | execute | `(기본)` |
| `domain/admin/service/impl/UpdateMemberStatusServiceImpl.java:32` | execute | `(기본)` |
| `domain/admin/service/impl/VerificationSignUpServiceImpl.java:38` | execute | `(기본)` |
| `domain/alert/service/impl/CreateAlertMembersServiceImpl.java:33` | execute | `(기본)` |
| `domain/alert/service/impl/CreateAlertServiceImpl.java:70` | execute | `(기본)` |
| `domain/alert/service/impl/ExistsUnreadAlertServiceImpl.java:20` | execute | `(readOnly = true)` |
| `domain/alert/service/impl/FindAlertByCurrentServiceImpl.java:35` | execute | `(readOnly = true)` |
| `domain/alert/service/impl/ReadAlertServiceImpl.java:22` | execute | `(기본)` |
| `application/app/service/FindAppVersionService.java` (PR03) | class FindAppVersionService | `(readOnly = true)` |
| `application/auth/service/FindNicknameService.java:21` | execute | `(readOnly = true)` |
| `application/auth/service/ReissueTokenService.java:25` | execute | `(기본)` |
| `application/auth/service/ResetPasswordService.java:24` | execute | `(기본)` |
| `application/auth/service/SignInService.java:32` | execute | `(기본)` |
| `application/auth/service/SignUpService.java:37` | execute | `(기본)` |
| `application/auth/service/CurrentMemberService.java:22` | execute | `(readOnly = true)` |
| `domain/block/service/impl/BlockMemberServiceImpl.java:25` | execute | `(기본)` |
| `domain/block/service/impl/GetBlockListServiceImpl.java:22` | execute | `(readOnly = true)` |
| `domain/block/service/impl/UnblockMemberServiceImpl.java:21` | execute | `(기본)` |
| `domain/chat/service/impl/ChatMessageMutationService.java:30` | update | `(기본)` |
| `domain/chat/service/impl/ChatMessageMutationService.java:45` | delete | `(기본)` |
| `domain/chat/service/impl/CreateChatRoomServiceImpl.java:30` | execute | `(기본)` |
| `domain/chat/service/impl/DeleteChatRoomServiceImpl.java:23` | execute | `(기본)` |
| `domain/chat/service/impl/FindChatMessageByRoomIdServiceImpl.java:52` | execute | `(기본)` |
| `domain/chat/service/impl/FindRoomIdByProductIdServiceImpl.java:22` | execute | `(readOnly = true)` |
| `domain/chat/service/impl/FindRoomsByCurrentUserServiceImpl.java:29` | execute | `(readOnly = true)` |
| `domain/chat/service/impl/ReadChatMessageServiceImpl.java:24` | execute | `(기본)` |
| `domain/chat/service/impl/SaveChatMessageServiceImpl.java:53` | execute | `(기본)` |
| `domain/chat/service/impl/ValidateChatSendableServiceImpl.java:35` | execute | `(readOnly = true)` |
| `application/image/service/DeleteImageService.java` (PR05) | execute | `(기본)` |
| `application/image/service/UploadImageService.java` (PR05) | execute | `(기본)` |
| `application/member/service/FindAllUserInfoService.java:23` | execute | `(readOnly = true)` |
| `application/member/service/FindMyInfoService.java:22` | execute | `(readOnly = true)` |
| `application/member/service/FindUserInfoService.java:21` | execute | `(readOnly = true)` |
| `domain/member/service/impl/MemberDeletionServiceImpl.java:51` | delete | `(기본)` |
| `domain/member/service/impl/MemberWithdrawalServiceImpl.java:26` | execute | `(기본)` |
| `application/member/service/UpdateMyInfoService.java:25` | execute | `(기본)` |
| `application/member/service/ChangeGwangsanService.java:21` (PR02 신규) | plus/minus/adjust | `(propagation = Propagation.MANDATORY)` 호출자 트랜잭션 필수 |
| `application/member/service/RegisterMemberService.java` (PR02 신규) | execute | `(기본)` 가입 트랜잭션에 참여 |
| `application/member/service/ChangePasswordService.java` (PR02 신규) | execute | `(기본)` |
| `application/member/service/MemberAccountService.java` (PR02 신규) | 클래스 | `(readOnly = true)` |
| `domain/notice/service/impl/CreateNoticeServiceImpl.java:47` | execute | `(기본)` |
| `domain/notice/service/impl/DeleteNoticeServiceImpl.java:25` | execute | `(기본)` |
| `domain/notice/service/impl/UpdateNoticeServiceImpl.java:32` | execute | `(기본)` |
| `domain/notification/service/impl/SendNotificationServiceImpl.java:29` | execute | `(기본)` |
| `domain/post/service/impl/CreateProductServiceImpl.java:31` | execute | `(기본)` |
| `domain/post/service/impl/DeleteProductByIdServiceImpl.java:24` | execute | `(기본)` |
| `domain/post/service/impl/DeleteReservationProductServiceImpl.java:42` | execute | `(기본)` |
| `domain/post/service/impl/FindProductByCurrentUserAndTypeAndModeServiceImpl.java:36` | execute | `(readOnly = true)` |
| `domain/post/service/impl/FindProductByCurrentUserAndTypeAndModeServiceImpl.java:42` | execute | `(readOnly = true)` |
| `domain/post/service/impl/FindProductByIdServiceImpl.java:42` | execute | `(readOnly = true)` |
| `domain/post/service/impl/FindProductsByMemberIdServiceImpl.java:34` | execute | `(readOnly = true)` |
| `domain/post/service/impl/FindProductsByMemberIdServiceImpl.java:40` | execute | `(readOnly = true)` |
| `domain/post/service/impl/FindProductsByTypeAndModeServiceImpl.java:36` | execute | `(readOnly = true)` |
| `domain/post/service/impl/RequestTradeCompleteServiceImpl.java:68` | execute | `(isolation = Isolation.READ_COMMITTED)` |
| `domain/post/service/impl/ReservationProductServiceImpl.java:42` | execute | `(기본)` |
| `domain/post/service/impl/ReservationReminderSender.java:30` | send | `(기본)` |
| `domain/post/service/impl/UpdateProductServiceImpl.java:40` | execute | `(기본)` |
| `domain/post/service/impl/WithdrawTradeCompleteServiceImpl.java:43` | execute | `(기본)` |
| `application/relatedkeyword/service/DeleteMemberRelatedKeywordService.java` (PR03) | execute | `(기본)` |
| `application/relatedkeyword/service/FindMemberRelatedKeywordsService.java` (PR03) | query | `(readOnly = true)` |
| `application/relatedkeyword/service/FindRelatedKeywordsService.java` (PR03) | query | `(readOnly = true)` |
| `domain/report/service/impl/CreateReportServiceImpl.java:45` | execute | `(기본)` |
| `domain/review/service/impl/CreateReviewServiceImpl.java:47` | execute | `(기본)` |
| `domain/review/service/impl/GetMyReviewListServiceImpl.java:27` | execute | `(readOnly = true)` |
| `domain/review/service/impl/GetReceivedReviewListServiceImpl.java:29` | execute | `(readOnly = true)` |
| `domain/review/service/impl/GetReceivedReviewListServiceImpl.java:35` | execute | `(readOnly = true)` |
| `domain/review/service/impl/GetReviewByMemberServiceImpl.java:29` | execute | `(readOnly = true)` |
| `domain/review/service/impl/GetReviewByMemberServiceImpl.java:35` | execute | `(readOnly = true)` |
| `domain/review/service/impl/GetReviewDetailServiceImpl.java:24` | execute | `(readOnly = true)` |
| `application/sms/service/SendFindNicknameSmsService.java:26` | execute | `(기본)` |
| `application/sms/service/SendResetPasswordSmsService.java:25` | execute | `(기본)` |
| `application/sms/service/VerifyCodeService.java:22` | execute | `(기본)` (기존 Verify 3개 서비스 통합) |
| `domain/suspend/service/impl/SuspendMemberServiceImpl.java:30` | execute | `(기본)` |
| `domain/trade/service/TradeCancelApplier.java:47` | apply | `(propagation = Propagation.MANDATORY)` |
| `domain/trade/service/impl/FindMyTradeHistoryServiceImpl.java:33` | execute | `(readOnly = true)` |
| `domain/trade/service/impl/TradeCancelServiceImpl.java:55` | execute | `(isolation = Isolation.READ_COMMITTED)` |
| `domain/trade/service/impl/TradeCancelWithdrawServiceImpl.java:37` | execute | `(isolation = Isolation.READ_COMMITTED)` |
| `global/event/handler/ChatMessageMutationEventListener.java:19` | handle | `(기본)` |
| `global/event/handler/CreateAdminAlertEventListener.java:18` | handleCreateAdminAlertEvent | `(기본)` |
| `global/event/handler/CreateAlertEventListener.java:18` | handleCreateAlertEvent | `(기본)` |
| `global/event/handler/CreateAlertMembersEventListener.java:18` | handleCreateAlertMembersEvent | `(기본)` |
| `adapter/event/image/DeleteNotUsedImageEventListener.java` (PR05 이동) | handleDeleteNotUsedImageEvent | `(기본)` — @TransactionalEventListener 매칭, 트랜잭션 없음 |
| `global/event/handler/ReservationCancelledEventListener.java:24` | handle | `(기본)` |
| `global/event/handler/SendNotificationEventListener.java:18` | handleNotification | `(기본)` |
| `global/event/handler/TradeStatusChangedEventListener.java:20` | handleTradeStatusChangedEvent | `(기본)` |
| ~~`global/scheduler/DeleteOrphanImageScheduler.java:67`~~ | findOrphanImages | ~~`(readOnly = true)`~~ self-call 로 미적용이었음. PR05: CleanupOrphanImagesService 는 트랜잭션 없음(저장소 기본 트랜잭션) |
| ~~`global/scheduler/DeleteOrphanImageScheduler.java:87`~~ | deleteFromDatabase | ~~`(REQUIRED, rollbackFor, timeout = 60)`~~ self-call 로 미적용이었음. PR05: 200건 배치마다 deleteAllByIdInBatch 자체 트랜잭션(기준선 동작 유지) |
| `global/scheduler/SuspendReleaseScheduler.java:24` | releaseSuspensions | `(기본)` |

@Transactional 94곳.

락·격리 관련 코드:

| 파일:행 | 코드 |
| --- | --- |
| `domain/admin/service/impl/ApproveTradeCancelServiceImpl.java:44` | `@Transactional(isolation = Isolation.READ_COMMITTED)` |
| `domain/admin/service/impl/RejectAdminAlertServiceImpl.java:46` | `@Transactional(isolation = Isolation.READ_COMMITTED)` |
| `domain/chat/repository/ChatMessageRepository.java:19` | `@Lock(LockModeType.PESSIMISTIC_WRITE)` |
| `domain/chat/repository/custom/impl/ChatMessageCustomRepositoryImpl.java:65` | `ChatMessage message = em.find(ChatMessage.class, messageId, LockModeType.PESSIMISTIC_READ);` |
| `domain/chat/repository/custom/impl/ChatMessageCustomRepositoryImpl.java:72` | `.setLockMode(LockModeType.PESSIMISTIC_READ)` |
| `domain/post/repository/ProductRepository.java:29` | `@Lock(LockModeType.PESSIMISTIC_WRITE)` |
| `domain/post/repository/ProductRepository.java:41` | `@Lock(LockModeType.PESSIMISTIC_WRITE)` |
| `domain/post/repository/ProductReservationRepository.java:21` | `@Lock(LockModeType.PESSIMISTIC_WRITE)` |
| `domain/post/service/impl/RequestTradeCompleteServiceImpl.java:68` | `@Transactional(isolation = Isolation.READ_COMMITTED)` |
| `domain/trade/service/impl/TradeCancelServiceImpl.java:55` | `@Transactional(isolation = Isolation.READ_COMMITTED)` |
| `domain/trade/service/impl/TradeCancelWithdrawServiceImpl.java:37` | `@Transactional(isolation = Isolation.READ_COMMITTED)` |

같은 클래스 안에 @Scheduled와 @Transactional 메서드가 공존하는 파일(자기 호출 시 프록시 미적용 여부를 담당 PR이 기준선으로 확인):

- `global/scheduler/DeleteOrphanImageScheduler.java` — PR05 확인: 프록시 미적용이 기준선. 전환 후에도 바깥 트랜잭션 없음(DeleteOrphanImageSchedulerTest 가 S3 호출 중 트랜잭션 비활성 assert)
- `global/scheduler/SuspendReleaseScheduler.java`

## 8. 스키마 (JPA 매핑)

운영 `application-prod.yml`: flyway enabled, `ddl-auto: validate`. 기본 `application.yml`: flyway disabled, `ddl-auto: update`. 테스트: `create-drop`. table/column/ID 생성/FK/cascade/enum 저장값을 전환 중 바꾸지 않는다. 배포형 validate 검증은 테스트 MariaDB에서만 한다.

Flyway migration: `V10__create_app_version.sql`, `V11__add_chat_message_mutation.sql`, `V12__reservation_reminder_delivery.sql`, `V2__add_query_path_indexes.sql`, `V3__add_trade_complete_requested_by_seller.sql`, `V4__add_chat_room_hidden_at.sql`, `V5__add_product_reservation_schedule.sql`, `V6__drop_product_reservation_status_unique.sql`, `V7__optimize_chat_room_participant_lookup.sql`, `V8__drop_member_detail_dong_unique.sql`, `V9__add_report_product.sql`

| 파일 | 종류 | table | 연관관계 | enum 저장 | auditing |
| --- | --- | --- | --- | --- | --- |
| `domain/admin/entity/AdminAlert.java` | Entity | tbl_admin_alert | ManyToOne otherMember:Member (fetch = FetchType.LAZY); ManyToOne requester:Member (fetch = FetchType.LAZY) | EnumType.STRING | Y |
| `domain/alert/entity/Alert.java` | Entity | tbl_alert | ManyToOne sendMember:Member (fetch = FetchType.LAZY) | EnumType.STRING | Y |
| `domain/alert/entity/AlertReceipt.java` | Entity | tbl_alert_receipt | ManyToOne alert:Alert (fetch = FetchType.LAZY, optional = false); ManyToOne member:Member (fetch = FetchType.LAZY, optional = false) | - | - |
| `adapter/persistence/app/AppVersionJpaEntity.java` (PR03) | Entity | tbl_app_version | - | - | - |
| `domain/block/entity/MemberBlock.java` | Entity | tbl_member_block | ManyToOne blocker:Member (fetch = FetchType.LAZY); ManyToOne blocked:Member (fetch = FetchType.LAZY) | - | Y |
| `domain/chat/entity/ChatMessage.java` | Entity | tbl_chat_message | ManyToOne room:ChatRoom (fetch = FetchType.LAZY); ManyToOne sender:Member (fetch = FetchType.LAZY) | EnumType.STRING | - |
| `domain/chat/entity/ChatMessageImage.java` | Entity | tbl_chat_message_image | ManyToOne image:Image (fetch = FetchType.LAZY); ManyToOne chatMessage:ChatMessage (fetch = FetchType.LAZY) | - | - |
| `domain/chat/entity/ChatRoom.java` | Entity | tbl_chat_room | ManyToOne buyer:Member (fetch = FetchType.LAZY); ManyToOne seller:Member (fetch = FetchType.LAZY); ManyToOne product:Product (fetch = FetchType.LAZY) | - | Y |
| `adapter/persistence/dong/DongJpaEntity.java` (PR03) | Entity | tbl_dong | - | - | - |
| `domain/image/entity/Image.java` | Entity | tbl_image | - | - | Y |
| `adapter/persistence/member/MemberJpaEntity.java` (entity name `Member`) | Entity | tbl_member | ManyToOne recommender:Member (fetch = FetchType.LAZY) | EnumType.STRING | Y |
| `adapter/persistence/member/MemberDetailJpaEntity.java` (entity name `MemberDetail`) | Entity | tbl_member_detail | OneToOne member:Member (fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.REMOVE}); ManyToOne dong:Dong (fetch = FetchType.LAZY); ManyToOne place:Place (fetch = FetchType.LAZY) | - | - |
| `adapter/persistence/member/WithdrawalRecordJpaEntity.java` (entity name `WithdrawalRecord`) | Entity | tbl_withdrawal_record | - | - | Y |
| `domain/notice/entity/Notice.java` | Entity | tbl_notice | ManyToOne place:Place (fetch = FetchType.LAZY); ManyToOne member:Member (fetch = FetchType.LAZY) | - | Y |
| `domain/notice/entity/NoticeImage.java` | Entity | tbl_notice_image | ManyToOne image:Image (fetch = FetchType.LAZY); ManyToOne notice:Notice (fetch = FetchType.LAZY) | - | - |
| `domain/place/entity/Head.java` | Entity | tbl_head_place | - | - | - |
| `domain/place/entity/Place.java` | Entity | tbl_place | ManyToOne head:Head (fetch = FetchType.LAZY) | - | - |
| `domain/post/entity/Product.java` | Entity | tbl_product | ManyToOne member:Member (fetch = FetchType.LAZY) | EnumType.STRING | Y |
| `domain/post/entity/ProductImage.java` | Entity | tbl_product_image | ManyToOne image:Image (fetch = FetchType.LAZY); ManyToOne product:Product (fetch = FetchType.LAZY) | - | - |
| `domain/post/entity/ProductReservation.java` | Entity | tbl_product_reservation | ManyToOne product:Product (fetch = FetchType.LAZY); ManyToOne reserver:Member (fetch = FetchType.LAZY) | EnumType.STRING | Y |
| `adapter/persistence/relatedkeyword/MemberRelatedKeywordJpaEntity.java` (PR03) | Entity | tbl_member_related_keyword | ManyToOne relatedKeyword:RelatedKeywordJpaEntity (fetch = FetchType.LAZY); ManyToOne member:MemberJpaEntity (fetch = FetchType.LAZY) | - | - |
| `adapter/persistence/relatedkeyword/RelatedKeywordJpaEntity.java` (PR03) | Entity | tbl_related_keyword | - | - | - |
| `domain/report/entity/Report.java` | Entity | tbl_report | ManyToOne reported:Member (fetch = FetchType.LAZY); ManyToOne reporter:Member (fetch = FetchType.LAZY); ManyToOne product:Product (fetch = FetchType.LAZY) | EnumType.STRING | - |
| `domain/report/entity/ReportImage.java` | Entity | tbl_report_image | ManyToOne image:Image (fetch = FetchType.LAZY); ManyToOne report:Report (fetch = FetchType.LAZY) | - | - |
| `domain/review/entity/Review.java` | Entity | tbl_review | ManyToOne reviewer:Member (fetch = FetchType.LAZY); ManyToOne reviewed:Member (fetch = FetchType.LAZY); ManyToOne product:Product (fetch = FetchType.LAZY) | - | - |
| `domain/suspend/entity/Suspend.java` | Entity | tbl_suspend | ManyToOne member:Member (fetch = FetchType.LAZY) | - | Y |
| `domain/trade/entity/TradeCancel.java` | Entity | tbl_trade_cancel | ManyToOne tradeComplete:TradeComplete (fetch = FetchType.LAZY); ManyToOne member:Member (fetch = FetchType.LAZY) | EnumType.STRING | Y |
| `domain/trade/entity/TradeCancelImage.java` | Entity | tbl_trade_cancel_image | ManyToOne tradeCancel:TradeCancel (fetch = FetchType.LAZY); ManyToOne image:Image (fetch = FetchType.LAZY) | - | - |
| `domain/trade/entity/TradeComplete.java` | Entity | tbl_trade_complete | ManyToOne product:Product (fetch = FetchType.LAZY); ManyToOne buyer:Member (fetch = FetchType.LAZY); ManyToOne seller:Member (fetch = FetchType.LAZY) | EnumType.STRING | Y |

업무 간 JPA association (persistence adapter 내부 저장 기술 예외로만 허용, core 교차 의존 허용 아님):

| 엔티티 | 필드 | 대상(업무.타입) | 종류 |
| --- | --- | --- | --- |
| `domain/admin/entity/AdminAlert.java` | otherMember | member.Member | ManyToOne |
| `domain/admin/entity/AdminAlert.java` | requester | member.Member | ManyToOne |
| `domain/alert/entity/Alert.java` | sendMember | member.Member | ManyToOne |
| `domain/alert/entity/AlertReceipt.java` | member | member.Member | ManyToOne |
| `domain/block/entity/MemberBlock.java` | blocked | member.Member | ManyToOne |
| `domain/block/entity/MemberBlock.java` | blocker | member.Member | ManyToOne |
| `domain/chat/entity/ChatMessage.java` | sender | member.Member | ManyToOne |
| `domain/chat/entity/ChatMessageImage.java` | image | image.Image | ManyToOne |
| `domain/chat/entity/ChatRoom.java` | buyer | member.Member | ManyToOne |
| `domain/chat/entity/ChatRoom.java` | product | post.Product | ManyToOne |
| `domain/chat/entity/ChatRoom.java` | seller | member.Member | ManyToOne |
| `adapter/persistence/member/MemberDetailJpaEntity.java` | dong | dong.DongJpaEntity (PR03) | ManyToOne |
| `adapter/persistence/member/MemberDetailJpaEntity.java` | place | place.Place | ManyToOne |
| `domain/notice/entity/Notice.java` | member | member.Member | ManyToOne |
| `domain/notice/entity/Notice.java` | place | place.Place | ManyToOne |
| `domain/notice/entity/NoticeImage.java` | image | image.Image | ManyToOne |
| `domain/post/entity/Product.java` | member | member.Member | ManyToOne |
| `domain/post/entity/ProductImage.java` | image | image.Image | ManyToOne |
| `domain/post/entity/ProductReservation.java` | reserver | member.Member | ManyToOne |
| `adapter/persistence/relatedkeyword/MemberRelatedKeywordJpaEntity.java` (PR03) | member | member.MemberJpaEntity | ManyToOne |
| `domain/report/entity/Report.java` | product | post.Product | ManyToOne |
| `domain/report/entity/Report.java` | reported | member.Member | ManyToOne |
| `domain/report/entity/Report.java` | reporter | member.Member | ManyToOne |
| `domain/report/entity/ReportImage.java` | image | image.Image | ManyToOne |
| `domain/review/entity/Review.java` | product | post.Product | ManyToOne |
| `domain/review/entity/Review.java` | reviewed | member.Member | ManyToOne |
| `domain/review/entity/Review.java` | reviewer | member.Member | ManyToOne |
| `domain/suspend/entity/Suspend.java` | member | member.Member | ManyToOne |
| `domain/trade/entity/TradeCancel.java` | member | member.Member | ManyToOne |
| `domain/trade/entity/TradeCancelImage.java` | image | image.Image | ManyToOne |
| `domain/trade/entity/TradeComplete.java` | buyer | member.Member | ManyToOne |
| `domain/trade/entity/TradeComplete.java` | product | post.Product | ManyToOne |
| `domain/trade/entity/TradeComplete.java` | seller | member.Member | ManyToOne |

## 9. bridge·legacy 제거 책임

| 항목 | 현재 상태 | 제거/교체 PR |
| --- | --- | --- |
| `legacy-classes.txt` 항목 | 전환 전 클래스 목록, PR별 태그 | 각 태그 PR (최종 0건은 PR12) |
| `MemberUtil` | 5절 호출처가 사용 | 호출처: 각 업무 PR, 클래스: PR12 |
| `@CheckBlocked`/`BlockAspect` | PR07에서 삭제. 거래 완료 요청은 `TradeBlockPort` → `adapter/integration/trade/TradeBlockBridge`(BlockValidator) | 완료 (bridge 교체는 PR09) |
| `BlockValidator` | 5절 사용처 | PR09 (공개 query 제공 후 각 호출처 PR에서 교체) |
| `GlobalException`/`ErrorCode` | 순수 JDK 타입, ArchUnit shared kernel로 허용 | PR12에서 domain/common 이동 |
| `GlobalExceptionHandler`/`ErrorResponse` | global/exception | PR12 → infrastructure/web |
| `QueryDslConfig`/`RedisConfig` | global, 다수 테스트가 @Import | PR12 → infrastructure/config |
| `JwtProvider`/`JwtProperties`/security exception | PR02 완료: adapter/security/auth(+/exception) | - |
| `MemberDetails`/`MemberDetailsService` | PR02 완료: member 공개 `MemberAccountQuery`·`MemberAccount` 사용 | - |
| PR02 bridge: `LegacySignUpLocationAdapter` (DongRepository·PlaceRepository) | PR03 완료: `adapter/integration/auth/SignUpLocationAdapter` → `DongQuery`·`PlaceQuery` | - |
| PR02 bridge: `LegacyMemberKeywordAdapter` (RelatedKeyword·MemberRelatedKeyword Repository) | PR03 완료: `adapter/integration/member/MemberKeywordAdapter` → `FindMemberRelatedKeywordsQuery`·`MemberKeywordCommand` | - |
| PR02 bridge: `LegacyDeviceTokenAdapter` (DeviceTokenRepository) | PR04 교체 완료: `adapter/integration/auth/DeviceTokenAdapter` → notification `RegisterDeviceTokenUseCase` | 완료 (PR04) |
| PR02 bridge: `SignUpAlertEventAdapter` (CreateAdminAlertEvent·CreateAlertEvent 발행, 확정 정책상 AFTER_COMMIT 비동기 유지·원자 저장 대상 아님) | adapter/event/auth | 이벤트 타입 이동 시 import 만 교체(CreateAlertEvent=PR04, CreateAdminAlertEvent=PR11), PR12 대조 |
| PR02 bridge: `MemberController` DELETE → legacy `MemberWithdrawalService`/`MemberDeletionService` | adapter/web/member, domain/member/service | PR11 |
| PR02 bridge: `MemberDetailJpaEntity` 직접 변경 메서드(plus/minus/adjustGwangsan·plusLight·updatePlace)와 `MemberJpaEntity.updateMemberRole/Status` | legacy 호출자 post/trade/admin/review/suspend/scheduler | PR07·PR09·PR11 (`ChangeGwangsanUseCase` 등 공개 command 로 교체 후 삭제) — **PR07 부분 완료**: 거래 잔액은 `ChangeGwangsanUseCase`, 회원 확인·별명은 `MemberAccountQuery`. 후기 `plusLight` 만 `ReviewMemberBridge` 에 남음(빛 공개 command 없음) |
| PR02 bridge: 타 업무의 `MemberJpaRepository`/`MemberDetailJpaRepository`/`MemberJpaEntity` 직접 사용(import 만 갱신) | admin·alert·block·chat·notice·post·relatedkeyword·report·review·suspend·trade·global | 각 소유 PR |
| PR02 bridge: `MemberCustomRepository` 가 post DTO `GetProductMemberResponse` 반환(admin 조회용) | adapter/persistence/member | PR06 또는 PR11 |
| `JwtProvider.generate*`/`RefreshTokenRepository` 의 admin 로그인 직접 사용 | domain/admin/service/impl/SignInAdminServiceImpl | PR11 (auth 공개 발급 command 필요 시 추가 요청) |
| PR04 `LegacyNotificationPortBridge` + `domain/notification/NotificationPort` | (삭제) 예약 리마인더/취소가 SendPushUseCase 직접 호출 | PR06 완료 |
| PR04 `LegacySendNotificationEventBridge` + `global/event/SendNotificationEvent` | 거래 확정·채팅·공지 발행자가 DeviceToken 목록 이벤트 발행 | PR07·PR08·PR10 (이벤트·bridge 삭제는 PR10, 대상: `PushRequestedEvent`) — **PR07 완료(거래 확정)**: `TradePushAdapter` 가 `PushRequestedEvent`(회원 ID) 발행, trade 의 DeviceToken 사용 0 |
| PR04 알림 저장 범위 (계획 "1차 구현 검토 확정") | `RecordAlertUseCase` 는 OTHER_MEMBER_TRADE_COMPLETE·TRADE_CANCEL 만 허용(`AlertType.isRecordedWithBusinessTransaction`). 그 밖의 유형은 `CreateAlertEvent`/`CreateAlertMembersEvent` → `AlertEventListener`(AFTER_COMMIT async) 유지 | PR07: `RequestTradeCompleteServiceImpl`(OTHER_MEMBER_TRADE_COMPLETE)·`TradeCancelApplier`(TRADE_CANCEL)를 동기 저장으로 바꾸고 같은 commit 에서 해당 `CreateAlertEvent` 발행 제거. PR02/09/10/11·후기는 비동기 유지. PR12 전 발행자 대조 |
| PR04 `LegacyAlertSourceAdapter` | 비동기 경로의 원천 조회를 legacy Repository 로 수행 (Port 는 유지) | 구현만 각 업무 공개 조회로 교체: PR02(회원)·PR06(상품, 완료: ProductSnapshotQuery)·PR07(거래·후기)·PR09(신고·정지)·PR10(공지), 최종 PR12 |
| PR04 `LegacyAlertImageAdapter` | 알림 이미지·거래 철회→상품 조회를 legacy Repository 로 수행 | PR06(상품, 완료: ProductImageQuery.findImageLinks — 연결 행 ID 유지)·PR07(철회)·PR09(신고)·PR10(공지), 최종 PR12 — **PR07 완료(철회)**: `productIdsByTradeCancel` → trade 공개 `TradeCancelQuery.findProductIds` |
| PR04 `AlertJpaRepository`/`AlertReceiptJpaRepository` 직접 사용 | MemberDeletionServiceImpl | PR11 (`AlertMemberCleanupUseCase`) |
| PR04 `DeviceTokenRepository` 직접 사용 | 거래 확정·채팅·공지(조회). 예약 리마인더·취소는 PR06 에서 SendPushUseCase 로 교체 완료. 로그인 저장은 PR04 에서 `RegisterDeviceTokenUseCase` 로 교체 완료 | 조회: PR07·08·10 — **PR07 완료(거래 확정 조회 제거)** |
| PR04 `LegacyAlertSourceAdapter` 의 `MemberJpaRepository.findAllByIdIn` 직접 사용 | 공지 알림 수신자 일괄 존재 확인 (N+1 회피 bridge). 단건 회원 확인은 `MemberAccountQuery` 공개 Port 사용 | 제거: 스택 끝 정리 단계 — PR02 회원 batch 조회 공개 Port 추가 후 PR04 교체 (PR12 가 PR11 이후 요청) |
| `domain.image.entity.Image`/`ImageRepository` (PR05 잔여) | ImagePersistenceAdapter·OrphanImageQueryAdapter 가 사용, 타 업무 Entity association·legacy 서비스(post/notice/report/trade/chat)도 직접 사용 | 소비 업무 PR06~PR10 이 ImageQuery/자기 persistence 로 교체 후 PR12 에서 adapter/persistence/image 로 이동 |
| `GetImageResponse` (PR05 잔여) | 타 업무 web 응답 DTO 약 40곳 공유 | 각 소비 업무 PR 에서 자기 web DTO 로 교체, 클래스 삭제는 PR12 |
| `ImageValidateUtil` (PR05 잔여) | chat ValidateChatSendable (post 는 PR06 에서 `ImageQuery.getAllByIds` 로 교체 완료) | PR08 에서 교체 후 삭제 |
| `DeleteNotUsedImageEvent`/`ImageType` (PR05 잔여) | PR06 완료: 이벤트는 `adapter/event/image` 로 이동(ImageOwnerType 사용), ImageType 삭제 | - |
| `AiModerationClient`/`AiModerationProperties` (PR05 잔여) | PR06 완료: `adapter/http/ai` 로 이동, AiImageModerationAdapter·PostImageBridgeAdapter 가 사용 | - |
| `LegacyImageReferenceCleanupAdapter` (adapter/integration/image) | Notice/ReportImageRepository 직접 사용 (post 는 UnlinkProductImageUseCase 로 교체 완료) | post=PR06 완료, report=PR09, notice=PR10 공개 command 로 교체 |
| PR06 `PostImageBridgeAdapter` (adapter/integration/post) | image 공개 ImageQuery 사용. 미사용 이미지 정리는 adapter/event/image 이벤트, 텍스트 검열은 adapter/http/ai 클라이언트 | - (전환 완료, 연결 adapter) |
| PR06 `PostMemberBridgeAdapter` (adapter/integration/post) | 닉네임은 MemberAccountQuery(공개). 지점 ID·작성자 프로필은 MemberDetailJpaRepository 직접, 차단은 BlockValidator | 프로필 공개 query PR11/PR12, 차단 PR09 |
| PR06 `PostChatBridgeAdapter` (adapter/integration/post) | ChatRoomRepository·ChatMessageRepository(방 조회, 예약 취소 시스템 메시지 저장 — 호출자 트랜잭션 참여) | PR08 |
| PR06 `PostTradeStatusBridgeAdapter` (adapter/integration/post) | TradeStateReader + legacy TradeStatusChangedEvent 발행 | PR07 — **PR07 완료**: 삭제, `adapter/integration/post/PostTradeStatusAdapter`(trade `TradeStateQuery` + `adapter.event.trade.TradeStatusChangedEvent`) |
| PR06 `ReservationNotificationBridgeAdapter`·`ReservationCancelledEventListener`·`ReservationCancelledEvent` (adapter/integration/post) | SendPushUseCase(공개, reminder 최종 실패 시 예외)·ChattingServerSystemMessageNotifier | 채팅 서버 HTTP PR08 (푸시는 PR06 에서 공개 Port 로 교체 완료) |
| PR06 `PostController` `/api/post/trade` POST·DELETE | legacy RequestTradeCompleteService/WithdrawTradeCompleteService 직접 주입 | PR07 (trade input Port로 교체) — **PR07 완료**: 핸들러 제거, 같은 URL은 `adapter/web/trade/controller/TradeCompletionController` |
| PR06 공개 Port 소비 전 타 업무 legacy 호출자(trade·review·chat·report·alert·admin·member·image 의 post Entity/Repository 직접 사용) | `adapter/persistence/post` 의 Entity·Repository 를 import 만 바꿔 계속 사용 | 각 호출자 PR(PR02·PR04·PR05·PR07·PR08·PR09·PR11)이 `.omo/evidence/hex-pr06-handoff.md` 의 post 공개 Port 로 교체 |
| PR07 남은 legacy bridge (`adapter/integration/{trade,review}/*Bridge`) | `TradeBlockBridge`·`ReviewBlockBridge`(BlockValidator), `TradeChatBridge`(chat Repository), `TradeAlertBridge` 의 관리자 알림 생성·삭제(CreateAdminAlertEvent·AdminAlertRepository, 중요 알림 2종은 `RecordAlertUseCase` 로 교체 완료), `ReviewMemberBridge.addLight`(member 빛 command 없음) | PR09, PR08, PR11, PR02 후속/PR12 |
| PR07 `TradeCancelPersistenceAdapter` 의 `ImageRepository`·legacy `Image` Entity | 철회 이미지 연결 저장(JPA association) | PR05 후속/PR12 |
| PR07 공개 Port 를 직접 쓰는 legacy 호출자 | chat `FindChatMessageByRoomIdServiceImpl`(`TradeStateQuery`·`CompletedTradeQuery`), admin `ApproveTradeCancelServiceImpl`(`ApproveTradeCancelUseCase`) | PR08, PR11 |
| trade/review JpaRepository 를 직접 쓰는 legacy 호출자 | admin `RejectAdminAlertServiceImpl`·`CreateAdminAlertServiceImpl`·`FindAlertByAlertTypeAndPlaceServiceImpl`, member `MemberDeletionServiceImpl`, PR04 `LegacyAlertSourceAdapter`(후기·철회 기각 원천) | PR11, PR02·PR11, PR04/PR12 |
