package com.autel.sdk.debugtools.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.autel.drone.sdk.SDKConstants
import com.autel.drone.sdk.libbase.error.IAutelCode
import com.autel.drone.sdk.vmodelx.manager.DeviceManager
import com.autel.drone.sdk.vmodelx.manager.keyvalue.callback.CommonCallbacks
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.AirLinkKey
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.base.KeyTools
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.alink.enums.VideoTransMissionModeEnum
import com.autel.player.MediaInfo
import com.autel.player.player.AutelPlayerManager
import com.autel.player.player.IVideoStreamListener
import com.autel.player.player.autelplayer.AutelPlayer
import com.autel.player.player.autelplayer.AutelPlayerView
import com.autel.sdk.debugtools.databinding.FragMuiltStreamBinding
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * multiple type of codec for video streaming
 * Copyright: Autel Robotics
 * @author huangsihua on 2022/12/17.
 */
class MuiltCodecFragment : AutelFragment() {

    private var mAutelPlayer: AutelPlayer? = null
    private var codecView: AutelPlayerView? = null

    private var mAutelPlayer2: AutelPlayer? = null
    private var codecView2: AutelPlayerView? = null
    private var isFrameSaved = false
    private lateinit var uiBinding: FragMuiltStreamBinding


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        uiBinding = FragMuiltStreamBinding.inflate(inflater, container, false)
        return uiBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        DeviceManager.getDeviceManager().getFirstDroneDevice()?.getKeyManager()?.setValue(
            KeyTools.createKey(AirLinkKey.KeyALinkTransmissionMode),
            VideoTransMissionModeEnum.HIGH_QUALITY, object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                }

                override fun onFailure(error: IAutelCode, msg: String?) {
                    //ToastUtils.showToast(msg?:getString(R.string.debug_setup_failed))
                }
            }
        )

        codecView = createAutelCodecView()
        uiBinding.layoutLeftView.addView(codecView)

        mAutelPlayer = AutelPlayer(SDKConstants.STREAM_CHANNEL_16110)

        mAutelPlayer?.setVideoInfoListener(object : IVideoStreamListener {
            override fun onVideoSizeChanged(playerId: Int, width: Int, height: Int) {
                isFrameSaved = false;
            }

            override fun onVideoInfoCallback(playerId: Int, x: Int, y: Int, w: Int, h: Int) {
            }

            override fun onFrameYuv(yuv: ByteBuffer?, mediaInfo: MediaInfo?) {
                Log.i("MuiltCodecFragment", " yuv ${yuv?.capacity()} mediaInfo ${mediaInfo.toString()}")
                if (!isFrameSaved && yuv != null && mediaInfo != null){
                    isFrameSaved = true;
                    if (mediaInfo.pixelFormat == MediaInfo.PixelFormat.PIX_FMT_NV12){
                        saveYuvToFile(yuv, mediaInfo.width, mediaInfo.height, mediaInfo.stride, mediaInfo.sliceHeight)
                    }

                }
            }


            override fun onVideoErrorCallback(playerId: Int, type: Int, errorContent: String?) {
            }

        })

        mAutelPlayer!!.addVideoView(codecView)
        AutelPlayerManager.getInstance().addAutelPlayer(mAutelPlayer)

        codecView2 = createAutelCodecView2()
        uiBinding.layoutRightView.addView(codecView2)

        mAutelPlayer2 = AutelPlayer(SDKConstants.STREAM_CHANNEL_16115)
        mAutelPlayer2!!.addVideoView(codecView2)
        AutelPlayerManager.getInstance().addAutelPlayer(mAutelPlayer2);

        mAutelPlayer!!.startPlayer()
        mAutelPlayer2!!.startPlayer()
    }


    fun removeNV12Padding(originalBuffer: ByteBuffer, width: Int, height: Int, stride: Int, sliceHeight:Int): ByteBuffer {
        val ySize = width * height
        val uvSize = ySize / 2
        val totalSize = ySize + uvSize

        val resultBuffer = ByteBuffer.allocateDirect(totalSize)
        resultBuffer.order(ByteOrder.nativeOrder())

        // Copy Y plane without padding
        for (row in 0 until height) {
            val srcOffset = row * stride
            val dstOffset = row * width
            originalBuffer.position(srcOffset)
            val temp = ByteArray(width)
            originalBuffer.get(temp)
            resultBuffer.position(dstOffset)
            resultBuffer.put(temp)
        }

        // Copy UV plane without padding
        val uvHeight = height / 2
        for (row in 0 until uvHeight) {
            val srcOffset = stride*sliceHeight + row * stride
            val dstOffset = ySize + row * width
            originalBuffer.position(srcOffset)
            val temp = ByteArray(width)
            originalBuffer.get(temp)
            resultBuffer.position(dstOffset)
            resultBuffer.put(temp)
        }

        resultBuffer.rewind()
        return resultBuffer
    }

    private fun saveYuvToFile(buffer: ByteBuffer, width: Int, height: Int, stride: Int, sliceHeight: Int) {

        val fileName = "frame_${System.currentTimeMillis()}.yuv"
        val file = File(requireContext().filesDir, fileName)

        try {
            FileOutputStream(file).use { fos ->
                var b = removeNV12Padding(buffer, width, height, stride, sliceHeight)
                val data = ByteArray(b.remaining())
                b.get(data)
                fos.write(data)
            }
            println("File saved at: ${file.absolutePath}")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * create code view for autel media player 1
     */
    private fun createAutelCodecView(): AutelPlayerView? {
        val codecView = AutelPlayerView(activity)
        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        codecView.layoutParams = params
        return codecView
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

    override fun onDestroy() {
        super.onDestroy()

        AutelPlayerManager.getInstance().endStreamChannel(SDKConstants.STREAM_CHANNEL_16110);

        AutelPlayerManager.getInstance().endStreamChannel(SDKConstants.STREAM_CHANNEL_16115);

        if (mAutelPlayer != null) {
            mAutelPlayer!!.removeVideoView()
            mAutelPlayer!!.releasePlayer()
        }

        if (mAutelPlayer2 != null) {
            mAutelPlayer2!!.removeVideoView()
            mAutelPlayer2!!.releasePlayer()
        }
	  
	   //for new home

        //AutelPlayerManager.getInstance().unregistStreamDataListener();
        //AutelPlayerManager.getInstance().release();
    }
}