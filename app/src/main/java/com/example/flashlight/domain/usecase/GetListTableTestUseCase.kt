package com.example.flashlight.domain.usecase

import com.example.flashlight.data.database.table.TableTest
import com.example.flashlight.data.repositories.LocalRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

class GetListTableTestUseCase @Inject constructor(private val localRepository: LocalRepository) :
    UseCase<GetListTableTestUseCase.Param, Flow<List<TableTest>>>() {

    open class Param : UseCase.Param()

    override suspend fun execute(param: Param): Flow<List<TableTest>> = withContext(Dispatchers.IO) {
        localRepository.getList()
    }
}