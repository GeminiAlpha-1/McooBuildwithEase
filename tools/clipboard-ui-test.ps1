# 剪贴板界面这一轮改动的验证：
#   1) 剪切板按钮排版（两排各三个等宽）
#   2) 视角面板的按钮文本 + 点轴按钮后自动退回
#   3) 快照历史列表能滚动看完
#
# 用法：
#   powershell -File tools\clipboard-ui-test.ps1
param(
    [string]$Screen = 'clipboard-full'
)
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
public class Win3 {
    [StructLayout(LayoutKind.Sequential)] public struct RECT { public int Left, Top, Right, Bottom; }
    [StructLayout(LayoutKind.Sequential)] public struct POINT { public int X, Y; }
    [DllImport("user32.dll")] public static extern bool GetWindowRect(IntPtr hWnd, out RECT rect);
    [DllImport("user32.dll")] public static extern bool GetClientRect(IntPtr hWnd, out RECT rect);
    [DllImport("user32.dll")] public static extern bool ClientToScreen(IntPtr hWnd, ref POINT pt);
    [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr hWnd);
    [DllImport("user32.dll")] public static extern bool ShowWindow(IntPtr hWnd, int nCmdShow);
    [DllImport("user32.dll")] public static extern bool SetCursorPos(int x, int y);
    [DllImport("user32.dll")] public static extern void mouse_event(uint f, uint dx, uint dy, int d, IntPtr e);
    public const uint LEFTDOWN = 0x0002, LEFTUP = 0x0004;
    public const uint RIGHTDOWN = 0x0008, RIGHTUP = 0x0010;
    public const uint WHEEL = 0x0800;
}
"@

function Get-MC {
    Get-Process | Where-Object { $_.MainWindowTitle -like '*Minecraft*' -and $_.MainWindowHandle -ne 0 } | Select-Object -First 1
}

# GUI 逻辑坐标 → 屏幕坐标（相对窗口中心的偏移量是逻辑像素）
function Get-ScreenPoint {
    param([int]$OffX, [int]$OffY)
    $proc = Get-MC
    if (-not $proc) { return $null }
    $client = New-Object Win3+RECT
    [Win3]::GetClientRect($proc.MainWindowHandle, [ref]$client) | Out-Null
    $origin = New-Object Win3+POINT
    [Win3]::ClientToScreen($proc.MainWindowHandle, [ref]$origin) | Out-Null
    $cw = $client.Right - $client.Left
    $ch = $client.Bottom - $client.Top
    # 开发客户端的窗口固定 854x480，MC 的 GUI 缩放是 2。
    # 【别用 min(floor(cw/320), floor(ch/240)) 推】那个公式算出来是 1
    # （窗口有标题栏，客户区高度 449，449/240 = 1），点下去会全部偏移。
    $guiScale = 2
    $x = $origin.X + [int](($cw / 2 + $OffX * $guiScale))
    $y = $origin.Y + [int](($ch / 2 + $OffY * $guiScale))
    return [PSCustomObject]@{ X = $x; Y = $y; Scale = $guiScale }
}

function Focus-MC {
    $proc = Get-MC
    if (-not $proc) { throw "找不到 MC 窗口" }
    [Win3]::ShowWindow($proc.MainWindowHandle, 9) | Out-Null
    [Win3]::SetForegroundWindow($proc.MainWindowHandle) | Out-Null
    Start-Sleep -Milliseconds 900
}

function Invoke-Click {
    param([int]$OffX, [int]$OffY, [int]$Button = 0)
    $p = Get-ScreenPoint -OffX $OffX -OffY $OffY
    Focus-MC
    Write-Host ("  click " + $p.X + "," + $p.Y + " (off $OffX,$OffY)")
    [Win3]::SetCursorPos($p.X, $p.Y) | Out-Null
    Start-Sleep -Milliseconds 350
    if ($Button -eq 0) { [Win3]::mouse_event([Win3]::LEFTDOWN, 0, 0, 0, [IntPtr]::Zero) } else { [Win3]::mouse_event([Win3]::RIGHTDOWN, 0, 0, 0, [IntPtr]::Zero) }
    Start-Sleep -Milliseconds 120
    if ($Button -eq 0) { [Win3]::mouse_event([Win3]::LEFTUP, 0, 0, 0, [IntPtr]::Zero) } else { [Win3]::mouse_event([Win3]::RIGHTUP, 0, 0, 0, [IntPtr]::Zero) }
    Start-Sleep -Milliseconds 900
}

function Invoke-Wheel {
    param([int]$Notches = 1, [int]$OffX = 0, [int]$OffY = 0)
    $p = Get-ScreenPoint -OffX $OffX -OffY $OffY
    Focus-MC
    [Win3]::SetCursorPos($p.X, $p.Y) | Out-Null
    Start-Sleep -Milliseconds 300
    Write-Host ("  wheel " + $Notches + " notches at " + $p.X + "," + $p.Y)
    for ($i = 0; $i -lt [Math]::Abs($Notches); $i++) {
        $delta = if ($Notches -gt 0) { 120 } else { -120 }
        [Win3]::mouse_event([Win3]::WHEEL, 0, 0, $delta, [IntPtr]::Zero)
        Start-Sleep -Milliseconds 250
    }
    Start-Sleep -Milliseconds 600
}

function Save-Shot {
    param([string]$Name)
    $path = Join-Path $out ("ui-$Screen-$Name.png")
    & powershell.exe -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot 'screenshot.ps1') -Out $path | Out-Null
    Write-Host ("saved " + $path)
}

# ---- 1) 起服 ----
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
    -WorkingDirectory $ts -PassThru -NoNewWindow -RedirectStandardOutput "$ts\logs\ui.out.log" -RedirectStandardError "$ts\logs\ui.err.log"
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

# ---- 2) 起客户端，自动连服并打开剪贴板界面 ----
$joinsBefore = (Select-String -Path "$ts\logs\latest.log" -Pattern 'joined the game' -ErrorAction SilentlyContinue | Measure-Object).Count
Push-Location $proj
Start-Process -FilePath 'cmd.exe' -ArgumentList @('/c', "gradlew.bat runClient -PmcDebugScreen=$Screen `"-PmcQuickPlay=127.0.0.1:25599`" --console=plain > `"$ts\logs\ui-client.log`" 2>&1") -WorkingDirectory $proj -PassThru -NoNewWindow | Out-Null
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

# ---- 3) 各状态截图 ----
# 剪切板面板：宽 264、高 238，GUI 尺寸 427x240 → 左上角 (81, 1)
#   第一排（上面）：历史 / 清除 / 视角   —— y = 189..209
#   第二排（下面）：下载 / 导出 / 完成   —— y = 213..233
#   按钮宽 81，x 分别是 87..168 / 172..253 / 257..338
$offHistoryX = 127 - 213
$offViewpointX = 297 - 213
$offRow1Y = 199 - 120
# 视角面板：高 144 → 左上角 (81, 48)；X 轴按钮在第 4 排 → y = 70..90
$offAxisX = 213 - 213
$offAxisY = 80 - 120

Save-Shot "1-clipboard"

Write-Host "=== 点「视角」==="
Invoke-Click -OffX $offViewpointX -OffY $offRow1Y
Save-Shot "2-viewpoint"

Write-Host "=== 点「绕 X 轴旋转」==="
Invoke-Click -OffX $offAxisX -OffY $offAxisY
Save-Shot "3-after-axis"

Write-Host "=== 点「历史」==="
Invoke-Click -OffX $offHistoryX -OffY $offRow1Y
Save-Shot "4-history"

Write-Host "=== 历史列表向下滚 ==="
Invoke-Wheel -Notches -5 -OffX 0 -OffY -20
Save-Shot "5-history-scrolled"

$cl = "$ts\logs\ui-client.log"
if (Test-Path $cl) {
    $errs = Select-String -Path $cl -Pattern '(?i)exception|InjectionError' | ForEach-Object { $_.Line } | Where-Object { $_ -notmatch 'Realms|401|profile key|attributes|InvalidCredentials' } | Select-Object -First 5
    if ($errs) { Write-Host "  client errors:"; $errs | ForEach-Object { Write-Host ("    " + $_) } } else { Write-Host "  client: no relevant errors" }
}

Get-CimInstance Win32_Process -Filter "Name='java.exe'" | ForEach-Object {
    if ($_.CommandLine -match 'devlaunchinjector') { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }
}
Stop-Process -Id $server.Id -Force -ErrorAction SilentlyContinue
Write-Host "done"
