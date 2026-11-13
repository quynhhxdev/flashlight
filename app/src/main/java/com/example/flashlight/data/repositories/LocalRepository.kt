package com.example.flashlight.data.repositories

import com.example.flashlight.data.database.table.TableTest
import com.example.flashlight.data.model.Language
import com.example.flashlight.data.model.OnBoarding
import com.example.flashlight.data.model.PermissionModel
import com.example.flashlight.data.sources.LocalDataSource
import javax.inject.Inject

class LocalRepository @Inject constructor(
    private val localDataSource: LocalDataSource,
) {

    fun getListLanguage(): List<Language> {
        return localDataSource.getListLanguage()
    }

    fun getListOnBoarding(): List<OnBoarding> {
        return localDataSource.getListOnBoarding()
    }

    fun insert(tableTest: TableTest) {
        localDataSource.insert(tableTest)
    }

    fun getList() = localDataSource.getList()

    fun delete(tableTest: TableTest) {
        localDataSource.delete(tableTest)
    }

    fun getListPermission(): List<PermissionModel> = localDataSource.getListPermission()
}