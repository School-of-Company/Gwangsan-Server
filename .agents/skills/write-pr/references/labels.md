# GitHub Labels Reference

Label names differ per repo, so **read the repo's own set first** and match against it:

```bash
gh label list --limit 100
```

Select **1–2** by meaning. If nothing matches, attach none — an undefined label makes PR creation fail,
and a wrong one is worse than no label.

## Mapping Guide

Match by meaning, not exact string. The middle column lists names seen across these repos; yours may
differ in wording, language, or the `name:설명` suffix style.

| Change | Names to look for | Fallback |
|---|---|---|
| New feature, improvement, refactoring | `enhancement`, `feature`, `개선작업` | the repo's most-used PR label |
| Bug fix | `bug`, `fix`, `버그` | — |
| Docs only (README, comments) | `documentation`, `docs`, `문서화` | — |
| Release prep, version bump | `release`, `릴리즈` | — |
| Build, CI, dependencies | `chore`, `ci`, `dependencies` | — |

## Off-limits Labels (do NOT assign)

These are assigned by people, not by this skill. Names vary; the reason is what matters.

| Label                      | Reason                                                                     |
| Label kind | Names to look for | Reason |
|---|---|---|
| Review state | `waiting for review`, `검토 대기`, `needs review` | The author applies it when the PR is ready |
| Contribution invites | `help wanted`, `good first issue`, `GFI` | Issues only |
| Triage verdicts | `invalid`, `duplicate`, `wontfix` | Issues only |
| Blocked | `blocked`, `차단됨`, `on hold` | Applied by a person when another PR/issue blocks this one |

## Quick Decision

```
Bug fix?                      → the repo's bug-ish label
New feature or improvement?   → the repo's enhancement-ish label
Docs only?                    → the repo's documentation-ish label
Release?                      → the repo's release-ish label
Nothing fits?                 → no label (never invent one — PR creation fails on an unknown label)
```
