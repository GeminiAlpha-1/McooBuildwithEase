# GUI 视觉验证：起服一次 → 逐个界面：起客户端(自动开界面) → 截图 → 只停客户端
param([string]$Screens = 'settings,radial', [string]$Click = '', [string]$Keys = '')
$ErrorActionPreference = 'Stop'
$proj = Split-Path -Parent $PSScriptRoot
$ts = if ($env:MCOO_TEST_SERVER) { $env:MCOO_TEST_SERVER } else {
    # 测试服可能在仓库同级目录，也可能在仓库内：两个都认，谁存在用谁
    $sibling = Join-Path (Split-Path -Parent (Split-Path -Parent $PSScriptRoot)) 'testserver'
    $inRepo = Join-Path (Split-Path -Parent $PSScriptRoot) 'testserver'
    if (Test-Path $sibling) { $sibling } else { $inRepo }
}
# 截图默认放仓库同级目录（和测试服一致），仓库根是盘根时才放仓库内
$out = if ($env:MCOO_SHOTS) { $env:MCOO_SHOTS } else {
    $parent = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
    if ($parent -and (Test-Path $parent)) { Join-Path $parent '_shots' } else { Join-Path (Split-Path -Parent $PSScriptRoot) '_shots' }
}
if ($env:MCOO_GRADLE_HOME) { $env:GRADLE_USER_HOME = $env:MCOO_GRADLE_HOME }
New-Item -ItemType Directory -Force -Path $out | Out-Null

Add-Type -AssemblyName System.Windows.Forms
Add-Type @"
using System;
using System.Runtime.InteropServices;
public class Win2 {
    [StructLayout(LayoutKind.Sequential)] public struct RECT { public int Left, Top, Right, Bottom; }
    [StructLayout(LayoutKind.Sequential)] public struct POINT { public int X, Y; }
    [DllImport("user32.dll")] public static extern bool GetWindowRect(IntPtr hWnd, out RECT rect);
    [DllImport("user32.dll")] public static extern bool GetClientRect(IntPtr hWnd, out RECT rect);
    [DllImport("user32.dll")] public static extern bool ClientToScreen(IntPtr hWnd, ref POINT pt);
    [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr hWnd);
    [DllImport("user32.dll")] public static extern IntPtr GetForegroundWindow();
    [DllImport("user32.dll")] public static extern bool ShowWindow(IntPtr hWnd, int nCmdShow);
    [DllImport("user32.dll")] public static extern bool SetCursorPos(int x, int y);
    [DllImport("user32.dll")] public static extern void mouse_event(uint f, uint dx, uint dy, uint d, IntPtr e);
    public const uint LEFTDOWN = 0x0002, LEFTUP = 0x0004;
    [DllImport("user32.dll")] public static extern void SwitchToThisWindow(IntPtr hWnd, bool fAltTab);
}
"@

# 把 MC 窗口切到前台。
#
# 为什么不直接用 SetForegroundWindow：调用进程本身不在前台时，Windows 会**静默忽略**它
# （不报错、不抛异常），表现为 GetForegroundWindow() 一直不是 MC，
# 后面 SetCursorPos + mouse_event 的点击就全打在别的窗口上 —— 截图看起来「什么都没发生」。
# 2026-10-27 实测踩过：日志打 `focused=False`，点击测试完全无效但看不出来。
# SwitchToThisWindow 没有这个限制，配合轮询确认才可靠。
# 找出「刚才启动的那个开发客户端」的进程。
#
# 为什么不能按窗口标题挑：Get-Process | Where MainWindowTitle -like '*Minecraft*' | Select -First 1
# 在多开时会抓到**别的实例**（包括上一次没退干净的僵尸客户端、或玩家自己开着的正式客户端）——
# 于是点击/按键全打进那个窗口，截图又是好的，表现成「控件点了没反应」，极难定位。
# 这里只认命令行带 devlaunchinjector 的 java 进程（= gradlew runClient 起的），取启动时间最新的一个。
function Get-ClientProc {
    param([int[]]$Exclude = @())
    $ids = @()
    Get-CimInstance Win32_Process -Filter "Name='java.exe'" -ErrorAction SilentlyContinue | ForEach-Object {
        if ($_.CommandLine -match 'devlaunchinjector' -and ($Exclude -notcontains [int]$_.ProcessId)) { $ids += [int]$_.ProcessId }
    }
    if ($ids.Count -eq 0) { return $null }
    return Get-Process -Id $ids -ErrorAction SilentlyContinue |
        Where-Object { $_.MainWindowHandle -ne 0 } |
        Sort-Object StartTime -Descending | Select-Object -First 1
}

function Focus-McWindow {
    param($Proc)
    if (-not $Proc) { return $false }
    [Win2]::ShowWindow($Proc.MainWindowHandle, 9) | Out-Null
    for ($i = 0; $i -lt 20; $i++) {
        [Win2]::SwitchToThisWindow($Proc.MainWindowHandle, $true)
        [Win2]::SetForegroundWindow($Proc.MainWindowHandle) | Out-Null
        Start-Sleep -Milliseconds 250
        if ([Win2]::GetForegroundWindow() -eq $Proc.MainWindowHandle) {
            Start-Sleep -Milliseconds 400
            return $true
        }
    }
    return $false
}

# 在 MC 窗口的屏幕中心（逻辑像素）偏移 (dx,dy) 处点一下
function Invoke-Click {
    param([int]$Dx, [int]$Dy, $Proc)
    $proc = $Proc
    if (-not $proc) { $proc = Get-ClientProc }
    if (-not $proc) { Write-Host "  找不到开发客户端窗口，跳过点击"; return }
    Write-Host ("  目标窗口 pid=" + $proc.Id)
    if (-not (Focus-McWindow $proc)) { Write-Host "  警告：MC 窗口没拿到前台焦点，点击可能丢到别的窗口" }
    Start-Sleep -Seconds 1
    $client = New-Object Win2+RECT
    [Win2]::GetClientRect($proc.MainWindowHandle, [ref]$client) | Out-Null
    $origin = New-Object Win2+POINT
    $origin.X = 0
    $origin.Y = 0
    [Win2]::ClientToScreen($proc.MainWindowHandle, [ref]$origin) | Out-Null
    $cw = $client.Right - $client.Left
    $ch = $client.Bottom - $client.Top
    # 焦点由上面的 Focus-McWindow 负责（原来的「先点一下标题栏」不可靠：
    # 点标题栏本身会改变窗口状态，而且 SetForegroundWindow 该失败时照样失败）
    $guiScale = [Math]::Max(1, [Math]::Min([Math]::Floor($cw / 320), [Math]::Floor($ch / 240)))
    $px = $origin.X + [int](($cw / 2 + $Dx * $guiScale))
    $py = $origin.Y + [int](($ch / 2 + $Dy * $guiScale))
    Write-Host ("  click at " + $px + "," + $py + " (guiScale=$guiScale, focused=" + ([Win2]::GetForegroundWindow() -eq $proc.MainWindowHandle) + ")")
    [Win2]::SetCursorPos($px, $py) | Out-Null
    Start-Sleep -Milliseconds 600
    [Win2]::mouse_event([Win2]::LEFTDOWN, 0, 0, 0, [IntPtr]::Zero)
    Start-Sleep -Milliseconds 120
    [Win2]::mouse_event([Win2]::LEFTUP, 0, 0, 0, [IntPtr]::Zero)
    Start-Sleep -Seconds 3
}

# 给 MC 窗口发按键（SendKeys 语法：^=Ctrl, {ENTER}=回车 …）
function Send-Keys {
    param([string]$KeySpec, $Proc)
    $proc = $Proc
    if (-not $proc) { $proc = Get-ClientProc }
    if (-not $proc) { Write-Host "  找不到开发客户端窗口，跳过按键"; return }
    if (-not (Focus-McWindow $proc)) { Write-Host "  警告：MC 窗口没拿到前台焦点，按键可能丢到别的窗口" }
    Start-Sleep -Milliseconds 800
    $alias = @{ 'ctrl+a' = '^a'; 'ctrl+c' = '^c'; 'ctrl+v' = '^v'; 'end' = '{END}'; 'home' = '{HOME}'; 'esc' = '{ESC}'; 'enter' = '{ENTER}' }
    $key = $KeySpec.ToLower()
    $spec = if ($alias.ContainsKey($key)) { $alias[$key] } else { $KeySpec }
    Write-Host ("  send keys " + $spec)
    [System.Windows.Forms.SendKeys]::SendWait($spec)
    Start-Sleep -Seconds 2
}

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
# 上一个服务器进程刚被杀时，世界文件的锁不会立刻放开，
# 这时候起服会直接报「另一个程序已锁定文件的一部分」然后秒退 —— 等端口空出来再起
for ($i = 0; $i -lt 30; $i++) {
    if (-not (Get-NetTCPConnection -LocalPort 25599 -ErrorAction SilentlyContinue)) { break }
    Start-Sleep -Seconds 1
}
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
    Write-Host ("服务器没起来！（exit=" + $server.ExitCode + "，testserver=" + $ts + "）日志尾部：")
    Get-Content "$ts\logs\gui.err.log" -Tail 15 -ErrorAction SilentlyContinue
    Get-Content "$ts\logs\gui.out.log" -Tail 15 -ErrorAction SilentlyContinue
    exit 1
}
Write-Host "server ready"

# 3) 逐个界面：起客户端 → 等进服 → 截图 → 停客户端（不动服务器）
foreach ($s in ($Screens -split ',')) {
    Stop-Client
    Write-Host ("=== client with debug screen: " + $s)
    # 先记下当前已经进过几次服 —— 服务端只起一次，日志会累积，
    # 直接 grep 'joined the game' 会把上一次的记录当成本次的，客户端窗口还没开就截图了。
    $joinsBefore = 0
    if (Test-Path "$ts\logs\latest.log") {
        $joinsBefore = (Select-String -Path "$ts\logs\latest.log" -Pattern 'joined the game' | Measure-Object).Count
    }
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
            $joinsNow = (Select-String -Path "$ts\logs\latest.log" -Pattern 'joined the game' | Measure-Object).Count
            if ($joinsNow -gt $joinsBefore) { $joined = $true; break }
        }
    }
    Write-Host ("  joined: " + $joined)
    if (-not $joined) { Write-Host "  跳过（未进服）"; continue }
    Start-Sleep -Seconds 12

    # 锁定「本次启动的那个客户端」：点击/按键/截图都用同一个 pid，避免抓错窗口
    $clientProc = $null
    for ($i = 0; $i -lt 15; $i++) {
        $clientProc = Get-ClientProc
        if ($clientProc) { break }
        Start-Sleep -Milliseconds 500
    }
    if (-not $clientProc) { Write-Host "  警告：没找到开发客户端进程，点击/按键/截图可能不准" }
    else { Write-Host ("  客户端进程 pid=" + $clientProc.Id + " title='" + $clientProc.MainWindowTitle + "'") }

    if ($Click -ne '') {
        # 支持多次点击：-Click "dx,dy;dx,dy"（每次都是相对屏幕中心的逻辑像素偏移）
        foreach ($one in ($Click -split ';')) {
            $parts = $one -split ','
            Invoke-Click -Dx ([int]$parts[0]) -Dy ([int]$parts[1]) -Proc $clientProc
        }
    }

    if ($Keys -ne '') {
        # 支持多次按键：-Keys "ctrl+a;end"（在点击之后发）
        foreach ($one in ($Keys -split ';')) {
            Send-Keys -KeySpec $one -Proc $clientProc
        }
    }

    $shot = Join-Path $out "gui-$s.png"
    & powershell.exe -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot 'screenshot.ps1') -Out $shot -TargetPid $(if ($clientProc) { $clientProc.Id } else { 0 })

    $cl = "$ts\logs\gui-client-$s.log"
    if (Test-Path $cl) {
        $errs = Select-String -Path $cl -Pattern '(?i)exception|InjectionError' | ForEach-Object { $_.Line } | Where-Object { $_ -notmatch 'Realms|401|profile key|attributes|InvalidCredentials' } | Select-Object -First 5
        if ($errs) { Write-Host "  client errors:"; $errs | ForEach-Object { Write-Host ("    " + $_) } } else { Write-Host "  client: no relevant errors" }
    }
}
Stop-Client
Write-Host "done"
