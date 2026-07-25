$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path

Push-Location $projectRoot
try
{
    git config core.hooksPath .githooks
    if ($LASTEXITCODE -ne 0)
    {
        exit $LASTEXITCODE
    }

    if ($env:OS -ne "Windows_NT")
    {
        & chmod +x (Join-Path $projectRoot ".githooks/pre-commit")
        if ($LASTEXITCODE -ne 0)
        {
            exit $LASTEXITCODE
        }
    }

    Write-Host "Git hooks installed. Local commits will run scripts/CheckCode.ps1 with PowerShell 7+ (pwsh)."
}
finally
{
    Pop-Location
}
