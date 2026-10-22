# 轮盘菜单截图：轮盘是「按住按键才显示、松开就关闭」，所以要模拟按住左 Alt 再截图
param([int]$SettleSeconds = 3)
$ErrorActionPreference = 'Stop'
$proj = Split-Path -Parent $PSScriptRoot
$ts = if ($env:MCOO_TEST_SERVER) { $env:MCOO_TEST_SERVER } else {
    $sibling = Join-Path (Split-Path -Parent (Split-Path -Parent $PSScriptRoot)) 'testserver'
    $inRepo = Join-Path (Split-Path -Parent $PSScriptRoot) 'testserver'
    if (Test-Path $sibling) { $sibling } else { $inRepo }
}
$out = if ($env:MCOO_SHOTS) { $env:MCOO_SHOTS } else {
    $parent = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
    if ($parent -and (Test-Path $parent)) { Join-Path $parent '_shots' } else { Join-Path (Split-Path -Parent $PSScriptRoot) '_shots' }
}
if ($env:MCOO_GRADLE_HOME) { $env:GRADLE_USER_HOME = $env:MCOO_GRADLE_HOME }
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
    [DllImport("user32.dll")] public static extern void keybd_event(byte bVk, byte bScan, uint dwFlags, IntPtr dwExtraInfo);
    [DllImport("user32.dll")] public static extern bool SetCursorPos(int x, int y);
    [DllImport("user32.dll")] public static extern void mouse_event(uint f, int dx, int dy, uint d, IntPtr e);
    public const uint KEYUP = 0x0002;
    public const uint MOVE = 0x0001;
}
"@

function Stop-Java {
    Get-CimInstance Win32_Process -Filter "Name='java.exe'" | ForEach-Object {
        $cl = $_.CommandLine
        if ($cl -match 'fabric-server-launch' -or $cl -match 'devlaunchinjector') {
            Write-Host ("  stop pid " + $_.ProcessId)
            Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue
        }
    }
    Start-Sleep -Seconds 10
}

# 1) 装 jar + 起服
Stop-Java
New-Item -ItemType Directory -Force -Path "$ts\mods" | Out-Null
$jar = Get-ChildItem "$proj\build\libs" -Filter '*.jar' -File | Sort-Object LastWriteTime -Descending | Select-Object -First 1
Get-ChildItem "$ts\mods" -Filter '*.jar' -File -ErrorAction SilentlyContinue | Where-Object { $_.Name -notlike 'fabric-api*' } | ForEach-Object { Remove-Item $_.FullName -Force -ErrorAction SilentlyContinue }
Copy-Item $jar.FullName "$ts\mods\$($jar.Name)" -Force
Write-Host ("installed " + $jar.Name)
Remove-Item "$ts\logs\latest.log" -Force -ErrorAction SilentlyContinue
$server = Start-Process -FilePath 'java' -ArgumentList @('-Xmx2G', '-jar', 'fabric-server-launch.jar', 'nogui') `
    -WorkingDirectory $ts -PassThru -NoNewWindow -RedirectStandardOutput "$ts\logs\rad.out.log" -RedirectStandardError "$ts\logs\rad.err.log"
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
if (-not $serverUp) { Write-Host "server failed"; Get-Content "$ts\logs\rad.err.log" -Tail 15; exit 1 }
Write-Host "server ready"

# 2) 起客户端（自动开轮盘）
# 轮盘现在是「按一下打开、再按一下关闭」的切换式，调试界面直接打开它就会一直留着，
# 不用再模拟按住左 Alt。
Push-Location $proj
Start-Process -FilePath 'cmd.exe' -ArgumentList @('/c', "gradlew.bat runClient -PmcDebugScreen=radial -PmcDebugGui=true `"-PmcQuickPlay=127.0.0.1:25599`" --console=plain > `"$ts\logs\rad-client.log`" 2>&1") -WorkingDirectory $proj -PassThru -NoNewWindow | Out-Null
Pop-Location

$deadline = (Get-Date).AddSeconds(150)
$joined = $false
while ((Get-Date) -lt $deadline) {
    Start-Sleep -Seconds 4
    if (Test-Path "$ts\logs\latest.log") {
        $c = Get-Content "$ts\logs\latest.log" -Raw -ErrorAction SilentlyContinue
        if ($c -and $c -match 'joined the game') { $joined = $true; break }
    }
}
Write-Host ("joined: " + $joined)
if (-not $joined) { Write-Host "not joined"; Stop-Java; exit 1 }
Start-Sleep -Seconds 8

# 3) 聚焦窗口 + 按住左 Alt（轮盘默认键）
$proc = $null
$deadline = (Get-Date).AddSeconds(40)
while ((Get-Date) -lt $deadline) {
    $candidates = Get-Process | Where-Object { $_.MainWindowHandle -ne 0 -and $_.MainWindowTitle -ne '' }
    $proc = $candidates | Where-Object { $_.MainWindowTitle -like '*Minecraft*' } | Select-Object -First 1
    if (-not $proc) { $proc = $candidates | Where-Object { $_.ProcessName -eq 'java' } | Select-Object -First 1 }
    if ($proc) { break }
    Start-Sleep -Seconds 2
}
if (-not $proc) {
    Write-Host "no MC window"
    Get-Process | Where-Object { $_.MainWindowHandle -ne 0 } | ForEach-Object { Write-Host ("  candidate: " + $_.ProcessName + " '" + $_.MainWindowTitle + "'") }
    Stop-Java
    exit 1
}
Write-Host ("window: " + $proc.MainWindowTitle)
[Win3]::ShowWindow($proc.MainWindowHandle, 9) | Out-Null
[Win3]::SetForegroundWindow($proc.MainWindowHandle) | Out-Null
Start-Sleep -Seconds 2

# 把鼠标悬停到方阵第 2 行第 1 个格子（CUBOID）上：悬停态底色会变亮，
# 还能看到格子的 tooltip，能直观看出「方块按钮 + 底部色条」画出来没有。
# 方阵：5 列、格子边长 28、间距 4 → 步距 32；整片宽 5*28+4*4 = 156、高 3*32+2*4 = 104，居中
$client = New-Object Win3+RECT
[Win3]::GetClientRect($proc.MainWindowHandle, [ref]$client) | Out-Null
$origin = New-Object Win3+POINT
$origin.X = 0
$origin.Y = 0
[Win3]::ClientToScreen($proc.MainWindowHandle, [ref]$origin) | Out-Null
$cw = $client.Right - $client.Left
$ch = $client.Bottom - $client.Top
Write-Host ("client area: ${cw}x${ch} at $($origin.X),$($origin.Y)")
$guiScale = [Math]::Max(1, [Math]::Min([Math]::Floor($cw / 320), [Math]::Floor($ch / 240)))
$guiW = $cw / $guiScale
$guiH = $ch / $guiScale
$gridWidth = 5 * 28 + 4 * 4
$gridHeight = 3 * 32 + 2 * 4
$startX = ($guiW - $gridWidth) / 2
$startY = ($guiH - $gridHeight) / 2
$hoverX = $origin.X + [int](($startX + 14) * $guiScale)
$hoverY = $origin.Y + [int](($startY + 32 + 14) * $guiScale)

# 原版在「界面打开」时会在右上角画一条「Move with W,A,S,D」提示（任何界面都有，不是我们画的），
# 所以悬停点挑左下那格，别被它挡住。
$hoverX = $origin.X + [int](($startX + 14) * $guiScale)
$hoverY = $origin.Y + [int](($startY + 32 + 14) * $guiScale)

[Win3]::SetCursorPos($hoverX, $hoverY) | Out-Null
# 只 SetCursorPos 的话游戏可能收不到鼠标移动事件（悬停色变了但 tooltip 不出），
# 补一个 1 像素的相对移动，逼出一个真实事件。
[Win3]::mouse_event([Win3]::MOVE, 1, 0, 0, [IntPtr]::Zero)
Start-Sleep -Milliseconds 200
[Win3]::mouse_event([Win3]::MOVE, -1, 0, 0, [IntPtr]::Zero)
Write-Host "hover at $hoverX,$hoverY"

Write-Host "settling..."
Start-Sleep -Seconds $SettleSeconds

& powershell.exe -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot 'screenshot.ps1') -Out (Join-Path $out 'gui-radial.png')

Start-Sleep -Seconds 1
Stop-Java
Write-Host "done"
