package com.rfw.clickhelper.data.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.rfw.clickhelper.data.ClickRepository
import com.rfw.clickhelper.data.db.entity.ClickData
import com.rfw.clickhelper.data.model.ClickArea
import com.rfw.clickhelper.tools.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ClickViewModel(private val repository: ClickRepository) : ViewModel() {

    val allData: LiveData<List<ClickData>> = repository.allClickData.asLiveData()

    fun insert(data: ClickData) = viewModelScope.launch {
        repository.insert(data)
    }

    fun insert(clickArea: ClickArea) {
        viewModelScope.launch {
            val clickData = ClickData(
                0, 0, allData.value?.size ?: 0,
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
            val clickData = allData.value?.get(id)
            if (clickData != null) {
                repository.delete(clickData)

                withContext(Dispatchers.IO) {
                    clickData.clickArea.imagePath?.let { FileUtils.delete(it) }
                }
            }
        }
    }
}