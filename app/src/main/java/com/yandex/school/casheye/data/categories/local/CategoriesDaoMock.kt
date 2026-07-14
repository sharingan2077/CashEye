package com.yandex.school.casheye.data.categories.local

import com.yandex.school.casheye.core.model.Category
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf


class CategoriesDaoMock @Inject constructor() : CategoriesDao {


    override fun observeCategories(): Flow<List<Category>> {
        return flowOf(CategoriesMock)
    }
}


val CategoriesMock = listOf<Category>(
    Category(101, "Покупка канцтоваров", "✏️", false),
    Category(102, "Обед в кафе", "☕", false),
    Category(103, "Топливо для машины", "⛽", false),
    Category(104, "Подписка на сервис", "📱", false),
    Category(105, "Ремонт техники", "🔧", false),
    Category(106, "Покупка билетов", "🎫", false),
    Category(107, "Оплата интернета", "🌐", false),
    Category(108, "Магазин продуктов", "🛒", false),

    Category(201, "Продажа старой мебели", "🛋️", true),
    Category(202, "Возврат налога", "📋", true),
    Category(203, "Премия за проект", "💼", true),
    Category(204, "Подработка фриланс", "💻", true),
    Category(205, "Сдача квартиры", "🏠", true),
    Category(206, "Кешбек на карту", "💳", true),
    Category(207, "Подарок от родителей", "🎁", true),
)