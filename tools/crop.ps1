# 裁剪 + 放大截图，用来肉眼检查细节（例：预览框到底画在哪）
param(
    [Parameter(Mandatory = $true)][string]$In,
    [Parameter(Mandatory = $true)][string]$Out,
    [int]$X = 0,
    [int]$Y = 0,
    [int]$W = 0,
    [int]$H = 0,
    [int]$Scale = 4
)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$src = [System.Drawing.Bitmap]::FromFile((Resolve-Path $In))
if ($W -le 0) { $W = $src.Width - $X }
if ($H -le 0) { $H = $src.Height - $Y }
$X = [Math]::Max(0, [Math]::Min($X, $src.Width - 1))
$Y = [Math]::Max(0, [Math]::Min($Y, $src.Height - 1))
$W = [Math]::Min($W, $src.Width - $X)
$H = [Math]::Min($H, $src.Height - $Y)
$dst = New-Object System.Drawing.Bitmap(($W * $Scale), ($H * $Scale))
$g = [System.Drawing.Graphics]::FromImage($dst)
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
$g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
$g.DrawImage($src, (New-Object System.Drawing.Rectangle 0, 0, ($W * $Scale), ($H * $Scale)),
    (New-Object System.Drawing.Rectangle $X, $Y, $W, $H), [System.Drawing.GraphicsUnit]::Pixel)
$g.Dispose()
$dir = Split-Path $Out -Parent
if ($dir -and -not (Test-Path $dir)) { New-Item -ItemType Directory -Force -Path $dir | Out-Null }
$dst.Save($Out, [System.Drawing.Imaging.ImageFormat]::Png)
Write-Host ("cropped " + $X + "," + $Y + " " + $W + "x" + $H + " -> " + ($W * $Scale) + "x" + ($H * $Scale) + "  " + $Out)
$dst.Dispose()
$src.Dispose()
