package net.taikula.autohelper.viewmodel

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
import net.taikula.autohelper.tools.FileUtils

/**
 * 点击助手的 ViewModel
 */
class ClickViewModel(private val repository: ClickRepository) : ViewModel() {
    private val currentConfigIdLiveData = MutableLiveData(1)

    val currentClickData: LiveData<List<ClickData>> =
        currentConfigIdLiveData.switchMap { configId ->
            repository.getAllClickData(configId).asLiveData()
        }

    fun updateCurrentConfigId(newConfigId: Int) {
        currentConfigIdLiveData.value = newConfigId
    }

    fun insert(data: ClickData) = viewModelScope.launch {
        repository.insert(data)
    }

    fun insert(clickArea: ClickArea) {
        viewModelScope.launch {
            val clickData = ClickData(
                0, currentConfigIdLiveData.value!!, currentClickData.value?.size ?: 0,
                clickArea, System.currentTimeMillis()
            )
            repository.insert(clickData)
        }
    }

    fun delete(data: ClickData) = viewModelScope.launch {
        repository.delete(data)
    }

    fun update(data: ClickData) = viewModelScope.launch {
        repository.update(data)
    }

    fun delete(id: Int) {
        viewModelScope.launch {
            val clickData = currentClickData.value?.get(id)
            if (clickData != null) {
                repository.delete(clickData)

                withContext(Dispatchers.IO) {
                    clickData.clickArea.imagePath?.let { FileUtils.delete(it) }
                }
            }
        }
    }

    val allConfigData: LiveData<List<ConfigData>> = repository.getAllConfig().asLiveData()

    fun insert(data: ConfigData) = viewModelScope.launch {
        repository.insert(data)
    }

    fun update(data: ConfigData) = viewModelScope.launch {
        repository.update(data)
    }

    fun delete(data: ConfigData) = viewModelScope.launch {
        repository.delete(data)

        repository.getAllClickData(data.id).collect { list ->
            list.forEach {
                this@ClickViewModel.delete(it.id)
            }
        }
    }

}