---
name: code-review
description: Run a structured checklist over changed files — DTO conventions, Java/Spring style, JPA/transaction correctness, test coverage, commit conventions, and security basics. Produces a ✓/⚠/✗ report.
allowed-tools: Bash(git *:*), Read, Glob, Grep
---

## Step 1 — Gather Changed Files

```bash
git diff develop...HEAD --name-only 2>/dev/null || git diff HEAD~5...HEAD --name-only
git diff develop...HEAD 2>/dev/null || git diff HEAD~5...HEAD
```

Read each changed `.java` file with the Read tool for detailed analysis.

---

## Step 2 — Run Checklist

### DTO
- [ ] Request DTO name follows `{Action}Request` format? (e.g. `SignInRequest`, `UpdateMyInfoRequest`)
- [ ] Response DTO name follows `{Action}{Resource}Response` format? (e.g. `FindMyInfoResponse`, `TokenResponse`)
- [ ] No `Dto` suffix used? (`MemberDto` → ✗)
- [ ] DTO declared as Java `record`?
- [ ] `@Valid` applied to Request DTO in controller method parameters?

### Java / Spring Style
- [ ] Entity class has **no** `Entity` suffix? (e.g. `Member`, not `MemberEntity`)
- [ ] Entity has `@Getter`, `@NoArgsConstructor`, `@Builder`?
- [ ] Domain methods used instead of direct field modification (setter)? (e.g. `updateStatus()`)
- [ ] Service separated into interface + `Impl` implementation?
- [ ] Single-purpose service uses `execute()` as the core method name?
- [ ] Constructor injection used? (`@RequiredArgsConstructor` + `final` fields)
- [ ] No unnecessary comments?

### JPA / Database
- [ ] Read-only service has `@Transactional(readOnly = true)`?
- [ ] Write service has `@Transactional`?
- [ ] No N+1 problem? (Fetch Join or `@EntityGraph` used for related entity queries)
- [ ] QueryDSL placed under `repository/custom/` package?
- [ ] DB column names in snake_case with explicit `@Column(name = "...")`?

### Package Structure
- [ ] Files placed in correct layer?
    - Entity → `domain/{domain}/entity/`
    - Repository → `domain/{domain}/repository/`
    - Service interface → `domain/{domain}/service/`
    - Service implementation → `domain/{domain}/service/impl/`
    - Controller → `domain/{domain}/presentation/`
    - Request DTO → `domain/{domain}/presentation/dto/request/`
    - Response DTO → `domain/{domain}/presentation/dto/response/`
    - Exception → `domain/{domain}/exception/`
- [ ] Global common code placed under `global/`?

### Test
- [ ] Test class name follows `{TestedClass}Test` format?
- [ ] Test method names follow `it_{description}()` format? (e.g. `it_throws_not_found_member_exception()`)
- [ ] Test data prepared in `@BeforeEach setUp()`?
- [ ] Given-When-Then structure followed?
- [ ] Tests written for core business logic?

### Commit Convention
- [ ] Commit message format follows `type :: Korean description`?
- [ ] Type is one of `add` / `update` / `fix` / `delete` / `docs` / `test` / `merge` / `init`?
- [ ] Description is concise, noun-ending style? (e.g. `엔티티 필드 추가`, `쿼리 수 최소화`)
- [ ] Commits split by logical unit of change?

### Security
- [ ] No hardcoded secrets (passwords, API keys, etc.)?
- [ ] No sensitive information (passwords, tokens) printed in logs?
- [ ] User input properly validated? (`@Valid`, `@NotBlank`, etc.)

---

## Step 3 — Report

Output each item in the following format:
- ✓ Pass
- ⚠ Warning (recommendation)
- ✗ Error (needs fix)

Group by category, and finish with a summary:

```
Total {n} items reviewed
✓ {p} passed / ⚠ {w} warnings / ✗ {e} errors

Errors and warnings:
- ✗ [filename] : [issue description]
- ⚠ [filename] : [recommendation]
```
