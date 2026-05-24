package com.talkie.app.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.talkie.app.data.local.ChannelEntity
import com.talkie.app.data.local.TalkieDatabase
import com.talkie.app.data.local.TransmissionLogEntity
import com.talkie.app.domain.PdfReportGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AdminViewModel(application: Application) : AndroidViewModel(application) {
    private val talkieDao = TalkieDatabase.getDatabase(application).talkieDao()

    val logs: StateFlow<List<TransmissionLogEntity>> = talkieDao.getAllTransmissionLogsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalTransmissions: StateFlow<Int> = talkieDao.getTotalTransmissionsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalAirtime: StateFlow<Int?> = talkieDao.getTotalAirtimeFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalFlaggedIncidents: StateFlow<Int> = talkieDao.getTotalFlaggedIncidentsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val registeredWorkersCount: StateFlow<Int> = talkieDao.getRegisteredWorkersCountFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val channels: StateFlow<List<ChannelEntity>> = talkieDao.getActiveChannelsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _reportPath = MutableStateFlow<String?>(null)
    val reportPath: StateFlow<String?> = _reportPath.asStateFlow()

    fun generateReport() {
        viewModelScope.launch {
            val file = PdfReportGenerator.generateLogReport(logs.value)
            if (file != null) {
                _reportPath.value = file.absolutePath
            } else {
                _reportPath.value = "Error generating report"
            }
        }
    }

    fun addChannel(name: String, frequency: String) {
        viewModelScope.launch {
            talkieDao.insertChannel(ChannelEntity(channelName = name, frequencyHex = frequency))
        }
    }

    fun deleteChannel(channel: ChannelEntity) {
        viewModelScope.launch {
            talkieDao.deleteChannel(channel)
        }
    }
}
