package com.yandex.school.casheye.data.categories.di

import com.yandex.school.casheye.data.categories.local.CategoriesDao
import com.yandex.school.casheye.data.categories.local.CategoriesDaoMock
import com.yandex.school.casheye.data.categories.repository.CategoriesRepositoryImpl
import com.yandex.school.casheye.domain.categories.repository.CategoriesRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
abstract class CategoriesDataModule {


    @Binds
    @Singleton
    abstract fun bindsCategoriesRepository(categoryRepositoryImpl: CategoriesRepositoryImpl): CategoriesRepository


    @Binds
    @Singleton
    abstract fun bindsCategoriesDao(categoriesDaoMock: CategoriesDaoMock): CategoriesDao
}