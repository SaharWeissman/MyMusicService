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
import java.lang.ref.WeakReference

class MusicService : Service(),
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener {

    private val TAG = "MusicService"
    lateinit var mPlayer : MediaPlayer
    var mSongOffset : Int = 0
    val mBinder = MusicBinder()
    lateinit var mCurrSongUri : WeakReference<Uri>

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
    fun play(songUri: Uri) {
        Log.d(TAG, "play: songUri = $songUri")
        mPlayer.reset()
        try{
            mPlayer.setDataSource(applicationContext, songUri)
            mPlayer.prepareAsync()
            mCurrSongUri = WeakReference(songUri)
        }catch (t: Throwable){
            Log.e(TAG, "play: error setting data source for uri: $songUri", t)
        }
    }

    // Media player callbacks
    override fun onPrepared(mp: MediaPlayer?) {
        Log.d(TAG, "onPrepared: start playing uri: ${mCurrSongUri.get()}")
        if(mp != null) {
            mp.start()
        }else {
            Log.e(TAG, "onPrepared: player is null!")
        }
    }

    override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
        Log.e(TAG, "onError: mp = $mp, what = $what, extra = $extra, currSong = ${mCurrSongUri.get()}")
        return false
    }

    override fun onCompletion(mp: MediaPlayer?) {
        Log.d(TAG, "onCompletion: curSong = $mCurrSongUri")
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
            return this@MusicService
        }
    }
}