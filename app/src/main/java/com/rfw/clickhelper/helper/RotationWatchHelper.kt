package com.rfw.clickhelper.helper

import android.annotation.SuppressLint
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import android.view.IRotationWatcher
import com.rfw.clickhelper.tool.Extensions.TAG
import java.lang.reflect.Method

typealias RotationWatchCallback = (Int) -> Unit

/**
 * https://blog.csdn.net/qq627578198/article/details/110521581
 */
class RotationWatchHelper(callback: RotationWatchCallback) {
    private val iWindowManagerService: Any? by lazy { reflectWindowManagerService() }

    private val iRotationWatcher: IRotationWatcher = object : IRotationWatcher.Stub() {
        @Throws(RemoteException::class)
        override fun onRotationChanged(rotation: Int) {
            callback(rotation)
        }
    }

    /**
     * 参考：https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/java/android/view/WindowManagerGlobal.java;l=166;bpv=1;bpt=1?q=WindowManagerService&ss=android%2Fplatform%2Fsuperproject
     */
    @SuppressLint("PrivateApi")
    private fun reflectWindowManagerService(): Any? {
        try {
            // reflect ServiceManager.getService("window")
            val serviceManagerClz = Class.forName("android.os.ServiceManager")
            val getServiceMethod = serviceManagerClz.getMethod("getService", String::class.java)
            val windowManagerService = getServiceMethod.invoke(null, "window")

            // reflect IWindowManager.Stub.asInterface
            val iWindowManagerStubClz = Class.forName("android.view.IWindowManager\$Stub")
            val asInterfaceMethod =
                iWindowManagerStubClz.getMethod("asInterface", IBinder::class.java)
            return asInterfaceMethod.invoke(null, windowManagerService)
        } catch (e: Exception) {
            Log.w(TAG, "reflectWindowManagerService failed!", e)
        }

        return null
    }

    /**
     * https://cs.android.com/android/platform/superproject/+/master:frameworks/base/services/core/java/com/android/server/wm/WindowManagerService.java;l=4463;bpv=1;bpt=0
     */
    fun watchRotation() {
        try {
            iWindowManagerService?.let {
                val methods = it.javaClass.declaredMethods
                for (method in methods) {
                    if (method.name.equals("watchRotation")) {
                        val paramsType = method.parameterTypes
                        val watchRotationMethod =
                            it.javaClass.getMethod("watchRotation", *paramsType)
                        method.invoke(it, iRotationWatcher, 0)
                        break
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "watchRotation failed!", e)
        }
    }

    fun removeRotationWatcher() {
        try {
            val removeWatchRotationMethod = iWindowManagerService?.javaClass?.getMethod(
                "removeRotationWatcher",
                IRotationWatcher::class.java
            );
            removeWatchRotationMethod?.invoke(iWindowManagerService, iRotationWatcher)
        } catch (e: Exception) {
            Log.w(TAG, "removeRotationWatcher failed!", e)
        }
    }

    fun getMethodParamTypes(clazz: Class<*>, methodName: String): Array<Class<*>>? {
        val methods: Array<Method> = clazz.declaredMethods
        for (method in methods) {
            Log.w(TAG, "method = " + method.toGenericString())
            if (methodName == method.name) {
                return method.parameterTypes
            }
        }
        return null
    }
}