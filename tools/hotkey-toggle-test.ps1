# 建造模式面板的开关验证：按一次快捷键打开，再按一次关掉。
#
# 用法：
#   powershell -File tools\hotkey-toggle-test.ps1
param()
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

Add-Type @"
using System;
using System.Runtime.InteropServices;
public class Win4 {
    [StructLayout(LayoutKind.Sequential)] public struct RECT { public int Left, Top, Right, Bottom; }
    [DllImport("user32.dll")] public static extern bool GetClientRect(IntPtr hWnd, out RECT rect);
    [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr hWnd);
    [DllImport("user32.dll")] public static extern bool ShowWindow(IntPtr hWnd, int nCmdShow);
    [DllImport("user32.dll")] public static extern void keybd_event(byte vk, byte scan, uint flags, IntPtr extra);
}
"@

function Get-MC {
    Get-Process | Where-Object { $_.MainWindowTitle -like '*Minecraft*' -and $_.MainWindowHandle -ne 0 } | Select-Object -First 1
}

function Focus-MC {
    $proc = Get-MC
    if (-not $proc) { throw "找不到 MC 窗口" }
    [Win4]::ShowWindow($proc.MainWindowHandle, 9) | Out-Null
    [Win4]::SetForegroundWindow($proc.MainWindowHandle) | Out-Null
    Start-Sleep -Milliseconds 900
}

# 按一次左 Alt（VK_LMENU = 0xA4）
function Invoke-Alt {
    param([string]$Label)
    Focus-MC
    Write-Host ("  press Left Alt (" + $Label + ")")
    [Win4]::keybd_event(0xA4, 0, 0, [IntPtr]::Zero)
    Start-Sleep -Milliseconds 120
    [Win4]::keybd_event(0xA4, 0, 2, [IntPtr]::Zero)
    Start-Sleep -Milliseconds 1200
}

function Save-Shot {
    param([string]$Name)
    $path = Join-Path $out ("hotkey-$Name.png")
    & powershell.exe -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot 'screenshot.ps1') -Out $path | Out-Null
    Write-Host ("saved " + $path)
}

# ---- 起服 ----
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
    -WorkingDirectory $ts -PassThru -NoNewWindow -RedirectStandardOutput "$ts\logs\hk.out.log" -RedirectStandardError "$ts\logs\hk.err.log"
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

# ---- 起客户端（用调试场景直接把建造模式面板打开，再按快捷键验证能关掉） ----
$joinsBefore = (Select-String -Path "$ts\logs\latest.log" -Pattern 'joined the game' -ErrorAction SilentlyContinue | Measure-Object).Count
Push-Location $proj
Start-Process -FilePath 'cmd.exe' -ArgumentList @('/c', "gradlew.bat runClient -PmcDebugScreen=radial `"-PmcQuickPlay=127.0.0.1:25599`" --console=plain > `"$ts\logs\hk-client.log`" 2>&1") -WorkingDirectory $proj -PassThru -NoNewWindow | Out-Null
Pop-Location

$joined = $false
$deadline = (Get-Date).AddSeconds(180)
while ((Get-Date) -lt $deadline) {
    Start-Sleep -Seconds 4
    $joinsNow = (Select-String -Path "$ts\logs\latest.log" -Pattern 'joined the game' -ErrorAction SilentlyContinue | Measure-Object).Count
    if ($joinsNow -gt $joinsBefore) { $joined = $true; break }
}
Write-Host ("  joined: " + $joined)
if (-not $joined) { Write-Host "未进服，跳过"; exit 1 }
Start-Sleep -Seconds 12
Focus-MC

Save-Shot "0-panel-open"

Write-Host "=== 按一次左 Alt（应该关掉建造模式面板）==="
Invoke-Alt "close"
Save-Shot "1-panel-closed"

Write-Host "=== 按 F3（验证合成按键到底有没有进 MC）==="
Focus-MC
[Win4]::keybd_event(0x72, 0, 0, [IntPtr]::Zero)
Start-Sleep -Milliseconds 120
[Win4]::keybd_event(0x72, 0, 2, [IntPtr]::Zero)
Start-Sleep -Milliseconds 1200
Save-Shot "2-f3"

Get-CimInstance Win32_Process -Filter "Name='java.exe'" | ForEach-Object {
    if ($_.CommandLine -match 'devlaunchinjector') { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }
}
Stop-Process -Id $server.Id -Force -ErrorAction SilentlyContinue
Write-Host "done"
