package com.rfw.clickhelper.data

import com.rfw.clickhelper.data.db.dao.ClickDao
import com.rfw.clickhelper.data.db.entity.ClickData
import kotlinx.coroutines.flow.Flow

class ClickRepository(private val clickDao: ClickDao) {
    var currentConfigId = 0

    val allClickData: Flow<List<ClickData>> = clickDao.getAllByConfigId(currentConfigId)

    suspend fun insert(data: ClickData) {
        clickDao.insert(data)
    }

    suspend fun delete(data: ClickData) {
        clickDao.delete(data)
    }

    suspend fun update(data: ClickData) {
        clickDao.update(data)
    }
}