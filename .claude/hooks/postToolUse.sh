#!/bin/bash
# .claude/hooks/postToolUse.sh
# Run compileJava after Edit or Write tool if the file is a Java file

if [[ "$CLAUDE_TOOL_NAME" == "Edit" ]] || [[ "$CLAUDE_TOOL_NAME" == "Write" ]]; then
    FILE_PATH="$CLAUDE_TOOL_ARG_path"
    if [[ "$FILE_PATH" == *.java ]]; then
        echo "[Hook] Running compileJava for $FILE_PATH"
        SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
        PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
        cd "$PROJECT_ROOT"
        ./gradlew compileJava -q
        if [ $? -eq 0 ]; then
            echo "[Hook] ✓ Compile successful"
        else
            echo "[Hook] ✗ Compile failed"
            exit 1
        fi
    fi
fi