package com.example.yourstory.today

import android.app.Application
import android.media.MediaPlayer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.yourstory.database.Repository
import com.example.yourstory.database.data.DiaryEntry
import com.example.yourstory.database.data.EmotionalState
import com.example.yourstory.database.data.Entry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.joda.time.DateTime

class TodayViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: Repository
    var deleteState = MutableLiveData(false)
    var selectedItems = MutableLiveData(listOf<Int>())
    var todayDiaryEntryData : LiveData<List<DiaryEntry>>
    var todayEmotionalStateEntryData : LiveData<List<EmotionalState>>

    var exposedTodayDiaryEntryData = MutableLiveData(listOf<DiaryEntry>())//MutableLiveData<List<DiaryEntry>>
    var exposedTodayEmotionalStateEntryData = MutableLiveData(listOf<EmotionalState>())//: MutableLiveData<List<EmotionalState>>

    var todayMediaPlayer: MediaPlayer? = null
    var currentAudioTrack = MutableLiveData("")
    var mediaPlayerRunning = MutableLiveData(false)

    init {
        repository = Repository(application)
        todayDiaryEntryData = repository.readAllEntriesOfaDate(DateTime.now().toString())
        todayEmotionalStateEntryData = repository.readAllEmotionalStatesOfADate(DateTime.now().toString())
        todayDiaryEntryData.observeForever { newEntries ->
            exposedTodayDiaryEntryData.value = newEntries
        }
        todayEmotionalStateEntryData.observeForever { newEmotionalStates ->
            exposedTodayEmotionalStateEntryData.value = newEmotionalStates
        }
    }

    fun setObservableTodayData() {
        todayDiaryEntryData = repository.readAllEntriesOfaDate(DateTime.now().toString())
        todayEmotionalStateEntryData = repository.readAllEmotionalStatesOfADate(DateTime.now().toString())
        todayDiaryEntryData.observeForever { newEntries ->
            exposedTodayDiaryEntryData.value = newEntries
        }
        todayEmotionalStateEntryData.observeForever { newEmotionalStates ->
            exposedTodayEmotionalStateEntryData.value = newEmotionalStates
        }
    }

    fun deleteDiaryEntries(entries: ArrayList<Entry>) {
        viewModelScope.launch (Dispatchers.IO) {
            for (entry in entries){
                if(entry is DiaryEntry){
                    repository.deleteDiaryEntry(entry.id)
                }
                if(entry is EmotionalState){
                    repository.deleteEmotionalState(entry.id)
                }
            }
        }
    }
}