package com.example.flashlight.data.sources

import android.os.Build
import com.kt.flashlight.R
import com.example.flashlight.data.database.AppDatabase
import com.example.flashlight.data.database.table.TableTest
import com.example.flashlight.data.model.Language
import com.example.flashlight.data.model.LanguageOptions
import com.example.flashlight.data.model.OnBoarding
import com.example.flashlight.data.model.PermissionModel
import com.example.flashlight.utils.Permission as PermissionRequest
import java.util.Locale
import javax.inject.Inject

class LocalDataSource @Inject constructor(private val appDatabase: AppDatabase) {
    fun getListLanguage(): List<Language> {
        val baseList = LanguageOptions.all
        val deviceLocale = Locale.getDefault()
        val deviceLangCode = deviceLocale.language

        val matchedItem = baseList.firstOrNull {
            it.languageCode.equals(deviceLangCode, ignoreCase = true)
        }

        return if (matchedItem != null) {
            val sortedList = ArrayList<Language>(baseList.size)
            sortedList.add(matchedItem)
            baseList.forEach { item ->
                if (!item.languageCode.equals(matchedItem.languageCode, ignoreCase = true)) {
                    sortedList.add(item)
                }
            }
            sortedList
        } else {
            baseList
        }
    }

    fun getListOnBoarding(): List<OnBoarding> {
        return listOf(
            OnBoarding(R.mipmap.ic_launcher, R.string.app_name),
            OnBoarding(R.mipmap.ic_launcher_round, R.string.app_name),
            OnBoarding(R.mipmap.ic_launcher, R.string.app_name)
        )
    }

    fun insert(tableTest: TableTest) {
        appDatabase.tableTestDao().insert(tableTest)
    }

    fun getList() = appDatabase.tableTestDao().getList()

    fun delete(tableTest: TableTest) {
        appDatabase.tableTestDao().delete(tableTest)
    }

    fun getListPermission(): List<PermissionModel> =
        buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(
                    PermissionModel(
                        R.drawable.ic_launcher_background,
                        R.string.allow_notification_title,
                        R.string.allow_notification_content,
                        listOf(PermissionRequest.POST_NOTIFICATIONS),
                    ),
                )
            }
            add(
                PermissionModel(
                    R.drawable.ic_launcher_background,
                    R.string.allow_record_audio_title,
                    R.string.allow_record_audio_content,
                    listOf(
                        PermissionRequest.RECORD_AUDIO,
                    ),
                ),
            )
            add(
                PermissionModel(
                    R.drawable.ic_launcher_background,
                    R.string.allow_camera_title,
                    R.string.allow_camera_content,
                    listOf(PermissionRequest.CAMERA),
                ),
            )
        }
}