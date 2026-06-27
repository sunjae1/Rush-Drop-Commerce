param(
    [switch]$KeepBackend,
    [int]$BackendWaitSeconds = 90
)

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$backendDir = Join-Path $root "shopping_be"
$frontendDir = Join-Path $root "shopping_fe"
$composeFile = Join-Path $backendDir "docker-compose.dev.yml"
$backendUrl = "http://localhost:8080/actuator/health"
$frontendLocationPushed = $false
$composeArgs = @("-f", $composeFile)
$composeOverrideFile = $null

function Assert-Command($name) {
    if (-not (Get-Command $name -ErrorAction SilentlyContinue)) {
        throw "Required command '$name' was not found in PATH."
    }
}

function Wait-Backend($url, $timeoutSeconds) {
    $deadline = (Get-Date).AddSeconds($timeoutSeconds)

    while ((Get-Date) -lt $deadline) {
        try {
            $response = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 3
            if ($response.StatusCode -ge 200 -and $response.StatusCode -lt 500) {
                return
            }
        } catch {
            Start-Sleep -Seconds 2
        }
    }

    Write-Warning "Backend did not respond within $timeoutSeconds seconds. Starting frontend anyway."
}

function Get-EnvValue($path, $name) {
    foreach ($line in Get-Content $path) {
        if ($line -match "^\s*#" -or $line -match "^\s*$") {
            continue
        }

        $parts = $line -split "=", 2
        if ($parts.Length -eq 2 -and $parts[0].Trim() -eq $name) {
            return $parts[1].Trim()
        }
    }

    return $null
}

function New-DbUrlOverrideIfNeeded($envPath) {
    $dbUrl = Get-EnvValue $envPath "DB_URL"
    if (-not $dbUrl) {
        return $null
    }

    $containerDbUrl = $dbUrl `
        -replace "jdbc:mysql://localhost:", "jdbc:mysql://host.docker.internal:" `
        -replace "jdbc:mysql://127\.0\.0\.1:", "jdbc:mysql://host.docker.internal:"

    if ($containerDbUrl -eq $dbUrl) {
        return $null
    }

    $overridePath = Join-Path ([System.IO.Path]::GetTempPath()) "shopping-compose-db-url.override.yml"
    @"
services:
  app:
    environment:
      DB_URL: "$containerDbUrl"
"@ | Set-Content -Path $overridePath -Encoding UTF8

    Write-Host "Detected host-local MySQL DB_URL. Docker backend will use host.docker.internal."
    return $overridePath
}

function Assert-DockerImage($imageName) {
    docker image inspect $imageName *> $null
    if ($LASTEXITCODE -ne 0) {
        throw "Docker image '$imageName' is not available locally. Pull it manually if needed, then run this script again."
    }
}

Assert-Command "docker"
Assert-Command "npm"
Assert-DockerImage "redis:7-alpine"
Assert-DockerImage "gradle:8.12-jdk21"

if (-not (Test-Path $composeFile)) {
    throw "Compose file not found: $composeFile"
}

$backendEnvPath = Join-Path $backendDir ".env"

if (-not (Test-Path $backendEnvPath)) {
    throw "Backend .env not found. Create shopping_be/.env from shopping_be/.env.example first."
}

$composeOverrideFile = New-DbUrlOverrideIfNeeded $backendEnvPath
if ($composeOverrideFile) {
    $composeArgs += @("-f", $composeOverrideFile)
}

try {
    Write-Host "Starting backend containers..."
    Push-Location $backendDir
    docker compose @composeArgs up --detach --pull never --no-build
    Pop-Location

    Write-Host "Waiting for backend: $backendUrl"
    Wait-Backend $backendUrl $BackendWaitSeconds

    Push-Location $frontendDir
    $frontendLocationPushed = $true

    if (-not (Test-Path (Join-Path $frontendDir "node_modules"))) {
        throw "Frontend dependencies are missing. Run 'cd shopping_fe; npm install' once, then run this script again."
    }

    Write-Host ""
    Write-Host "Frontend: http://localhost:5173"
    Write-Host "Backend:  http://localhost:8080"
    Write-Host "Press Ctrl+C to stop the frontend."
    Write-Host ""

    npm run dev
} finally {
    if ($frontendLocationPushed) {
        Pop-Location -ErrorAction SilentlyContinue
    }

    if (-not $KeepBackend) {
        Write-Host "Stopping backend containers..."
        Push-Location $backendDir
        docker compose @composeArgs down
        Pop-Location
    } else {
        Write-Host "Backend containers are still running. Stop them with:"
        Write-Host "cd shopping_be; docker compose -f docker-compose.dev.yml down"
    }
}
