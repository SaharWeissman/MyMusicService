package com.saharw.mymusicservice

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.MediaController
import android.widget.Toast
import com.saharw.mymusicservice.entities.MusicController

class MainActivity : AppCompatActivity(), MediaController.MediaPlayerControl{

    private val TAG = "MainActivity"
    private lateinit var mController : MusicController
    private lateinit var mMusicSrvc : Music

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupMediaController()
    }

    override fun onStart() {
        Log.d(TAG, "onStart")
        super.onStart()
        if()
    }

    override fun isPlaying(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun canSeekForward(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getDuration(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun pause() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getBufferPercentage(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun seekTo(pos: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getCurrentPosition(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun canSeekBackward(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun start() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getAudioSessionId(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun canPause(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun setupMediaController() {
        Log.d(TAG, "setupMediaController")
        mController = MusicController(this@MainActivity)
        mController.setPrevNextListeners(

                // next
                {
                    Log.d(TAG, "onNext clicked!")
                    Toast.makeText(this@MainActivity, "Next pressed!", Toast.LENGTH_SHORT).show()
                },

                // prev
                {
                    Log.d(TAG, "onPrec clicked!")
                    Toast.makeText(this@MainActivity, "Prev pressed!", Toast.LENGTH_SHORT).show()
                })
        mController.setMediaPlayer(this@MainActivity)
        mController.setAnchorView(findViewById(R.id.main_view))
        mController.isEnabled = true
    }
}
