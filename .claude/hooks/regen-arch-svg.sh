#!/bin/bash
# PostToolUse: docs/gen_arch_svg.py 저장 시 architecture.svg 자동 재생성
FILE=$(jq -r '.tool_input.file_path // .tool_response.filePath // empty' 2>/dev/null)
echo "$FILE" | grep -q 'docs/gen_arch_svg\.py$' || exit 0
python3 "${CLAUDE_PROJECT_DIR}/docs/gen_arch_svg.py" 2>&1
