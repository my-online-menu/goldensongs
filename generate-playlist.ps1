# Regenerates playlist.js from all .mp3 files in this folder.
# Run it any time you add or remove songs:  right-click > Run with PowerShell
$here  = Split-Path -Parent $MyInvocation.MyCommand.Path
$names = Get-ChildItem -Path $here -Filter *.mp3 -File | Sort-Object Name | Select-Object -ExpandProperty Name
$json  = $names | ConvertTo-Json
if ($names.Count -eq 1) { $json = "[`n  `"$($names[0])`"`n]" }  # keep array shape for single file
"// Auto-generated playlist. Re-run generate-playlist.ps1 to refresh.`nwindow.AUTO_PLAYLIST = $json;" |
    Out-File -FilePath (Join-Path $here 'playlist.js') -Encoding utf8
Write-Host "playlist.js updated with $($names.Count) track(s)."
