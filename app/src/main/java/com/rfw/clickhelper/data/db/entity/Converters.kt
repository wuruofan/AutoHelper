package com.rfw.clickhelper.data.db.entity

import android.util.Log
import androidx.room.TypeConverter
import com.rfw.clickhelper.data.model.ClickArea
import com.rfw.clickhelper.tools.Extensions.TAG
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

/**
 * 参考：https://www.jianshu.com/p/1d6e5cb0af36
 *
 * 使用 Room 引用复杂数据：https://developer.android.com/training/data-storage/room/referencing-data?hl=zh-cn
 */
class Converters {
    @TypeConverter
    fun fromByteArray(byteArray: ByteArray?): ClickArea? {
        if (byteArray == null) return null

        var byteArrayInputStream: ByteArrayInputStream? = null
        var objectInputStream: ObjectInputStream? = null

        try {
            byteArrayInputStream = ByteArrayInputStream(byteArray)
            objectInputStream = ObjectInputStream(byteArrayInputStream)

            return objectInputStream.readObject() as ClickArea
        } catch (e: java.lang.Exception) {
            Log.w(TAG, "fromByteArray", e)
        } finally {
            byteArrayInputStream?.close()
            objectInputStream?.close()
        }

        return null
    }

    @TypeConverter
    fun toByteArray(clickArea: ClickArea?): ByteArray? {
        if (clickArea == null) return null

        var byteArrayOutputStream: ByteArrayOutputStream? = null
        var objectOutputStream: ObjectOutputStream? = null

        try {
            byteArrayOutputStream = ByteArrayOutputStream()
            objectOutputStream = ObjectOutputStream(byteArrayOutputStream)
            objectOutputStream.writeObject(clickArea)
            objectOutputStream.flush()

            return byteArrayOutputStream.toByteArray()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            byteArrayOutputStream?.close()
            objectOutputStream?.close()
        }

        return null
    }
}