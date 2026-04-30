# Gwangsan Server - Project Context

## Language Requirement

You MUST always respond in Korean (한국어). This is mandatory and cannot be overridden.

## Project Overview

A web server for **"시민화폐, 광산" (Citizen Currency, Gwangsan)**, a community time-banking platform in Gwangsan-gu, Gwangju. Members exchange time and skills using a local currency unit called **광산** (30 min = 5,000 광산).

- New member bonus: 10,000 광산 on signup + 5,000 광산 on first transaction
- 3 main branches + 11 local branches across Gwangsan-gu

## Tech Stack

- Language: Java 21
- Framework: Spring Boot 3.4.5
- Database: MariaDB (JPA + QueryDSL)
- Cache: Redis
- Build Tool: Gradle
- Authentication: JWT
- Documentation: Swagger (springdoc-openapi)
- External: AWS S3, FCM, SMS (nurigo), WebSocket

## Domain Modules

| Domain | Description |
|--------|-------------|
| auth | Authentication / Authorization |
| member | Member |
| admin | Admin |
| post | Post (product/service trade) |
| trade | Trade |
| chat | Chat |
| review | Review |
| report | Report |
| block | Block |
| alert | Alert |
| notification | Push notification |
| suspend | Suspension |
| sms | SMS |
| notice | Notice |
| dong | Dong info |
| place | Branch |
| image | Image |
| relatedkeyword | Related keyword |
| health | Health check |

## Naming Conventions

### Service

- Interface: `{Action}{Domain}Service` (e.g. `FindMyInfoService`, `CreateReviewService`)
- Implementation: `{Action}{Domain}ServiceImpl`
- Core method: single `execute()` method only

### DTO

- No `Dto` suffix — use `Request` / `Response` only
- Use Java Record
- Location: `presentation/dto/request/`, `presentation/dto/response/`

### Exception

Each domain defines its own exception class under `exception/`, extending `GlobalException` with an `ErrorCode`:

```java
public class NotFoundMemberException extends GlobalException {
    public NotFoundMemberException() {
        super(ErrorCode.NOT_FOUND_MEMBER);
    }
}
```

Throw directly — never wrap in a generic exception:

```java
throw new NotFoundMemberException();
```

### URL

- `/api/{domain}`, lowercase kebab-case, singular (e.g. `/api/member`, `/api/post`)

### DB

- Column: snake_case
- Java field: camelCase

### Test Method

- Use `@Nested` + `@DisplayName`
- Method name: `it_{description}()` (e.g. `it_throws_not_found_member_exception()`)

## Exception Handling

Use `GlobalExceptionHandler` + `ErrorCode` + domain-specific custom exceptions.

## Transaction Management

- Open transactions in the `service` layer only
- Never open transactions in the `repository` layer

## Git Conventions

| Type | Usage |
|------|-------|
| add | Add new code or files |
| update | Modify existing code |
| fix | Bug fix |
| delete | Delete code or files |
| docs | Documentation |
| test | Add/modify tests |
| merge | Merge branch |
| init | Project initialization |

## New Feature Checklist

- [ ] Create Controller
- [ ] Create Service interface + implementation
- [ ] Create Repository
- [ ] Create Entity
- [ ] Create Request / Response Record
- [ ] Add ErrorCode
- [ ] Add custom Exception class
- [ ] Add Swagger documentation
