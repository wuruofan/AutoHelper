package net.taikula.autohelper.data.db.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import net.taikula.autohelper.data.db.entity.ClickData

@Dao
interface ClickDao {

    @Query("select * from ${ClickData.TABLE_NAME}")
    fun getAll(): Flow<List<ClickData>>

    @Query("select * from ${ClickData.TABLE_NAME} where config_id = :configId")
    fun getAllByConfigId(configId: Int): Flow<List<ClickData>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vararg data: ClickData)

    @Delete
    suspend fun delete(vararg data: ClickData)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(vararg data: ClickData)
}