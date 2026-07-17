function CheckCode
{
    $projectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path

    Push-Location $projectRoot
    try
    {
        & .\gradlew.bat `
            :app:ktlintCheck `
            :app:detekt `
            :app:lintDebug

        if ($LASTEXITCODE -ne 0)
        {
            exit $LASTEXITCODE
        }
    }
    finally
    {
        Pop-Location
    }
}

CheckCode
