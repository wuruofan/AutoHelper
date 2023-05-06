package net.taikula.autohelper.data.db.entity

import androidx.room.*
import net.taikula.autohelper.data.db.entity.ClickData.Companion.TABLE_NAME
import net.taikula.autohelper.data.model.ClickArea

@Entity(tableName = TABLE_NAME)
@TypeConverters(Converters::class)
data class ClickData(
    @PrimaryKey(autoGenerate = true) val id: Int, // When set to true, Insert methods treat 0 as not-set while inserting the item.
    @ColumnInfo("config_id") val configId: Int, // 配置id
    @ColumnInfo("index") val index: Int, // 点击序列
    @ColumnInfo("click_area") val clickArea: ClickArea, // 序列化后点击区域数据
//    @ColumnInfo("image_path") val imagePath: String, // 点击区域截图路径
    @ColumnInfo("timestamp") val time: Long
) {

    companion object {
        const val TABLE_NAME = "click_data"
    }
}
