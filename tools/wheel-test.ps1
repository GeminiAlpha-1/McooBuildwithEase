# 单独测滚轮方向：复用已在运行的客户端（预览界面已打开）。
# 用法：powershell -File tools\wheel-test.ps1
param([int]$Dx = 0, [int]$Dy = -60)
$ErrorActionPreference = 'Stop'
$parent = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$out = Join-Path $parent '_shots'

Add-Type @"
using System;
using System.Runtime.InteropServices;
public class Win4 {
    [StructLayout(LayoutKind.Sequential)] public struct RECT { public int Left, Top, Right, Bottom; }
    [StructLayout(LayoutKind.Sequential)] public struct POINT { public int X, Y; }
    [DllImport("user32.dll")] public static extern bool GetClientRect(IntPtr hWnd, out RECT rect);
    [DllImport("user32.dll")] public static extern bool ClientToScreen(IntPtr hWnd, ref POINT pt);
    [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr hWnd);
    [DllImport("user32.dll")] public static extern bool ShowWindow(IntPtr hWnd, int nCmdShow);
    [DllImport("user32.dll")] public static extern bool SetCursorPos(int x, int y);
    [DllImport("user32.dll")] public static extern void mouse_event(uint f, uint dx, uint dy, int d, IntPtr e);
    public const uint WHEEL = 0x0800;
}
"@

$proc = Get-Process | Where-Object { $_.MainWindowTitle -like '*Minecraft*' -and $_.MainWindowHandle -ne 0 } | Select-Object -First 1
if (-not $proc) { Write-Host "找不到 MC 窗口"; exit 1 }
[Win4]::ShowWindow($proc.MainWindowHandle, 9) | Out-Null
[Win4]::SetForegroundWindow($proc.MainWindowHandle) | Out-Null
Start-Sleep -Milliseconds 900

$client = New-Object Win4+RECT
[Win4]::GetClientRect($proc.MainWindowHandle, [ref]$client) | Out-Null
$origin = New-Object Win4+POINT
[Win4]::ClientToScreen($proc.MainWindowHandle, [ref]$origin) | Out-Null
$cw = $client.Right - $client.Left
$ch = $client.Bottom - $client.Top
$guiScale = [Math]::Max(1, [Math]::Min([Math]::Floor($cw / 320), [Math]::Floor($ch / 240)))
$x = $origin.X + [int](($cw / 2 + $Dx * $guiScale))
$y = $origin.Y + [int](($ch / 2 + $Dy * $guiScale))
[Win4]::SetCursorPos($x, $y) | Out-Null
Start-Sleep -Milliseconds 400
Write-Host ("wheel at " + $x + "," + $y)

Write-Host "--- 向下滚 4 格（应缩小）---"
for ($i = 0; $i -lt 4; $i++) {
    [Win4]::mouse_event([Win4]::WHEEL, 0, 0, -120, [IntPtr]::Zero)
    Start-Sleep -Milliseconds 250
}
Start-Sleep -Milliseconds 700
& powershell.exe -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot 'screenshot.ps1') -Out (Join-Path $out 'wheel-down.png') | Out-Null
Write-Host ("saved " + (Join-Path $out 'wheel-down.png'))

Write-Host "--- 向上滚 8 格（应放大）---"
for ($i = 0; $i -lt 8; $i++) {
    [Win4]::mouse_event([Win4]::WHEEL, 0, 0, 120, [IntPtr]::Zero)
    Start-Sleep -Milliseconds 250
}
Start-Sleep -Milliseconds 700
& powershell.exe -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot 'screenshot.ps1') -Out (Join-Path $out 'wheel-up.png') | Out-Null
Write-Host ("saved " + (Join-Path $out 'wheel-up.png'))
Write-Host "done"
