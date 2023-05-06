package net.taikula.autohelper.tools

import android.content.Context
import java.io.File
import java.io.FileOutputStream

object FileUtils {
    /**
     * 写应用内部文件
     */
    @Throws(java.lang.RuntimeException::class)
    fun writeInnerFile(
        context: Context,
        dirName: String,
        fileName: String,
        action: (FileOutputStream) -> Unit
    ): File {
        val dir = context.getDir(dirName, Context.MODE_PRIVATE)
        val file = File(dir, fileName)
        val outputStream = FileOutputStream(file)

        action.invoke(outputStream) // 真正的写操作

        outputStream.flush()
        outputStream.close()

        return file
    }

    /**
     * 写应用外部文件
     */
    @Throws(java.lang.RuntimeException::class)
    fun writeOuterFile(filePath: String, action: (FileOutputStream) -> Unit): File {
        val file = File(filePath)
        val outputStream = FileOutputStream(file)

        action.invoke(outputStream) // 真正的写操作

        outputStream.flush()
        outputStream.close()

        return file
    }

    fun delete(filePath: String): Boolean {
        val file = File(filePath)
        return file.delete()
    }
}