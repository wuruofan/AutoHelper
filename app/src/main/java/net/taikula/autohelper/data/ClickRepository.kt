package net.taikula.autohelper.data

import android.graphics.BitmapFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.taikula.autohelper.algorithm.pHash
import net.taikula.autohelper.data.db.dao.ClickDao
import net.taikula.autohelper.data.db.entity.ClickData
import net.taikula.autohelper.data.db.entity.ConfigData

/**
 * 仓库类，用于操作 DAO
 */
class ClickRepository(private val clickDao: ClickDao) {

    /**
     * 根据配置的 Id 获取所有点击数据
     */
    fun getAllClickData(configId: Int): Flow<List<ClickData>> {
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

    /**
     * 插入数据
     * @return 新行 id
     */
    suspend fun insert(data: ClickData): Long {
        return clickDao.insert(data)[0]
    }

    /**
     * 删除数据
     * @return 成功删除的行数
     */
    suspend fun delete(data: ClickData): Int {
        return clickDao.delete(data)
    }

    /**
     * 删除数据
     * @return 成功删除的行数
     */
    suspend fun delete(id: Int): Int {
        val data = clickDao.queryClickData(id) ?: return -1

        return clickDao.delete(data)
    }

    /**
     * 更新数据
     * @return 成功更新的行数
     */
    suspend fun update(data: ClickData): Int {
        return clickDao.update(data)
    }

    /**
     * 根据 id 查询 ClickData
     */
    suspend fun queryClickData(id: Int): ClickData? {
        return clickDao.queryClickData(id)
    }

    /**
     * 获取所有配置数据
     */
    fun getAllConfig(): Flow<List<ConfigData>> {
        return clickDao.getAllConfig()
    }

    /**
     * 插入数据
     * @return 新行 id
     */
    suspend fun insert(data: ConfigData): Long {
        return clickDao.insert(data)[0]
    }

    /**
     * 删除数据
     * @return 成功删除的行数
     */
    suspend fun delete(data: ConfigData): Int {
        return clickDao.delete(data)
    }

    /**
     * 更新数据
     * @return 成功更新的行数
     */
    suspend fun update(data: ConfigData): Int {
        return clickDao.update(data)
    }

    /**
     * 根据 id 查询配置数据
     */
    suspend fun queryConfigData(id: Int): ConfigData? {
        return try {
            clickDao.queryConfigData(id)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}