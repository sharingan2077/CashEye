[CmdletBinding()]
param()

. (Join-Path $PSScriptRoot "DemoFinanceData.Common.ps1")

$clearScript = Join-Path $PSScriptRoot "ClearDemoFinanceData.ps1"
if (Test-Path -LiteralPath (Get-DemoFinanceManifestPath))
{
    Write-Host "Cleaning the previous demo dataset."
    & $clearScript
    if (Test-Path -LiteralPath (Get-DemoFinanceManifestPath))
    {
        throw "The previous demo dataset was not fully cleaned. Seed was stopped."
    }
}

$apiKey = Get-DemoFinanceApiKey
$categories = Get-DemoFinanceCategories -ApiKey $apiKey
$manifest = New-DemoFinanceManifest
Save-DemoFinanceManifest -Manifest $manifest

$accountSpecifications = @(
    [pscustomobject]@{ Key = "main"; Name = "Основной счёт"; Emoji = "💳"; Balance = "150000.00" },
    [pscustomobject]@{ Key = "cash"; Name = "Наличные"; Emoji = "💵"; Balance = "25000.00" },
    [pscustomobject]@{ Key = "savings"; Name = "Накопления"; Emoji = "🏦"; Balance = "300000.00" }
)
$accountIds = @{ }

try
{
    foreach ($accountSpecification in $accountSpecifications)
    {
        $account = Invoke-DemoFinanceApi -Method Post -RelativePath "accounts" -ApiKey $apiKey -Body ([ordered]@{
            name = $accountSpecification.Name
            emoji = $accountSpecification.Emoji
            balance = $accountSpecification.Balance
            currency = "RUB"
        })
        $accountIds[$accountSpecification.Key] = [int]$account.id
        $manifest.accounts += [pscustomobject]@{
            id = [int]$account.id
            key = $accountSpecification.Key
            name = $accountSpecification.Name
        }
        Save-DemoFinanceManifest -Manifest $manifest
        Write-Host "Created account '$( $accountSpecification.Name )' ($( $account.id ))."
    }

    $transactionSpecifications = @(
        [pscustomobject]@{ DaysAgo = 89; Account = "main"; Category = "Зарплата"; IsIncome = $true; Amount = "125000.00"; Comment = "Зарплата" },
        [pscustomobject]@{ DaysAgo = 84; Account = "main"; Category = "Жильё"; IsIncome = $false; Amount = "42000.00"; Comment = "Аренда" },
        [pscustomobject]@{ DaysAgo = 78; Account = "cash"; Category = "Продукты"; IsIncome = $false; Amount = "1850.00"; Comment = "Покупки" },
        [pscustomobject]@{ DaysAgo = 74; Account = "main"; Category = "Фриланс"; IsIncome = $true; Amount = "18000.00"; Comment = "Проект" },
        [pscustomobject]@{ DaysAgo = 68; Account = "main"; Category = "Транспорт"; IsIncome = $false; Amount = "540.00"; Comment = "Такси" },
        [pscustomobject]@{ DaysAgo = 62; Account = "savings"; Category = "Проценты по вкладам"; IsIncome = $true; Amount = "2100.00"; Comment = "Проценты" },
        [pscustomobject]@{ DaysAgo = 59; Account = "main"; Category = "Зарплата"; IsIncome = $true; Amount = "125000.00"; Comment = "Зарплата" },
        [pscustomobject]@{ DaysAgo = 54; Account = "cash"; Category = "Рестораны"; IsIncome = $false; Amount = "1240.00"; Comment = "Ужин" },
        [pscustomobject]@{ DaysAgo = 49; Account = "main"; Category = "Подписки"; IsIncome = $false; Amount = "699.00"; Comment = "Подписка" },
        [pscustomobject]@{ DaysAgo = 43; Account = "main"; Category = "Здоровье"; IsIncome = $false; Amount = "3200.00"; Comment = "Аптека" },
        [pscustomobject]@{ DaysAgo = 38; Account = "cash"; Category = "Развлечения"; IsIncome = $false; Amount = "1750.00"; Comment = "Кино" },
        [pscustomobject]@{ DaysAgo = 32; Account = "main"; Category = "Фриланс"; IsIncome = $true; Amount = "9500.00"; Comment = "Консультация" },
        [pscustomobject]@{ DaysAgo = 29; Account = "main"; Category = "Зарплата"; IsIncome = $true; Amount = "125000.00"; Comment = "Зарплата" },
        [pscustomobject]@{ DaysAgo = 24; Account = "main"; Category = "Жильё"; IsIncome = $false; Amount = "42000.00"; Comment = "Аренда" },
        [pscustomobject]@{ DaysAgo = 19; Account = "cash"; Category = "Продукты"; IsIncome = $false; Amount = "2360.00"; Comment = "Покупки" },
        [pscustomobject]@{ DaysAgo = 14; Account = "main"; Category = "Транспорт"; IsIncome = $false; Amount = "430.00"; Comment = "Метро" },
        [pscustomobject]@{ DaysAgo = 9; Account = "main"; Category = "Подписки"; IsIncome = $false; Amount = "399.00"; Comment = "Музыка" },
        [pscustomobject]@{ DaysAgo = 5; Account = "savings"; Category = "Проценты по вкладам"; IsIncome = $true; Amount = "2350.00"; Comment = "Проценты" },
        [pscustomobject]@{ DaysAgo = 2; Account = "main"; Category = "Здоровье"; IsIncome = $false; Amount = "1400.00"; Comment = "Врач" },
        [pscustomobject]@{ DaysAgo = 0; Account = "main"; Category = "Фриланс"; IsIncome = $true; Amount = "7800.00"; Comment = "Срочная задача" },
        [pscustomobject]@{ DaysAgo = 0; Account = "main"; Category = "Продукты"; IsIncome = $false; Amount = "1350.00"; Comment = "Продукты" },
        [pscustomobject]@{ DaysAgo = 0; Account = "main"; Category = "Транспорт"; IsIncome = $false; Amount = "280.00"; Comment = "Такси" },
        [pscustomobject]@{ DaysAgo = 0; Account = "cash"; Category = "Рестораны"; IsIncome = $false; Amount = "920.00"; Comment = "Обед" }
    )
    $today = [DateOnly]::FromDateTime((Get-Date).Date)

    foreach ($transactionSpecification in $transactionSpecifications)
    {
        $transactionDate =
        [DateTime]::SpecifyKind(
                $today.AddDays(-$transactionSpecification.DaysAgo).ToDateTime([TimeOnly]::Parse("12:00")),
                [DateTimeKind]::Utc
        ).ToString("o")
        $categoryId = Resolve-DemoFinanceCategoryId -CategorySource $categories -Name $transactionSpecification.Category -IsIncome $transactionSpecification.IsIncome
        $transaction = Invoke-DemoFinanceApi -Method Post -RelativePath "transactions" -ApiKey $apiKey -Body ([ordered]@{
            accountId = $accountIds[$transactionSpecification.Account]
            categoryId = $categoryId
            amount = $transactionSpecification.Amount
            transactionDate = $transactionDate
            comment = $transactionSpecification.Comment
        })
        $manifest.transactions += [pscustomobject]@{
            id = [int]$transaction.id
            accountId = $accountIds[$transactionSpecification.Account]
        }
        Save-DemoFinanceManifest -Manifest $manifest
        Write-Host "Created transaction $( $transaction.id ): $( $transactionSpecification.Category )."
    }
}
catch
{
    Write-Warning "Seed failed. The manifest keeps created IDs for safe cleanup."
    throw
}

Write-Host "Demo finance data was created."
