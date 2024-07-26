package com.autel.sdk.debugtools.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import com.autel.drone.sdk.SDKConstants
import com.autel.drone.sdk.vmodelx.constants.SDKUtils
import com.autel.drone.sdk.vmodelx.manager.RtspServiceManager
import com.autel.module_player.player.AutelPlayerManager
import com.autel.module_player.player.autelplayer.AutelPlayer
import com.autel.module_player.player.autelplayer.AutelPlayerView
import com.autel.rtmp.publisher.IPublishListener

import com.autel.sdk.debugtools.R
import com.autel.sdk.debugtools.databinding.FragmentRtspclientBinding

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RtspClientFragment : AutelFragment()  {
    var TAG = "RtspClientFragment"
    var contentView: View? = null
    lateinit var edit_url: EditText
    lateinit var btn_start: Button

    var right_view: LinearLayout? = null
    private var mAutelPlayer: AutelPlayer? = null
    var codecView: AutelPlayerView? = null

    var left_view: LinearLayout? = null
    private var mAutelPlayer2: AutelPlayer? = null
    var codecView2: AutelPlayerView? = null

    var rtmpPort:Int = SDKConstants.STREAM_CHANNEL_16110;

    private lateinit var uiBinding: FragmentRtspclientBinding

    private var mPublishFlag: Boolean = false

    private var iCurrentPort:Int = SDKConstants.STREAM_CHANNEL_16110 //可见光
    private var rtmpUrl:String  ="rtsp://139.9.227.17:8554/live";

    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        uiBinding = FragmentRtspclientBinding.inflate(inflater, container, false)

        return uiBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        edit_url = uiBinding.root.findViewById(R.id.edit_url)
        btn_start = uiBinding.root.findViewById(R.id.btn_start)

        left_view = uiBinding.root.findViewById(R.id.layout_left_view)
        codecView = createAutelCodecView()
        with(left_view) { this?.addView(codecView) }

        mAutelPlayer = AutelPlayer(SDKConstants.STREAM_CHANNEL_16110)
        mAutelPlayer!!.addVideoView(codecView)

        AutelPlayerManager.getInstance().addAutelPlayer(mAutelPlayer);


        right_view = uiBinding.root.findViewById(R.id.layout_right_view)
        codecView2 = createAutelCodecView2()
        with(right_view) { this?.addView(codecView2) }


        mAutelPlayer2 = AutelPlayer(SDKConstants.STREAM_CHANNEL_16115)
        mAutelPlayer2!!.addVideoView(codecView2)

        AutelPlayerManager.getInstance().addAutelPlayer(mAutelPlayer2);

        mAutelPlayer!!.startPlayer()
        mAutelPlayer2!!.startPlayer()

        initView();

    }

    /**
     * create code view for autel media player 2
     */
    private fun createAutelCodecView2(): AutelPlayerView? {
        val codecView = AutelPlayerView(activity)
        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        codecView.layoutParams = params
        return codecView
    }

    private fun createAutelCodecView(): AutelPlayerView? {
        val codecView = AutelPlayerView(activity)
        val params = ConstraintLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        codecView.layoutParams = params
        return codecView
    }

    private fun initView() {

        RtspServiceManager.getInstance().initRtspConfig(rtmpUrl,16110)
        RtspServiceManager.getInstance().setRtspPublishListener(object:IPublishListener{
            override fun onConnecting() {

            }

            override fun onConnected() {

            }

            override fun onConnectedFailed(code: Int) {

            }

            override fun onStartPublish() {

            }

            override fun onStopPublish() {

            }

            override fun onFpsStatistic(fps: Int) {

            }

            override fun onRtmpDisconnect() {

            }

            override fun onVideoBriate(value: Int) {

            }

            override fun onAudioBriate(value: Int) {

            }

            override fun onPublishSuccess() {

            }

            override fun onPublishFailed(errorCode: Int) {

            }
        })

            edit_url.setText(rtmpUrl)
            btn_start.isEnabled = true



        btn_start.setOnClickListener {

            if (mPublishFlag) {
                mPublishFlag = false;
                stopPublish()
            } else {
                mPublishFlag = true
                startPublish()
            }
        }

    }

    private fun stopPublish() {
        coroutineScope.launch {
            RtspServiceManager.getInstance().stopPublishStream();
        }

        btn_start.text = getString(R.string.debug_start)
    }

    private fun startPublish() {
        coroutineScope.launch {
           RtspServiceManager.getInstance().startPublishStream();
        }
        btn_start.text = getString(R.string.debug_stop)
    }

    override fun onDestroy() {
        super.onDestroy()

        AutelPlayerManager.getInstance().endStreamChannel(SDKConstants.STREAM_CHANNEL_16110)
        if (mAutelPlayer != null) {
            mAutelPlayer?.removeVideoView()
            mAutelPlayer?.releasePlayer()
        }

        AutelPlayerManager.getInstance().endStreamChannel(SDKConstants.STREAM_CHANNEL_16115)
        if (mAutelPlayer2 != null) {
            mAutelPlayer2?.removeVideoView()
            mAutelPlayer2?.releasePlayer()
        }
    }
}