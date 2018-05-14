package com.saharw.mymusicservice

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.MediaController
import android.widget.TextView
import com.saharw.musicservice.MusicService
import com.saharw.mymusicplayer.entities.data.base.ItemType
import com.saharw.mymusicplayer.entities.data.base.MediaItem
import com.saharw.mymusicservice.entities.MusicController
import com.saharw.mymusicservice.utils.FileUtil
import com.tbruyelle.rxpermissions2.RxPermissions

class MainActivity : AppCompatActivity(), MediaController.MediaPlayerControl, View.OnClickListener {

    private val TAG = "MainActivity"
    private lateinit var mController : MusicController
    private var mMusicSrvc : MusicService? = null
    private var mMusicServiceBounded = false
    private var mPlayIntent : Intent? = null
    private var mIsPaused = false
    private var mPlaybackPaused = false
    private lateinit var mChooseFileBtn: Button
    private lateinit var mSongUriEdTxt: TextView
    private val REQ_CODE_CHOOSE_FILE: Int = 1
    private var mSongList: List<MediaItem> = emptyList()

    // service connection
    private var mMusicServiceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(p0: ComponentName?) {
            Log.d(TAG, "onServiceDisconnected")
            mMusicServiceBounded = false
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d(TAG, "onServiceConnected")
            var binder = service as MusicService.MusicBinder
            mMusicSrvc = binder.getService(this@MainActivity)
            mMusicServiceBounded = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initUIComponents()
        setupMediaController()
    }

    private fun initUIComponents() {
        Log.d(TAG, "initUIComponents")
        mChooseFileBtn = findViewById(R.id.btn_choose_file)
        mChooseFileBtn.setOnClickListener(this)
        mSongUriEdTxt = findViewById(R.id.txtV_file_uri)
    }

    override fun onStart() {
        Log.d(TAG, "onStart")
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            requestRuntimePermissions()
        }
        super.onStart()
        if(mPlayIntent == null){
            mPlayIntent = Intent(this@MainActivity, MusicService::class.java)
            bindService(mPlayIntent, mMusicServiceConnection, Context.BIND_AUTO_CREATE)
            startService(mPlayIntent)
        }
    }

    override fun onPause() {
        Log.d(TAG, "onPause")
        super.onPause()
        mIsPaused = true
    }

    override fun onResume() {
        Log.d(TAG, "onResume")
        super.onResume()
        if(mIsPaused){
            mIsPaused = false
            setupMediaController()
        }
    }

    override fun onStop() {
        Log.d(TAG, "onStop")
        mController.hide()
        super.onStop()
    }

    override fun isPlaying(): Boolean {
        if(mMusicSrvc != null && mMusicServiceBounded){
            return mMusicSrvc!!.isPlaying()
        }else {
            return false
        }
    }

    override fun canSeekForward(): Boolean {
        Log.d(TAG, "canSeekForward")
        return true
    }

    override fun getDuration(): Int {
        Log.d(TAG, "getDuration")
        if(mMusicSrvc != null && mMusicServiceBounded && mMusicSrvc!!.isPlaying()){
            return mMusicSrvc!!.getSongDuration()
        }else {
            if(mSongList.isNotEmpty()){
                return mSongList[0].duration.toInt()
            }else {
                Log.e(TAG, "getDuration: either service is null or not playing or activity is not bounded to service - returning 0")
                return 0
            }
        }
    }

    override fun pause() {
        Log.d(TAG, "pause")
        mPlaybackPaused = true
        if(mMusicSrvc != null && mMusicServiceBounded){
            mMusicSrvc!!.pausePlayer()
        }
    }

    override fun getBufferPercentage(): Int {
        Log.d(TAG, "getBufferPercentage")
        return 0
    }

    override fun seekTo(pos: Int) {
        Log.d(TAG, "seekTo: pos = $pos")
        if(mMusicSrvc != null && mMusicServiceBounded){
            mMusicSrvc!!.seek(pos)
        }
    }

    override fun getCurrentPosition(): Int {
        Log.d(TAG, "getCurrentPosition")
        if(mMusicSrvc != null && mMusicServiceBounded && mMusicSrvc!!.isPlaying()){
            return mMusicSrvc!!.getSongPosition()
        }else {
            return 0
        }
    }

    override fun canSeekBackward(): Boolean {
        Log.d(TAG, "canSeekBackward")
        return true
    }

    override fun start() {
        Log.d(TAG, "start")
        if(mMusicSrvc != null && mMusicServiceBounded){
            mMusicSrvc!!.setSongsUris(mSongList)
            mMusicSrvc!!.start()
        }
    }

    override fun getAudioSessionId(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun canPause(): Boolean {
        Log.d(TAG, "canPause")
        return true
    }

    private fun setupMediaController() {
        Log.d(TAG, "setupMediaController")
        mController = MusicController(this@MainActivity)
        mController.setPrevNextListeners(

                // next
                {
                    Log.d(TAG, "onNext clicked!")
                    mMusicSrvc!!.playNext()
                    if(mPlaybackPaused){
                        setupMediaController()
                        mPlaybackPaused = false
                    }
                    mController.show(0)
                },

                // prev
                {
                    Log.d(TAG, "onPrec clicked!")
                    mMusicSrvc!!.playPrevious()
                    if(mPlaybackPaused){
                        setupMediaController()
                        mPlaybackPaused = false
                    }
                    mController.show(0)
                })
        mController.setMediaPlayer(this@MainActivity)
        mController.setAnchorView(findViewById(R.id.main_view))
        mController.isEnabled = true
    }

    override fun onClick(p0: View?) {
        Log.d(TAG, "onClick")
        when(p0?.id){
            R.id.btn_choose_file -> {
                Log.d(TAG, "onClick: case \"R.id.btn_choose_file\"")
                var chooseFileIntent = Intent(Intent.ACTION_GET_CONTENT)
                chooseFileIntent.type = "audio/*"
                startActivityForResult(chooseFileIntent, REQ_CODE_CHOOSE_FILE)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.d(TAG, "onActivityResult: requsetCode: $requestCode, resultCode: $resultCode")
        super.onActivityResult(requestCode, resultCode, data)
        when(requestCode){
            REQ_CODE_CHOOSE_FILE -> {
                Log.d(TAG, "onActivityResult: case \"$REQ_CODE_CHOOSE_FILE\"")
                if(data != null) {
                    var dataUri = data.data
                    handleSongPicked(dataUri)
                }else {
                    Log.e(TAG, "onActivityResult: case \"$REQ_CODE_CHOOSE_FILE\": data is null!")
                }
            }
        }
    }

    private fun requestRuntimePermissions() {
        Log.d(TAG, "requestRuntimePermissions")
        var rxPermissions = RxPermissions(this@MainActivity)
        rxPermissions.request(Manifest.permission.READ_EXTERNAL_STORAGE)
                .subscribe { granted ->
                    if(granted){
                        Log.d(TAG, "requestRuntimePermissions: permission granted")
                    }else {
                        Log.e(TAG, "requestRuntimePermissions: permission NOT granted")
                    }
                }
    }

    private fun handleSongPicked(songUri: Uri){
        Log.d(TAG, "handleSongPicked: songUri = $songUri")

        val dataPath = FileUtil.getPath(this@MainActivity, songUri)!!
        mSongUriEdTxt.text = dataPath

        var mediaItem = MediaItem(
                0,
                0,
                "Sahar W.",
                dataPath,
                "Song1",
                ItemType.FileMusic,
                1234L,
                1024 * 5L)
        mSongList = listOf(mediaItem)
        mController.show()
    }
}
