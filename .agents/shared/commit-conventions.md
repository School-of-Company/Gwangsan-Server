# Commit & PR Conventions

The single source shared by `git-commit` and `write-pr`. Whichever of the two is installed pulls this
file in automatically (`server/src/pr/shared-references.ts`), so the same rules are never maintained
in two places.

## The Project's Own Rules Win

If the target repo states its own commit format, that beats everything below. Read it first:

```bash
ls CLAUDE.md AGENTS.md CONTRIBUTING.md 2>/dev/null
find .claude/rules -name "*.md" 2>/dev/null
```

## Commit Message Format

`type(scope): description` — **unless the repo writes them differently.** Check the shape before the
vocabulary; not every repo uses Conventional Commits at all:

```bash
git log --pretty=%s -20
```

One School-of-Company repo writes `update :: 엑셀 메모리 성능 개선` — a different separator, no scope,
120 commits deep. Matching a repo's existing shape matters more than importing this one, so if the
history is consistent and different, follow it and skip the rest of this section.

- **Type**: see "Choosing the Type" below
- **Scope**: see "Choosing the Scope" below
- **Description**: 한글, 명사형 종결, 마침표 없음
  - Good: `레포 선택 드롭다운 구현`, `PR 생성 시 base branch 조회 실패 처리`
- Subject line only (no body) — except for a breaking change, which gets a `BREAKING CHANGE: <설명>` body
- Never add AI as a co-author

## Choosing the Type

Read the repo's own type vocabulary before reaching for the Conventional Commits set:

```bash
git log --pretty=%s -300 | grep -oE '^[a-z]+' | sort | uniq -c | sort -rn | head
```

**Use what comes back.** `feat` / `fix` / `refactor` / `docs` / `chore` / `test` is the fallback for a
repo with no history to read, not the answer. Repos diverge from that set more often than they follow
it — one repo here leads with `update` and has no `feat` at all, another leads with `add`, and others
carry `style`, `ui`, `translate`, `delete` or `build`. Writing `feat` into a history like that invents a
convention rather than following one.

Pick the type whose existing commits most resemble the change at hand, and read a few of them when the
name is ambiguous — `update` versus `fix` is split differently in every repo that uses both.

## Choosing the Scope

The scope is not a fixed list. **Reuse the vocabulary the repo already uses:**

```bash
# scopes in commit history:  feat(member): ... -> member
git log --pretty=%s -200 | grep -oE '^[a-z]+\(([^)]+)\)' | sed -E 's/.*\((.*)\)/\1/' | sort | uniq -c | sort -rn
# scopes in past PR titles:  [member] ... -> member
gh pr list --state all --limit 100 --json title -q '.[].title' | grep -oE '^\[[^]]+\]' | sort | uniq -c | sort -rn
```

**If the history shows a vocabulary, reuse it verbatim** — matching the project beats a more accurate
word of your own, and a one-off scope makes the history unsearchable.

If it has none (new repo, or no convention yet), derive it from the paths you're touching:

| Layout         | Path                                    | Scope       |
| -------------- | --------------------------------------- | ----------- |
| Domain package | `src/main/kotlin/.../domain/member/...`  | `member`    |
| Feature module | `src/expo/form/...`, `modules/expo/...`  | `expo`      |
| Monorepo app   | `apps/web/...`, `packages/ui/...`        | `web`, `ui` |
| Flat project   | `src/services/payment.ts`                | `payment`   |

Prefer the domain over the layer — `member` tells a reviewer more than `service` or `controller`. If the
change spans several scopes, use `global`; for build/CI-only changes, `ci`.

## PR Title Format

`[scope] description`

- Same vocabulary as the commit scope, lowercase in brackets: `[member]`, `[expo]`, `[global]`
- Description: Korean, concise, no emojis, max 50 characters total
