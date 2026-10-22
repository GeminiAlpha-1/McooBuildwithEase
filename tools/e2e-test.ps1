# 端到端自测：装新 jar → 起服 → 客户端 quickPlay 连服 → 检查双方日志
$ErrorActionPreference = 'Stop'
$proj = Split-Path -Parent $PSScriptRoot
$ts = if ($env:MCOO_TEST_SERVER) { $env:MCOO_TEST_SERVER } else { Join-Path (Split-Path -Parent $PSScriptRoot) 'testserver' }
if ($env:MCOO_GRADLE_HOME) { $env:GRADLE_USER_HOME = $env:MCOO_GRADLE_HOME }

# 1) 停掉旧进程
Get-CimInstance Win32_Process -Filter "Name='java.exe'" | ForEach-Object {
    $cl = $_.CommandLine
    if ($cl -match 'fabric-server-launch' -or $cl -match 'devlaunchinjector') {
        Write-Host ("stopping pid " + $_.ProcessId)
        Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue
    }
}
Start-Sleep -Seconds 3

# 2) 装新 jar
New-Item -ItemType Directory -Force -Path "$ts\mods" | Out-Null
$jar = Get-ChildItem "$proj\build\libs" -Filter '*.jar' -File | Sort-Object LastWriteTime -Descending | Select-Object -First 1
Get-ChildItem "$ts\mods" -Filter 'mcoo-*.jar' -File | ForEach-Object { Remove-Item $_.FullName -Force }
Copy-Item $jar.FullName "$ts\mods\$($jar.Name)" -Force
Write-Host ("installed: " + $jar.Name)

# 3) 起服
Remove-Item "$ts\logs\latest.log" -Force -ErrorAction SilentlyContinue
$server = Start-Process -FilePath 'java' -ArgumentList @('-Xmx2G', '-jar', 'fabric-server-launch.jar', 'nogui') `
    -WorkingDirectory $ts -PassThru -NoNewWindow -RedirectStandardOutput "$ts\logs\e2e.out.log" -RedirectStandardError "$ts\logs\e2e.err.log"
Write-Host ("server pid " + $server.Id)
$deadline = (Get-Date).AddSeconds(60)
while ((Get-Date) -lt $deadline) {
    Start-Sleep -Seconds 2
    if (Test-Path "$ts\logs\latest.log") {
        $c = Get-Content "$ts\logs\latest.log" -Raw -ErrorAction SilentlyContinue
        if ($c -and $c -match 'Done \(') { break }
    }
}
Write-Host "server ready"

# 4) 起客户端（quickPlay 连服）
Push-Location $proj
$client = Start-Process -FilePath 'cmd.exe' -ArgumentList @('/c', "gradlew.bat runClient `"-PmcQuickPlay=127.0.0.1:25599`" --console=plain > `"$ts\logs\e2e-client.out.log`" 2>&1") `
    -WorkingDirectory $proj -PassThru -NoNewWindow
Write-Host ("client launcher pid " + $client.Id)
Pop-Location

# 5) 等客户端进服
$joined = $false
$deadline = (Get-Date).AddSeconds(150)
while ((Get-Date) -lt $deadline) {
    Start-Sleep -Seconds 5
    if (Test-Path "$ts\logs\latest.log") {
        $c = Get-Content "$ts\logs\latest.log" -Raw -ErrorAction SilentlyContinue
        if ($c -and $c -match 'joined the game') { $joined = $true; break }
    }
    if (Test-Path "$proj\run\crash-reports") {
        $cr = Get-ChildItem "$proj\run\crash-reports" -File | Sort-Object LastWriteTime -Descending | Select-Object -First 1
        if ($cr -and $cr.LastWriteTime -gt (Get-Date).AddMinutes(-3)) { Write-Host ("CLIENT CRASHED: " + $cr.Name); break }
    }
}
Start-Sleep -Seconds 20
Write-Host "`n=== 结果 ==="
Write-Host ("joined: " + $joined)
Write-Host "`n--- 服务端异常 ---"
if (Test-Path "$ts\logs\latest.log") {
    $errs = Select-String -Path "$ts\logs\latest.log" -Pattern '(?i)Exception|ERROR' | ForEach-Object { $_.Line } | Where-Object { $_ -notmatch 'Realms' }
    if ($errs) { $errs | Select-Object -First 10 } else { Write-Host "(无)" }
}
Write-Host "`n--- 客户端崩溃报告 ---"
$cr = Get-ChildItem "$proj\run\crash-reports" -File -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending | Select-Object -First 1
if ($cr) { Write-Host ($cr.Name + "  " + $cr.LastWriteTime) } else { Write-Host "(无)" }
