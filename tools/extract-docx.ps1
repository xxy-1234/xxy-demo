param([string]$Path)
if (-not (Test-Path -LiteralPath $Path)) {
    Write-Error "Not found: $Path"
    exit 1
}
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::OpenRead((Resolve-Path -LiteralPath $Path))
try {
    $entry = $zip.GetEntry('word/document.xml')
    $sr = New-Object System.IO.StreamReader($entry.Open())
    $xml = $sr.ReadToEnd()
    $sr.Close()
} finally {
    $zip.Dispose()
}
$plain = $xml -replace '</w:p>', "`n" -replace '<[^>]+>', '' `
    -replace '&lt;', '<' -replace '&gt;', '>' -replace '&amp;', '&' -replace '&quot;', '"'
$lines = $plain -split "`n" | ForEach-Object { $_.Trim() } | Where-Object { $_ -ne '' }
$out = Join-Path $PSScriptRoot '..\taskbook6-extracted.txt'
$lines | Out-File -FilePath $out -Encoding utf8
$lines
