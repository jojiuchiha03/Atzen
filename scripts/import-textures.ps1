$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$projectRoot = Split-Path -Parent $PSScriptRoot
$manifest = Get-Content -LiteralPath (Join-Path $projectRoot 'art\prompts.json') -Raw | ConvertFrom-Json
foreach ($texture in $manifest.textures) {
    $sourcePath = Join-Path $projectRoot "art\$($texture.original)"
    $outputPath = [IO.Path]::GetFullPath((Join-Path $projectRoot "art\$($texture.gameTexture)"))
    New-Item -ItemType Directory -Force (Split-Path -Parent $outputPath) | Out-Null
    $source = [Drawing.Bitmap]::new($sourcePath)
    $output = [Drawing.Bitmap]::new(64, 64, [Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $graphics = [Drawing.Graphics]::FromImage($output)
    try {
        $graphics.CompositingMode = [Drawing.Drawing2D.CompositingMode]::SourceCopy
        $graphics.InterpolationMode = [Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
        $graphics.PixelOffsetMode = [Drawing.Drawing2D.PixelOffsetMode]::Half
        if ($texture.id.StartsWith('skin_')) {
            $graphics.DrawImage($source, [Drawing.Rectangle]::new(0, 0, 64, 64))
        } else {
            $scale = [Math]::Min(64.0 / $source.Width, 64.0 / $source.Height)
            $width = [int][Math]::Round($source.Width * $scale)
            $height = [int][Math]::Round($source.Height * $scale)
            $graphics.DrawImage($source, [Drawing.Rectangle]::new([int]((64-$width)/2), [int]((64-$height)/2), $width, $height))
        }
        $output.Save($outputPath, [Drawing.Imaging.ImageFormat]::Png)
    } finally {
        $graphics.Dispose()
        $output.Dispose()
        $source.Dispose()
    }
}
Write-Output "$($manifest.textures.Count) textures imported at 64x64."
