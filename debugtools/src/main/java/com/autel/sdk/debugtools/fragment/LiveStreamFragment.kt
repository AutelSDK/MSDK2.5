package com.autel.sdk.debugtools.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.autel.drone.sdk.SDKConstants
import com.autel.drone.sdk.vmodelx.utils.ToastUtils
import com.autel.player.codec.OnRenderFrameInfoListener
import com.autel.player.player.AutelPlayerManager
import com.autel.player.player.autelplayer.AutelPlayer
import com.autel.player.player.autelplayer.AutelPlayerView
import com.autel.publisher.IPublishListener
import com.autel.publisher.PublishErrorCode
import com.autel.publisher.PublishParam
import com.autel.publisher.rtmp.RTMPPublisherNew
import com.autel.sdk.debugtools.R
import com.autel.sdk.debugtools.databinding.FragmentLivestreamBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.webrtc.VideoFrame

/**
 * live streaming of video on autel media player(custom exoplayer)
 * Copyright: Autel Robotics
 * @author huangsihua on 2022/12/17.
 */

class LiveStreamFragment : AutelFragment() , OnRenderFrameInfoListener {
    var TAG = "LiveStreamFragment"
    var contentView: View? = null
    lateinit var edit_url: EditText
    lateinit var btn_start: Button
    lateinit var btn_switch:Button
    lateinit var btn_refersh:Button

    var right_view: LinearLayout? = null
    private var mAutelPlayer: AutelPlayer? = null
    var codecView: AutelPlayerView? = null

    var left_view: LinearLayout? = null
    private var mAutelPlayer2: AutelPlayer? = null
    var codecView2: AutelPlayerView? = null

    var rtmpPort:Int = SDKConstants.STREAM_CHANNEL_16110;

    private lateinit var uiBinding: FragmentLivestreamBinding

    private var mPublishFlag: Boolean = false

    private var iCurrentPort:Int = SDKConstants.STREAM_CHANNEL_16110 //可见光
    //"rtmp://183.6.112.146:17072/live/YD202220530_flight"; // 南网外网环境
    // "rtmp://183.6.112.146:1935/live/NEST202203038_flight_zoom" // 南网内网推流地址
    //"rtmp://116.205.231.28/live/livestream/zoom77" // 公司内网推流地址
    private var rtmpUrl:String  = "rtmp://hwtest-mediacenter.autelrobotics.cn:1936/live/mediacenter" //"rtmp://a.rtmp.youtube.com/live2/5hh6-xas1-btk7-2cmc-2rz9"

    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private var connectStatus = -1;

    private var mPublisher = RTMPPublisherNew();

    private val handler = Handler(Looper.getMainLooper()) {
        getVideoFps()
        true
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        uiBinding = FragmentLivestreamBinding.inflate(inflater, container, false)
        return uiBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        edit_url = uiBinding.root.findViewById(R.id.edit_url)
        btn_start = uiBinding.root.findViewById(R.id.btn_start)
        btn_switch= uiBinding.root.findViewById(R.id.btn_switch)
        btn_refersh = uiBinding.root.findViewById(R.id.btn_refersh)

        left_view = uiBinding.root.findViewById(R.id.layout_left_view)
        codecView = createAutelCodecView()
        with(left_view) { this?.addView(codecView) }

        mAutelPlayer = AutelPlayer(SDKConstants.STREAM_CHANNEL_16110)
        mAutelPlayer!!.addVideoView(codecView)

        right_view = uiBinding.root.findViewById(R.id.layout_right_view)
        codecView2 = createAutelCodecView2()
        with(right_view) { this?.addView(codecView2) }

        mAutelPlayer2 = AutelPlayer(SDKConstants.STREAM_CHANNEL_16115)
        mAutelPlayer2!!.addVideoView(codecView2)

        //mAutelPlayer!!.startPlayer()
        //mAutelPlayer2!!.startPlayer()

        //AutelPlayerManager.getInstance().addCodecListeners(TAG, SDKConstants.STREAM_CHANNEL_16110, this)

        initListener()
        initView()
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

        edit_url.setText(rtmpUrl)

        btn_start.setOnClickListener {
                if (mPublishFlag) {
                    mPublishFlag = false;
                    stopPublish()
                } else {
                    mPublishFlag = true
                    startPublish()
                }
        }

        btn_switch.setOnClickListener{
            coroutineScope.launch {
                if(iCurrentPort == SDKConstants.STREAM_CHANNEL_16110){
                    if (mPublishFlag) {
                        ToastUtils.showToast("开始推流红外摄像头画面")
                    } else {
                        ToastUtils.showToast(" 切换到红外摄像头端口，还没推流")
                    }
                    iCurrentPort = SDKConstants.STREAM_CHANNEL_16115
                    mPublisher.switchStreamSource(iCurrentPort, true)
                } else {
                    if (mPublishFlag) {
                        ToastUtils.showToast("开始推流广角摄像头画面")
                    } else {
                        ToastUtils.showToast(" 切换到广角摄像头端口，还没推流")
                    }
                    iCurrentPort = SDKConstants.STREAM_CHANNEL_16110
                    mPublisher.switchStreamSource(iCurrentPort, true)
                }
            }
        }

        btn_refersh.setOnClickListener{
            if(!mPublishFlag)
                return@setOnClickListener
            coroutineScope.launch {

            }
        }

        coroutineScope.launch {
            var param = PublishParam.Builder().setUrl(rtmpUrl).setStreamSource(iCurrentPort).build()
            mPublisher.configure(param)
        }

        handler.sendEmptyMessageDelayed(1, 1000)
    }

    private fun initListener() {
        mPublisher.setOnPublishListener(object :
            IPublishListener{
            override fun onConnecting() {
                Log.i(TAG, "onConnecting");
                ToastUtils.showToast("正在连接服务器...")
            }

            override fun onConnected() {
                Log.i(TAG, "onConnected");
                ToastUtils.showToast("成功连接服务器！！！")
            }

            override fun onConnectedFailed(code: PublishErrorCode) {
                Log.e(TAG, "onConnectedFailed:$code");
                ToastUtils.showToast("连接服务器失败！！！")
            }

            override fun onStartPublish() {
                Log.i(TAG, "onStartPublish");
                if (iCurrentPort == SDKConstants.STREAM_CHANNEL_16110) {
                    ToastUtils.showToast("开始推流广角摄像头画面")
                } else if (iCurrentPort == SDKConstants.STREAM_CHANNEL_16115) {
                    ToastUtils.showToast("开始推流红外摄像头画面")
                }
            }

            override fun onStopPublish() {
                Log.i(TAG, "onStopPublish");
                ToastUtils.showToast("停止推流")
            }

            @SuppressLint("SetTextI18n")
            override fun onFpsStatistic(fps: Int, channelName: String) {
                Log.d(TAG, "onFpsStatistic$fps");
                lifecycleScope.launch(Dispatchers.Main) {
                    if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                        if (iCurrentPort == SDKConstants.STREAM_CHANNEL_16110) {
                            uiBinding.leftPushFps.text = "推流帧率:" + fps + "fps"
                            uiBinding.rightPushFps.text = "推流帧率:" + 0 + "fps"
                        } else if (iCurrentPort == SDKConstants.STREAM_CHANNEL_16115) {
                            uiBinding.leftPushFps.text = "推流帧率:" + 0 + "fps"
                            uiBinding.rightPushFps.text = "推流帧率:" + fps + "fps"
                        }
                    }
                }
            }

            override fun onVideoBitrate(value: Int, channelName: String) {
                Log.d(TAG, "onVideoBitrate:$value");
            }

            override fun onAudioBitrate(value: Int) {
                Log.d(TAG, "onAudioBitrate:$value");
            }

            override fun onPublishSuccess() {
                Log.i(TAG, "onPublishSuccess:");
            }

            override fun onPublishFailed(errorCode: PublishErrorCode) {
                Log.e(TAG, "onPublishFailed:$errorCode");
                ToastUtils.showToast("推流失败")
            }

            override fun onPublishFailed(channelName: String, errorCode: PublishErrorCode) {
            }

            override fun onReconnect() {
                Log.i(TAG, "onReconnect");
                ToastUtils.showToast("正在重连...")
            }

        });
    }

    private fun stopPublish() {
        mPublishFlag = false;
        coroutineScope.launch {
            mPublisher.stop()
        }

        lifecycleScope.launch(Dispatchers.Main) {
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                btn_start.text = getString(R.string.debug_start)
                uiBinding.leftPushFps.text = "推流帧率:" + 0 + "fps"
                uiBinding.rightPushFps.text = "推流帧率:" + 0 + "fps"
            }
        }
    }

    private fun startPublish() {
        coroutineScope.launch {
            mPublisher.start();
        }

        lifecycleScope.launch(Dispatchers.Main) {
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                btn_start.text = getString(R.string.debug_stop)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.launch {
            mPublisher.release()
        }

        handler.removeCallbacksAndMessages(null)


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

    override fun onRenderFrameTimestamp(pts: Long) {

    }

    override fun onRenderFrameSizeChanged(width: Int, height: Int) {

    }

    override fun onFrameStream(videoBuffer: ByteArray?, isIFrame: Boolean, size: Int, pts: Long, videoType: Int) {
       //todo:264码流回调到此处
    }

    override fun onFrameStream(frame: VideoFrame?, mAutelPlayerID: Int) {
        Log.d(TAG, "onFrameStream: AutelPlayerID: $mAutelPlayerID")
    }

    @SuppressLint("SetTextI18n")
    private fun getVideoFps() {
        lifecycleScope.launch(Dispatchers.Main) {
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                uiBinding.leftPlayFps.text = "播放帧率:" + mAutelPlayer?.videoFps + "fps"
                uiBinding.rightPlayFps.text = "播放帧率:" + mAutelPlayer?.videoFps + "fps"
            }
        }
        handler.sendEmptyMessageDelayed(1, 1000)
    }

}