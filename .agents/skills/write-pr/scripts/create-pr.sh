#!/bin/bash
set -e

TITLE="${1:?Error: PR title is required. Usage: create-pr.sh <title> <body-file> [label1,label2,...]}"
BODY_FILE="${2:?Error: Body file is required. Usage: create-pr.sh <title> <body-file> [label1,label2,...]}"
LABELS="${3:-}"

if [ ! -f "$BODY_FILE" ]; then
  echo "ERROR: Body file not found: $BODY_FILE" >&2
  exit 1
fi

# Base branch — ask the repo instead of assuming a branching model.
#
# A hardcoded develop/master pair fails in two directions: it targets a branch that doesn't exist in
# trunk-based repos, and it picks the wrong one where the integration branch has another name. So reuse
# the base of an existing PR for this branch, else prefer an integration branch if the remote has one,
# else fall back to whatever GitHub reports as the default branch.
BASE=$(gh pr view --json baseRefName -q .baseRefName 2>/dev/null || true)

if [ -z "$BASE" ]; then
  DEFAULT=$(gh repo view --json defaultBranchRef -q .defaultBranchRef.name 2>/dev/null || echo main)
  CURRENT=$(git branch --show-current)

  for candidate in develop development dev; do
    if [ "$CURRENT" != "$candidate" ] && git ls-remote --exit-code --heads origin "$candidate" >/dev/null 2>&1; then
      BASE="$candidate"
      break
    fi
  done

  [ -z "$BASE" ] && BASE="$DEFAULT"
  # Standing on the integration branch means this is a release PR — target the default branch.
  [ "$CURRENT" = "$BASE" ] && BASE="$DEFAULT"
fi

ARGS=(gh pr create --title "$TITLE" --body-file "$BODY_FILE" --base "$BASE")

# Labels — only pass ones this repo actually defines. `gh pr create` fails outright on an unknown label,
# which would throw away a finished title and body over a naming difference between repos.
APPLIED=""
if [ -n "$LABELS" ]; then
  EXISTING=$(gh label list --limit 200 --json name -q '.[].name' 2>/dev/null || true)
  IFS=',' read -ra LABEL_ARRAY <<< "$LABELS"
  for label in "${LABEL_ARRAY[@]}"; do
    trimmed=$(echo "$label" | xargs)
    [ -z "$trimmed" ] && continue
    if printf '%s\n' "$EXISTING" | grep -Fxq "$trimmed"; then
      ARGS+=(--label "$trimmed")
      APPLIED="${APPLIED:+$APPLIED, }$trimmed"
    else
      echo "  (label '$trimmed' not defined in this repo — skipped)" >&2
    fi
  done
fi

echo "Creating PR..."
echo "  Title : $TITLE"
echo "  Base  : $BASE"
[ -n "$APPLIED" ] && echo "  Labels: $APPLIED"
echo ""

"${ARGS[@]}"
