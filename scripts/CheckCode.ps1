function CheckCode
{
    $projectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
    $gradleWrapper = Join-Path $projectRoot "gradlew"

    Push-Location $projectRoot
    try
    {
        if ($env:OS -eq "Windows_NT")
        {
            & "$gradleWrapper.bat" `
                ktlintCheck `
                detekt `
                test `
                testDebugUnitTest `
                lintDebug
        }
        else
        {
            & sh $gradleWrapper `
                ktlintCheck `
                detekt `
                test `
                testDebugUnitTest `
                lintDebug
        }
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
