package com.example.yourstory.database

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.yourstory.database.data.DiaryEntry
import com.example.yourstory.database.data.DiaryEntryDao
import com.example.yourstory.database.data.EmotionalState
import com.example.yourstory.database.data.EmotionalStateDao
import com.example.yourstory.utils.DateEpochConverter
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import java.time.ZoneOffset.UTC
import java.util.*

class Repository(application: Application){

    var diaryEntryDao: DiaryEntryDao
    var emotionalStateDao: EmotionalStateDao

    init {
        diaryEntryDao = Database.getDatabase(application).diaryEntryDao()
        emotionalStateDao = Database.getDatabase(application).emotionalStateDao()
    }

    //Diary Entry functions

    fun readAllEntriesSortedByID(): LiveData<List<DiaryEntry>> {
        return diaryEntryDao.readAllEntriesSortedByID()
    }

    fun readAllEntriesSortedByDate(): LiveData<List<DiaryEntry>>{
        return diaryEntryDao.readAllEntriesSortedByDate()
    }

    fun getEmotionalStateOfDiaryEntry(diaryEntry: DiaryEntry): LiveData<List<EmotionalState>>{
        return diaryEntryDao.getEmotionalStateOfDiaryEntry(diaryEntry.emotionalStateID)
    }

     fun addDiaryEntry(diaryEntry: DiaryEntry){
        diaryEntryDao.addDiaryEntry(diaryEntry)
    }

    fun readAllEntriesOfaDate(isoDate: String): LiveData<List<EmotionalState>>{
        var epochCurrentDateStart = DateEpochConverter.convertDateTimeToEpoch(DateTime(isoDate, DateTimeZone.UTC).withTime(0, 0, 0, 0).toDateTimeISO().toString())
        var epochCurrentDateEnd = DateEpochConverter.convertDateTimeToEpoch(DateTime(isoDate, DateTimeZone.UTC).withTime(23, 59, 59, 999).toDateTimeISO().toString())
        return diaryEntryDao.readAllEntriesOfaDate(epochCurrentDateStart,epochCurrentDateEnd)
    }

    //Emotional State functions

    fun readAllEmotionalStatesSortedByID(): LiveData<List<EmotionalState>>{
        return emotionalStateDao.readAllEmotionalStatesSortedByID()
    }

    fun readAllEmotionalStatesSortedByDate(): LiveData<List<EmotionalState>>{
        return emotionalStateDao.readAllEmotionalStatesSortedByDate()
    }

     fun addEmotionalState(emotionalState: EmotionalState){
        emotionalStateDao.addEmotionalState(emotionalState)
    }

    fun readLastEmotionalStateID(): Int {
        var emotionalState = emotionalStateDao.readAllEmotionalStatesSortedByDateWithoutLiveData()
        if(emotionalState.isEmpty()){
            return -1
        }else {
            return emotionalState[0].id
        }
    }
}