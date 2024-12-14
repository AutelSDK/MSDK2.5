package com.autel.sdk.debugtools.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.autel.drone.sdk.log.SDKLog
import com.autel.drone.sdk.v2.manager.LTEManager
import com.autel.drone.sdk.vmodelx.SDKManager
import com.autel.drone.sdk.vmodelx.device.IAutelDroneListener
import com.autel.drone.sdk.vmodelx.interfaces.IAutelDroneDevice
import com.autel.sdk.debugtools.databinding.FragmentLteBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Copyright: Autel Robotics
 * @author R24033 on 2024/10/18
 * LTE
 */
class LteFragment : AutelFragment(), IAutelDroneListener {

    private lateinit var binding: FragmentLteBinding
    private val tag = "LteFragment"

    private val logList: MutableList<String> = arrayListOf()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentLteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        SDKManager.get().getDeviceManager().addDroneListener(this)
        initViews()
    }

    private fun initViews() {
        val isConnected = SDKManager.get().getDeviceManager().isConnected()
        if (isConnected) {
            updateLogInfo("drone has connected")
            getLTEModuleEnable()
        }
        binding.btnOpenLte.setOnClickListener {
            updateLogInfo(">> btnOpenLte click")
            LTEManager.get().setLTEModuleEnable(isEnable = true,
                onSuccess = {
                    updateLogInfo("open lte module success")
                }, onFailure = { code, msg ->
                    updateLogInfo("open lte module onFailure:[code:$code;msg:$msg]")
                })
        }

        binding.btnSetDomainName.setOnClickListener {
            updateLogInfo(">> btnSetDomainName click")
            LTEManager.get().setLTEAPNDomainName(domainName = "test domain name",
                onSuccess = {
                    updateLogInfo("set domain name success")
                    getLTEDomainName()
                }, onFailure = { code, msg ->
                    updateLogInfo("set domain name onFailure:[code:$code;msg:$msg]")
                })
        }

        binding.btnSetUsername.setOnClickListener {
            updateLogInfo(">> btnSetUsername click")
            LTEManager.get().setLTEAPNUserName(userName = "test userName",
                onSuccess = {
                    updateLogInfo("set username success")
                    getLTEUserName()
                }, onFailure = { code, msg ->
                    updateLogInfo("set username onFailure:[code:$code;msg:$msg]")
                })
        }
        binding.btnSetPsd.setOnClickListener {
            updateLogInfo(">> btnSetPsd click")
            LTEManager.get().setLTEAPNPsd(passWord = "test passWord",
                onSuccess = {
                    updateLogInfo("set password success")
                    getLTEPassWord()
                }, onFailure = { code, msg ->
                    updateLogInfo("set password onFailure:[code:$code;msg:$msg]")
                })
        }
        binding.btnGetCcid.setOnClickListener {
            updateLogInfo(">> btnGetCcid click")
            LTEManager.get().getLTECcid(onSuccess = { ccid ->
                updateLogInfo("get ccid onSuccess:[$ccid]")
            }, onFailure = { code, msg ->
                updateLogInfo("get ccid onFailure:[code:$code;msg:$msg]")
            })
        }
        binding.btnGetPhonenumber.setOnClickListener {
            updateLogInfo(">> btnGetPhonenumber click")
            LTEManager.get().getLTEPhoneNumber(onSuccess = { phone ->
                updateLogInfo("get phone onSuccess:[$phone]")
            }, onFailure = { code, msg ->
                updateLogInfo("get phone onFailure:[code:$code;msg:$msg]")
            })
        }

        binding.btnClearLog.setOnClickListener {
            clearLog()
        }
    }


    override fun onDroneChangedListener(connected: Boolean, drone: IAutelDroneDevice) {
        super.onDroneChangedListener(connected, drone)
        updateLogInfo("drone connect state:$connected")
        if (connected) {
            getLTEModuleEnable()
        }

    }

    private fun getLTEDomainName() {
        lifecycleScope.launch {
            delay(1500)
            LTEManager.get().getLTEAPNDomainName(onSuccess = { domain ->
                updateLogInfo("get lte domain name onSuccess:[$domain]")
            }, onFailure = { code, msg ->
                updateLogInfo("get lte domain name failure:[code:$code;msg:$msg]")
            })
        }
    }

    private fun getLTEUserName() {
        lifecycleScope.launch {
            delay(1500)
            LTEManager.get().getLTEAPNUserName(onSuccess = { userName ->
                updateLogInfo("get lte userName onSuccess:[$userName]")

            }, onFailure = { code, msg ->
                updateLogInfo("get lte userName onFailure:[code:$code;msg:$msg]")
            })
        }
    }

    private fun getLTEPassWord() {
        lifecycleScope.launch {
            delay(1500)
            LTEManager.get().getLTEAPNPsd(onSuccess = { passWord ->
                updateLogInfo("get lte passWord onSuccess:[$passWord]")

            }, onFailure = { code, msg ->
                updateLogInfo("get lte passWord onFailure:[code:$code;msg:$msg]")
            })
        }
    }

    private fun getLTEModuleEnable() {
        lifecycleScope.launch {
            delay(1000)
            LTEManager.get().getLTEModuleEnable(onSuccess = { state ->
                updateButtonEnable(state)
                binding.btnOpenLte.isEnabled = !state
                updateLogInfo("get lte module enable state:$state")
            }, onFailure = { code, msg ->
                updateLogInfo("get lte module enable state onFailure:[code:$code;msg:$msg]")
            })
        }

    }

    /**
     * 更新日志
     */
    private fun updateLogInfo(log: String, isError: Boolean = false) {
        logList.add(log)

        if (!isError) {
            SDKLog.d(tag, log)
        } else {
            SDKLog.e(tag, log)
        }

        val sb = StringBuilder()
        logList.forEach {
            sb.append(it).append("\n")
        }

        binding.tvLogInfo.text = sb.toString()
    }

    private fun clearLog() {
        if (logList.isNotEmpty()) {
            logList.clear()
        }

        binding.tvLogInfo.text = ""
    }

    private fun updateButtonEnable(connected: Boolean) {
        binding.btnSetDomainName.isEnabled = connected
        binding.btnSetUsername.isEnabled = connected
        binding.btnSetPsd.isEnabled = connected
        binding.btnGetCcid.isEnabled = connected
        binding.btnGetPhonenumber.isEnabled = connected
    }

    override fun onDestroyView() {
        super.onDestroyView()
        SDKManager.get().getDeviceManager().removeDroneListener(this)
    }
}