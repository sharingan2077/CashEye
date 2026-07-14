package com.yandex.school.casheye.data.categories.repository

import com.yandex.school.casheye.core.model.Category
import com.yandex.school.casheye.data.categories.local.CategoriesDao
import com.yandex.school.casheye.domain.categories.repository.CategoriesRepository
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow

class CategoriesRepositoryImpl @Inject constructor(
    private val dao: CategoriesDao
) : CategoriesRepository {


    override fun observeCategories(): Flow<List<Category>> {
        return dao.observeCategories()
    }
}