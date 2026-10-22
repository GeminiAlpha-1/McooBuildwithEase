# 构建 + 在真实 Fabric 专用服务器里冒烟测试
# 用法: -SkipBuild / -TimeoutSeconds 120
param(
    [switch]$SkipBuild,
    [int]$TimeoutSeconds = 120
)
$ErrorActionPreference = 'Stop'
$proj = Split-Path -Parent $PSScriptRoot
$ts = if ($env:MCOO_TEST_SERVER) { $env:MCOO_TEST_SERVER } else { Join-Path (Split-Path -Parent $PSScriptRoot) 'testserver' }
$gradleHome = if ($env:MCOO_GRADLE_HOME) { $env:MCOO_GRADLE_HOME } else { $env:GRADLE_USER_HOME }
if ($env:MCOO_GRADLE_HOME) { $env:GRADLE_USER_HOME = $env:MCOO_GRADLE_HOME }

if (-not $SkipBuild) {
    Write-Host "=== 构建 ==="
    Push-Location $proj
    & .\gradlew.bat build --console=plain 2>&1 | Select-Object -Last 15
    $code = $LASTEXITCODE
    Pop-Location
    if ($code -ne 0) { Write-Host "BUILD FAILED ($code)"; exit $code }
}

$jar = Get-ChildItem "$proj\build\libs" -Filter '*.jar' -File | Sort-Object LastWriteTime -Descending | Select-Object -First 1
if (-not $jar) { Write-Host "jar not found"; exit 1 }
Write-Host "jar: $($jar.Name) ($([math]::Round($jar.Length/1KB)) KB)"

# 准备测试服
New-Item -ItemType Directory -Force -Path "$ts\mods" | Out-Null
Get-ChildItem "$ts\mods" -Filter '*.jar' -File | ForEach-Object { Remove-Item $_.FullName -Force }
Copy-Item $jar.FullName "$ts\mods\$($jar.Name)" -Force

# fabric-api 换成服务器同款 0.153.0+26.2
$api = Get-ChildItem (Join-Path $gradleHome 'caches\modules-2\files-2.1\net.fabricmc.fabric-api\fabric-api') -Recurse -File -Filter 'fabric-api-0.153.0+26.2.jar' -ErrorAction SilentlyContinue | Select-Object -First 1
if ($api) { Copy-Item $api.FullName "$ts\mods\fabric-api-0.153.0+26.2.jar" -Force; Write-Host "fabric-api 0.153.0+26.2 installed" }
else { Write-Host "WARN: fabric-api 0.153.0+26.2 not found in cache" }

if (-not (Test-Path "$ts\eula.txt")) { Set-Content "$ts\eula.txt" "eula=true" -Encoding UTF8 }
else { (Get-Content "$ts\eula.txt") -replace 'eula=false', 'eula=true' | Set-Content "$ts\eula.txt" -Encoding UTF8 }

# 启动服务器（后台），收集日志
$logOut = "$ts\logs\self-test.out.log"
$logErr = "$ts\logs\self-test.err.log"
Write-Host "=== 启动服务器（最多 $TimeoutSeconds 秒）==="
$p = Start-Process -FilePath 'java' -ArgumentList @('-Xmx2G', '-jar', 'fabric-server-launch.jar', 'nogui') `
    -WorkingDirectory $ts -PassThru -NoNewWindow -RedirectStandardOutput $logOut -RedirectStandardError $logErr
$deadline = (Get-Date).AddSeconds($TimeoutSeconds)
$done = $false
while ((Get-Date) -lt $deadline) {
    Start-Sleep -Seconds 3
    if ($p.HasExited) { break }
    if (Test-Path $logOut) {
        $c = Get-Content $logOut -Raw -ErrorAction SilentlyContinue
        if ($c -and $c -match 'Done \(.*\)! For help, type "help"') { $done = $true; break }
    }
}
if (-not $p.HasExited) { Write-Host "stopping server..."; $p.CloseMainWindow() | Out-Null; Start-Sleep -Seconds 3; if (-not $p.HasExited) { $p.Kill() } }
Start-Sleep -Seconds 2

Write-Host "`n================ 服务器日志（关键行） ================"
if (Test-Path $logOut) {
    Get-Content $logOut | Select-String -Pattern 'McooBuildwithEase|mcoo_build_with_ease|Loading .* mods|Done \(|ERROR|Exception|Caused by|mixin|Mixin|DiscardedPayload|Unknown' | Select-Object -First 60
}
Write-Host "`n================ stderr ================"
if (Test-Path $logErr) { Get-Content $logErr | Select-Object -First 40 }
Write-Host "`nserver ready: $done"
