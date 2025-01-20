package com.autel.sdk.debugtools.fragment

import android.os.Bundle
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
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.CameraKey
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.ext.create
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.ext.set
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.camera.enums.VideoCompressStandardEnum
import com.autel.drone.sdk.vmodelx.utils.ToastUtils
import com.autel.player.player.AutelPlayerManager
import com.autel.player.player.autelplayer.AutelPlayer
import com.autel.player.player.autelplayer.AutelPlayerView
import com.autel.publisher.BasePublisher
import com.autel.publisher.IPublishListener
import com.autel.publisher.PublishErrorCode
import com.autel.publisher.PublishParam
import com.autel.publisher.rtsp.RTSPPublisher

import com.autel.sdk.debugtools.R
import com.autel.sdk.debugtools.databinding.FragmentRtspclientBinding

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RtspClientFragment : AutelFragment()  {
    private var TAG = "RtspClientFragment"
    private var mAutelPlayer: AutelPlayer? = null
    private var codecView: AutelPlayerView? = null

    private var mAutelPlayer2: AutelPlayer? = null
    private var codecView2: AutelPlayerView? = null

    private lateinit var uiBinding: FragmentRtspclientBinding

    private var mPublishFlag: Boolean = false

    private var iCurrentPort:Int = SDKConstants.STREAM_CHANNEL_16110 //可见光
    private var url:String  ="rtsp://hwtest-mediacenter.autelrobotics.cn/live/test";

    private var url2:String  ="rtsp://hwtest-mediacenter.autelrobotics.cn/live/test2";

    private val leftPublisher = "广角"
    private val rightPublisher = "红外"

    private var mPublisher = RTSPPublisher();

    private var mPublisher2 = RTSPPublisher();

    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        uiBinding = FragmentRtspclientBinding.inflate(inflater, container, false)
        return uiBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        codecView = createAutelCodecView()
        with(uiBinding.layoutLeftView) { this.addView(codecView) }
        mAutelPlayer = AutelPlayer(SDKConstants.STREAM_CHANNEL_16110)
        mAutelPlayer!!.addVideoView(codecView)
        AutelPlayerManager.getInstance().addAutelPlayer(mAutelPlayer);

        codecView2 = createAutelCodecView2()
        with(uiBinding.layoutRightView) { this.addView(codecView2) }
        mAutelPlayer2 = AutelPlayer(SDKConstants.STREAM_CHANNEL_16115)
        mAutelPlayer2!!.addVideoView(codecView2)
        AutelPlayerManager.getInstance().addAutelPlayer(mAutelPlayer2);

        mAutelPlayer!!.startPlayer()
        mAutelPlayer2!!.startPlayer()

        initView()
        initListener()
    }

    /**
     * create code view for autel media player 2
     */
    private fun createAutelCodecView2(): AutelPlayerView {
        val codecView = AutelPlayerView(activity)
        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        codecView.layoutParams = params
        return codecView
    }

    private fun createAutelCodecView(): AutelPlayerView {
        val codecView = AutelPlayerView(activity)
        val params = ConstraintLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        codecView.layoutParams = params
        return codecView
    }

    private fun initView() {
        uiBinding.editUrl.setText(url)
        uiBinding.btnStart.isEnabled = true
        uiBinding.btnStart.setOnClickListener {
            if (mPublishFlag) {
                mPublishFlag = false;
                stopPublish()
            } else {
                mPublishFlag = true
                startPublish()
            }
        }
    }

    private fun initListener() {
        mPublisher.setOnPublishListener(object :
            IPublishListener{
            override fun onConnecting() {
                Log.i(TAG, "onConnecting");
                ToastUtils.showToast(leftPublisher + "正在连接服务器...")
            }

            override fun onConnected() {
                Log.i(TAG, "onConnected");
                ToastUtils.showToast(leftPublisher + "成功连接服务器！！！")
            }

            override fun onConnectedFailed(code: PublishErrorCode) {
                Log.e(TAG, "onConnectedFailed:$code");
                ToastUtils.showToast(leftPublisher + "连接服务器失败！！！")
            }

            override fun onStartPublish() {
                Log.i(TAG, "onStartPublish");
                ToastUtils.showToast(leftPublisher + "开始推流")
            }

            override fun onStopPublish() {
                Log.i(TAG, "onStopPublish");
                ToastUtils.showToast(leftPublisher + "停止推流")
            }

            override fun onFpsStatistic(fps: Int, channelName: String) {
                Log.i(TAG, "onFpsStatistic");
            }

            override fun onVideoBitrate(value: Int, channelName: String) {
                Log.e(TAG, "onVideoBitrate");
            }

            override fun onAudioBitrate(value: Int) {
                Log.e(TAG, "onAudioBitrate");
            }

            override fun onPublishSuccess() {
                Log.i(TAG, "onPublishSuccess");
            }

            override fun onPublishFailed(errorCode: PublishErrorCode) {
                Log.e(TAG, "onPublishFailed:$errorCode");
                ToastUtils.showToast(leftPublisher + "推流失败")
            }

            override fun onPublishFailed(channelName: String, errorCode: PublishErrorCode) {
            }

            override fun onReconnect() {
                Log.i(TAG, "onReconnect");
                ToastUtils.showToast("正在重连...")
            }
        });

        mPublisher2.setOnPublishListener(object :
            IPublishListener{
            override fun onConnecting() {
                Log.i(TAG, "onConnecting");
                ToastUtils.showToast(rightPublisher + "正在连接服务器...")
            }

            override fun onConnected() {
                Log.i(TAG, "onConnected");
                ToastUtils.showToast(rightPublisher + "成功连接服务器！！！")
            }

            override fun onConnectedFailed(code: PublishErrorCode) {
                Log.e(TAG, "onConnectedFailed:$code");
                ToastUtils.showToast(rightPublisher + "连接服务器失败！！！")
            }

            override fun onStartPublish() {
                Log.i(TAG, "onStartPublish");
                ToastUtils.showToast(rightPublisher + "开始推流")
            }

            override fun onStopPublish() {
                Log.i(TAG, "onStopPublish");
                ToastUtils.showToast(rightPublisher + "停止推流")
            }

            override fun onFpsStatistic(fps: Int, channelName: String) {
                Log.i(TAG, "onFpsStatistic");
            }

            override fun onVideoBitrate(value: Int, channelName: String) {
                Log.e(TAG, "onVideoBitrate");
            }

            override fun onAudioBitrate(value: Int) {
                Log.e(TAG, "onAudioBitrate");
            }

            override fun onPublishSuccess() {
                Log.i(TAG, "onPublishSuccess");
            }

            override fun onPublishFailed(errorCode: PublishErrorCode) {
                Log.e(TAG, "onPublishFailed:$errorCode");
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
        coroutineScope.launch {
            mPublisher.stop()
            mPublisher2.stop()
        }

        lifecycleScope.launch(Dispatchers.Main) {
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                uiBinding.btnStart.text = getString(R.string.debug_start)
            }
        }
    }

    private fun startPublish() {
        coroutineScope.launch {
            val inputUrl = uiBinding.editUrl.text.toString().ifEmpty { url }
            val param = PublishParam.Builder().setUrl("$inputUrl/zoom").setStreamSource(iCurrentPort).setWidth(1920).setHeight(1440).setFps(30).build()
            mPublisher.configure(param)

            val param2 = PublishParam.Builder().setUrl("$inputUrl/ir").setStreamSource(SDKConstants.STREAM_CHANNEL_16115).setWidth(640).setHeight(512).setFps(30).build()
            mPublisher2.configure(param2)

            mPublisher.start()
            mPublisher2.start()
        }
        lifecycleScope.launch(Dispatchers.Main) {
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                uiBinding.btnStart.text = getString(R.string.debug_stop)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        coroutineScope.launch {
            mPublisher.release()
            mPublisher2.release()
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