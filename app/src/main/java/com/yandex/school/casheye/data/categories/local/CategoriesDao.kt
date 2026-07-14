package com.yandex.school.casheye.data.categories.local

import com.yandex.school.casheye.core.model.Category
import kotlinx.coroutines.flow.Flow

interface CategoriesDao {

    fun observeCategories(): Flow<List<Category>>
}