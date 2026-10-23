# 验证剪贴板预览是否支持客户端的材质包（资源包）。
#
# 做法：造一个最小材质包，把「石头」换成纯品红，启动客户端看预览里的石头有没有变色。
#
# 用法：
#   powershell -File tools\resourcepack-preview-test.ps1
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

# ---- 1) 造测试材质包 ----
# 【pack_format】26.2 的资源包格式是 88（SharedConstants#RESOURCE_PACK_FORMAT_MAJOR）。
# 【min_format / max_format】格式号 >= 65 之后这两个字段是**必填**的，
# 少一个就会报 "Pack declares support for version newer than 64, but is missing
# mandatory fields min_format and max_format"，然后材质包被判为「不兼容」直接丢掉。
$packName = 'mcoo_test'
$packDir = Join-Path $proj "run\resourcepacks\$packName"
New-Item -ItemType Directory -Force -Path (Join-Path $packDir 'assets\minecraft\textures\block') | Out-Null
# 【描述只用 ASCII】中文经 PowerShell 管道会被编码搞坏，JSON 直接解析失败 → 材质包被判不兼容。
$meta = '{' + "`n" +
'  "pack": {' + "`n" +
'    "pack_format": 88,' + "`n" +
'    "min_format": 88,' + "`n" +
'    "max_format": 88,' + "`n" +
'    "description": "McooBuildwithEase preview resource pack test"' + "`n" +
'  }' + "`n" +
'}' + "`n"
[System.IO.File]::WriteAllText((Join-Path $packDir 'pack.mcmeta'), $meta, [System.Text.UTF8Encoding]::new($false))
Add-Type -AssemblyName System.Drawing
# 覆盖「石头」和「沙子」—— clipboard-full 场景里有这两种方块，一眼就能看出材质包有没有生效
foreach ($tex in @('stone', 'sand')) {
    $bmp = New-Object System.Drawing.Bitmap 16, 16
    for ($y = 0; $y -lt 16; $y++) {
        for ($x = 0; $x -lt 16; $x++) {
            $bmp.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(255, 255, 0, 255))
        }
    }
    $bmp.Save((Join-Path $packDir "assets\minecraft\textures\block\$tex.png"), [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
}
Write-Host ("测试材质包已生成: " + $packDir)

# ---- 2) 在客户端选项里启用它 ----
$options = Join-Path $proj 'run\options.txt'
if (Test-Path $options) {
    $lines = [System.IO.File]::ReadAllLines($options, [System.Text.UTF8Encoding]::new($false))
    $lines = $lines | ForEach-Object {
        if ($_ -match '^resourcePacks:') { 'resourcePacks:["' + $packName + '"]' } else { $_ }
    }
    [System.IO.File]::WriteAllLines($options, $lines, [System.Text.UTF8Encoding]::new($false))
}

# ---- 3) 起服 + 起客户端 ----
Get-CimInstance Win32_Process -Filter "Name='java.exe'" | ForEach-Object {
    if ($_.CommandLine -match 'fabric-server-launch' -or $_.CommandLine -match 'devlaunchinjector') {
        Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue
    }
}
Start-Sleep -Seconds 4
$jar = Get-ChildItem "$proj\build\libs" -Filter '*.jar' -File | Sort-Object LastWriteTime -Descending | Select-Object -First 1
Get-ChildItem "$ts\mods" -Filter '*.jar' -File -ErrorAction SilentlyContinue | Where-Object { $_.Name -notlike 'fabric-api*' } | ForEach-Object { Remove-Item $_.FullName -Force -ErrorAction SilentlyContinue }
Copy-Item $jar.FullName "$ts\mods\$($jar.Name)" -Force
Write-Host ("installed " + $jar.Name)

$server = Start-Process -FilePath 'java' -ArgumentList @('-Xmx2G', '-jar', 'fabric-server-launch.jar', 'nogui') `
    -WorkingDirectory $ts -PassThru -NoNewWindow -RedirectStandardOutput "$ts\logs\rp.out.log" -RedirectStandardError "$ts\logs\rp.err.log"
$deadline = (Get-Date).AddSeconds(90)
while ((Get-Date) -lt $deadline) {
    Start-Sleep -Seconds 2
    if (Test-Path "$ts\logs\latest.log") {
        $c = Get-Content "$ts\logs\latest.log" -Raw -ErrorAction SilentlyContinue
        if ($c -and $c -match 'Done \(') { break }
    }
}
Write-Host "server ready"

$joinsBefore = (Select-String -Path "$ts\logs\latest.log" -Pattern 'joined the game' -ErrorAction SilentlyContinue | Measure-Object).Count
Push-Location $proj
Start-Process -FilePath 'cmd.exe' -ArgumentList @('/c', "gradlew.bat runClient -PmcDebugScreen=clipboard-full `"-PmcQuickPlay=127.0.0.1:25599`" --console=plain > `"$ts\logs\rp-client.log`" 2>&1") -WorkingDirectory $proj -PassThru -NoNewWindow | Out-Null
Pop-Location
$deadline = (Get-Date).AddSeconds(180)
while ((Get-Date) -lt $deadline) {
    Start-Sleep -Seconds 4
    $j = (Select-String -Path "$ts\logs\latest.log" -Pattern 'joined the game' -ErrorAction SilentlyContinue | Measure-Object).Count
    if ($j -gt $joinsBefore) { break }
}
Start-Sleep -Seconds 12
& powershell.exe -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot 'screenshot.ps1') -Out (Join-Path $out 'resourcepack-preview.png') | Out-Null
Write-Host "--- 资源包相关日志 ---"
$log = Select-String -Path "$proj\run\logs\latest.log" -Pattern '(?i)resource pack|resourcepack' | ForEach-Object { $_.Line.Trim() } | Select-Object -First 10
if ($log) { $log } else { Write-Host "（没有资源包相关警告 → 材质包被正常加载了）" }

Get-CimInstance Win32_Process -Filter "Name='java.exe'" | ForEach-Object {
    if ($_.CommandLine -match 'devlaunchinjector') { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }
}
Stop-Process -Id $server.Id -Force -ErrorAction SilentlyContinue
Write-Host "done"
