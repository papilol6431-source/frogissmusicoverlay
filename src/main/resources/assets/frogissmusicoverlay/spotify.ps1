[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
Add-Type -AssemblyName System.Runtime.WindowsRuntime
$asTask = ([System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object { $_.Name -eq 'AsTask' -and $_.GetParameters().Count -eq 1 -and $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1' })[0]
function Await($op, $type) {
    $t = $asTask.MakeGenericMethod($type).Invoke($null, @($op))
    $t.Wait(-1) | Out-Null
    $t.Result
}
[void][Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager, Windows.Media.Control, ContentType = WindowsRuntime]
[void][Windows.Media.Control.GlobalSystemMediaTransportControlsSessionMediaProperties, Windows.Media.Control, ContentType = WindowsRuntime]
$mgr = Await ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]::RequestAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager])
while ($true) {
    $out = 'NONE'
    try {
        foreach ($s in $mgr.GetSessions()) {
            if ($s.SourceAppUserModelId -like '*Spotify*') {
                $p = Await ($s.TryGetMediaPropertiesAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionMediaProperties])
                $status = $s.GetPlaybackInfo().PlaybackStatus.ToString()
                $tl = $s.GetTimelineProperties()
                $pos = $tl.Position.TotalMilliseconds
                if ($status -eq 'Playing') { $pos += ([DateTimeOffset]::UtcNow - $tl.LastUpdatedTime).TotalMilliseconds }
                $dur = $tl.EndTime.TotalMilliseconds
                $out = "$status`t$($p.Artist)`t$($p.Title)`t$($p.AlbumTitle)`t$([int64]$pos)`t$([int64]$dur)"
                break
            }
        }
    } catch { $out = 'NONE' }
    Write-Output $out
    Start-Sleep -Milliseconds 400
}
