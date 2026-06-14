# 在新建的 git worktree 中运行，从主工作树拷贝「本地必需但未入库」的文件
# （.lingji 看板 id、各 .env）。新 worktree 是独立检出，gitignore 的文件不会带过来。
# 用法：在新 worktree 根目录执行  ./scripts/worktree-init.ps1
$ErrorActionPreference = "Stop"

$main = ((git worktree list --porcelain) | Where-Object { $_ -like 'worktree *' } | Select-Object -First 1) -replace '^worktree ', ''
$here = git rev-parse --show-toplevel

if ((Resolve-Path $main).Path -eq (Resolve-Path $here).Path) {
    Write-Host "当前就是主工作树，无需初始化。"
    exit 0
}

foreach ($f in @('.lingji', 'deploy/.env', 'frontend/.env.local', 'backend/.env')) {
    $src = Join-Path $main $f
    $dst = Join-Path $here $f
    if ((Test-Path $src) -and -not (Test-Path $dst)) {
        New-Item -ItemType Directory -Force -Path (Split-Path $dst) | Out-Null
        Copy-Item $src $dst
        Write-Host "已拷贝 $f"
    }
}
Write-Host "worktree 初始化完成。"
