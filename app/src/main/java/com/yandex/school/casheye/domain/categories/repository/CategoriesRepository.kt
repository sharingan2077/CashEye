package com.yandex.school.casheye.domain.categories.repository

import com.yandex.school.casheye.core.model.Category
import kotlinx.coroutines.flow.Flow

interface CategoriesRepository {

    fun observeCategories(): Flow<List<Category>>

}