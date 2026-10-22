# 原版客户端（无 Fabric、无任何 mod）连服测试
#
# 验证项目的核心诉求：服务端装了 McooBuildwithEase，客户端什么都没装也能正常进服。
# 用启动器里那份**官方客户端 jar**直接起一个纯净客户端（不走 Fabric Loader，
# 所以 mod 目录里的东西一概不加载），用 --quickPlayMultiplayer 直接连测试服。
param(
    [string]$Server = '127.0.0.1',
    [int]$Port = 25599,
    [string]$Username = 'VanillaTester',
    [string]$MinecraftRoot = 'C:\Minecraft\自用客户端\.minecraft',
    [string]$VersionPrefix = '26.2',
    [string]$GameDir = '',
    [int]$TimeoutSeconds = 180
)
$ErrorActionPreference = 'Stop'
$proj = Split-Path -Parent $PSScriptRoot
$ts = if ($env:MCOO_TEST_SERVER) { $env:MCOO_TEST_SERVER } else {
    $sibling = Join-Path (Split-Path -Parent (Split-Path -Parent $PSScriptRoot)) 'testserver'
    $inRepo = Join-Path (Split-Path -Parent $PSScriptRoot) 'testserver'
    if (Test-Path $sibling) { $sibling } else { $inRepo }
}
$out = if ($env:MCOO_SHOTS) { $env:MCOO_SHOTS } else { Join-Path (Split-Path -Parent (Split-Path -Parent $PSScriptRoot)) '_shots' }
New-Item -ItemType Directory -Force -Path $out | Out-Null
if (-not $GameDir) { $GameDir = Join-Path $out 'vanilla-client' }
New-Item -ItemType Directory -Force -Path $GameDir | Out-Null

# ---------- 1) 找官方客户端 jar + 库清单 ----------
$versionDir = Get-ChildItem (Join-Path $MinecraftRoot 'versions') -Directory -ErrorAction SilentlyContinue |
    Where-Object { $_.Name -like "$VersionPrefix*" } | Sort-Object Name | Select-Object -First 1
if (-not $versionDir) { Write-Host "找不到 $VersionPrefix 的实例目录"; exit 1 }
$jar = Get-ChildItem $versionDir.FullName -Filter '*.jar' -File | Where-Object { $_.Name -notlike '*-natives*' } | Select-Object -First 1
$jsonFile = Get-ChildItem $versionDir.FullName -Filter '*.json' -File | Select-Object -First 1
if (-not $jar -or -not $jsonFile) { Write-Host "实例目录里没有客户端 jar / 版本 json"; exit 1 }
$json = Get-Content $jsonFile.FullName -Raw | ConvertFrom-Json

# 全新 gameDir 会卡在「无障碍向导」首启界面（quickPlay 也要等它过去），
# 复制一份实例的 options.txt（里面 onboardAccessibility:false）跳过
$optsSrc = Join-Path $versionDir.FullName 'options.txt'
if ((Test-Path $optsSrc) -and -not (Test-Path (Join-Path $GameDir 'options.txt'))) {
    Copy-Item $optsSrc (Join-Path $GameDir 'options.txt') -Force
    Write-Host "options.txt: 从实例复制（跳过首启向导）"
}

# 客户端 jar 路径里不能有空格（classpath 是一整个参数），实例目录名经常带空格（"26.2-Fabric 0.19.3"），
# 所以复制一份到工作目录再用。
$clientJar = Join-Path $GameDir 'vanilla-client.jar'
if (-not (Test-Path $clientJar)) { Copy-Item $jar.FullName $clientJar -Force }
Write-Host ("client jar: " + $jar.Name + " -> " + $clientJar + "  (assetIndex=" + $json.assetIndex.id + ")")

# classpath：客户端 jar + 所有库（排除 fabric —— 原版客户端不该带 Fabric）
$cp = New-Object System.Collections.Generic.List[string]
$cp.Add($clientJar)
foreach ($lib in $json.libraries) {
    if ($lib.name -match 'fabric') { continue }
    $paths = @()
    if ($lib.downloads.artifact.path) { $paths += $lib.downloads.artifact.path }
    if ($lib.downloads.classifiers) {
        foreach ($c in $lib.downloads.classifiers.PSObject.Properties) { $paths += $c.Value.path }
    }
    foreach ($p in $paths) {
        # 版本 json 里的 artifact/classifier 路径是相对「libraries」目录的
        $full = Join-Path (Join-Path $MinecraftRoot 'libraries') ($p -replace '/', '\')
        if (Test-Path $full) { $cp.Add($full) }
    }
}
Write-Host ("classpath entries: " + $cp.Count)

# ---------- 2) 起原版客户端 ----------
$clientLog = Join-Path $GameDir 'client.log'
$errLog = Join-Path $GameDir 'client.err.log'
$rawArgs = @(
    '-Xmx2G',
    '--enable-native-access=ALL-UNNAMED',
    '--sun-misc-unsafe-memory-access=allow',
    '-cp', ($cp -join ';'),
    'net.minecraft.client.main.Main',
    '--username', $Username,
    '--version', $json.clientVersion,
    '--gameDir', $GameDir,
    '--assetsDir', (Join-Path $MinecraftRoot 'assets'),
    '--assetIndex', $json.assetIndex.id,
    '--uuid', '00000000-0000-0000-0000-000000000026',
    '--accessToken', '0',
    '--userType', 'legacy',
    '--versionType', 'release',
    '--quickPlayMultiplayer', ($Server + ':' + $Port)
)
# Start-Process 的数组参数不会保住引号，这里自己拼成**一个**参数字符串：
# 含空格的参数加引号；-D 形式只能给「值」加引号（-Dkey="a b"），整个 token 加引号 JVM 不认
$argString = ($rawArgs | ForEach-Object {
    if ($_ -match '^-D([^=]+)=(.*)$') { '-D' + $Matches[1] + '="' + $Matches[2] + '"' }
    elseif ($_ -match '\s') { '"' + $_ + '"' }
    else { $_ }
}) -join ' '
$client = Start-Process -FilePath 'java' -ArgumentList $argString -WorkingDirectory $GameDir -PassThru -NoNewWindow `
    -RedirectStandardOutput $clientLog -RedirectStandardError $errLog
Write-Host ("client pid = " + $client.Id)

# ---------- 3) 等服务端记录到这次进服 ----------
$serverLog = Join-Path $ts 'logs\latest.log'
$before = 0
if (Test-Path $serverLog) { $before = (Select-String -Path $serverLog -Pattern 'joined the game' | Measure-Object).Count }
$deadline = (Get-Date).AddSeconds($TimeoutSeconds)
$joined = $false
while ((Get-Date) -lt $deadline) {
    Start-Sleep -Seconds 3
    if ($client.HasExited) { break }
    if (Test-Path $serverLog) {
        $now = (Select-String -Path $serverLog -Pattern 'joined the game' | Measure-Object).Count
        if ($now -gt $before) { $joined = $true; break }
    }
}
Write-Host ("joined = " + $joined)

# ---------- 4) 结论 + 证据 ----------
if ($joined) {
    Write-Host ">>> 结论：原版客户端（无 Fabric、无 mod）成功进服。"
    Start-Sleep -Seconds 8
    $shot = Join-Path $out 'vanilla-client.png'
    & powershell.exe -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot 'screenshot.ps1') -Out $shot
} else {
    Write-Host ">>> 结论：原版客户端没能进服。"
}
Write-Host "--- 服务端日志尾部 ---"
Get-Content $serverLog -Tail 12 -ErrorAction SilentlyContinue
Write-Host "--- 客户端日志里的错误 ---"
if (Test-Path $clientLog) {
    $errs = Select-String -Path $clientLog -Pattern '(?i)exception|disconnect|kicked|unknown payload|payload' |
        ForEach-Object { $_.Line } | Where-Object { $_ -notmatch 'Realms|401|profile key|InvalidCredentials' } | Select-Object -First 12
    if ($errs) { $errs | ForEach-Object { Write-Host ("    " + $_) } } else { Write-Host "    （没有相关错误）" }
}
if (Test-Path $errLog) {
    $e2 = Get-Content $errLog -Tail 10 -ErrorAction SilentlyContinue | Where-Object { $_ -match '(?i)error|exception' }
    if ($e2) { Write-Host "--- 客户端 stderr ---"; $e2 | ForEach-Object { Write-Host ("    " + $_) } }
}

# ---------- 5) 收尾 ----------
if (-not $client.HasExited) { Stop-Process -Id $client.Id -Force -ErrorAction SilentlyContinue }
if ($joined) { exit 0 } else { exit 1 }
