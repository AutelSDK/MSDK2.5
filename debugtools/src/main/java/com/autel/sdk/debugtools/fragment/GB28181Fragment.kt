package com.autel.sdk.debugtools.fragment

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.autel.drone.sdk.SDKConstants
import com.autel.drone.sdk.vmodelx.utils.ToastUtils
import com.autel.player.player.AutelPlayerManager
import com.autel.player.player.autelplayer.AutelPlayer
import com.autel.player.player.autelplayer.AutelPlayerView
import com.autel.publisher.IPublishListener
import com.autel.publisher.PublishErrorCode
import com.autel.publisher.PublishParam
import com.autel.publisher.gb28181.ConfigInfo
import com.autel.publisher.gb28181.GB28181PublisherNew
import com.autel.publisher.gb28181.GBChannel
import com.autel.sdk.debugtools.R
import com.autel.sdk.debugtools.WiFiUtils
import com.autel.sdk.debugtools.databinding.FragmentGb28181PublisherBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.TimeZone
import kotlin.collections.HashMap
import kotlin.collections.hashMapOf
import kotlin.collections.set


class GB28181Fragment : AutelFragment()  {
    var TAG = "GB28181Fragment"
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

    private lateinit var uiBinding: FragmentGb28181PublisherBinding

    private var mPublishFlag: Boolean = false

    private var iCurrentPort:Int = SDKConstants.STREAM_CHANNEL_16110 //可见光

    private var mPublisher = GB28181PublisherNew();

    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        uiBinding = FragmentGb28181PublisherBinding.inflate(inflater, container, false)

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

        initView();
        initListener()
        initData()

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

        uiBinding.btnSwitch.setOnClickListener {
            coroutineScope.launch {
                if(iCurrentPort == SDKConstants.STREAM_CHANNEL_16110){
                    ToastUtils.showToast(" 切换到红外摄像头端口")

                    iCurrentPort = SDKConstants.STREAM_CHANNEL_16115
                    mPublisher.switchStreamSource(iCurrentPort, true)
                } else {
                    ToastUtils.showToast(" 切换到广角摄像头端口")

                    iCurrentPort = SDKConstants.STREAM_CHANNEL_16110
                    mPublisher.switchStreamSource(iCurrentPort, true)
                }
            }
        }

        //CameraKey.KeyCameraTransferPayLoadType.create().set(VideoCompressStandardEnum.H264)
        var strIP = WiFiUtils.getIPAddress()
        var url = ConfigInfo.Builder().setDeviceName("GB28181Test")
            .setDeviceModel("ModelX")
            .setDeviceId("34010000001311152900")
            .setServerIp("124.71.57.212")
            .setServerId("41010500002000000001")
            .setServerPort(8116)
            .setLocalPort(5065).setLocalIp(strIP)
            .setUserName("34010000001311152900")
            .setPasswd("Autel123").setChannels(hashMapOf(
                "44010200492000000001" to GBChannel("Zoom", "44010200492000000001", SDKConstants.STREAM_CHANNEL_16110),
                "44010200492000000002" to GBChannel("infrared", "44010200492000000002", SDKConstants.STREAM_CHANNEL_16115)
            )).build().toString();

        var param = PublishParam.Builder().setUrl(url).setTimeout(10).build()
        Log.d(TAG, "initView: " + url)
        if (mPublisher.configure(param) != 0) {
            ToastUtils.showToast("配置失败")
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun initData() {
        val devicePosition = HashMap<String, String>()
        devicePosition["Longitude"] = "123.12"
        devicePosition["Latitude"] = "123.12"

        val calendar = Calendar.getInstance()
        val timeZone = TimeZone.getTimeZone("UTC")
        calendar.timeZone = timeZone
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
        dateFormat.timeZone = timeZone
        devicePosition["Time"] = dateFormat.format(calendar.time)

        mPublisher.setDevicePosition(devicePosition)
    }

    private fun initListener() {
        mPublisher.setOnPublishListener(object :
            IPublishListener {
            override fun onConnecting() {
                Log.i(TAG, "onConnecting");
                ToastUtils.showToast("正在注册设备...")
            }

            override fun onConnected() {
                Log.i(TAG, "onConnected");
                ToastUtils.showToast("成功注册到服务器！！！")
            }

            override fun onConnectedFailed(code: PublishErrorCode) {
                Log.e(TAG, "onConnectedFailed:$code");
                ToastUtils.showToast("注册设备失败！！！")
            }

            override fun onStartPublish() {
                Log.i(TAG, "onStartPublish");
                ToastUtils.showToast("开始推流")
            }

            override fun onStopPublish() {
                Log.i(TAG, "onStopPublish");
                ToastUtils.showToast("停止推流")
            }

            override fun onFpsStatistic(fps: Int, channelName: String) {
                Log.d(TAG, "onFpsStatistic: $channelName")
            }

            override fun onVideoBitrate(value: Int, channelName: String) {
            }

            override fun onAudioBitrate(value: Int) {
            }

            override fun onPublishSuccess() {
                Log.i(TAG, "onPublishSuccess");
                ToastUtils.showToast("推流成功")
            }

            override fun onPublishFailed(errorCode: PublishErrorCode) {
                Log.i(TAG, "onPublishFailed");
                ToastUtils.showToast("推流失败")
            }

            override fun onPublishFailed(channelName: String, errorCode: PublishErrorCode) {
                Log.d(TAG, "onPublishFailed: $errorCode $channelName")
                ToastUtils.showToast("推流失败:$channelName")
            }

            override fun onReconnect() {
                Log.i(TAG, "onReconnect");
                ToastUtils.showToast("正在重连...")
            }
        })
    }

    private fun stopPublish() {
        coroutineScope.launch {
            mPublisher.stop()
        }

        lifecycleScope.launch(Dispatchers.Main) {
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                btn_start.text = getString(R.string.debug_start)
            }
        }
    }

    private fun startPublish() {
        coroutineScope.launch {
            mPublisher.start()
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