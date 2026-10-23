# 剪贴板预览的正确性与性能验证：
# 起服 → 起客户端（打开指定 debug 界面）→ 截图 → 读客户端日志里的剔除统计。
#
# 用法：powershell -File tools\preview-stress.ps1 -Screen clipboard-partial
#   clipboard-full     混合方块（完整方块）
#   clipboard-partial  雪片/雪块/台阶/半砖/栅栏/沙子（不完整方块）
#   clipboard-snow     石头地板 + 一层雪片
#   clipboard-fence    相邻的栅栏与楼梯
#   clipboard-big      实心 32x32x32（性能）
param([string]$Screen = 'clipboard-big')
$ErrorActionPreference = 'Stop'
$proj = Split-Path -Parent $PSScriptRoot
$parent = Split-Path -Parent $proj
$ts = if ($env:MCOO_TEST_SERVER) { $env:MCOO_TEST_SERVER } else { Join-Path $parent 'testserver' }
$out = if ($env:MCOO_SHOTS) { $env:MCOO_SHOTS } else { Join-Path $parent '_shots' }
if ($env:MCOO_GRADLE_HOME) { $env:GRADLE_USER_HOME = $env:MCOO_GRADLE_HOME }
elseif (-not $env:GRADLE_USER_HOME) {
    $shared = Join-Path (Split-Path -Parent $parent) 'McooServerFeatures\.gradle-home'
    if (Test-Path $shared) { $env:GRADLE_USER_HOME = $shared }
}
New-Item -ItemType Directory -Force -Path $out | Out-Null

# ---- 停残留 ----
Get-CimInstance Win32_Process -Filter "Name='java.exe'" | ForEach-Object {
    if ($_.CommandLine -match 'fabric-server-launch' -or $_.CommandLine -match 'devlaunchinjector') {
        Write-Host ("  stop pid " + $_.ProcessId)
        Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue
    }
}
Start-Sleep -Seconds 5
for ($i = 0; $i -lt 30; $i++) {
    if (-not (Get-NetTCPConnection -LocalPort 25599 -ErrorAction SilentlyContinue)) { break }
    Start-Sleep -Seconds 1
}

# ---- 装最新 jar ----
$jar = Get-ChildItem "$proj\build\libs" -Filter '*.jar' -File | Sort-Object LastWriteTime -Descending | Select-Object -First 1
Get-ChildItem "$ts\mods" -Filter '*.jar' -File -ErrorAction SilentlyContinue | Where-Object { $_.Name -notlike 'fabric-api*' } | ForEach-Object { Remove-Item $_.FullName -Force -ErrorAction SilentlyContinue }
Copy-Item $jar.FullName "$ts\mods\$($jar.Name)" -Force
Write-Host ("installed " + $jar.Name)

# ---- 起服 ----
$server = Start-Process -FilePath 'java' -ArgumentList @('-Xmx2G', '-jar', 'fabric-server-launch.jar', 'nogui') `
    -WorkingDirectory $ts -PassThru -NoNewWindow -RedirectStandardOutput "$ts\logs\ps.out.log" -RedirectStandardError "$ts\logs\ps.err.log"
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
    Write-Host "服务器没起来！"
    Get-Content "$ts\logs\ps.err.log" -Tail 15 -ErrorAction SilentlyContinue
    exit 1
}
Write-Host "server ready"

# ---- 起客户端（带 GUI 调试日志）----
$joinsBefore = (Select-String -Path "$ts\logs\latest.log" -Pattern 'joined the game' -ErrorAction SilentlyContinue | Measure-Object).Count
Push-Location $proj
Start-Process -FilePath 'cmd.exe' -ArgumentList @('/c', "gradlew.bat runClient -PmcDebugScreen=$Screen -PmcDebugGui=true `"-PmcQuickPlay=127.0.0.1:25599`" --console=plain > `"$ts\logs\ps-client.log`" 2>&1") -WorkingDirectory $proj -PassThru -NoNewWindow | Out-Null
Pop-Location

$joined = $false
$deadline = (Get-Date).AddSeconds(240)
while ((Get-Date) -lt $deadline) {
    Start-Sleep -Seconds 4
    $joinsNow = (Select-String -Path "$ts\logs\latest.log" -Pattern 'joined the game' -ErrorAction SilentlyContinue | Measure-Object).Count
    if ($joinsNow -gt $joinsBefore) { $joined = $true; break }
}
Write-Host ("  joined: " + $joined)
if (-not $joined) { Write-Host "未进服"; exit 1 }
Start-Sleep -Seconds 25

# ---- 截图 ----
$shot = Join-Path $out ("stress-$Screen.png")
& powershell.exe -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot 'screenshot.ps1') -Out $shot | Out-Null
Write-Host ("saved " + $shot)

# ---- 剔除统计 ----
$cl = "$ts\logs\ps-client.log"
if (Test-Path $cl) {
    Write-Host "--- 剔除统计 ---"
    Select-String -Path $cl -Pattern '\[preview\]' | Select-Object -Last 3 | ForEach-Object { Write-Host ("  " + $_.Line.Trim()) }
    Write-Host "--- 耗时相关 ---"
    Select-String -Path $cl -Pattern '(?i)exception|OutOfMemory' | Select-Object -First 3 | ForEach-Object { Write-Host ("  " + $_.Line.Trim()) }
}

Get-CimInstance Win32_Process -Filter "Name='java.exe'" | ForEach-Object {
    if ($_.CommandLine -match 'devlaunchinjector') { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }
}
Stop-Process -Id $server.Id -Force -ErrorAction SilentlyContinue
Write-Host "done"
