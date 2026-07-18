[CmdletBinding()]
param()

. (Join-Path $PSScriptRoot "DemoFinanceData.Common.ps1")

$manifest = Read-DemoFinanceManifest
if ($null -eq $manifest)
{
    Write-Host "No demo finance manifest found. Nothing to clean."
    return
}

$apiKey = Get-DemoFinanceApiKey

foreach ($transaction in @($manifest.transactions))
{
    try
    {
        Invoke-DemoFinanceApi -Method Delete -RelativePath "transactions/$( $transaction.id )" -ApiKey $apiKey | Out-Null
        $manifest.transactions = @($manifest.transactions | Where-Object { $_.id -ne $transaction.id })
        Save-DemoFinanceManifest -Manifest $manifest
        Write-Host "Deleted transaction $( $transaction.id )."
    }
    catch
    {
        $statusCode = Get-DemoFinanceHttpStatusCode -ErrorRecord $_
        if ($statusCode -eq 404)
        {
            $manifest.transactions = @($manifest.transactions | Where-Object { $_.id -ne $transaction.id })
            Save-DemoFinanceManifest -Manifest $manifest
            Write-Host "Transaction $( $transaction.id ) was already absent."
            continue
        }

        Write-Warning "Could not delete transaction $( $transaction.id ): $( $_.Exception.Message )"
    }
}

if (@($manifest.transactions).Count -gt 0)
{
    throw "Some demo transactions remain. Resolve the errors and run this script again before deleting accounts."
}

foreach ($account in @($manifest.accounts))
{
    try
    {
        Invoke-DemoFinanceApi -Method Delete -RelativePath "accounts/$( $account.id )" -ApiKey $apiKey | Out-Null
        $manifest.accounts = @($manifest.accounts | Where-Object { $_.id -ne $account.id })
        Save-DemoFinanceManifest -Manifest $manifest
        Write-Host "Deleted account $( $account.id )."
    }
    catch
    {
        $statusCode = Get-DemoFinanceHttpStatusCode -ErrorRecord $_
        if ($statusCode -eq 404)
        {
            $manifest.accounts = @($manifest.accounts | Where-Object { $_.id -ne $account.id })
            Save-DemoFinanceManifest -Manifest $manifest
            Write-Host "Account $( $account.id ) was already absent."
            continue
        }

        Write-Warning "Could not delete account $( $account.id ): $( $_.Exception.Message )"
    }
}

if (@($manifest.accounts).Count -gt 0)
{
    throw "Some demo accounts remain. Resolve the errors and run this script again."
}

Remove-Item -LiteralPath (Get-DemoFinanceManifestPath)
Write-Host "Demo finance data was cleaned."
