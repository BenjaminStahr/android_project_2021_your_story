package com.example.yourstory.today

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.*
import android.graphics.Color
import android.media.MediaPlayer
import android.view.*
import android.view.LayoutInflater
import android.view.ViewGroup.MarginLayoutParams
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.example.yourstory.MainActivity
import com.example.yourstory.R
import com.example.yourstory.database.data.DiaryEntry
import com.example.yourstory.database.data.EmotionalState
import com.example.yourstory.database.data.Entry
import com.example.yourstory.utils.DateEpochConverter
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.android.synthetic.main.insertable_map_view.view.*
import kotlinx.android.synthetic.main.text_entry_diary_layout.view.*
import java.io.File
import java.io.IOException

class DiaryEntriesAdapter(var lifeCycleOwner: LifecycleOwner) : RecyclerView.Adapter<DiaryEntriesAdapter.ViewHolder>() {

    private var todayModelData: List<Entry> = listOf()
    private lateinit var view: View
    private lateinit var context: Context
    private lateinit var selectedItems: ArrayList<Int>
    private lateinit var todayViewModel: TodayViewModel

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DiaryEntriesAdapter.ViewHolder {
        view = LayoutInflater.from(parent.context).inflate(R.layout.text_entry_diary_layout, parent, false)
        context = parent.context
        todayViewModel = ViewModelProvider(context as MainActivity)[TodayViewModel::class.java]
        todayViewModel.selectedItems.observe(lifeCycleOwner,{
            selectedItems = ArrayList(it)
        })
        return ViewHolder(view)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: DiaryEntriesAdapter.ViewHolder, position: Int) {
        // views cant be recycled if nodes from the view are removed
        holder.setIsRecyclable(false)
        holder.date.text =
            DateEpochConverter.convertEpochToDateTime(todayModelData[position].date).toString()
                .split("T")[1].subSequence(0, 5)

        // in this list are diary entries and emotional states and both get handled separately
        if (todayModelData[position] is DiaryEntry) {
            // nodes from emotional state arent needed
            if (holder.emotionalStateRoot.parent is ViewGroup) {
               (holder.emotionalStateRoot.parent as ViewGroup).removeView(holder.emotionalStateRoot)
            }
            val entry = todayModelData[position] as DiaryEntry

            // Following each component of the possible entry will be processed separately and in the end the layout will be reorganized
            // based on the remaining nodes
            var imageFlag = false
            var locationFlag = false
            var textFlag = false
            var audioFlag = false
            // for each possible part in the entry we check if its our defined zero and if so we cut the node from the layout
            // because its not part of the entry. It looks good anyway, because it was worked with linear layout, which resizes based on
            // layout weights
            if (entry.text.isEmpty() && holder.diaryText.parent != null) {
                (holder.diaryText.parent as ViewGroup).removeView(holder.diaryText)
            } else {
                textFlag = true
                holder.diaryText.text = entry.text
            }

            if (entry.image.isEmpty() && holder.diaryImage.parent != null) {
                (holder.diaryImage.parent as ViewGroup).removeView(holder.diaryImage)
            } else {
                imageFlag = true
                holder.diaryImage.setImageURI(File(context.filesDir, entry.image + ".png").toUri())
                holder.diaryImage.clipToOutline = true
            }

            if (entry.locationLat == 0.0 && entry.locationLong == 0.0 && holder.diaryLocation.parent != null) {
                (holder.diaryLocation.parent as ViewGroup).removeView(holder.diaryLocation)
            } else {
                // if we have a map we add it later to the layout because having it in there from the beginning is bad for performance
                locationFlag = true
                holder.diaryLocation.clipToOutline = true
                val mapContainer = View.inflate(context, R.layout.insertable_map_view, holder.diaryLocationViewGroupHolder as ViewGroup)
                val mapView = mapContainer.main_today_map_view
                (mapView as MapView).onCreate(null)
                mapView.getMapAsync { map ->
                    val location = LatLng(entry.locationLat, entry.locationLong)
                    map.addMarker(
                        MarkerOptions()
                            .position(location)
                    )
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 11f))
                    map.uiSettings.setAllGesturesEnabled(false)
                    map.uiSettings.isMapToolbarEnabled = false
                    mapView.onResume()
                }

            }

            if (entry.audio == "" && holder.diaryAudio.parent != null) {
                (holder.diaryAudio.parent as ViewGroup).removeView(holder.diaryAudio)
            } else {
                // we have one audio player which is shared between all audio objects, when this specific audio object
                // is the active one, it persists its state in the view model
                audioFlag = true
                var animSeekbar: ValueAnimator? = null
                holder.diaryAudio.clipToOutline = true

                fun setupMediaPlayer() {
                    todayViewModel.todayMediaPlayer = MediaPlayer().apply {
                        try {
                            setDataSource(todayViewModel.currentAudioTrack.value)
                            prepare()
                        } catch (e: IOException) { }
                    }
                    holder.seekBar.max = todayViewModel.todayMediaPlayer!!.duration
                    animSeekbar = ValueAnimator.ofInt(0, holder.seekBar.max)
                    animSeekbar!!.duration = todayViewModel.todayMediaPlayer!!.duration.toLong()
                    animSeekbar!!.addUpdateListener { animation ->
                        val animProgress = animation.animatedValue as Int
                        holder.seekBar.progress = animProgress
                    }
                    todayViewModel.todayMediaPlayer!!.start()
                    animSeekbar!!.addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            holder.seekBar.progress = 0
                        }
                    })
                    animSeekbar!!.start()
                    todayViewModel.todayMediaPlayer!!.setOnCompletionListener {
                        todayViewModel.todayMediaPlayer!!.release()
                        todayViewModel.todayMediaPlayer = null
                        todayViewModel.mediaPlayerRunning.value = false
                        todayViewModel.currentAudioTrack.value = ""
                    }
                }
                fun setupCurrentTrackRunning() {
                    holder.seekBar.progress = todayViewModel.todayMediaPlayer!!.currentPosition
                    holder.seekBar.max = todayViewModel.todayMediaPlayer!!.duration
                    animSeekbar = ValueAnimator.ofInt(todayViewModel.todayMediaPlayer!!.currentPosition, holder.seekBar.max)
                    animSeekbar!!.duration = todayViewModel.todayMediaPlayer!!.duration.toLong() - todayViewModel.todayMediaPlayer!!.currentPosition
                    animSeekbar!!.addUpdateListener { animation ->
                        val animProgress = animation.animatedValue as Int
                        holder.seekBar.progress = animProgress
                    }
                    animSeekbar!!.addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            holder.seekBar.progress = 0
                        }
                    })
                    animSeekbar!!.start()
                }
                fun setupCurrentTrackPaused() {
                    holder.seekBar.progress = todayViewModel.todayMediaPlayer!!.currentPosition
                    holder.seekBar.max = todayViewModel.todayMediaPlayer!!.duration
                    animSeekbar = ValueAnimator.ofInt(todayViewModel.todayMediaPlayer!!.currentPosition, holder.seekBar.max)
                    animSeekbar!!.duration = todayViewModel.todayMediaPlayer!!.duration.toLong() - todayViewModel.todayMediaPlayer!!.currentPosition
                    animSeekbar!!.addUpdateListener { animation ->
                        val animProgress = animation.animatedValue as Int
                        holder.seekBar.progress = animProgress
                    }
                    animSeekbar!!.addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            holder.seekBar.progress = 0
                        }
                    })
                    animSeekbar!!.start()
                    animSeekbar!!.pause()
                }
                todayViewModel.mediaPlayerRunning.observe(lifeCycleOwner,{
                    if (todayViewModel.currentAudioTrack.value == entry.audio && todayViewModel.mediaPlayerRunning.value!!) {
                        holder.playButton.setImageResource(R.drawable.pause_icon_media_player)
                    } else {
                        holder.playButton.setImageResource(R.drawable.ic_baseline_play_arrow_24)
                    }
                })
                todayViewModel.currentAudioTrack.observe(lifeCycleOwner,{
                    if(todayViewModel.currentAudioTrack.value != entry.audio && animSeekbar != null) {
                        animSeekbar!!.removeAllListeners()
                        animSeekbar!!.cancel()
                        holder.seekBar.progress = 0
                    }
                })

                if (todayViewModel.todayMediaPlayer != null &&
                    todayViewModel.mediaPlayerRunning.value!! && todayViewModel.currentAudioTrack.value == entry.audio) {
                    setupCurrentTrackRunning()
                } else if (todayViewModel.todayMediaPlayer != null &&
                    !todayViewModel.mediaPlayerRunning.value!! && todayViewModel.currentAudioTrack.value == entry.audio) {
                    setupCurrentTrackPaused()
                }

                holder.seekBar.setOnSeekBarChangeListener (object: SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(
                        seekBar: SeekBar?,
                        progress: Int,
                        fromUser: Boolean
                    ) {
                        if (fromUser) {
                            if (todayViewModel.todayMediaPlayer != null &&
                                todayViewModel.mediaPlayerRunning.value!! && todayViewModel.currentAudioTrack.value == entry.audio) {
                                if (animSeekbar != null) {
                                    animSeekbar!!.removeAllListeners()
                                    animSeekbar!!.cancel()
                                }
                                todayViewModel.todayMediaPlayer!!.seekTo(progress)
                                setupCurrentTrackRunning()
                            } else if (todayViewModel.todayMediaPlayer != null &&
                                !todayViewModel.mediaPlayerRunning.value!! && todayViewModel.currentAudioTrack.value == entry.audio) {
                                if (animSeekbar != null) {
                                    animSeekbar!!.removeAllListeners()
                                    animSeekbar!!.cancel()
                                }
                                todayViewModel.todayMediaPlayer!!.seekTo(progress)
                                setupCurrentTrackPaused()
                            }
                        }
                    }
                    override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    }
                    override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    }
                })
                holder.playButton.setOnClickListener {
                    if (todayViewModel.todayMediaPlayer != null &&
                        !todayViewModel.mediaPlayerRunning.value!! && todayViewModel.currentAudioTrack.value == entry.audio) {
                        todayViewModel.todayMediaPlayer!!.start()
                        animSeekbar = ValueAnimator.ofInt(todayViewModel.todayMediaPlayer!!.currentPosition, holder.seekBar.max)
                        animSeekbar!!.duration = todayViewModel.todayMediaPlayer!!.duration.toLong() - todayViewModel.todayMediaPlayer!!.currentPosition
                        animSeekbar!!.addUpdateListener { animation ->
                            val animProgress = animation.animatedValue as Int
                            holder.seekBar.progress = animProgress
                        }
                        animSeekbar!!.addListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator) {
                                holder.seekBar.progress = 0
                            }
                        })
                        animSeekbar!!.start()
                        todayViewModel.mediaPlayerRunning.value = true
                    } else if (todayViewModel.mediaPlayerRunning.value!! && todayViewModel.currentAudioTrack.value == entry.audio) {
                        todayViewModel.todayMediaPlayer!!.pause()
                        animSeekbar!!.pause()
                        todayViewModel.mediaPlayerRunning.value = false
                    } else if (todayViewModel.todayMediaPlayer != null && todayViewModel.currentAudioTrack.value != entry.audio) {
                        todayViewModel.todayMediaPlayer!!.stop()
                        todayViewModel.todayMediaPlayer!!.release()
                        todayViewModel.todayMediaPlayer = null
                        todayViewModel.currentAudioTrack.value = entry.audio
                        todayViewModel.mediaPlayerRunning.value = true
                        setupMediaPlayer()
                    }

                    if (todayViewModel.todayMediaPlayer == null) {
                        todayViewModel.currentAudioTrack.value = entry.audio
                        todayViewModel.mediaPlayerRunning.value = true
                        setupMediaPlayer()
                    }
                }
            }
            // its not always completely true that the layout looks good after removing some nodes, where this is not the case
            // some layout properties get adjusted programmatically
            if (imageFlag) {
                if (!locationFlag) {
                    (holder.firstRowLinearLayoutSecondItem.parent as ViewGroup).removeView(holder.firstRowLinearLayoutSecondItem)
                    (holder.diaryImage.layoutParams as LinearLayout.LayoutParams).weight = 0f
                    holder.diaryImage.layoutParams.width = holder.itemView.context.resources.getDimension(R.dimen.new_image_width).toInt()
                    (holder.diaryImage.layoutParams as LinearLayout.LayoutParams).gravity =  Gravity.CENTER
                    holder.firstRowLinearLayout.orientation = LinearLayout.VERTICAL
                }
            }
            if (!textFlag && audioFlag) {
                val layoutParams = holder.diaryImage.layoutParams as MarginLayoutParams
                layoutParams.bottomMargin = holder.itemView.context.resources.getDimension(R.dimen.standard_margin).toInt()
                holder.diaryImage.layoutParams = layoutParams

                val layoutParamsLocation = holder.diaryLocation.layoutParams as MarginLayoutParams
                layoutParamsLocation.bottomMargin = holder.itemView.context.resources.getDimension(R.dimen.standard_margin).toInt()
                holder.diaryLocation.layoutParams = layoutParamsLocation
            }
            if (!imageFlag && locationFlag) {
                val layoutParamsLocation = holder.firstRowLinearLayoutSecondItem.layoutParams as MarginLayoutParams
                layoutParamsLocation.leftMargin = 0
                holder.firstRowLinearLayoutSecondItem.layoutParams = layoutParamsLocation
            }
        }
        if (todayModelData[position] is EmotionalState) {
            val state = todayModelData[position] as EmotionalState
            if (holder.diaryLocation.parent is ViewGroup) {
                (holder.diaryLocation.parent as ViewGroup).removeView(holder.diaryLocation)
            }
            if (holder.diaryImage.parent is ViewGroup) {
                (holder.diaryImage.parent as ViewGroup).removeView(holder.diaryImage)
            }
            if (holder.diaryAudio.parent is ViewGroup) {
                (holder.diaryAudio.parent as ViewGroup).removeView(holder.diaryAudio)
            }
            if (holder.diaryText.parent is ViewGroup) {
                (holder.diaryText.parent as ViewGroup).removeView(holder.diaryText)
            }
            holder.joyLikert.text = state.joy.toString()
            holder.surpriseLikert.text = state.surprise.toString()
            holder.angerLikert.text = state.anger.toString()
            holder.sadnessLikert.text = state.sadness.toString()
            holder.fearLikert.text = state.fear.toString()
            holder.disgustLikert.text = state.disgust.toString()
            holder.joyEmoji.alpha = (0.2 + ((state.joy.toFloat()/5) * 0.8)).toFloat()
            holder.surpriseEmoji.alpha = (0.2 + ((state.surprise.toFloat()/5) * 0.8)).toFloat()
            holder.angerEmoji.alpha = (0.2 + ((state.anger.toFloat()/5) * 0.8)).toFloat()
            holder.sadnessEmoji.alpha = (0.2 + ((state.sadness.toFloat()/5) * 0.8)).toFloat()
            holder.fearEmoji.alpha = (0.2 + ((state.fear.toFloat()/5) * 0.8)).toFloat()
            holder.disgustEmoji.alpha = (0.2 + ((state.disgust.toFloat()/5) * 0.8)).toFloat()
        }

        if(!selectedItems.contains(position)){
            holder.itemView.checkbox.visibility = View.INVISIBLE
            holder.itemView.cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.egg_white))
        }else if (selectedItems.contains(position)) {
            holder.itemView.checkbox.visibility = View.VISIBLE
            holder.itemView.cardView.setCardBackgroundColor(Color.LTGRAY)
        }


        holder.itemView.setOnTouchListener(View.OnTouchListener {
                view, event ->

            if (event.action == MotionEvent.ACTION_DOWN) {
                view.tag = true
            } else if (view.tag as Boolean) {
                val eventDuration = event.eventTime - event.downTime
                if (eventDuration > ViewConfiguration.getLongPressTimeout()) {
                    view.tag = false

                    if(selectedItems.contains(position)){
                        holder.itemView.checkbox.visibility = View.INVISIBLE
                        holder.itemView.cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.egg_white))
                        selectedItems.remove(position)
                        todayViewModel.selectedItems.postValue(selectedItems)
                    }else{
                        holder.itemView.checkbox.visibility = View.VISIBLE
                        holder.itemView.cardView.setCardBackgroundColor(Color.LTGRAY)
                        selectedItems.add(position)
                        todayViewModel.selectedItems.postValue(selectedItems)
                    }

                    if(selectedItems.isNotEmpty()){
                        todayViewModel.deleteState.postValue(true)
                    }else{
                        todayViewModel.deleteState.postValue(false)
                    }
                    return@OnTouchListener true
                }
            }
            return@OnTouchListener true
        })
    }

    override fun getItemCount(): Int {
        return todayModelData.size
    }

    fun getSelectedEntries(): ArrayList<Entry>{
    var list = arrayListOf<Entry>()
        for (int in selectedItems){
            list.add(todayModelData[int])
        }
        return list
    }

    fun deleteSelectedEntries(){
        todayViewModel.selectedItems.postValue(listOf())
        todayViewModel.deleteState.postValue(false)
    }

    inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        // entry nodes
        val diaryText: TextView = itemView.findViewById(R.id.main_today_text)
        val diaryImage: ImageView = itemView.findViewById(R.id.main_today_image)
        val diaryAudio: View = itemView.findViewById(R.id.main_today_audio_source)
        val diaryLocation: CardView = itemView.findViewById(R.id.main_today_location)
        val diaryLocationViewGroupHolder: LinearLayout = itemView.findViewById(R.id.main_today_map_view_holder)
        val playButton: ImageView = itemView.findViewById(R.id.entry_diary_play_button)
        val seekBar: SeekBar = itemView.findViewById(R.id.entry_diary_seek_bar)
        val date: TextView = itemView.findViewById((R.id.entry_date))
        val firstRowLinearLayout: LinearLayout = itemView.findViewById(R.id.text_diary_entry_first_row)
        val firstRowLinearLayoutSecondItem: LinearLayout = itemView.findViewById(R.id.text_entry_diary_layout_second_item)
        // emotional state nodes
        val joyLikert: TextView = itemView.findViewById(R.id.joy_today_likert)
        val surpriseLikert: TextView = itemView.findViewById(R.id.surprise_today_likert)
        val angerLikert: TextView = itemView.findViewById(R.id.anger_today_likert)
        val sadnessLikert: TextView = itemView.findViewById(R.id.sadness_today_likert)
        val fearLikert: TextView = itemView.findViewById(R.id.fear_today_likert)
        val disgustLikert: TextView = itemView.findViewById(R.id.disgust_today_likert)
        val emotionalStateRoot: CardView = itemView.findViewById(R.id.emotional_state_root)
        val joyEmoji: ImageView = itemView.findViewById(R.id.emoji_today_joy)
        val surpriseEmoji: ImageView = itemView.findViewById(R.id.emoji_today_surprise)
        val angerEmoji: ImageView = itemView.findViewById(R.id.emoji_today_anger)
        val sadnessEmoji: ImageView = itemView.findViewById(R.id.emoji_today_sadness)
        val fearEmoji: ImageView = itemView.findViewById(R.id.emoji_today_fear)
        val disgustEmoji: ImageView = itemView.findViewById(R.id.emoji_today_disgust)
    }
    fun setData(diaries: List<Entry>){
        if (diaries.isEmpty()) {
            return
        }
        if (diaries[0] is DiaryEntry) {
            val emotionalOnly = this.todayModelData.toMutableList()
            val iterator = emotionalOnly.listIterator()
            while (iterator.hasNext()) {
                val e = iterator.next()
                if (e is DiaryEntry) {
                    iterator.remove()
                }
            }
            this.todayModelData = (emotionalOnly + diaries).sortedBy { it.date }.reversed()
        }
        else {
            val diaryEntriesOnly = this.todayModelData.toMutableList()
            val iterator = diaryEntriesOnly.listIterator()
            while (iterator.hasNext()) {
                val e = iterator.next()
                if (e is EmotionalState) {
                    iterator.remove()
                }
            }
            this.todayModelData = ((diaryEntriesOnly + diaries).sortedBy { it.date }).reversed()
        }
        notifyDataSetChanged()
    }

    fun removeDiaryEntries() {
        val tempList = arrayListOf<Entry>()
        for (entry in todayModelData){
            if(entry !is DiaryEntry){
                tempList.add(entry)
            }
        }
        todayModelData = tempList.sortedBy { it.date }.reversed()
        notifyDataSetChanged()
    }

    fun removeEmotionalStates() {
        val tempList = arrayListOf<Entry>()
        for (entry in todayModelData){
            if(entry !is EmotionalState){
                tempList.add(entry)
            }
        }
        todayModelData = tempList.sortedBy { it.date }.reversed()
        notifyDataSetChanged()
    }

}

