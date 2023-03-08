package com.rfw.clickhelper.data

import android.graphics.BitmapFactory
import com.rfw.clickhelper.algorithm.pHash
import com.rfw.clickhelper.data.db.dao.ClickDao
import com.rfw.clickhelper.data.db.entity.ClickData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ClickRepository(private val clickDao: ClickDao) {
    var currentConfigId = 0

    val allClickData: Flow<List<ClickData>> = clickDao.getAllByConfigId(currentConfigId).map {
        it.forEach { data ->
            data.clickArea.apply {
                bitmap = BitmapFactory.decodeFile(imagePath)
                phash = pHash.dctImageHash(bitmap)
            }
        }

        it
    }

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