function CheckCode
{
    $projectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path

    Push-Location $projectRoot
    try
    {
        & .\gradlew.bat `
            ktlintCheck `
            detekt `
            test `
            testDebugUnitTest `
            lintDebug
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
