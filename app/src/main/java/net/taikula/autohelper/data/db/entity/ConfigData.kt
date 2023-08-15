package net.taikula.autohelper.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 配置表，保存配置名称
 */
@Entity(tableName = ConfigData.TABLE_NAME)
data class ConfigData(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "name") var name: String
) {
    override fun toString(): String {
        return "[${id}, ${name}]"
    }

    companion object {
        const val TABLE_NAME = "config_data"
    }
}
