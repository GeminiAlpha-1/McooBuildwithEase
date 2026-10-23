# 预览渲染链路的诊断运行：开 -Dmcoo.debugPreview，
# 日志里会打印「提交 PiP 状态」和「renderToTexture 被调用」两类信息。
#
# 用法：powershell -File tools\preview-diag.ps1 -Screen clipboard-entity
param([string]$Screen = 'clipboard-entity')
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

Get-CimInstance Win32_Process -Filter "Name='java.exe'" | ForEach-Object {
    if ($_.CommandLine -match 'fabric-server-launch' -or $_.CommandLine -match 'devlaunchinjector') {
        Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue
    }
}
Start-Sleep -Seconds 5
for ($i = 0; $i -lt 30; $i++) {
    if (-not (Get-NetTCPConnection -LocalPort 25599 -ErrorAction SilentlyContinue)) { break }
    Start-Sleep -Seconds 1
}

$jar = Get-ChildItem "$proj\build\libs" -Filter '*.jar' -File | Sort-Object LastWriteTime -Descending | Select-Object -First 1
Get-ChildItem "$ts\mods" -Filter '*.jar' -File -ErrorAction SilentlyContinue | Where-Object { $_.Name -notlike 'fabric-api*' } | ForEach-Object { Remove-Item $_.FullName -Force -ErrorAction SilentlyContinue }
Copy-Item $jar.FullName "$ts\mods\$($jar.Name)" -Force
Write-Host ("installed " + $jar.Name)

$server = Start-Process -FilePath 'java' -ArgumentList @('-Xmx2G', '-jar', 'fabric-server-launch.jar', 'nogui') `
    -WorkingDirectory $ts -PassThru -NoNewWindow -RedirectStandardOutput "$ts\logs\pd.out.log" -RedirectStandardError "$ts\logs\pd.err.log"
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
if (-not $serverUp) { Write-Host "服务器没起来"; exit 1 }
Write-Host "server ready"

$joinsBefore = (Select-String -Path "$ts\logs\latest.log" -Pattern 'joined the game' -ErrorAction SilentlyContinue | Measure-Object).Count
Push-Location $proj
Start-Process -FilePath 'cmd.exe' -ArgumentList @('/c', "gradlew.bat runClient -PmcDebugScreen=$Screen -PmcDebugPreview=true `"-PmcQuickPlay=127.0.0.1:25599`" --console=plain > `"$ts\logs\pd-client.log`" 2>&1") -WorkingDirectory $proj -PassThru -NoNewWindow | Out-Null
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
Start-Sleep -Seconds 20

$shot = Join-Path $out ("diag-$Screen.png")
& powershell.exe -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot 'screenshot.ps1') -Out $shot | Out-Null
Write-Host ("saved " + $shot)

Write-Host "--- 预览链路日志 ---"
Select-String -Path "$ts\logs\pd-client.log" -Pattern '\[preview\]' -ErrorAction SilentlyContinue |
    Select-Object -First 12 | ForEach-Object { Write-Host ("  " + $_.Line.Trim()) }
$count = (Select-String -Path "$ts\logs\pd-client.log" -Pattern '\[preview\]' -ErrorAction SilentlyContinue | Measure-Object).Count
Write-Host ("  （共 " + $count + " 行）")

Get-CimInstance Win32_Process -Filter "Name='java.exe'" | ForEach-Object {
    if ($_.CommandLine -match 'devlaunchinjector') { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }
}
Stop-Process -Id $server.Id -Force -ErrorAction SilentlyContinue
Write-Host "done"
