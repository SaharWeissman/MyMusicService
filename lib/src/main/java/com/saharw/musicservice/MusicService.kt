package com.saharw.musicservice

import android.app.Activity
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import com.saharw.mymusicplayer.entities.data.base.MediaItem
import java.io.File


class MusicService : Service(),
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener {

    private val Playing: CharSequence = "Playing"

    private val TAG = "MusicService"
    lateinit var mPlayer : MediaPlayer
    var mSongOffset : Int = 0
    val mBinder = MusicBinder()
    var mSongIdx = 0
    lateinit var mSongsList: List<MediaItem>
    lateinit var mActivity: Activity
    private var mSongTitle: CharSequence = "Empty"
    private val NOTIFICATION_ID: Int = 1

    override fun onCreate() {
        Log.d(TAG, "onCreate")
        super.onCreate()
        initMusicPlayer()
    }

    override fun onBind(intent: Intent?): IBinder {
        Log.d(TAG, "onBind")
        return mBinder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "onUnbind")
        mPlayer.stop()
        mPlayer.release()
        return false
    }

    // methods for music service
    fun play() {
        Log.d(TAG, "play: mSongIdx = $mSongIdx")
        mPlayer.reset()
        try{
            val mediaItem = mSongsList[mSongIdx]
            mSongTitle = mediaItem.name
            mPlayer.setDataSource(applicationContext, Uri.fromFile(File(mediaItem.dataPath)))
            mPlayer.prepareAsync()
        }catch (t: Throwable){
            Log.e(TAG, "play: error setting data source for song pos: $mSongIdx", t)
        }
    }

    fun setSongsUris(songsList: List<MediaItem>) {
        Log.d(TAG, "setSongsUris")
        this.mSongsList = songsList
    }

    fun setSong(idx: Int){
        Log.d(TAG, "setSong: idx = $idx")
        mSongIdx = idx
    }

    fun playPrevious() {
        Log.d(TAG, "playPrevious")
        mSongIdx--

        // cyclic behavior
        if(mSongIdx < 0) mSongIdx = mSongsList.size-1
        play()
    }

    fun playNext() {
        Log.d(TAG, "playNext")
        mSongIdx++

        // cyclic behavior
        if(mSongIdx >= mSongsList.size) mSongIdx = 0
        play()
    }

    // methods for music controller
    fun getSongPosition(): Int {
        Log.d(TAG, "getSongPosition")
        return mPlayer.currentPosition
    }

    fun getSongDuration(): Int {
        Log.d(TAG, "getSongDuration")
        return mPlayer.duration
    }

    fun isPlaying(): Boolean {
        Log.d(TAG, "isPlaying")
        return mPlayer.isPlaying
    }

    fun pausePlayer() {
        Log.d(TAG, "pausePlayer")
        mPlayer.pause()
    }


    fun seek(position: Int) {
        Log.d(TAG, "seek: position = $position")
        mPlayer.seekTo(position)
    }

    fun start() {
        Log.d(TAG, "start")
        mPlayer.start()
    }

    // Media player callbacks
    override fun onPrepared(mp: MediaPlayer?) {
        Log.d(TAG, "onPrepared: start playing song : ${mSongsList[mSongIdx]}")
        if(mp != null) {
            mp.start()

            // create pending intent (for starting activity from notification) & add notification
            var intent = Intent(this@MusicService, mActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            var pendingIntent = PendingIntent.getActivity(this@MusicService, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

            var notificationBuilder = Notification.Builder(this@MusicService)
            notificationBuilder.setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.ic_play)
                .setTicker(mSongTitle)
                .setOngoing(true)
                .setContentTitle(Playing)
                .setContentText(mSongTitle)
            
            startForeground(NOTIFICATION_ID, notificationBuilder.build())

        }else {
            Log.e(TAG, "onPrepared: player is null!")
        }
    }

    override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
        Log.e(TAG, "onError: mp = $mp, what = $what, extra = $extra, currSongIdx = ${mSongIdx}")
        mPlayer.reset()
        return false
    }

    override fun onCompletion(mp: MediaPlayer?) {
        Log.d(TAG, "onCompletion: curSongIdx = $mSongIdx")
        if(mPlayer.currentPosition > 0){
            mPlayer.reset()
            playNext()
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        super.onDestroy()
        stopForeground(true)
    }

    private fun initMusicPlayer() {
        Log.d(TAG, "initMusicPlayer")
        mSongOffset = 0
        mPlayer = MediaPlayer()

        mPlayer.setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)

        // distinguish between API < Lollipop (before deprecation) and after
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            Log.d(TAG, "initMusicPlayer: api < Lollipop, using 'setAudioAttributes' method")
            mPlayer.setAudioAttributes(AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build())

        }else {
            Log.d(TAG, "initMusicPlayer: api < Lollipop, using 'setAudioStreamType' method")
            mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC)
        }

        // set listeners
        mPlayer.setOnPreparedListener(this)
        mPlayer.setOnCompletionListener (this)
        mPlayer.setOnErrorListener(this)
    }

    // for binding to service
    inner class MusicBinder : Binder() {
        fun getService(activity: Activity) : MusicService {
            Log.d(TAG, "getService")
            this@MusicService.mActivity = activity
            return this@MusicService
        }
    }
}