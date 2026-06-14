# 本地一键启停（Windows PowerShell）。用法: ./scripts/dev.ps1 [up|down|restart|logs|ps|build|clean]
param([Parameter(Position = 0)][string]$Cmd = "up")

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
$ComposeFile = Join-Path $Root "deploy/docker-compose.yml"

function Compose { docker compose -f $ComposeFile @args }

switch ($Cmd) {
    "up"      { Compose up -d }
    "down"    { Compose down }
    "restart" { Compose down; Compose up -d }
    "logs"    { Compose logs -f }
    "ps"      { Compose ps }
    "build"   { Compose up -d --build }
    "clean"   { Compose down -v }
    default   { Write-Host "用法: ./scripts/dev.ps1 [up|down|restart|logs|ps|build|clean]" }
}
