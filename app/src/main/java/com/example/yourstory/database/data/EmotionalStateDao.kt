package com.example.yourstory.database.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface EmotionalStateDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun addEmotionalState(emotionalState: EmotionalState)

    @Query("DELETE FROM emotional_states_table WHERE id = :emotionalStateId")
    fun deleteEmotionalStateByID(emotionalStateId: Int)

    @Query( "SELECT * FROM emotional_states_table ORDER BY id ASC")
    fun readAllEmotionalStatesSortedByID(): LiveData<List<EmotionalState>>

    @Query( "SELECT * FROM emotional_states_table ORDER BY date ASC")
    fun readAllEmotionalStatesSortedByDate(): LiveData<List<EmotionalState>>

    @Query( "SELECT * FROM emotional_states_table ORDER BY date ASC")
    fun readAllEmotionalStatesSortedByDateWithoutLiveData(): List<EmotionalState>

    @Query("SELECT * FROM emotional_states_table WHERE date BETWEEN :from AND :to")
    fun readAllEmotionalSatesBetweenDates(from:Long, to:Long): LiveData<List<EmotionalState>>

    @Query("SELECT * FROM emotional_states_table WHERE date = (SELECT MIN(date) FROM emotional_states_table)")
    fun readOldestEmotionalState(): EmotionalState

}