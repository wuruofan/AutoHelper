package net.taikula.autohelper.data.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.forEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.taikula.autohelper.data.ClickRepository
import net.taikula.autohelper.data.db.entity.ClickData
import net.taikula.autohelper.data.db.entity.ConfigData
import net.taikula.autohelper.data.model.ClickArea
import net.taikula.autohelper.tools.FileUtils

class ClickViewModel(private val repository: ClickRepository) : ViewModel() {
    var currentConfigId = 0

    val allClickData: LiveData<List<ClickData>> = repository.getAllClickData(currentConfigId).asLiveData()

    var currentClickData = MutableLiveData<List<ClickData>>()

    fun queryClickData(configId: Int) {
        currentClickData.value = repository.getAllClickData(configId).asLiveData().value
    }

    fun insert(data: ClickData) = viewModelScope.launch {
        repository.insert(data)
    }

    fun insert(clickArea: ClickArea) {
        viewModelScope.launch {
            val clickData = ClickData(
                0, 0, allClickData.value?.size ?: 0,
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
            val clickData = allClickData.value?.get(id)
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