# Scope Selection Guide

## Domain Names

| Scope | Description |
|-------|-------------|
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

## Module Names (Cross-cutting concerns only)

| Scope | Description |
|-------|-------------|
| global | Affects multiple modules |
| ci/cd | Build / deployment |

## Examples

**Wrong:**
- `fix/login-bug` → `fix/auth-login-bug`

**Correct:**
- `add/member-profile-api`
- `fix/auth-login-bug`
- `update/post-trade-logic`