[CmdletBinding()]
param(
    [ValidateRange(0, [int]::MaxValue)]
    [int]$RandomSeed = 20260718
)

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
$categories = Get-DemoFinanceCategories
$manifest = New-DemoFinanceManifest
Save-DemoFinanceManifest -Manifest $manifest

$accountSpecifications = @(
    [pscustomobject]@{ Key = "main"; Name = "Основной счёт"; Emoji = "💳"; Balance = "150000.00" },
    [pscustomobject]@{ Key = "cash"; Name = "Наличные"; Emoji = "💵"; Balance = "25000.00" },
    [pscustomobject]@{ Key = "savings"; Name = "Накопления"; Emoji = "🏦"; Balance = "300000.00" },
    [pscustomobject]@{ Key = "reserve"; Name = "Резерв"; Emoji = "🛟"; Balance = "75000.00" },
    [pscustomobject]@{ Key = "travel"; Name = "Путешествия"; Emoji = "✈️"; Balance = "45000.00" }
)
$incomeTemplates = @(
    [pscustomobject]@{ Category = "Зарплата"; MinimumAmount = 85000; MaximumAmount = 145000; Comments = @("Зарплата", "Аванс", "Премия") },
    [pscustomobject]@{ Category = "Фриланс"; MinimumAmount = 3500; MaximumAmount = 28000; Comments = @("Проект", "Консультация", "Срочная задача") },
    [pscustomobject]@{ Category = "Подарки"; MinimumAmount = 1000; MaximumAmount = 12000; Comments = @("Подарок", "Перевод от близких") },
    [pscustomobject]@{ Category = "Проценты по вкладам"; MinimumAmount = 800; MaximumAmount = 4500; Comments = @("Проценты", "Доход по вкладу") },
    [pscustomobject]@{ Category = "Возврат долга"; MinimumAmount = 1000; MaximumAmount = 15000; Comments = @("Вернули долг", "Возврат") },
    [pscustomobject]@{ Category = "Продажа имущества"; MinimumAmount = 2500; MaximumAmount = 50000; Comments = @("Продажа", "Объявление") }
)
$expenseTemplates = @(
    [pscustomobject]@{ Category = "Жильё"; MinimumAmount = 12000; MaximumAmount = 45000; Comments = @("Аренда", "Ремонт", "Дом") },
    [pscustomobject]@{ Category = "Продукты"; MinimumAmount = 350; MaximumAmount = 4500; Comments = @("Магазин", "Покупки", "Продукты") },
    [pscustomobject]@{ Category = "Транспорт"; MinimumAmount = 120; MaximumAmount = 2500; Comments = @("Такси", "Метро", "Заправка") },
    [pscustomobject]@{ Category = "Развлечения"; MinimumAmount = 300; MaximumAmount = 7000; Comments = @("Кино", "Концерт", "Встреча с друзьями") },
    [pscustomobject]@{ Category = "Рестораны"; MinimumAmount = 450; MaximumAmount = 6000; Comments = @("Обед", "Кофе", "Ужин") },
    [pscustomobject]@{ Category = "Одежда"; MinimumAmount = 900; MaximumAmount = 12000; Comments = @("Одежда", "Обувь") },
    [pscustomobject]@{ Category = "Здоровье"; MinimumAmount = 400; MaximumAmount = 10000; Comments = @("Аптека", "Врач", "Анализы") },
    [pscustomobject]@{ Category = "Коммунальные услуги"; MinimumAmount = 1800; MaximumAmount = 9000; Comments = @("Коммунальные услуги", "Интернет") },
    [pscustomobject]@{ Category = "Техника"; MinimumAmount = 1500; MaximumAmount = 35000; Comments = @("Техника", "Аксессуар") },
    [pscustomobject]@{ Category = "Образование"; MinimumAmount = 700; MaximumAmount = 18000; Comments = @("Курс", "Книги") },
    [pscustomobject]@{ Category = "Путешествия"; MinimumAmount = 1500; MaximumAmount = 55000; Comments = @("Билеты", "Отель", "Поездка") },
    [pscustomobject]@{ Category = "Подписки"; MinimumAmount = 199; MaximumAmount = 2500; Comments = @("Подписка", "Сервис") },
    [pscustomobject]@{ Category = "Подарки"; MinimumAmount = 500; MaximumAmount = 15000; Comments = @("Подарок", "Сюрприз") },
    [pscustomobject]@{ Category = "Красота"; MinimumAmount = 500; MaximumAmount = 7000; Comments = @("Красота", "Уход") },
    [pscustomobject]@{ Category = "Спорт"; MinimumAmount = 300; MaximumAmount = 8500; Comments = @("Спорт", "Тренировка") },
    [pscustomobject]@{ Category = "Домашние животные"; MinimumAmount = 400; MaximumAmount = 8000; Comments = @("Питомец", "Ветеринар") },
    [pscustomobject]@{ Category = "Хобби"; MinimumAmount = 300; MaximumAmount = 10000; Comments = @("Хобби", "Материалы") },
    [pscustomobject]@{ Category = "Кредиты"; MinimumAmount = 3000; MaximumAmount = 30000; Comments = @("Платёж по кредиту", "Рассрочка") }
)
$accountIds = @{ }
$random = [System.Random]::new($RandomSeed)
$createdTransactionCount = 0

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

    $today = [DateOnly]::FromDateTime((Get-Date).Date)

    foreach ($daysAgo in 89..0)
    {
        $transactionCountForDay = $random.Next(0, 4)
        for ($transactionIndex = 0; $transactionIndex -lt $transactionCountForDay; $transactionIndex++)
        {
            $isIncome = $random.Next(0, 4) -eq 0
            $templates = if ($isIncome)
            {
                $incomeTemplates
            }
            else
            {
                $expenseTemplates
            }
            $template = $templates[$random.Next(0, $templates.Count)]
            $accountSpecification = $accountSpecifications[$random.Next(0, $accountSpecifications.Count)]
            $amount = $random.Next($template.MinimumAmount, $template.MaximumAmount + 1).ToString("F2", [System.Globalization.CultureInfo]::InvariantCulture)
            $commentOptions = @($template.Comments)
            $comment = $commentOptions[$random.Next(0, $commentOptions.Count)]
            $time = [TimeOnly]::new($random.Next(7, 23),$random.Next(0, 60))
            $transactionDate =
            [DateTime]::SpecifyKind(
                    $today.AddDays(-$daysAgo).ToDateTime($time),
                    [DateTimeKind]::Utc
            ).ToString("o")
            $categoryId = Resolve-DemoFinanceCategoryId -Categories $categories -Name $template.Category -IsIncome $isIncome
            $transaction = Invoke-DemoFinanceApi -Method Post -RelativePath "transactions" -ApiKey $apiKey -Body ([ordered]@{
                accountId = $accountIds[$accountSpecification.Key]
                categoryId = $categoryId
                amount = $amount
                transactionDate = $transactionDate
                comment = $comment
            })
            $manifest.transactions += [pscustomobject]@{
                id = [int]$transaction.id
                accountId = $accountIds[$accountSpecification.Key]
            }
            $createdTransactionCount++
            Save-DemoFinanceManifest -Manifest $manifest
        }
    }
}
catch
{
    Write-Warning "Seed failed. The manifest keeps created IDs for safe cleanup."
    throw
}

Write-Host "Demo finance data was created: $( $accountSpecifications.Count ) accounts and $createdTransactionCount transactions over 90 days."
