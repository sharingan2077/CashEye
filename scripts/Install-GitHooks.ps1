$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path

Push-Location $projectRoot
try
{
    git config core.hooksPath .githooks
    if ($LASTEXITCODE -ne 0)
    {
        exit $LASTEXITCODE
    }

    Write-Host "Git hooks installed. Local commits will run scripts/CheckCode.ps1."
}
finally
{
    Pop-Location
}
