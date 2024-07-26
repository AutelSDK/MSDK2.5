package com.autel.sdk.debugtools.fragment

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.autel.drone.sdk.SDKConstants
import com.autel.drone.sdk.log.SDKLog
import com.autel.drone.sdk.vmodelx.constants.SDKUtils
import com.autel.drone.sdk.vmodelx.manager.RtspServerManager
import com.autel.module_player.player.AutelPlayerManager
import com.autel.module_player.player.autelplayer.AutelPlayer
import com.autel.module_player.player.autelplayer.AutelPlayerView
import com.autel.rtspserver.IRtspServerCallBack
import com.autel.sdk.debugtools.R
import com.autel.sdk.debugtools.WiFiUtils
import com.autel.sdk.debugtools.databinding.FragmentRtspserverBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RtspServerFragment : AutelFragment()  {
    var TAG = "RtspServerFragment"
    var contentView: View? = null
    lateinit var edit_url: TextView
    lateinit var btn_start: Button

    var right_view: LinearLayout? = null
    private var mAutelPlayer: AutelPlayer? = null
    var codecView: AutelPlayerView? = null

    var left_view: LinearLayout? = null
    private var mAutelPlayer2: AutelPlayer? = null
    var codecView2: AutelPlayerView? = null

    var rtmpPort:Int = SDKConstants.STREAM_CHANNEL_16110;

    private lateinit var uiBinding: FragmentRtspserverBinding

    private var mPublishFlag: Boolean = false

    private var strIP:String = "0.0.0.0"
    private var iPort = 1024

    private var iCurrentPort:Int = SDKConstants.STREAM_CHANNEL_16110 //可见光
    //"rtmp://183.6.112.146:17072/live/YD202220530_flight"; // 南网外网环境
    // "rtmp://183.6.112.146:1935/live/NEST202203038_flight_zoom" // 南网内网推流地址
    //"rtmp://116.205.231.28/live/livestream/zoom77" // 公司内网推流地址
    private var rtmpUrl:String  ="rtmp://183.6.112.146:17072/live/YD202220530_flight";

    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        uiBinding = FragmentRtspserverBinding.inflate(inflater, container, false)

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

        strIP = WiFiUtils.getIPAddress().toString()
        if (TextUtils.equals(strIP, "0.0.0.0")) {
            strIP = WiFiUtils.getWiFiWlan0IPAddress()
        }

        if(strIP == null || strIP.equals("0.0.0.0")) {
            edit_url.setText("获取ip地址无效，请检查网络")
            btn_start.isEnabled = false
        }
        else {
            edit_url.setText("rtsp://" + strIP + ":" + iPort + "/live")
            btn_start.isEnabled = true;
        }

        btn_start.setOnClickListener {

            if (mPublishFlag) {
                mPublishFlag = false;
                stopPublish()
            } else {
                mPublishFlag = true
                startPublish()
            }
        }


        RtspServerManager.getInstance().setRtspServerCallback(object :IRtspServerCallBack{
            override fun onStartPush(streamid: Int) {
                SDKLog.d("RtspServer"," onStartPush...")
            }

            override fun onStopPush(streamid: Int) {
                SDKLog.d("RtspServer"," onStopPush...")
            }

            override fun onKeyFrameRequest(streamlid: Int) {
                SDKLog.d("RtspServer"," onKeyFrameRequest...")
            }

            override fun onError(errorType: Int, detail: String?) {
                SDKLog.d("RtspServer"," onError..."+ detail)
            }

            override fun onLogPrint(level: Int, detail: String?) {
                SDKLog.d("RtspServer"," onLogPrint..."+ detail)
            }

            override fun onRtspServerStart() {
                SDKLog.d("RtspServer"," onRtspServerStart...")
            }

            override fun onRtspServerStartSuccess() {
                SDKLog.d("RtspServer"," onRtspServerStartSuccess...")
            }

            override fun onRtspServerStartFailed(code: Int) {
                SDKLog.d("RtspServer"," onRtspServerStartFailed...")
            }

            override fun onStartPushStream() {
                SDKLog.d("RtspServer"," onStartPushStream...")
            }

            override fun onStopPushStream() {
                SDKLog.d("RtspServer"," onStopPushStream...")
            }

            override fun onRtspFpsStatistic(fps: Int) {
                SDKLog.d("RtspServer"," onRtspFpsStatistic value:"+ fps)
            }

            override fun onRtspVideoBriate(value: Int) {
                SDKLog.d("RtspServer"," onRtspVideoBriate value:"+ value)
            }

            override fun onRtspAudioBriate(value: Int) {
                SDKLog.d("RtspServer"," onRtspAudioBriate ")
            }

            override fun onRtspPublishSuccess() {
                SDKLog.d("RtspServer"," onRtspPublishSuccess ")
            }

            override fun onRtspPublishFailed(errorCode: Int) {
                SDKLog.d("RtspServer"," onRtspPublishFailed ")
            }

        })

    }

    private fun stopPublish() {
        coroutineScope.launch {
            RtspServerManager.getInstance().stopRtspServer()
        }

        btn_start.text = getString(R.string.debug_start)
    }

    private fun startPublish() {
        coroutineScope.launch {
            RtspServerManager.getInstance().startRtspServerByPort(strIP,iPort,SDKConstants.STREAM_CHANNEL_16110)
        }
        btn_start.text = getString(R.string.debug_stop)
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.launch {
            RtspServerManager.getInstance().stopRtspServer()
        }


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