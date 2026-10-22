# 行为验证：模拟「按住左键」与「松开」，用服务端 [build] 日志判断自动破坏是否已修复
param(
    [int]$HoldSeconds = 3,
    [int]$IdleSeconds = 3,
    # 轮盘槽位（0 基）：0=DISABLED 1=SINGLE 2=LINE 3=SQUARE ...
    # 用 LINE(2) 才会产生批量结果 → 才会走「方块预览」渲染路径
    [int]$Slot = 1
)
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
# 去掉 BOM：本机 Console.InputEncoding 默认是「带 BOM 的 UTF-8」，
# Process 的 StandardInput 会用它建 StreamWriter → 给服务端发的命令前面会多一个 U+FEFF。
try { [Console]::InputEncoding = New-Object System.Text.UTF8Encoding($false) } catch { }

Add-Type -AssemblyName System.Windows.Forms
Add-Type -AssemblyName System.Drawing
Add-Type @"
using System;
using System.Runtime.InteropServices;
public class Win2 {
    [StructLayout(LayoutKind.Sequential)] public struct RECT { public int Left, Top, Right, Bottom; }
    [StructLayout(LayoutKind.Sequential)] public struct POINT { public int X, Y; }
    [DllImport("user32.dll")] public static extern bool GetWindowRect(IntPtr hWnd, out RECT rect);
    [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr hWnd);
    [DllImport("user32.dll")] public static extern bool ShowWindow(IntPtr hWnd, int nCmdShow);
    [DllImport("user32.dll")] public static extern IntPtr GetForegroundWindow();
    [DllImport("user32.dll")] public static extern bool SetCursorPos(int x, int y);
    [DllImport("user32.dll")] public static extern void mouse_event(uint f, uint dx, uint dy, uint d, IntPtr e);
    [DllImport("user32.dll")] public static extern void keybd_event(byte bVk, byte bScan, uint dwFlags, IntPtr dwExtraInfo);
    [DllImport("user32.dll")] public static extern bool GetClientRect(IntPtr hWnd, out RECT rect);
    [DllImport("user32.dll")] public static extern bool ClientToScreen(IntPtr hWnd, ref POINT pt);
    public const uint LEFTDOWN = 0x0002, LEFTUP = 0x0004, MOVE = 0x0001, KEYUP = 0x0002;
}
"@

function Stop-Java {
    Get-CimInstance Win32_Process -Filter "Name='java.exe'" | ForEach-Object {
        $cl = $_.CommandLine
        if ($cl -match 'fabric-server-launch' -or $cl -match 'devlaunchinjector') {
            Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue
        }
    }
    Start-Sleep -Seconds 3
}

# 给服务端控制台发一条命令。
# 【坑】不能用 $server.StandardInput.WriteLine()：Process 的 StandardInput 用
# [Console]::InputEncoding 建 StreamWriter，本机默认是「带 BOM 的 UTF-8」，
# 写出去的第一条命令前面会多一个 U+FEFF → 服务端解析出的命令名变成 "﻿tp"
# → "Unknown or incomplete command"，命令根本没执行。
# 两道保险：① 脚本开头把 Console.InputEncoding 换成不带 BOM 的 UTF-8
#          ② 直接往底层流写不带 BOM 的 UTF-8 字节，绕开 StreamWriter。
function Send-ServerCommand {
    param($Proc, [string]$Command)
    $enc = New-Object System.Text.UTF8Encoding($false)
    $bytes = $enc.GetBytes($Command + "`n")
    $Proc.StandardInput.BaseStream.Write($bytes, 0, $bytes.Length)
    $Proc.StandardInput.BaseStream.Flush()
}

# 装 jar + 起服
# 服务器用 Process 起（而不是 Start-Process），这样能用控制台指令把玩家传送到大坐标 ——
# 世界渲染的坐标偏移问题只有在世界坐标很大时才会暴露（近原点的世界看不出来）。
Stop-Java
New-Item -ItemType Directory -Force -Path "$ts\mods" | Out-Null
$jar = Get-ChildItem "$proj\build\libs" -Filter '*.jar' -File | Sort-Object LastWriteTime -Descending | Select-Object -First 1
Get-ChildItem "$ts\mods" -Filter '*.jar' -File -ErrorAction SilentlyContinue | Where-Object { $_.Name -notlike 'fabric-api*' } | ForEach-Object { Remove-Item $_.FullName -Force -ErrorAction SilentlyContinue }
Copy-Item $jar.FullName "$ts\mods\$($jar.Name)" -Force
Remove-Item "$ts\logs\latest.log" -Force -ErrorAction SilentlyContinue
Remove-Item "$ts\logs\beh.out.log" -Force -ErrorAction SilentlyContinue
$psi = New-Object System.Diagnostics.ProcessStartInfo
$psi.FileName = 'java'
$psi.Arguments = '-Xmx2G -jar fabric-server-launch.jar nogui'
$psi.WorkingDirectory = $ts
$psi.UseShellExecute = $false
$psi.RedirectStandardInput = $true
$psi.RedirectStandardOutput = $true
$psi.RedirectStandardError = $true
$server = New-Object System.Diagnostics.Process
$server.StartInfo = $psi
$server.Start() | Out-Null
Register-ObjectEvent -InputObject $server -EventName OutputDataReceived -Action { if ($EventArgs.Data) { Add-Content -Path "$ts\logs\beh.out.log" -Value $EventArgs.Data } } | Out-Null
Register-ObjectEvent -InputObject $server -EventName ErrorDataReceived -Action { if ($EventArgs.Data) { Add-Content -Path "$ts\logs\beh.err.log" -Value $EventArgs.Data } } | Out-Null
$server.BeginOutputReadLine()
$server.BeginErrorReadLine()
$deadline = (Get-Date).AddSeconds(60)
while ((Get-Date) -lt $deadline) {
    Start-Sleep -Seconds 2
    if (Test-Path "$ts\logs\latest.log") {
        $c = Get-Content "$ts\logs\latest.log" -Raw -ErrorAction SilentlyContinue
        if ($c -and $c -match 'Done \(') { break }
    }
}
Write-Host "server ready"

# 起客户端
Push-Location $proj
Start-Process -FilePath 'cmd.exe' -ArgumentList @('/c', "gradlew.bat runClient -PmcDebugGui=true `"-PmcQuickPlay=127.0.0.1:25599`" --console=plain > `"$ts\logs\beh-client.log`" 2>&1") -WorkingDirectory $proj -PassThru -NoNewWindow | Out-Null
Pop-Location
Write-Host "client launching, waiting for join..."
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
if (-not $joined) { Write-Host "client did not join, aborting"; Stop-Java; exit 1 }

# 把玩家传送到大坐标（1000+），让「世界坐标偏移类」的渲染问题暴露出来
# MCOO_TELEPORT 是完整参数串（含旋转）：例 "1200 -63 1200 0 45"
# 26.2 的 /tp 语法：控制台命令不带斜杠，且坐标后面必须跟旋转（yaw pitch），
# 否则报 "Unknown or incomplete command"
if ($env:MCOO_TELEPORT) {
    Write-Host ("teleport -> " + $env:MCOO_TELEPORT)
    # 用 execute as @a run tp @s 而不是 tp @a：
    # 26.2 的 tp 对「选择器匹配到多个实体」会直接报错，execute 逐个执行就没这个问题。
    Send-ServerCommand $server "execute as @a run tp @s $($env:MCOO_TELEPORT)"
    Start-Sleep -Seconds 10
    # 回读玩家真实坐标，确认传送真的生效了
    # 注意：@a 可能匹配到多个实体（data get 只允许一个），所以用 execute as @a 逐个取
    Send-ServerCommand $server "execute as @a run data get entity @s Pos"
    Start-Sleep -Seconds 2
}
Start-Sleep -Seconds 5

# 找到 MC 窗口，把鼠标移到窗口中心
$proc = $null
$deadline = (Get-Date).AddSeconds(40)
while ((Get-Date) -lt $deadline) {
    $candidates = Get-Process | Where-Object { $_.MainWindowHandle -ne 0 -and $_.MainWindowTitle -ne '' }
    $proc = $candidates | Where-Object { $_.MainWindowTitle -like '*Minecraft*' } | Select-Object -First 1
    if (-not $proc) { $proc = $candidates | Where-Object { $_.ProcessName -eq 'java' } | Select-Object -First 1 }
    if ($proc) { break }
    Start-Sleep -Seconds 2
}
if (-not $proc) { Write-Host "no MC window"; Stop-Java; exit 1 }
[Win2]::ShowWindow($proc.MainWindowHandle, 9) | Out-Null
[Win2]::SetForegroundWindow($proc.MainWindowHandle) | Out-Null
Start-Sleep -Seconds 2
$rect = New-Object Win2+RECT
[Win2]::GetWindowRect($proc.MainWindowHandle, [ref]$rect) | Out-Null
$cx = [int](($rect.Left + $rect.Right) / 2)
$cy = [int](($rect.Top + $rect.Bottom) / 2)
[System.Windows.Forms.Cursor]::Position = New-Object System.Drawing.Point $cx, $cy
Start-Sleep -Seconds 1

# 窗口没拿到焦点的话，鼠标相对移动不会变成视角旋转 —— 点一下标题栏强制聚焦（点在标题栏不会影响游戏）。
if ([Win2]::GetForegroundWindow() -ne $proc.MainWindowHandle) {
    Write-Host "focus retry via titlebar"
    [Win2]::SetCursorPos($rect.Left + 60, $rect.Top + 10) | Out-Null
    [Win2]::mouse_event([Win2]::LEFTDOWN, 0, 0, 0, [IntPtr]::Zero)
    [Win2]::mouse_event([Win2]::LEFTUP, 0, 0, 0, [IntPtr]::Zero)
    Start-Sleep -Seconds 1
    [Win2]::SetForegroundWindow($proc.MainWindowHandle) | Out-Null
    Start-Sleep -Seconds 1
}
Write-Host ("foreground ok: " + ([Win2]::GetForegroundWindow() -eq $proc.MainWindowHandle))

# 准星必须对着方块，否则点不出破坏行为，测不出来。
# 游戏里光标是被捕获的，用相对移动（mouse_event MOVE）把视角往下压。
for ($i = 0; $i -lt 12; $i++) {
    [Win2]::mouse_event([Win2]::MOVE, 0, 30, 0, [IntPtr]::Zero)
    Start-Sleep -Milliseconds 100
}
Start-Sleep -Seconds 2
& powershell.exe -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot 'screenshot.ps1') -Out (Join-Path $out 'behavior-aim.png') | Out-Null

# ---- 切到建造模式（服务端 [build] 日志只在 build 模式下才打）----
# 轮盘是「按一下打开」的切换式：按左 Alt 打开 → 鼠标移到第 2 个槽位（BuildMode 枚举顺序：
# DISABLED, SINGLE, LINE, ...，槽位 i 在 (i*2PI/N - PI/2) 方向、半径 51 逻辑像素处）→ 左键选中。
$client = New-Object Win2+RECT
[Win2]::GetClientRect($proc.MainWindowHandle, [ref]$client) | Out-Null
$origin = New-Object Win2+POINT
$origin.X = 0
$origin.Y = 0
[Win2]::ClientToScreen($proc.MainWindowHandle, [ref]$origin) | Out-Null
$cw = $client.Right - $client.Left
$ch = $client.Bottom - $client.Top
$guiScale = [Math]::Max(1, [Math]::Min([Math]::Floor($cw / 320), [Math]::Floor($ch / 240)))
$guiW = [int]($cw / $guiScale)
$guiH = [int]($ch / $guiScale)
Write-Host ("client ${cw}x${ch} guiScale=$guiScale gui=${guiW}x${guiH}")

# 建造模式现在是**方阵**（5 列），不再是轮盘：
#   格子边长 28、底部色条 4、格子间距 4 → 格子步距 32
#   整片方阵宽 5*28+4*4 = 156、高 3*32+2*4 = 104，居中放在界面正中
$gridColumns = 5
$cellSize = 28
$cellBar = 4
$cellGap = 4
$pitch = $cellSize + $cellBar + $cellGap
$gridWidth = $gridColumns * $cellSize + ($gridColumns - 1) * $cellGap
$gridHeight = 3 * $pitch + 2 * $cellGap
$startX = [int](($guiW - $gridWidth) / 2)
$startY = [int](($guiH - $gridHeight) / 2)
$col = $Slot % $gridColumns
$row = [Math]::Floor($Slot / $gridColumns)
$sx = $startX + $col * ($cellSize + $cellGap) + $cellSize / 2
$sy = $startY + $row * $pitch + $cellSize / 2
$slotX = $origin.X + [int]($sx * $guiScale)
$slotY = $origin.Y + [int]($sy * $guiScale)
Write-Host ("slot $Slot at $slotX,$slotY (grid $col,$row)")

# 按一下左 Alt 打开轮盘
[Win2]::keybd_event(0xA4, 0, 0, [IntPtr]::Zero)
Start-Sleep -Milliseconds 120
[Win2]::keybd_event(0xA4, 0, 2, [IntPtr]::Zero)
Start-Sleep -Seconds 2
& powershell.exe -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot 'screenshot.ps1') -Out (Join-Path $out 'behavior-wheel.png') | Out-Null

# 移到槽位并点击选中
[Win2]::SetCursorPos($slotX, $slotY) | Out-Null
Start-Sleep -Seconds 1
[Win2]::mouse_event([Win2]::LEFTDOWN, 0, 0, 0, [IntPtr]::Zero)
Start-Sleep -Milliseconds 100
[Win2]::mouse_event([Win2]::LEFTUP, 0, 0, 0, [IntPtr]::Zero)
Start-Sleep -Seconds 2
& powershell.exe -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot 'screenshot.ps1') -Out (Join-Path $out 'behavior-mode.png') | Out-Null
# 放大准星附近，方便肉眼确认「预览框」到底画在哪
& powershell.exe -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot 'crop.ps1') `
    -In (Join-Path $out 'behavior-mode.png') -Out (Join-Path $out 'behavior-mode-zoom.png') `
    -X 280 -Y 165 -W 300 -H 200 -Scale 3 | Out-Null

function Count-Build {
    if (-not (Test-Path "$ts\logs\latest.log")) { return 0 }
    return (Select-String -Path "$ts\logs\latest.log" -Pattern '\[build\]' | Measure-Object).Count
}

$before = Count-Build
Write-Host ("build lines before      : " + $before)

# 按住左键
[Win2]::mouse_event([Win2]::LEFTDOWN, 0, 0, 0, [IntPtr]::Zero)
Start-Sleep -Seconds $HoldSeconds
[Win2]::mouse_event([Win2]::LEFTUP, 0, 0, 0, [IntPtr]::Zero)
$afterHold = Count-Build
Write-Host ("build lines after hold   : " + $afterHold + "   (按住 " + $HoldSeconds + " 秒)")

# 松开后再等
Start-Sleep -Seconds $IdleSeconds
$afterIdle = Count-Build
Write-Host ("build lines after idle   : " + $afterIdle + "   (松开 " + $IdleSeconds + " 秒)")
Write-Host ""
Write-Host ("=== 判定 ===")
Write-Host ("  按住期间新增 : " + ($afterHold - $before) + "  (连续破坏 = 预期 > 0)")
Write-Host ("  松开期间新增 : " + ($afterIdle - $afterHold) + "  (自动破坏 = 期望 0)")
if (($afterHold - $before) -gt 0 -and ($afterIdle - $afterHold) -eq 0) {
    Write-Host "  结论: 通过（按住能破坏，松开后不再自动破坏）"
} elseif (($afterHold - $before) -eq 0) {
    Write-Host "  结论: 无结论（按住期间也没破坏 —— 大概率准星没对着方块，看 behavior-aim.png）"
} else {
    Write-Host "  结论: 失败（松开后仍在自动破坏）"
}

Write-Host "`n=== 服务端 [build] 日志（最后 15 条）==="
Select-String -Path "$ts\logs\latest.log" -Pattern '\[build\]' | Select-Object -Last 15 | ForEach-Object { "  " + $_.Line }

& powershell.exe -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot 'screenshot.ps1') -Out (Join-Path $out 'behavior.png') | Out-Null
Stop-Java
Write-Host "done"
