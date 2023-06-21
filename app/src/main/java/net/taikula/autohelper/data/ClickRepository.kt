package net.taikula.autohelper.data

import android.graphics.BitmapFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.taikula.autohelper.algorithm.pHash
import net.taikula.autohelper.data.db.dao.ClickDao
import net.taikula.autohelper.data.db.entity.ClickData
import net.taikula.autohelper.data.db.entity.ConfigData

class ClickRepository(private val clickDao: ClickDao) {

    fun getAllClickData(configId: Int = 0): Flow<List<ClickData>> {
        return clickDao.getAllByConfigId(configId).map {
            it.forEach { data ->
                data.clickArea.apply {
                    bitmap = BitmapFactory.decodeFile(imagePath)
                    phash = pHash.dctImageHash(bitmap)
                }
            }

            it
        }
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


    fun getAllConfig(): Flow<List<ConfigData>> {
        return clickDao.getAllConfig()
    }

    suspend fun insert(data: ConfigData) {
        clickDao.insert(data)
    }

    suspend fun delete(data: ConfigData) {
        clickDao.delete(data)
    }

    suspend fun update(data: ConfigData) {
        clickDao.update(data)
    }
}