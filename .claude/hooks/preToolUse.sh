#!/bin/bash
# .claude/hooks/preToolUse.sh
# Block dangerous commands before execution

if [[ "$CLAUDE_TOOL_NAME" == "Bash" ]]; then
    COMMAND="$CLAUDE_TOOL_ARG_command"
    BLOCKED_PATTERNS=(
        "rm[[:space:]]+-rf[[:space:]]+/"
        "sudo[[:space:]]+rm[[:space:]]+-rf"
        ">[[:space:]]*/dev/"
        "dd[[:space:]]+if="
        "mkfs[[:space:]]+"
        "curl[[:space:]]+.*\|[[:space:]]*sh"
        "wget[[:space:]]+.*\|[[:space:]]*sh"
    )
    for pattern in "${BLOCKED_PATTERNS[@]}"; do
        if [[ "$COMMAND" =~ $pattern ]]; then
            echo "[Hook] ✗ Blocked dangerous command: $COMMAND"
            echo "This command is not allowed for safety reasons."
            exit 2
        fi
    done
fi