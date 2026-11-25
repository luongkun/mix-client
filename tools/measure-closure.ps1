$ErrorActionPreference = 'Stop'
$src = 'C:\Users\lucif\Downloads\Badlion\captured\src\net\badlion'
$map = New-Object 'System.Collections.Generic.Dictionary[string,string]' ([System.StringComparer]::Ordinal)
Get-ChildItem $src -Recurse -File -Filter *.java | ForEach-Object { $map[$_.FullName.Substring($src.Length + 1)] = $_.FullName }

$interface = @('IR','gM','zw','zu','zt','zC','zB','yM','Ka','CZ','yO','Ah','Jm','Ji','Jg','Jk','Jo','Jn','Jl','JV','JX','JY','gI','Ay','AO','AC','II','KJ','KM','KO','KP','KQ','KR','KS','KT','KU','KV','KW','KX','KY','Kc','Kd','Ke','Kf','Kg','Kh','Ki','Kj','Kk','Kl','Km','Kn','Ko','Kp','Kq','Kr','Ks','Kt','Ku','Kv','Kw','Kx','Ky','Kz','LD','aNE','yW','yz','za','VZ','Wg','Wm','VY','Ly','Vt','Vs','KN','MC','MM','MU','MT','MO','aBQ','aCO','aBT','Je','aBO','Ae','aNF','Aq','cx','gH','gN','At','zd','U','Ja','Jb','Jf','Jh','Jj','Jp','Jq','Jr','Jt','Jv','Pf','gG','yc','yf','mW','mx','Iq','Aw','XQ')

$wanted = New-Object 'System.Collections.Generic.Dictionary[string,string]' ([System.StringComparer]::Ordinal)
foreach ($n in $interface) { $k = "a\$n.java"; if ($map.ContainsKey($k)) { $wanted[$k] = $map[$k] } else { Write-Host "not in capture: $n" } }
foreach ($n in @('h','j','cc','Pair','bp')) { $k = "clientcommon\util\$n.java"; if ($map.ContainsKey($k)) { $wanted[$k] = $map[$k] } }
Write-Host ("seed: " + $wanted.Count)

function Get-Refs([string]$text) {
    $refs = @{}
    foreach ($m in ([regex]::Matches($text, 'new\s+([A-Za-z][A-Za-z0-9_$]*)\s*[<(]'))) { $refs[$m.Groups[1].Value] = 1 }
    foreach ($m in ([regex]::Matches($text, '(?m)^[ \t]*(?:public|protected|private|static|final|abstract|synchronized|native|[ \t])*([A-Za-z_$][A-Za-z0-9_$]*)(?:[ \t]*<[^;{]*?>)?[ \t]+[a-zA-Z_$][A-Za-z0-9_$]*[ \t]*\([^)]*\)[ \t]*[;=,)]'))) { $refs[$m.Groups[1].Value] = 1 }
    foreach ($m in ([regex]::Matches($text, 'import\s+net\.badlion\.([A-Za-z0-9_.]+);'))) { $refs['pkg/' + $m.Groups[1].Value] = 1 }
    foreach ($m in ([regex]::Matches($text, 'extends\s+([A-Za-z][A-Za-z0-9_$]*)'))) { $refs[$m.Groups[1].Value] = 1 }
    foreach ($m in ([regex]::Matches($text, 'implements\s+([A-Za-z][A-Za-z0-9_$]*)'))) { $refs[$m.Groups[1].Value] = 1 }
    foreach ($m in ([regex]::Matches($text, 'catch\s*\(\s*([A-Za-z][A-Za-z0-9_$]*)'))) { $refs[$m.Groups[1].Value] = 1 }
    foreach ($m in ([regex]::Matches($text, 'instanceof\s+([A-Za-z][A-Za-z0-9_$]*)'))) { $refs[$m.Groups[1].Value] = 1 }
    return $refs
}

$skipped = @{}
$parent = @{}
$changed = $true
while ($changed) {
    $changed = $false
    foreach ($key in @($wanted.Keys)) {
        $text = [System.IO.File]::ReadAllText($wanted[$key])
        if ([string]::IsNullOrEmpty($text)) { continue }
        foreach ($r in (Get-Refs $text).Keys) {
            if ($r -like 'pkg/*') { $rel = ($r -replace '^pkg/', '') -replace '\.', '\' } else { $rel = 'a\' + $r }
            $t = $rel + '.java'
            if ($wanted.ContainsKey($t)) { continue }
            if (-not $map.ContainsKey($t)) { continue }
            $body = [System.IO.File]::ReadAllText($map[$t])
            if ($body -match 'import\s+net\.minecraft\.') { $skipped[$r] = 1; continue }
            $wanted[$t] = $map[$t]
            $parent[$t] = $key
            $changed = $true
        }
    }
}
Write-Host ("closure size: " + $wanted.Count)
Write-Host ("skipped at boundary: " + (($skipped.Keys | Sort-Object) -join ', '))
$big = @('aCY','YD','Ym','yQ','dE','Xw','aPs','aPt','aOm','aKw')
foreach ($b in $big) {
    $t = "a\$b.java"
    if ($wanted.ContainsKey($t)) {
        $chain = @($t)
        $cur = $t
        while ($parent.ContainsKey($cur)) { $cur = $parent[$cur]; $chain += $cur }
        [array]::Reverse($chain)
        Write-Host ("chain: " + ($chain -join ' -> '))
    }
}