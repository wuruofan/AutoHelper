package net.taikula.autohelper.tools

import android.graphics.Point
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

object Extensions {
    /**
     * extension function to provide TAG value
     */
    val Any.TAG: String
        get() {
            return if (!javaClass.isAnonymousClass) {
                val name = javaClass.simpleName
                if (name.length <= 23) name else name.substring(0, 23)// first 23 chars
            } else {
                val name = javaClass.name
                if (name.length <= 23) name else name.substring(
                    name.length - 23,
                    name.length
                )// last 23 chars
            }
        }

    fun Any.log(msg: String) {
        Log.d("_" + this::class.java.simpleName, msg)
    }

//    val gson
//        get() = Gson()

//    fun Any.toJson(): String {
//        return gson.toJson(this)
//    }


    fun Int.dp(): Int {
        return DisplayUtils.dip2px(net.taikula.autohelper.MainApp.appContext, this.toFloat())
    }

    fun Float.dp(): Int {
        return DisplayUtils.dip2px(net.taikula.autohelper.MainApp.appContext, this)
    }


    /**
     *  [协程实现定时任务](https://stackoverflow.com/questions/54827455/how-to-implement-timer-with-kotlin-coroutines)
     */
    fun CoroutineScope.launchPeriodicAsync(
        repeatMillis: Long,
        action: suspend () -> Unit
    ) = this.async {
        if (repeatMillis > 0) {
            while (isActive) {
                action()
                delay(repeatMillis)
            }
        } else {
            action()
        }
    }

    /**
     * Point x, y 坐标对调
     */
    fun Point.invert(): Point {
        return Point().apply {
            this.x = this@invert.y
            this.y = this@invert.x
        }
    }
}