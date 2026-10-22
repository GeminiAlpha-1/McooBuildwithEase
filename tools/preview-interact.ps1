# 剪贴板 3D 预览的交互验证：基线 → 左键旋转 → 右键平移 → 滚轮缩放，各截一张图对比。
#
# 用法：
#   powershell -File tools\preview-interact.ps1
#
# 前提：测试服已在 25599 端口（与 gui-verify.ps1 相同的 testserver 目录）。
param(
    [int]$Dx = 0,
    [int]$Dy = -60,
    [int]$DragDx = 60,
    [int]$DragDy = 40
)
$ErrorActionPreference = 'Stop'
$proj = Split-Path -Parent $PSScriptRoot
$parent = Split-Path -Parent $proj
$ts = if ($env:MCOO_TEST_SERVER) { $env:MCOO_TEST_SERVER } else { Join-Path $parent 'testserver' }
$out = if ($env:MCOO_SHOTS) { $env:MCOO_SHOTS } else { Join-Path $parent '_shots' }
if ($env:MCOO_GRADLE_HOME) { $env:GRADLE_USER_HOME = $env:MCOO_GRADLE_HOME }
elseif (-not $env:GRADLE_USER_HOME) {
    # 本机默认 gradle home 里没有 kotlin-metadata-jvm（Loom 启动客户端要拉），
    # 用默认 home 会去连 Maven Central，而本机出口 IP 被 403 → 构建失败。
    # 共享的 gradle home 在 D:\WorkSpace\McooServerFeatures\.gradle-home（仓库的同级目录）。
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
    [DllImport("user32.dll")] public static extern IntPtr GetForegroundWindow();
    [DllImport("user32.dll")] public static extern bool ShowWindow(IntPtr hWnd, int nCmdShow);
    [DllImport("user32.dll")] public static extern bool SetCursorPos(int x, int y);
    [DllImport("user32.dll")] public static extern void mouse_event(uint f, uint dx, uint dy, int d, IntPtr e);
    [DllImport("user32.dll")] public static extern void keybd_event(byte vk, byte scan, uint flags, IntPtr extra);
    public const uint LEFTDOWN = 0x0002, LEFTUP = 0x0004;
    public const uint RIGHTDOWN = 0x0008, RIGHTUP = 0x0010;
    public const uint WHEEL = 0x0800;
}
"@

function Get-MC {
    Get-Process | Where-Object { $_.MainWindowTitle -like '*Minecraft*' -and $_.MainWindowHandle -ne 0 } | Select-Object -First 1
}

# 把「相对窗口中心的逻辑像素偏移」换算成屏幕坐标
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
    $guiScale = [Math]::Max(1, [Math]::Min([Math]::Floor($cw / 320), [Math]::Floor($ch / 240)))
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

function Invoke-Drag {
    param([int]$FromX, [int]$FromY, [int]$ToX, [int]$ToY, [int]$Button = 0, [int]$Steps = 12)
    $p1 = Get-ScreenPoint -OffX $FromX -OffY $FromY
    $p2 = Get-ScreenPoint -OffX $ToX -OffY $ToY
    Focus-MC
    Write-Host ("  drag " + $p1.X + "," + $p1.Y + " -> " + $p2.X + "," + $p2.Y + " button=" + $Button)
    [Win3]::SetCursorPos($p1.X, $p1.Y) | Out-Null
    Start-Sleep -Milliseconds 300
    if ($Button -eq 0) {
        [Win3]::mouse_event([Win3]::LEFTDOWN, 0, 0, 0, [IntPtr]::Zero)
    } else {
        [Win3]::mouse_event([Win3]::RIGHTDOWN, 0, 0, 0, [IntPtr]::Zero)
    }
    Start-Sleep -Milliseconds 200
    for ($i = 1; $i -le $Steps; $i++) {
        $x = [int]($p1.X + ($p2.X - $p1.X) * $i / $Steps)
        $y = [int]($p1.Y + ($p2.Y - $p1.Y) * $i / $Steps)
        [Win3]::SetCursorPos($x, $y) | Out-Null
        Start-Sleep -Milliseconds 45
    }
    Start-Sleep -Milliseconds 200
    if ($Button -eq 0) {
        [Win3]::mouse_event([Win3]::LEFTUP, 0, 0, 0, [IntPtr]::Zero)
    } else {
        [Win3]::mouse_event([Win3]::RIGHTUP, 0, 0, 0, [IntPtr]::Zero)
    }
    Start-Sleep -Milliseconds 800
}

function Invoke-Wheel {
    param([int]$Notches = 1)
    $p = Get-ScreenPoint -OffX $Dx -OffY $Dy
    Focus-MC
    [Win3]::SetCursorPos($p.X, $p.Y) | Out-Null
    Start-Sleep -Milliseconds 300
    Write-Host ("  wheel " + $Notches + " notches at " + $p.X + "," + $p.Y)
    for ($i = 0; $i -lt [Math]::Abs($Notches); $i++) {
        # mouse_event 的 dwData 是 uint32，但 P/Invoke 声明成 int 才对得上 WHEEL_DELTA 的符号。
        # 负方向必须先转成 int 再隐式转 uint32 —— 直接 `[uint32](-120 -band 0xFFFFFFFF)` 会抛异常。
        $delta = if ($Notches -gt 0) { 120 } else { -120 }
        [Win3]::mouse_event([Win3]::WHEEL, 0, 0, [uint32]([int]$delta), [IntPtr]::Zero)
        Start-Sleep -Milliseconds 250
    }
    Start-Sleep -Milliseconds 600
}

function Save-Shot {
    param([string]$Name)
    $path = Join-Path $out ("preview-$Name.png")
    & powershell.exe -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot 'screenshot.ps1') -Out $path | Out-Null
    Write-Host ("saved " + $path)
}

# ---- 1) 起服 ----
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
$jar = Get-ChildItem "$proj\build\libs" -Filter '*.jar' -File | Sort-Object LastWriteTime -Descending | Select-Object -First 1
Get-ChildItem "$ts\mods" -Filter '*.jar' -File -ErrorAction SilentlyContinue | Where-Object { $_.Name -notlike 'fabric-api*' } | ForEach-Object { Remove-Item $_.FullName -Force -ErrorAction SilentlyContinue }
Copy-Item $jar.FullName "$ts\mods\$($jar.Name)" -Force
Write-Host ("installed " + $jar.Name)

$server = Start-Process -FilePath 'java' -ArgumentList @('-Xmx2G', '-jar', 'fabric-server-launch.jar', 'nogui') `
    -WorkingDirectory $ts -PassThru -NoNewWindow -RedirectStandardOutput "$ts\logs\pi.out.log" -RedirectStandardError "$ts\logs\pi.err.log"
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
    Get-Content "$ts\logs\pi.err.log" -Tail 15 -ErrorAction SilentlyContinue
    exit 1
}
Write-Host "server ready"

# ---- 2) 起客户端，自动连服并打开剪贴板界面 ----
$joinsBefore = (Select-String -Path "$ts\logs\latest.log" -Pattern 'joined the game' -ErrorAction SilentlyContinue | Measure-Object).Count
Push-Location $proj
Start-Process -FilePath 'cmd.exe' -ArgumentList @('/c', "gradlew.bat runClient -PmcDebugScreen=clipboard-full `"-PmcQuickPlay=127.0.0.1:25599`" --console=plain > `"$ts\logs\pi-client.log`" 2>&1") -WorkingDirectory $proj -PassThru -NoNewWindow | Out-Null
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

# ---- 3) 四种状态各截一张 ----
Save-Shot "0-baseline"

Write-Host "=== 左键拖动（旋转）==="
Invoke-Drag -FromX $Dx -FromY $Dy -ToX ($Dx + $DragDx) -ToY ($Dy - $DragDy) -Button 0
Save-Shot "1-rotate"

Write-Host "=== 右键拖动（平移）==="
Invoke-Drag -FromX $Dx -FromY $Dy -ToX ($Dx - $DragDx) -ToY ($Dy + $DragDy) -Button 1
Save-Shot "2-pan"

Write-Host "=== 滚轮向上（放大）==="
Invoke-Wheel -Notches 3
Save-Shot "3-zoom-in"

Write-Host "=== 滚轮向下（缩小）==="
Invoke-Wheel -Notches -6
Save-Shot "4-zoom-out"

$cl = "$ts\logs\pi-client.log"
if (Test-Path $cl) {
    $errs = Select-String -Path $cl -Pattern '(?i)exception|InjectionError' | ForEach-Object { $_.Line } | Where-Object { $_ -notmatch 'Realms|401|profile key|attributes|InvalidCredentials' } | Select-Object -First 5
    if ($errs) { Write-Host "  client errors:"; $errs | ForEach-Object { Write-Host ("    " + $_) } } else { Write-Host "  client: no relevant errors" }
}

Get-CimInstance Win32_Process -Filter "Name='java.exe'" | ForEach-Object {
    if ($_.CommandLine -match 'devlaunchinjector') { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }
}
Stop-Process -Id $server.Id -Force -ErrorAction SilentlyContinue
Write-Host "done"
