---
name: write-pr
description: Generate PR title, body, and labels from commits since the base branch, then create the PR on GitHub. Handles base branch detection, label selection, and PR creation end-to-end.
allowed-tools: Bash(git *:*), Bash(bash *create-pr.sh:*), Bash(cat *:*), Read, Write
---

## Step 1 — Find the Base Branch

Don't assume `develop` or `main` — ask the repo, since projects here use both.

```bash
git branch --show-current
gh repo view --json defaultBranchRef -q .defaultBranchRef.name
git ls-remote --heads origin develop development dev | sed 's#.*refs/heads/##'
```

`BASE` is the integration branch if the remote has one, otherwise the default branch. Use it everywhere
below.

## Step 2 — Gather Context

```bash
git fetch origin "$BASE" --quiet 2>/dev/null || true
git log "origin/$BASE..HEAD" --oneline
git diff "origin/$BASE...HEAD" --stat
git diff "origin/$BASE...HEAD"
cat .github/PULL_REQUEST_TEMPLATE.md 2>/dev/null
```

## Step 3 — Learn This Project's Scope Vocabulary

Read `.agents/shared/commit-conventions.md` — it holds the commands for reading the repo's existing
scope vocabulary, the fallback for deriving one from the changed paths, and the title format. It ships
with this skill, so it is always present.

## Step 4 — Determine Labels

Labels differ per repo, so read the repo's own set and match against it:

```bash
gh label list --limit 100
```

Pick **1–2** by meaning, using `.agents/skills/write-pr/references/labels.md` as the mapping guide. If nothing
matches, attach none — a wrong label is worse than no label, and an undefined one fails PR creation.

## Step 5 — Generate PR Content

**Title** — Generate 3 options in the format `[scope] description`:

- Scope: from Step 3. Lowercase, in brackets — `[member]`, `[expo]`, `[global]`
- Description: Korean, concise, no emojis, max 50 characters total
- Wrap class names, method names, annotations, file names, and technical terms in backticks (e.g., `@Transactional`, `MemberService`, `SKILL.md`)

**Body** — Follow the `.github/PULL_REQUEST_TEMPLATE.md` structure:

- Korean 합쇼체: `~하였습니다`, `~되었습니다`, `~추가하였습니다`
- No emojis
- Max 2500 characters
- Wrap all proper nouns and technical identifiers in backticks: class names, method names, annotations, file names, field names, config keys, module names, and agent names.

## Step 6 — Write Body & Show Preview

Write the body to `PR_BODY.md`, then display:

```
## PR 제목 후보
1. [title1]
2. [title2]
3. [title3]

## 선택된 라벨
- label1, label2

## PR 본문 미리보기
[body content]
```

Ask the user which title to use (present options 1/2/3). Wait for the answer before proceeding.

## Step 7 — Create PR

Run the creation script with the confirmed title and labels (it resolves the base branch the same way
and silently drops labels this repo doesn't define):

```bash
bash .agents/skills/write-pr/scripts/create-pr.sh "<confirmed-title>" "PR_BODY.md" "<label1>,<label2>"
```

After creation, display the PR URL.
Cleanup: remove `PR_BODY.md`.
