package com.autel.sdk.debugtools.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.autel.drone.sdk.SDKConstants
import com.autel.drone.sdk.log.SDKLog
import com.autel.drone.sdk.vmodelx.interfaces.WiredRelayListener
import com.autel.drone.sdk.vmodelx.manager.WiredRelayManager
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.rchidden.enums.Role
import com.autel.player.player.AutelPlayerManager
import com.autel.player.player.autelplayer.AutelPlayer
import com.autel.player.player.autelplayer.AutelPlayerView
import com.autel.sdk.debugtools.SystemUtil
import com.autel.sdk.debugtools.databinding.FragmentWiredRelayBinding
import com.autel.sdk.debugtools.dialog.RemoterRoleSettingDialog
import java.util.concurrent.atomic.AtomicBoolean

class WiredRelayFragment : AutelFragment() {

    companion object {
        private const val TAG = "WiredRelayFragment"
    }

    private lateinit var binding: FragmentWiredRelayBinding
    private var dialog: RemoterRoleSettingDialog? = null
    private var codecView: AutelPlayerView? = null
    private var autelPlayer: AutelPlayer? = null
    private var isShowDialog = AtomicBoolean(false)

    private var isConnected = false
    private var myRole = Role.UNKNOWN

    private val listener = object : WiredRelayListener {
        override fun onConnectStateChanged(connected: Boolean, role: Role) {
            if (isConnected == connected && myRole == role) {
                return
            }

            isConnected = connected
            myRole = role
            SDKLog.i(TAG, "onConnectStateChanged = $connected, role = ${role.name}")
            binding.tvStatus.text = if (connected) "Connected" else "Disconnected"
            binding.tvRole.text = if (role == Role.PRIMARY) "Master" else if (role == Role.RELAY) "Relay" else "Unknown"
            dealSystemState()
        }

        override fun onNeedChooseRole(need: Boolean) {
            if (!need) {
                dialog?.dismiss()
                isShowDialog.set(false)
                return
            }

            if (isShowDialog.get()) {
                return
            }

            isShowDialog.set(true)
            dialog = RemoterRoleSettingDialog()
            dialog?.let { dlg ->
                dlg.setOnConfirmListener { onChooseRole(it) }
                dlg.dialog?.setOnDismissListener {
                    isShowDialog.set(false)
                }

                SDKLog.i(TAG, "RoleDialog -> Both parties are unknown role, popup confirmation dialog")
                activity?.supportFragmentManager
                    ?.beginTransaction()
                    ?.add(dlg, "RemoterRoleSettingDialog")
                    ?.commitAllowingStateLoss()
            }
        }
    }

    private fun onChooseRole(isMaster: Boolean) {
        WiredRelayManager.get().setSelfRole(if (isMaster) Role.PRIMARY else Role.RELAY)
    }

    private fun dealSystemState() {
        activity?.let {
            //if not master, go to turn off screen and set volume to 0
            if (myRole == Role.RELAY) {
                //need DEVICE_POWER permission
                SystemUtil.goToSleep(it)
                SystemUtil.setStreamVolume(it, 0)
            } else {
                //need WAKE_LOCK permission
                SystemUtil.goToAlive(it)
                SystemUtil.setStreamVolume(it, 10)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentWiredRelayBinding.inflate(inflater)
        WiredRelayManager.get().init()
        WiredRelayManager.get().addListener(listener)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //init player
        autelPlayer = AutelPlayer(SDKConstants.getZoomChancelId())
        codecView = createAutelCodecView()
        binding.videoView.addView(codecView)
        autelPlayer?.addVideoView(codecView)

        AutelPlayerManager.getInstance().addAutelPlayer(autelPlayer)
        autelPlayer?.startPlayer()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        WiredRelayManager.get().removeListener(listener)
        WiredRelayManager.get().unInit()

        autelPlayer?.removeVideoView()
        autelPlayer?.releasePlayer()
    }

    private fun createAutelCodecView(): AutelPlayerView {
        val codecView = AutelPlayerView(activity)
        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        codecView.layoutParams = params
        return codecView
    }
}