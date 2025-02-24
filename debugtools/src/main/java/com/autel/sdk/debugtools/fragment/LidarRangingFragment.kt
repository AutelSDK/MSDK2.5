package com.autel.sdk.debugtools.fragment

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.isVisible
import com.autel.drone.sdk.SDKConstants
import com.autel.drone.sdk.libbase.error.IAutelCode
import com.autel.drone.sdk.log.SDKLog
import com.autel.drone.sdk.vmodelx.manager.DeviceManager
import com.autel.drone.sdk.vmodelx.manager.keyvalue.callback.CommonCallbacks
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.GimbalKey
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.base.KeyTools
import com.autel.drone.sdk.vmodelx.module.camera.bean.LensTypeEnum
import com.autel.player.player.autelplayer.AutelPlayer
import com.autel.player.player.autelplayer.AutelPlayerView
import com.autel.sdk.debugtools.databinding.FragmentLidarRangBinding

class LidarRangingFragment : AutelFragment() {

    companion object {
        private const val TAG = "LidarRangingFragment"
        private const val MSG_UPDATE_RESULT = 0x01
    }

    private lateinit var binding: FragmentLidarRangBinding
    private var isRunning = false
    private var autelPlayer: AutelPlayer? = null

    private val lidarKey = KeyTools.createKey(GimbalKey.KeyLaserRangingSwitch)
    private val handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MSG_UPDATE_RESULT -> {
                    updateResult()
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLidarRangBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initPlayer()

        binding.btnStartRecognize.setOnClickListener {
            val keyManager = DeviceManager.getDeviceManager().getFirstDroneDevice()?.getKeyManager()
            keyManager?.setValue(lidarKey, !isRunning, object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    SDKLog.i(TAG, "set lidar success ${!isRunning}")
                    isRunning = !isRunning
                    toggleLidar()
                }

                override fun onFailure(code: IAutelCode, msg: String?) {
                    SDKLog.i(TAG, "set lidar failed ${!isRunning} $code $msg")
                }
            })
        }

        //get current status
        val keyManager = DeviceManager.getDeviceManager().getFirstDroneDevice()?.getKeyManager()
        keyManager?.getValue(
            lidarKey,
            object : CommonCallbacks.CompletionCallbackWithParam<Boolean> {
                override fun onSuccess(t: Boolean?) {
                    isRunning = t ?: false
                    SDKLog.i(TAG, "get lidar success $isRunning")
                    toggleLidar()
                }

                override fun onFailure(error: IAutelCode, msg: String?) {
                    SDKLog.i(TAG, "get lidar failed $error $msg")
                }
            })
    }

    private fun toggleLidar() {
        activity?.runOnUiThread {
            binding.ivLidar.isVisible = isRunning
            binding.tvResult.isVisible = isRunning
        }
        if (isRunning) {
            handler.sendEmptyMessage(MSG_UPDATE_RESULT)
        } else {
            handler.removeMessages(MSG_UPDATE_RESULT)
        }
    }

    private fun updateResult() {
        val data = DeviceManager.getDeviceManager().getFirstDroneDevice()
            ?.getDeviceStateData()?.flightControlData
        if (data?.laserDistanceIsValid == true) {
            binding.tvResult.text = "Distance: ${data.laserDistance / 100.0}m"
        } else {
            binding.tvResult.text = "Distance: N/A"
        }
        handler.sendEmptyMessageDelayed(MSG_UPDATE_RESULT, 1000)
    }

    private fun initPlayer() {
        val codecView = createAutelCodecView()
        binding.layoutVideo.addView(codecView)

        val channelId = getChannelId(LensTypeEnum.Zoom)
        autelPlayer = AutelPlayer(channelId)
        autelPlayer?.addVideoView(codecView)
    }

    private fun getChannelId(lensType: LensTypeEnum): Int {
        val drone = DeviceManager.getDeviceManager().getFirstDroneDevice()
            ?: return SDKConstants.getZoomChancelId()
        val lens = drone.getAbilitySetManager().getCameraAbilitySetManager()
            .getLensList(drone.getGimbalDeviceType()) ?: emptyList()
        return when (lensType) {
            LensTypeEnum.Zoom -> if (lens.contains(LensTypeEnum.WideAngle)) SDKConstants.getTelZoomChancelId() else SDKConstants.getZoomChancelId()
            LensTypeEnum.TeleZoom -> SDKConstants.getTelZoomChancelId()
            LensTypeEnum.WideAngle -> SDKConstants.getWideAngleChannelId()
            LensTypeEnum.Thermal, LensTypeEnum.TeleThermal -> SDKConstants.getInfraredChannelId()
            LensTypeEnum.NightVision -> SDKConstants.getNightVisionChannelId()
            else -> SDKConstants.getZoomChancelId()
        }
    }

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
        handler.removeCallbacksAndMessages(null)

        autelPlayer?.removeVideoView()
        autelPlayer?.releasePlayer()
    }
}