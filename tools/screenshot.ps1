# 用 PrintWindow 直接抓取 Minecraft 窗口内容（窗口被遮挡也能抓到）
param(
    [Parameter(Mandatory = $true)][string]$Out,
    [int]$DelaySeconds = 0,
    [string]$TitleLike = 'Minecraft',
    # 指定窗口所属进程。多开客户端时按标题挑会抓到别的实例（甚至上一个没退干净的），
    # 而截图内容看着「也是 Minecraft 界面」，极难发现 —— 所以调用方抓到 pid 就显式传进来。
    [int]$TargetPid = 0
)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
Add-Type -ReferencedAssemblies System.Drawing -TypeDefinition @"
using System;
using System.Runtime.InteropServices;
using System.Drawing;
public class Cap {
    [StructLayout(LayoutKind.Sequential)] public struct RECT { public int Left, Top, Right, Bottom; }
    [DllImport("user32.dll")] public static extern bool GetWindowRect(IntPtr hWnd, out RECT rect);
    [DllImport("user32.dll")] public static extern bool PrintWindow(IntPtr hWnd, IntPtr hdcBlt, uint nFlags);
    [DllImport("user32.dll")] public static extern bool IsWindowVisible(IntPtr hWnd);
    public static Bitmap Capture(IntPtr hWnd) {
        RECT r;
        if (!GetWindowRect(hWnd, out r)) return null;
        int w = r.Right - r.Left, h = r.Bottom - r.Top;
        if (w <= 0 || h <= 0) return null;
        Bitmap bmp = new Bitmap(w, h);
        using (Graphics g = Graphics.FromImage(bmp)) {
            IntPtr hdc = g.GetHdc();
            PrintWindow(hWnd, hdc, 2);
            g.ReleaseHdc(hdc);
        }
        return bmp;
    }
}
"@
if ($DelaySeconds -gt 0) { Start-Sleep -Seconds $DelaySeconds }

$candidates = Get-Process | Where-Object { $_.MainWindowHandle -ne 0 -and $_.MainWindowTitle -ne '' }
Write-Host "候选窗口:"
$candidates | ForEach-Object { Write-Host ("   pid=" + $_.Id + " title='" + $_.MainWindowTitle + "'") }
$target = $null
if ($TargetPid -ne 0) {
    $target = Get-Process -Id $TargetPid -ErrorAction SilentlyContinue
    if (-not $target -or $target.MainWindowHandle -eq 0) { Write-Host ("指定 pid " + $TargetPid + " 没有可用窗口"); exit 1 }
}
if (-not $target) {
    $target = $candidates | Where-Object { $_.MainWindowTitle -like "*$TitleLike*" } | Select-Object -First 1
}
if (-not $target) {
    $target = $candidates | Where-Object { $_.ProcessName -eq 'java' } | Select-Object -First 1
    if ($target) { Write-Host ("没有名字匹配 '" + $TitleLike + "' 的窗口，退而使用 java 进程窗口: " + $target.MainWindowTitle) }
}
if (-not $target) { Write-Host "找不到可截图的窗口"; exit 1 }

$bmp = [Cap]::Capture($target.MainWindowHandle)
if (-not $bmp) { Write-Host "截图失败"; exit 1 }
$dir = Split-Path $Out -Parent
if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Force -Path $dir | Out-Null }
$bmp.Save($Out, [System.Drawing.Imaging.ImageFormat]::Png)
Write-Host ("saved " + $Out + " (" + $bmp.Width + "x" + $bmp.Height + ") from '" + $target.MainWindowTitle + "'")
$bmp.Dispose()
