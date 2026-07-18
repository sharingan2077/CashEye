Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$script:DemoFinanceBaseUrl = "https://shmr-finance.ru/api/v1"

function Get-DemoFinanceProjectRoot
{
    return (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
}

function Get-DemoFinanceManifestPath
{
    return (Join-Path (Get-DemoFinanceProjectRoot) "local\demo-finance-manifest.json")
}

function Get-DemoFinanceApiKey
{
    $apiKeyPath = Join-Path (Get-DemoFinanceProjectRoot) "local\api_key.txt"
    if (-not (Test-Path -LiteralPath $apiKeyPath))
    {
        throw "API key file was not found: $apiKeyPath"
    }

    $apiKey = (Get-Content -LiteralPath $apiKeyPath -Raw -Encoding UTF8).Trim()
    if ( [string]::IsNullOrWhiteSpace($apiKey))
    {
        throw "API key file is empty: $apiKeyPath"
    }

    return $apiKey
}

function Get-DemoFinanceHttpStatusCode
{
    param(
        [Parameter(Mandatory)]
        [System.Management.Automation.ErrorRecord]$ErrorRecord
    )

    $statusCode = $ErrorRecord.Exception.Data["StatusCode"]
    if ($null -ne $statusCode)
    {
        return [int]$statusCode
    }

    $responseProperty = $ErrorRecord.Exception.PSObject.Properties["Response"]
    if ($null -eq $responseProperty)
    {
        return $null
    }

    $response = $responseProperty.Value
    if ($null -eq $response)
    {
        return $null
    }

    try
    {
        return [int]$response.StatusCode
    }
    catch
    {
        return $null
    }
}

function Invoke-DemoFinanceApi
{
    param(
        [Parameter(Mandatory)]
        [ValidateSet("Get", "Post", "Delete")]
        [string]$Method,

        [Parameter(Mandatory)]
        [string]$RelativePath,

        [Parameter(Mandatory)]
        [string]$ApiKey,

        [object]$Body
    )

    $uri = "$script:DemoFinanceBaseUrl/$($RelativePath.TrimStart('/') )"
    $requestParameters = @{
        Method = $Method
        Uri = $uri
        Headers = @{ Authorization = "Bearer $ApiKey" }
        ErrorAction = "Stop"
    }

    if ( $PSBoundParameters.ContainsKey("Body"))
    {
        $requestParameters.ContentType = "application/json; charset=utf-8"
        $requestParameters.Body = $Body | ConvertTo-Json -Depth 5 -Compress
    }

    try
    {
        return Invoke-RestMethod @requestParameters
    }
    catch
    {
        $statusCode = Get-DemoFinanceHttpStatusCode -ErrorRecord $_
        if ($null -eq $statusCode)
        {
            throw "API request $Method $RelativePath failed."
        }

        $exception = [System.Exception]::new("API request $Method $RelativePath failed with status $statusCode.")
        $exception.Data["StatusCode"] = $statusCode
        throw $exception
    }
}

function Get-DemoFinanceCategories
{
    param(
        [Parameter(Mandatory)]
        [string]$ApiKey
    )

    $localCategoriesPath = Join-Path (Get-DemoFinanceProjectRoot) "local\categories.json"
    if (-not (Test-Path -LiteralPath $localCategoriesPath))
    {
        throw "Local category reference was not found: $localCategoriesPath"
    }

    $localCategories = @(Get-Content -LiteralPath $localCategoriesPath -Raw -Encoding UTF8 | ConvertFrom-Json)
    $apiCategories = @(Invoke-DemoFinanceApi -Method Get -RelativePath "categories" -ApiKey $ApiKey)

    return [pscustomobject]@{
        Local = $localCategories
        Api = $apiCategories
    }
}

function Resolve-DemoFinanceCategoryId
{
    param(
        [Parameter(Mandatory)]
        [object]$CategorySource,

        [Parameter(Mandatory)]
        [string]$Name,

        [Parameter(Mandatory)]
        [bool]$IsIncome
    )

    $localCategory = @($CategorySource.Local | Where-Object { $_.name -eq $Name -and $_.isIncome -eq $IsIncome }) | Select-Object -First 1
    if ($null -eq $localCategory)
    {
        throw "Category '$Name' with isIncome=$IsIncome is absent from local/categories.json."
    }

    $apiCategory = @($CategorySource.Api | Where-Object { $_.name -eq $Name -and $_.isIncome -eq $IsIncome }) | Select-Object -First 1
    if ($null -eq $apiCategory)
    {
        throw "Category '$Name' with isIncome=$IsIncome is absent from the API response."
    }

    return [int]$apiCategory.id
}

function New-DemoFinanceManifest
{
    return [pscustomobject]@{
        version = 1
        baseUrl = $script:DemoFinanceBaseUrl
        createdAt = [DateTime]::UtcNow.ToString("o")
        accounts = @()
        transactions = @()
    }
}

function Read-DemoFinanceManifest
{
    $manifestPath = Get-DemoFinanceManifestPath
    if (-not (Test-Path -LiteralPath $manifestPath))
    {
        return $null
    }

    $manifest = Get-Content -LiteralPath $manifestPath -Raw -Encoding UTF8 | ConvertFrom-Json
    if ($null -eq $manifest.accounts -or $null -eq $manifest.transactions)
    {
        throw "Demo finance manifest is invalid: $manifestPath"
    }

    return $manifest
}

function Save-DemoFinanceManifest
{
    param(
        [Parameter(Mandatory)]
        [object]$Manifest
    )

    $manifestPath = Get-DemoFinanceManifestPath
    $Manifest | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath $manifestPath -Encoding UTF8
}
