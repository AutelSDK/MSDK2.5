package com.autel.sdk.debugtools.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Spinner
import com.autel.drone.sdk.SDKConstants
import com.autel.drone.sdk.libbase.error.IAutelCode
import com.autel.drone.sdk.log.SDKLog
import com.autel.drone.sdk.vmodelx.interfaces.IAutelDroneDevice
import com.autel.drone.sdk.vmodelx.manager.DeviceManager
import com.autel.drone.sdk.vmodelx.manager.keyvalue.callback.CommonCallbacks
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.AirLinkKey
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.base.KeyTools
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.alink.enums.VideoTransMissionModeEnum
import com.autel.drone.sdk.vmodelx.module.camera.bean.GimbalTypeEnum
import com.autel.drone.sdk.vmodelx.module.camera.bean.LensTypeEnum
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

    private var droneDevice: IAutelDroneDevice? = null
    private var gimbalType: GimbalTypeEnum = GimbalTypeEnum.XL801
    private var lensTypeLeft: LensTypeEnum = LensTypeEnum.Zoom
    private var lensTypeRight: LensTypeEnum = LensTypeEnum.Thermal

    private var changedFromUser = true

    private val listener = object : IVideoStreamListener {
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

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        uiBinding = FragMuiltStreamBinding.inflate(inflater, container, false)
        return uiBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        droneDevice = DeviceManager.getDeviceManager().getFirstDroneDevice()
        droneDevice?.getKeyManager()?.setValue(
            KeyTools.createKey(AirLinkKey.KeyALinkTransmissionMode),
            VideoTransMissionModeEnum.HIGH_QUALITY, object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                }

                override fun onFailure(code: IAutelCode, msg: String?) {
                    //ToastUtils.showToast(msg?:getString(R.string.debug_setup_failed))
                }
            }
        )

        gimbalType = droneDevice?.getGimbalDeviceType() ?: GimbalTypeEnum.XL801
        val lensList = droneDevice?.getCameraAbilitySetManger()?.getLensList(gimbalType)
        lensList?.firstOrNull()?.let {
            lensTypeLeft = it

            codecView = createAutelCodecView()
            uiBinding.layoutLeftView.addView(codecView)
            mAutelPlayer = AutelPlayer(getChannelIdByLens(lensTypeLeft))
            mAutelPlayer?.setVideoInfoListener(listener)

            mAutelPlayer!!.addVideoView(codecView)
            AutelPlayerManager.getInstance().addAutelPlayer(mAutelPlayer)
            mAutelPlayer!!.startPlayer()
        }

        lensList?.getOrNull(1)?.let {
            lensTypeRight = it

            codecView2 = createAutelCodecView()
            uiBinding.layoutRightView.addView(codecView2)
            mAutelPlayer2 = AutelPlayer(getChannelIdByLens(lensTypeRight))
            mAutelPlayer2!!.addVideoView(codecView2)
            AutelPlayerManager.getInstance().addAutelPlayer(mAutelPlayer2)
            mAutelPlayer2!!.startPlayer()
        }

        val moreLens = lensList?.filter { it != lensTypeLeft && it != lensTypeRight }.orEmpty()
        val leftLens: MutableList<LensTypeEnum> = mutableListOf()
        leftLens.add(0, lensTypeLeft)
        leftLens.addAll(moreLens)
        initSpinnerOption(uiBinding.spinnerLens1, leftLens)

        val rightLens: MutableList<LensTypeEnum> = mutableListOf()
        rightLens.add(0, lensTypeRight)
        rightLens.addAll(moreLens)
        initSpinnerOption(uiBinding.spinnerLens2, rightLens)
    }

    private fun getChannelIdByLens(lensType: LensTypeEnum): Int {
        val lensList = droneDevice?.getCameraAbilitySetManger()?.getLensList(gimbalType)
        return when(lensType) {
            LensTypeEnum.WideAngle -> SDKConstants.getWideAngleChannelId()
            LensTypeEnum.NightVision -> SDKConstants.getNightVisionChannelId()
            LensTypeEnum.TeleZoom -> SDKConstants.getNightVisionChannelId()
            LensTypeEnum.Thermal, LensTypeEnum.TeleThermal -> SDKConstants.getInfraredChannelId()
            LensTypeEnum.Zoom -> if (lensList?.contains(LensTypeEnum.WideAngle) == true) {
                SDKConstants.getTelZoomChancelId()
            } else {
                SDKConstants.getZoomChancelId()
            }
            else -> SDKConstants.getZoomChancelId()
        }
    }

    private fun initSpinnerOption(spinner: Spinner, data: List<LensTypeEnum>) {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, data.map { it.name })
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.adapter = adapter
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val lensName = adapter.getItem(position) as String
                SDKLog.d("MuiltCodecFragment", "${spinner.id} onItemSelected: $position $lensName")
                val lensList = droneDevice?.getCameraAbilitySetManger()?.getLensList(gimbalType)
                val lensType = lensList?.firstOrNull { it.name == lensName } ?: return
                when (spinner) {
                    uiBinding.spinnerLens1 -> switchLeftVideo(lensType)
                    uiBinding.spinnerLens2 -> switchRightVideo(lensType)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun switchLeftVideo(lensType: LensTypeEnum) {
        SDKLog.d("MuiltCodecFragment", "switchLeftVideo $lensTypeLeft -> $lensType")
        if (lensTypeLeft == lensType) return

        mAutelPlayer?.removeVideoView()
        mAutelPlayer?.releasePlayer()

        lensTypeLeft = lensType
        mAutelPlayer = AutelPlayer(getChannelIdByLens(lensTypeLeft))
        mAutelPlayer?.setVideoInfoListener(listener)

        mAutelPlayer!!.addVideoView(codecView)
        AutelPlayerManager.getInstance().addAutelPlayer(mAutelPlayer)
        mAutelPlayer!!.startPlayer()

        val lensList = droneDevice?.getCameraAbilitySetManger()?.getLensList(gimbalType)
        val rightLens = lensList?.filter { it != lensTypeLeft && it != lensTypeRight }.orEmpty().toMutableList()
        val position = uiBinding.spinnerLens2.selectedItemPosition
        rightLens.add(position, lensTypeRight)
        SDKLog.d("MuiltCodecFragment", "switchLeftVideo lensList: $rightLens")
        notifySpinnerDataChanged(uiBinding.spinnerLens2, rightLens)
    }

    private fun switchRightVideo(lensType: LensTypeEnum) {
        SDKLog.d("MuiltCodecFragment", "switchRightVideo $lensTypeRight -> $lensType")
        if (lensTypeRight == lensType) return

        mAutelPlayer2?.removeVideoView()
        mAutelPlayer2?.releasePlayer()

        lensTypeRight = lensType
        mAutelPlayer2 = AutelPlayer(getChannelIdByLens(lensTypeRight))
        mAutelPlayer2!!.addVideoView(codecView2)
        AutelPlayerManager.getInstance().addAutelPlayer(mAutelPlayer2)
        mAutelPlayer2!!.startPlayer()

        val lensList = droneDevice?.getCameraAbilitySetManger()?.getLensList(gimbalType)
        val leftLens = lensList?.filter { it != lensTypeLeft && it != lensTypeRight }.orEmpty().toMutableList()
        val position = uiBinding.spinnerLens1.selectedItemPosition
        leftLens.add(position, lensTypeLeft)
        SDKLog.d("MuiltCodecFragment", "switchLeftVideo lensList: $leftLens")
        notifySpinnerDataChanged(uiBinding.spinnerLens1, leftLens)
    }

    private fun notifySpinnerDataChanged(spinner: Spinner, data: List<LensTypeEnum>) {
        val adapter: ArrayAdapter<String> = spinner.adapter as ArrayAdapter<String>
        adapter.setNotifyOnChange(false)
        adapter.clear()
        adapter.addAll(data.map { it.name }.toList())
        adapter.notifyDataSetChanged()
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