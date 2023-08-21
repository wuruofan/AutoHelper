package net.taikula.autohelper.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.taikula.autohelper.data.ClickRepository
import net.taikula.autohelper.data.db.entity.ClickData
import net.taikula.autohelper.data.db.entity.ConfigData
import net.taikula.autohelper.model.ClickArea
import net.taikula.autohelper.tools.Extensions.TAG
import net.taikula.autohelper.tools.FileUtils

/**
 * 点击助手的 ViewModel
 */
class ClickViewModel(private val repository: ClickRepository) : ViewModel() {
    /**
     * 当前选中的配置 Id
     */
    private val currentConfigIdLiveData = MutableLiveData(1)

    /**
     * 当前所有点击数据
     */
    val currentClickData: LiveData<List<ClickData>> =
        currentConfigIdLiveData.switchMap { configId ->
            repository.getAllClickData(configId).asLiveData()
        }

    /**
     * 更新当前选择的配置 Id，触发 [currentClickData] 的 switchMap 会影响界面展示的点击数据
     */
    fun updateCurrentConfigId(newConfigId: Int) {
        currentConfigIdLiveData.value = newConfigId
    }

    /**
     * 当前选中的配置 Id
     */
    val currentConfigId: Int
        get() = currentConfigIdLiveData.value ?: 1

    /**
     * 插入点击数据
     */
    fun insert(data: ClickData, callback: (Long) -> Unit = {}) = viewModelScope.launch {
        val ret = repository.insert(data)
        if (ret > 0) {
            callback(ret)
        } else {
            Log.w(TAG, "insert ClickData failed: $ret!")
        }
    }

    /**
     * 插入点击数据
     */
    fun insert(clickArea: ClickArea, callback: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val clickData = ClickData(
                0, currentConfigIdLiveData.value!!, currentClickData.value?.size ?: 0,
                clickArea, clickArea.toString()
            )
            val ret = repository.insert(clickData)
            if (ret > 0) {
                callback(ret)
            } else {
                Log.w(TAG, "insert clickArea failed: $ret!")
            }
        }
    }

    /**
     * 删除点击数据
     */
    fun delete(data: ClickData, callback: (Boolean) -> Unit = {}) = viewModelScope.launch {
        val ret = repository.delete(data)
        if (ret > 0) {
            callback(true)

            // 删除存储在数据库外的图片
            withContext(Dispatchers.IO) {
                data.clickArea.imagePath?.let { FileUtils.delete(it) }
            }
        } else {
            Log.w(TAG, "delete $data failed: $ret!")
            callback(false)
        }
    }

    /**
     * 更新点击数据
     */
    fun update(data: ClickData, callback: (Boolean) -> Unit = {}) = viewModelScope.launch {
        val ret = repository.update(data)
        if (ret > 0) {
            callback(true)
        } else {
            Log.w(TAG, "update $data failed: $ret!")
            callback(false)
        }
    }

    /**
     * 根据当前数据的 index 删除点击数据
     */
    fun deleteClickData(index: Int, callback: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val clickData = currentClickData.value?.get(index)
            if (clickData != null) {
                repository.delete(clickData)

                withContext(Dispatchers.IO) {
                    clickData.clickArea.imagePath?.let { FileUtils.delete(it) }
                }
            }
        }
    }

    /**
     * 所有配置数据
     */
    val allConfigData: LiveData<List<ConfigData>> = repository.getAllConfig().asLiveData()

    /**
     * 插入配置数据
     */
    fun insert(data: ConfigData, callback: (Long) -> Unit = {}) = viewModelScope.launch {
        val ret = repository.insert(data)
        if (ret > 0) {
            callback(ret)
        } else {
            Log.w(TAG, "insert $data failed: $ret!")
        }
    }

    /**
     * 更新配置数据
     */
    fun update(data: ConfigData, callback: (Boolean) -> Unit = {}) = viewModelScope.launch {
        val ret = repository.update(data)
        if (ret > 0) {
            callback(true)
        } else {
            Log.w(TAG, "update $data failed: $ret!")
            callback(false)
        }
    }

    /**
     * 删除配置数据
     */
    fun delete(data: ConfigData, callback: (Boolean) -> Unit = {}) = viewModelScope.launch {
        val ret = repository.delete(data)

        if (ret > 0) {
            callback(true)
            try {

                repository.getAllClickData(data.id).collect { list ->
                    list.forEach {
                        this@ClickViewModel.delete(it)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

        } else {
            Log.w(TAG, "delete $data failed: $ret!")
            callback(false)
        }
    }

}