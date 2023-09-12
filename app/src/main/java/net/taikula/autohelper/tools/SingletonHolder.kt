package net.taikula.autohelper.tools

/**
 * 单例帮助类
 * [单例模式](https://www.jianshu.com/p/3fc4bd25fdb2)
 *
 * 使用方法：
 * ```
 * class SkinManager private constructor(context: Context) {
 * companion object : SingletonHolder<SkinManager, Context>(::SkinManager)
 * }
 * ```
 */
open class SingletonHolder<out T, in A>(creator: (A) -> T) {
    /**
     * T 的构造方法
     */
    private var creator: ((A) -> T)? = creator

    @Volatile
    private var instance: T? = null

    fun getInstance(arg: A): T =
        instance ?: synchronized(this) {
            instance ?: creator!!(arg).apply {
                instance = this
            }
        }
}