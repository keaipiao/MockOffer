# 本地一键启停（Windows PowerShell）。用法: ./scripts/dev.ps1 [up|down|restart|logs|ps|build|clean]
param([Parameter(Position = 0)][string]$Cmd = "up")

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
$ComposeFile = Join-Path $Root "deploy/docker-compose.yml"

function Compose { docker compose -f $ComposeFile @args }

function Show-Urls {
    Write-Host "`n  服务已启动 / Services up:" -ForegroundColor Green
    Write-Host "    前端 Frontend     http://localhost:3000"
    Write-Host "    后端 Backend      http://localhost:8080   (健康 /actuator/health)"
    Write-Host "    MinIO 控制台      http://localhost:9001   (minioadmin/minioadmin)"
    Write-Host "    MinIO API (S3)    http://localhost:9000"
    Write-Host "    PostgreSQL        localhost:5432          (mockoffer/mockoffer)"
    Write-Host "    Redis             localhost:6379"
}

function Invoke-Up {
    Compose up -d
    if ($LASTEXITCODE -ne 0) {
        Write-Host "`n启动失败 / Failed to start。服务状态：" -ForegroundColor Red
        Compose ps
        Write-Host "`n失败服务日志（最近 40 行）/ Recent logs:" -ForegroundColor Red
        Compose logs --tail=40
        exit 1
    }
    Show-Urls
}

switch ($Cmd) {
    "up"      { Invoke-Up }
    "down"    { Compose down }
    "restart" { Compose down; Invoke-Up }
    "logs"    { Compose logs -f }
    "ps"      { Compose ps }
    "build"   { Compose up -d --build; if ($LASTEXITCODE -eq 0) { Show-Urls } }
    "clean"   { Compose down -v }
    default   { Write-Host "用法: ./scripts/dev.ps1 [up|down|restart|logs|ps|build|clean]" }
}
