package com.autel.sdk.debugtools.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.autel.module_player.player.AutelPlayerManager
import com.autel.module_player.player.autelplayer.AutelPlayer
import com.autel.module_player.player.autelplayer.AutelPlayerView

import com.autel.sdk.debugtools.R
import com.autel.sdk.debugtools.databinding.FragMuiltStreamBinding
import kotlinx.coroutines.*


class NetMeshMuiltCodecFragment : AutelFragment(){

    var left_view: LinearLayout? = null
    private var mAutelPlayer: AutelPlayer? = null
    var codecView: AutelPlayerView? = null


//    var right_view: LinearLayout? = null
//    private var mAutelPlayer2: AutelPlayer? = null
//    var codecView2: AutelPlayerView? = null

    private lateinit var uiBinding: FragMuiltStreamBinding
    val scope = CoroutineScope(Dispatchers.IO)
    private var autelPlayerManager: AutelPlayerManager? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        uiBinding = FragMuiltStreamBinding.inflate(inflater, container, false)

        //autelPlayerManager?.startStreamChannel(SDKConstants.STREAM_CHANNEL_15);

        return uiBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        left_view = uiBinding.root.findViewById(R.id.layout_left_view)
//        codecView = createAutelCodecView()
//        with(left_view) { this?.addView(codecView) }
//
//        mAutelPlayer = AutelPlayer(SDKConstants.STREAM_CHANNEL_16130)
//        mAutelPlayer!!.addSurfaceRenderView(codecView)
//
//        autelPlayerManager?.addAutelPlayer(mAutelPlayer);
//
//
////        right_view = uiBinding.root.findViewById(R.id.layout_right_view)
////        codecView2 = createAutelCodecView2()
////        with(right_view) { this?.addView(codecView2) }
////
////
////        mAutelPlayer2 = AutelPlayer(SDKConstants.STREAM_CHANNEL_15)
////        mAutelPlayer2!!.addVideoView(codecView2)
////
////        autelPlayerManager?.addAutelPlayer(mAutelPlayer2);
//
//        mAutelPlayer!!.startPlayer()
// //       mAutelPlayer2!!.startPlayer()

//        autelPlayerManager = DeviceManager.getDeviceManager().getFirstDroneDevice()?.getAutelPlayerManager()
//        if(autelPlayerManager?.initlize == false) {
//            autelPlayerManager?.init(activity, false, SDKConfig.isSingle());
//        }
//
//        mAutelPlayer = AutelPlayerManager.getInstance().getAutelPlayer(SDKConstants.STREAM_CHANNEL_16110) //AutelPlayer(SDKConstants.STREAM_CHANNEL_16110)
//        codecView = createAutelCodecView()
//
//        if(mAutelPlayer != null){
//            mAutelPlayer?.removeVideoView();
//        }
//        mAutelPlayer!!.addVideoView(codecView)
//        with(left_view) {
//            this?.addView(codecView,0)
//            this?.invalidate()
//        }

        requestKeyFrameTest()
    }

    private fun requestKeyFrameTest() {
        scope.launch(CoroutineExceptionHandler { _, _ ->
        }) {
            while (true) {
                //autelPlayerManager?.postKeyFrameReqForPlayer(SDKConstants.STREAM_CHANNEL_16130)
                //autelPlayerManager?.postKeyFrameReqForPlayer(SDKConstants.STREAM_CHANNEL_15)

                delay(5000)
            }
        }
    }

    private  fun createAutelCodecView(): AutelPlayerView? {
        val codecView = AutelPlayerView(activity)
        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        codecView.layoutParams = params
        return codecView
    }

    private  fun createAutelCodecView2(): AutelPlayerView? {
        val codecView = AutelPlayerView(activity)
        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        codecView.layoutParams = params
        return codecView
    }

    override fun onDestroy() {
        super.onDestroy()

//        autelPlayerManager?.endStreamChannel(SDKConstants.STREAM_CHANNEL_16130);
//
//
//
//        if (mAutelPlayer != null) {
//            mAutelPlayer!!.removeVideoView()
//            mAutelPlayer!!.releasePlayer()
//        }
//
//
//        autelPlayerManager?.unregistStreamDataListener();
//        autelPlayerManager?.release();
    }
}