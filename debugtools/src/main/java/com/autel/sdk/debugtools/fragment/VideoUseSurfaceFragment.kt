package com.autel.sdk.debugtools.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.SurfaceHolder
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
import com.autel.player.codec.StreamData
import com.autel.player.player.AutelPlayerManager
import com.autel.player.player.IVideoStreamListener
import com.autel.player.player.autelplayer.AutelPlayer
import com.autel.player.player.autelplayer.AutelPlayerView
import com.autel.sdk.debugtools.R
import com.autel.sdk.debugtools.databinding.FragMuiltStreamBinding
import com.autel.sdk.debugtools.databinding.FragmentWithSurfaceBinding
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * multiple type of codec for video streaming
 * Copyright: Autel Robotics
 * @author huangsihua on 2022/12/17.
 */
class VideoUseSurfaceFragment : AutelFragment() {

    private var mAutelPlayer: AutelPlayer? = null
    private var mAutelPlayer2: AutelPlayer? = null
    private lateinit var uiBinding: FragmentWithSurfaceBinding


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        uiBinding = FragmentWithSurfaceBinding.inflate(inflater, container, false)
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
        mAutelPlayer = AutelPlayer(SDKConstants.getZoomChancelId())
        uiBinding.leftView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                Log.d("VideoUseSurfaceFragment", "surfaceCreated")
                mAutelPlayer!!.setExternalSurface(holder.surface)
                AutelPlayerManager.getInstance().addAutelPlayer(mAutelPlayer)
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                Log.d("VideoUseSurfaceFragment", "surfaceChanged")
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                Log.d("VideoUseSurfaceFragment", "surfaceDestroyed")
            }
        })

        mAutelPlayer2 = AutelPlayer(SDKConstants.getInfraredChannelId())
        uiBinding.rightView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                Log.d("VideoUseSurfaceFragment", "2 surfaceCreated")
                mAutelPlayer2!!.setExternalSurface(holder.surface)
                AutelPlayerManager.getInstance().addAutelPlayer(mAutelPlayer2)
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                Log.d("VideoUseSurfaceFragment", "2 surfaceChanged")
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                Log.d("VideoUseSurfaceFragment", "2 surfaceDestroyed")
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()

//        AutelPlayerManager.getInstance().endLocalStreamChannel(SDKConstants.getZoomChancelId())
//        AutelPlayerManager.getInstance().endLocalStreamChannel(SDKConstants.getInfraredChannelId())

        if (mAutelPlayer != null) {
            mAutelPlayer!!.setExternalSurface(null)
            mAutelPlayer!!.releasePlayer()
        }

        if (mAutelPlayer2 != null) {
            mAutelPlayer2!!.setExternalSurface(null)
            mAutelPlayer2!!.releasePlayer()
        }
	  
	   //for new home

        //AutelPlayerManager.getInstance().unregistStreamDataListener();
        //AutelPlayerManager.getInstance().release();
    }
}