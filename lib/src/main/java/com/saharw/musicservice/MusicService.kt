package com.saharw.musicservice

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


class MusicService : Service(),
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener {

    private val TAG = "MusicService"
    lateinit var mPlayer : MediaPlayer
    var mSongOffset : Int = 0
    val mBinder = MusicBinder()
    var mSongIdx = 0
    lateinit var mSongsUrisList : List<Uri>

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
            mPlayer.setDataSource(applicationContext, mSongsUrisList[mSongIdx])
            mPlayer.prepareAsync()
        }catch (t: Throwable){
            Log.e(TAG, "play: error setting data source for song pos: $mSongIdx", t)
        }
    }

    fun setSongsUris(songsUrisList: List<Uri>) {
        Log.d(TAG, "setSongsUris")
        this.mSongsUrisList = songsUrisList
    }

    fun setSong(idx: Int){
        Log.d(TAG, "setSong: idx = $idx")
        mSongIdx = idx
    }

    fun playPrevious() {
        Log.d(TAG, "playPrevious")
        mSongIdx--

        // cyclic behavior
        if(mSongIdx < 0) mSongIdx = mSongsUrisList.size-1
        play()
    }

    fun playNext() {
        Log.d(TAG, "playNext")
        mSongIdx++

        // cyclic behavior
        if(mSongIdx >= mSongsUrisList.size) mSongIdx 0
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

    fun go() {
        Log.d(TAG, "go")
        mPlayer.start()
    }

    // Media player callbacks
    override fun onPrepared(mp: MediaPlayer?) {
        Log.d(TAG, "onPrepared: start playing song : ${mSongsUrisList[mSongIdx]}")
        if(mp != null) {
            mp.start()
        }else {
            Log.e(TAG, "onPrepared: player is null!")
        }
    }

    override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
        Log.e(TAG, "onError: mp = $mp, what = $what, extra = $extra, currSongIdx = ${mSongIdx}")
        return false
    }

    override fun onCompletion(mp: MediaPlayer?) {
        Log.d(TAG, "onCompletion: curSongIdx = $mSongIdx")
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
        fun getService() : MusicService {
            Log.d(TAG, "getService")
            return this@MusicService
        }
    }
}