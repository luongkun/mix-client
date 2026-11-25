# Rebuilds libs/dango-mix.jar from the upstream release jar.
#
# The release client ships Dango (me.sorihiko.dango) already compiled against the
# obfuscated Minecraft, so its classes reference notch members (axu, a(IIF)V, ...).
# Mix Client boots on the deobfuscated client jar, so the classes have to be
# remapped notch -> mcp before they can be linked, then the Dango i18n bridge is
# remapped to the MixUI package.
param(
    [Parameter(Mandatory = $true)]
    [string]$UpstreamPackage,
    [Parameter(Mandatory = $true)]
    [string]$OldDangoI18nOwner,
    [string]$NewDangoI18nOwner = "mix/client/mixui/integration/dango/DangoI18n",
    [string]$Repo = "C:\Users\lucif\Downloads\Mix Client"
)

$ErrorActionPreference = "Stop"

$releaseJar = Get-ChildItem (Join-Path $UpstreamPackage "libraries") -Recurse -Filter "*.jar" |
              Where-Object { $_.Name -like "*7.1.2*" -and $_.Length -gt 1MB } |
              Select-Object -First 1
if (-not $releaseJar) { throw "upstream 7.1.2 client jar not found under libraries/" }
$mapping  = Join-Path $UpstreamPackage "source\remap\notch2mcp.tiny"
$notchJar = Get-ChildItem (Join-Path $UpstreamPackage "versions") -Recurse -Filter "*.jar" |
            Where-Object { $_.Length -gt 1MB } | Select-Object -First 1
if (-not $notchJar) { throw "notch client jar not found under versions/" }

# tiny-remapper + ASM 9 (the Dango classes are Java 17 bytecode)
$cache = "$env:USERPROFILE\.gradle\caches\modules-2\files-2.1"
$tr = (Get-ChildItem "$cache\dev.architectury\tiny-remapper" -Recurse -Filter "*.jar" |
       Sort-Object FullName -Descending | Select-Object -First 1).FullName
$lt = (Get-ChildItem "$cache\net.fabricmc\lorenz-tiny" -Recurse -Filter "*.jar" |
       Select-Object -First 1).FullName
$tp = (Get-ChildItem "$cache\net.fabricmc\tiny-mappings-parser" -Recurse -Filter "*.jar" |
       Select-Object -First 1).FullName
$asm = Get-ChildItem "$cache\org.ow2.asm" -Recurse -Filter "*.jar" |
       Where-Object { $_.Name -match '^asm(-tree|-commons|-util|-analysis)?-9\.7' } |
       ForEach-Object { $_.FullName }

$cp = (@($tr, $lt, $tp) + $asm) -join ';'
$out = Join-Path $Repo "libs\dango-mix.jar"
Remove-Item $out -ErrorAction SilentlyContinue

& java -cp $cp dev.architectury.tinyremapper.Main `
    $releaseJar $out $mapping notch mcp $notchJar.FullName

$toolDir = Join-Path $Repo "build\tools\dango-brand"
New-Item -ItemType Directory -Force -Path $toolDir | Out-Null
& javac -proc:none -source 8 -target 8 -cp $cp -d $toolDir `
    (Join-Path $Repo "tools\java\mix\tools\DangoOwnerRemapper.java")
& java -cp "$toolDir;$cp" mix.tools.DangoOwnerRemapper $out $out $OldDangoI18nOwner $NewDangoI18nOwner

Write-Host "wrote $out"
