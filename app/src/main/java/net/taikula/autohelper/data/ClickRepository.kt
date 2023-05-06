package net.taikula.autohelper.data

import android.graphics.BitmapFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.taikula.autohelper.algorithm.pHash
import net.taikula.autohelper.data.db.dao.ClickDao
import net.taikula.autohelper.data.db.entity.ClickData

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