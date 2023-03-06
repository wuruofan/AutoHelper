package com.rfw.clickhelper.data.db

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.rfw.clickhelper.MainApp
import com.rfw.clickhelper.data.db.dao.ClickDao
import com.rfw.clickhelper.data.db.entity.ClickData

/**
 * https://developer.android.com/training/data-storage/room?hl=zh-cn#kotlin
 */
@Database(entities = [ClickData::class], version = 1)
abstract class ClickDatabase : RoomDatabase() {
    abstract fun clickDao(): ClickDao

    companion object {
        // 单例参考：https://juejin.cn/post/6844903590545326088
        val instance: ClickDatabase by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            Room.databaseBuilder(MainApp.appContext, ClickDatabase::class.java, "click_db").build()
        }
    }
}