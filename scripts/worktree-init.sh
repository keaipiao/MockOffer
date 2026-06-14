#!/usr/bin/env bash
# 在新建的 git worktree 中运行，从主工作树拷贝「本地必需但未入库」的文件
# （.lingji 看板 id、各 .env）。新 worktree 是独立检出，gitignore 的文件不会带过来，
# 不拷的话 AI 会在新 worktree 里找不到 .lingji、误判成新项目。
# 用法：在新 worktree 根目录执行  ./scripts/worktree-init.sh
set -euo pipefail

MAIN=$(git worktree list --porcelain | awk '/^worktree /{print $2; exit}')
HERE=$(git rev-parse --show-toplevel)

if [ "$MAIN" = "$HERE" ]; then
  echo "当前就是主工作树，无需初始化。"
  exit 0
fi

FILES=(".lingji" "deploy/.env" "frontend/.env.local" "backend/.env")
for f in "${FILES[@]}"; do
  if [ -f "$MAIN/$f" ] && [ ! -f "$HERE/$f" ]; then
    mkdir -p "$(dirname "$HERE/$f")"
    cp "$MAIN/$f" "$HERE/$f"
    echo "已拷贝 $f"
  fi
done
echo "worktree 初始化完成。"
