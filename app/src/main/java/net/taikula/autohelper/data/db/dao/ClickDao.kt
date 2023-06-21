package net.taikula.autohelper.data.db.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import net.taikula.autohelper.data.db.entity.ClickData
import net.taikula.autohelper.data.db.entity.ConfigData

@Dao
interface ClickDao {

    @Query("select * from ${ClickData.TABLE_NAME}")
    fun getAllClickData(): Flow<List<ClickData>>

    @Query("select * from ${ClickData.TABLE_NAME} where config_id = :configId")
    fun getAllByConfigId(configId: Int): Flow<List<ClickData>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vararg data: ClickData)

    @Delete
    suspend fun delete(vararg data: ClickData)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(vararg data: ClickData)

    @Query("select * from ${ConfigData.TABLE_NAME}")
    fun getAllConfig(): Flow<List<ConfigData>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vararg data: ConfigData)

    @Delete
    suspend fun delete(vararg data: ConfigData)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(vararg data: ConfigData)
}