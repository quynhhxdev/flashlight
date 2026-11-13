package com.example.flashlight.domain.usecase

import com.example.flashlight.data.model.PermissionModel
import com.example.flashlight.data.repositories.LocalRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class GetListPermissionUseCase @Inject constructor(private val localRepository: LocalRepository) :
    UseCase<GetListPermissionUseCase.Param, List<PermissionModel>>() {
    open class Param : UseCase.Param()

    override suspend fun execute(param: Param): List<PermissionModel> =
        withContext(Dispatchers.IO) {
            localRepository.getListPermission()
        }
}
