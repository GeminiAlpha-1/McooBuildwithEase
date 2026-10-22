# GUI 视觉验证：起服一次 → 逐个界面：起客户端(自动开界面) → 截图 → 只停客户端
param([string]$Screens = 'settings,radial')
$ErrorActionPreference = 'Stop'
$proj = Split-Path -Parent $PSScriptRoot
$ts = if ($env:MCOO_TEST_SERVER) { $env:MCOO_TEST_SERVER } else { Join-Path (Split-Path -Parent $PSScriptRoot) 'testserver' }
$out = if ($env:MCOO_SHOTS) { $env:MCOO_SHOTS } else { Join-Path (Split-Path -Parent $PSScriptRoot) '_shots' }
if ($env:MCOO_GRADLE_HOME) { $env:GRADLE_USER_HOME = $env:MCOO_GRADLE_HOME }
New-Item -ItemType Directory -Force -Path $out | Out-Null

function Stop-Client {
    Get-CimInstance Win32_Process -Filter "Name='java.exe'" | ForEach-Object {
        if ($_.CommandLine -match 'devlaunchinjector') {
            Write-Host ("  stop client pid " + $_.ProcessId)
            Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue
        }
    }
    # 上一个客户端的 OpenGL 上下文/窗口没完全释放时，下一个客户端会在
    # "Registering Indigo renderer!" 之后直接退出（NTSTATUS 0xFFFFFFFF），所以这里多等一会儿
    Start-Sleep -Seconds 12
}

# 1) 停掉残留进程 → 装最新 jar
Get-CimInstance Win32_Process -Filter "Name='java.exe'" | ForEach-Object {
    if ($_.CommandLine -match 'fabric-server-launch' -or $_.CommandLine -match 'devlaunchinjector') {
        Write-Host ("  stop pid " + $_.ProcessId)
        Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue
    }
}
Start-Sleep -Seconds 5
New-Item -ItemType Directory -Force -Path "$ts\mods" | Out-Null
$jar = Get-ChildItem "$proj\build\libs" -Filter '*.jar' -File | Sort-Object LastWriteTime -Descending | Select-Object -First 1
Get-ChildItem "$ts\mods" -Filter '*.jar' -File -ErrorAction SilentlyContinue | Where-Object { $_.Name -notlike 'fabric-api*' } | ForEach-Object { Remove-Item $_.FullName -Force -ErrorAction SilentlyContinue }
Copy-Item $jar.FullName "$ts\mods\$($jar.Name)" -Force
Write-Host ("installed " + $jar.Name)

# 2) 起服（整个流程只起一次）
# 先把残留的 java 进程全部停掉，避免端口 25599 被占导致服务器起不来
Get-CimInstance Win32_Process -Filter "Name='java.exe'" | ForEach-Object {
    if ($_.CommandLine -match 'fabric-server-launch' -or $_.CommandLine -match 'devlaunchinjector') {
        Write-Host ("  stop pid " + $_.ProcessId)
        Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue
    }
}
Start-Sleep -Seconds 5
Remove-Item "$ts\logs\latest.log" -Force -ErrorAction SilentlyContinue
$server = Start-Process -FilePath 'java' -ArgumentList @('-Xmx2G', '-jar', 'fabric-server-launch.jar', 'nogui') `
    -WorkingDirectory $ts -PassThru -NoNewWindow -RedirectStandardOutput "$ts\logs\gui.out.log" -RedirectStandardError "$ts\logs\gui.err.log"
$deadline = (Get-Date).AddSeconds(90)
$serverUp = $false
while ((Get-Date) -lt $deadline) {
    Start-Sleep -Seconds 2
    if ($server.HasExited) { break }
    if (Test-Path "$ts\logs\latest.log") {
        $c = Get-Content "$ts\logs\latest.log" -Raw -ErrorAction SilentlyContinue
        if ($c -and $c -match 'Done \(') { $serverUp = $true; break }
    }
}
if (-not $serverUp) {
    Write-Host "服务器没起来！日志尾部："
    Get-Content "$ts\logs\gui.err.log" -Tail 15 -ErrorAction SilentlyContinue
    Get-Content "$ts\logs\gui.out.log" -Tail 15 -ErrorAction SilentlyContinue
    exit 1
}
Write-Host "server ready"

# 3) 逐个界面：起客户端 → 等进服 → 截图 → 停客户端（不动服务器）
foreach ($s in ($Screens -split ',')) {
    Stop-Client
    Write-Host ("=== client with debug screen: " + $s)
    Push-Location $proj
    Start-Process -FilePath 'cmd.exe' -ArgumentList @('/c', "gradlew.bat runClient -PmcDebugScreen=$s -PmcDebugGui=true `"-PmcQuickPlay=127.0.0.1:25599`" --console=plain > `"$ts\logs\gui-client-$s.log`" 2>&1") -WorkingDirectory $proj -PassThru -NoNewWindow | Out-Null
    Pop-Location

    # 等进服
    $joined = $false
    $deadline = (Get-Date).AddSeconds(150)
    while ((Get-Date) -lt $deadline) {
        Start-Sleep -Seconds 4
        if (Test-Path "$ts\logs\latest.log") {
            $c = Get-Content "$ts\logs\latest.log" -Raw -ErrorAction SilentlyContinue
            if ($c -and $c -match 'joined the game') { $joined = $true; break }
        }
    }
    Write-Host ("  joined: " + $joined)
    if (-not $joined) { Write-Host "  跳过（未进服）"; continue }
    Start-Sleep -Seconds 6

    $shot = Join-Path $out "gui-$s.png"
    & powershell.exe -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot 'screenshot.ps1') -Out $shot

    $cl = "$ts\logs\gui-client-$s.log"
    if (Test-Path $cl) {
        $errs = Select-String -Path $cl -Pattern '(?i)exception|InjectionError' | ForEach-Object { $_.Line } | Where-Object { $_ -notmatch 'Realms|401|profile key|attributes|InvalidCredentials' } | Select-Object -First 5
        if ($errs) { Write-Host "  client errors:"; $errs | ForEach-Object { Write-Host ("    " + $_) } } else { Write-Host "  client: no relevant errors" }
    }
}
Stop-Client
Write-Host "done"
