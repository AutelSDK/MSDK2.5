package com.autel.sdk.debugtools.fragment

import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.autel.drone.sdk.libbase.error.IAutelCode
import com.autel.drone.sdk.store.SDKStorage
import com.autel.drone.sdk.vmodelx.SDKManager
import com.autel.drone.sdk.vmodelx.device.IAutelDroneListener
import com.autel.drone.sdk.vmodelx.interfaces.IAutelDroneDevice
import com.autel.drone.sdk.vmodelx.interfaces.IRTKManager
import com.autel.drone.sdk.vmodelx.manager.DeviceManager
import com.autel.drone.sdk.vmodelx.manager.keyvalue.callback.CommonCallbacks
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.CommonKey
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.base.KeyTools
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.flight.bean.DroneSystemStateLFNtfyBean
import com.autel.sdk.debugtools.beans.RTKConstans
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.rtk.bean.NestRtkStatusNotifyBean
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.rtk.bean.RtkReportBean
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.rtk.enums.RTKPositionTypeEnum
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.rtk.enums.RTKSignalEnum
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.rtk.enums.RTKSignalModeEnum
import com.autel.drone.sdk.vmodelx.utils.ToastUtils
import com.autel.sdk.debugtools.R
import com.autel.sdk.debugtools.databinding.FragmentRtkBinding
import com.autel.sdk.debugtools.dialog.RtkAuthorDialog
import kotlinx.coroutines.launch
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

const val SP_NTRIP_RTK_HOST = "SP_NTRIP_RTK_HOST"//主机名
const val SP_NTRIP_RTK_ACCOUNT = "SP_NTRIP_RTK_ACCOUNT"//账号
const val SP_NTRIP_RTK_PWD = "SP_NTRIP_RTK_PWD"//密码
const val SP_NTRIP_RTK_PORT = "SP_NTRIP_RTK_PORT"//端口
const val SP_NTRIP_RTK_MOUNT_POINT = "SP_NTRIP_RTK_MOUNT_POINT"//挂载点

class RtkFragment : AutelFragment(), IRTKManager.RTKReportInfoCallback,
    IAutelDroneListener {
    companion object {
        const val TAG = "RtkFragment"
        private const val LISTEN_RECORD_MAX_LENGTH = 6000
    }

    /**
     * 飞控低频上报数据，包含4G/5G网络状态
     */
    private var droneSystemStateLFNtfyBean: DroneSystemStateLFNtfyBean? = null
    private lateinit var binding: FragmentRtkBinding
    var signalIndex = ""
    val rtkSignalEnumsList = mutableListOf<String>()


    var signalModeIndex = ""
    val rtkSignalModeList = mutableListOf<String>()

    var refreshTime = 0L
    var uslHost = RTKConstans.NTRIP_RTK_HOST
    var post = RTKConstans.NTRIP_RTK_PORT
    var mAccount = RTKConstans.NTRIP_RTK_ACCOUNT
    var mPassWord = RTKConstans.NTRIP_RTK_PWD
    var mMountPoint = RTKConstans.NTRIP_RTK_MOUNT_POINT
    private val _rtkReportBean = MutableLiveData<RtkReportBean?>()

    private var connectDroneTime: Long = System.currentTimeMillis()
    private var tvRtkFix = "N/A"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentRtkBinding.inflate(layoutInflater)
        return binding.root
    }


    override fun onDestroyView() {
        super.onDestroyView()
        SDKManager.get().getDeviceManager().removeDroneListener(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initData()
        SDKManager.get().getDeviceManager().addDroneListener(this)
        _rtkReportBean.observe(viewLifecycleOwner) {
            binding.rtkReportInfo.text = appendLogMessageRecord(it.toString())
            var posTypeName = getString(R.string.rtk_unknown_solution)
            if (it != null) {
                posTypeName = when (it.posType) {
                    RTKPositionTypeEnum.UNKNOWN_POSITION -> getString(R.string.rtk_no_solution)
                    RTKPositionTypeEnum.POSITION_LOCATION -> getString(R.string.rtk_position_by_fix)
                    RTKPositionTypeEnum.UNSUPPORT -> getString(R.string.rtk_not_supported)
                    RTKPositionTypeEnum.DOPPLER -> getString(R.string.rtk_doppler_velocity)
                    RTKPositionTypeEnum.SINGLE_POINT -> getString(R.string.rtk_single_point)
                    RTKPositionTypeEnum.PSEUDORANGE -> getString(R.string.rtk_pseudorange)
                    RTKPositionTypeEnum.SBAS -> getString(R.string.rtk_sbas)
                    RTKPositionTypeEnum.L1_FLOAT -> getString(R.string.rtk_l1_float)
                    RTKPositionTypeEnum.IONOSPHERIC_FLOAT -> getString(R.string.rtk_ionospheric_float)
                    RTKPositionTypeEnum.NARROW_FLOAT -> getString(R.string.rtk_narrow_float)
                    RTKPositionTypeEnum.L1_LOCATION -> getString(R.string.rtk_l1_fixed)
                    RTKPositionTypeEnum.WIDE_LANE_LOCATION -> getString(R.string.rtk_wide_lane_fixed)
                    RTKPositionTypeEnum.NARROW_LOCATION -> getString(R.string.rtk_narrow_fixed)
                    RTKPositionTypeEnum.INERTIAL_NAVIGATION -> getString(R.string.rtk_inertial_nav)
                    RTKPositionTypeEnum.INERTIAL_SINGLE -> getString(R.string.rtk_inertial_single)
                    RTKPositionTypeEnum.INERTIAL_PSEUDORANGE -> getString(R.string.rtk_inertial_pseudorange)
                    RTKPositionTypeEnum.INERTIAL_CARRIER_FLOAT -> getString(R.string.rtk_inertial_float)
                    RTKPositionTypeEnum.INERTIAL_CARRIER -> getString(R.string.rtk_inertial_fixed)
                    else -> getString(R.string.rtk_unknown_solution)

                }
            }

            tvRtkFix = when (it?.posType) {
                RTKPositionTypeEnum.UNRECOGNIZED,
                RTKPositionTypeEnum.UNKNOWN_POSITION -> getString(R.string.rtk_na)
                RTKPositionTypeEnum.SINGLE_POINT -> getString(R.string.rtk_single)
                RTKPositionTypeEnum.PSEUDORANGE,
                RTKPositionTypeEnum.SBAS,
                RTKPositionTypeEnum.L1_FLOAT,
                RTKPositionTypeEnum.IONOSPHERIC_FLOAT,
                RTKPositionTypeEnum.NARROW_FLOAT,
                RTKPositionTypeEnum.INERTIAL_NAVIGATION,
                RTKPositionTypeEnum.INERTIAL_SINGLE,
                RTKPositionTypeEnum.INERTIAL_CARRIER_FLOAT,
                RTKPositionTypeEnum.INERTIAL_CARRIER -> getString(R.string.rtk_float)
                else -> getString(R.string.rtk_fix)
            }
            when (it?.fixSta) {
                0 -> {
                    tvRtkFix = "${
                        when (it.posType) {
                            RTKPositionTypeEnum.UNRECOGNIZED,
                            RTKPositionTypeEnum.UNKNOWN_POSITION -> {
                                "N/A"
                            }
                            RTKPositionTypeEnum.SINGLE_POINT -> {
                                "SINGLE"
                            }
                            RTKPositionTypeEnum.PSEUDORANGE,
                            RTKPositionTypeEnum.SBAS,
                            RTKPositionTypeEnum.L1_FLOAT,
                            RTKPositionTypeEnum.IONOSPHERIC_FLOAT,
                            RTKPositionTypeEnum.NARROW_FLOAT,
                            RTKPositionTypeEnum.INERTIAL_NAVIGATION,
                            RTKPositionTypeEnum.INERTIAL_SINGLE,
                            RTKPositionTypeEnum.INERTIAL_CARRIER_FLOAT,
                            RTKPositionTypeEnum.INERTIAL_CARRIER -> {
                                "FLOAT"
                            }
                            else -> {
                                "FIX"
                            }
                        }
                    } ,duration ${(System.currentTimeMillis() - connectDroneTime) / 1000}s"
                }
                1 ->
                    if (connectDroneTime != 0L) {
                        tvRtkFix = getString(R.string.rtk_time_used, (System.currentTimeMillis() - connectDroneTime) / 1000)
                        connectDroneTime = 0L
                    }
                else -> {
                    tvRtkFix = "N/A"
                }
            }
            binding.tvReportInfo.text = getString(R.string.rtk_status_info,
                posTypeName, tvRtkFix, it?.posType?.value,
                if (it?.fixSta == 1) getString(R.string.rtk_yes) else getString(R.string.rtk_no),
                it?.gpsCnt, it?.beidouCnt ?: 0, it?.glonassCnt ?: 0, it?.galileoCnt ?: 0, it?.svCnt ?: 0,
                getFloatNoMoreThanTwoDigits((it?.lon ?: 0.0) / Math.pow(10.0, 7.0)),
                getFloatNoMoreThanTwoDigits((it?.lat ?: 0.0) / Math.pow(10.0, 7.0)),
                getFloatNoMoreThanTwoDigits((it?.hgt ?: 0.0) / Math.pow(10.0, 7.0)),
                getFloatNoMoreThanTwoDigits(((it?.lonSigma ?: 0.0f).toDouble())),
                getFloatNoMoreThanTwoDigits(((it?.latSigma ?: 0.0f).toDouble())),
                getFloatNoMoreThanTwoDigits(((it?.hgtSigma ?: 0.0f).toDouble()))
            ) + if (droneSystemStateLFNtfyBean != null) {
                getString(R.string.rtk_mobile_network_status,
                    droneSystemStateLFNtfyBean?.lteStatus,
                    droneSystemStateLFNtfyBean?.ntripStatus,
                    droneSystemStateLFNtfyBean?.lteCardIsDetected,
                    droneSystemStateLFNtfyBean?.lteSignal
                )
            } else {
                getString(R.string.rtk_mobile_network_unavailable)
            }
            scrollToBottom()
        }
    }

    private fun initView() {

        iniRtkSingleMode()

        initSingleType()
        /**
         * 是否启用RTK精确定位开关
         */
        binding.switchRtkEnable.setOnCheckedChangeListener { _, p1 ->
            DeviceManager.getFirstDroneDevice()?.getRtkManager()
                ?.enableRTKLocation(p1, object : IRTKManager.ChangeRTKConfigCallback {
                    override fun onNeedAuterInfo(singnalEnum: RTKSignalEnum, isQianxun: Boolean) {
                        autorNetRtk(singnalEnum, isQianxun)
                    }

                    override fun onUpdateConfigSuccess() {
                        onUpdateConfigFininsh()
                    }

                    override fun onUpdateConfigFailure(error: IAutelCode, msg: String?) {
                        onUpdateConfigFininsh()
                        binding.rtkReportInfo.text = appendLogMessageRecord("\nonUpdateConfigFailure,error:$error,msg:$msg\n")

                    }

                })
        }
        /**
         * 切换RTK类型，分别为网络RTK与机巢RTK
         */
        binding.rtkTvService.setSpinnerViewListener { position ->
            signalIndex = rtkSignalEnumsList[position]
            DeviceManager.getFirstDroneDevice()?.getRtkManager()
                ?.switchRTKSignalEnum(RTKSignalEnum.findEnum(position + 1), object : IRTKManager.ChangeRTKConfigCallback {
                    override fun onNeedAuterInfo(singnalEnum: RTKSignalEnum, isQianxun: Boolean) {
                        autorNetRtk(singnalEnum, isQianxun)
                    }

                    override fun onUpdateConfigSuccess() {
                        onUpdateConfigFininsh()
                    }

                    override fun onUpdateConfigFailure(error: IAutelCode, msg: String?) {
                        onUpdateConfigFininsh()
                        binding.rtkReportInfo.text = appendLogMessageRecord("\nonUpdateConfigFailure,error:$error,msg:$msg\n")

                    }

                })
        }

        // 清除日志
        binding.btnClearlog.setOnClickListener {
            binding.rtkReportInfo.text = ""
            logMessage.delete(0, logMessage.length)
        }
        // 重新授权
        binding.tvReAutor.setOnClickListener {
            RtkAuthorDialog(requireContext()).apply {
                setTitle(getString(R.string.debug_setup_title))
                setOnConfirmListener(listener = fun(uslHost: String, post: Int, userName: String, passWord: String, pointMount: String) {
                    this@RtkFragment.uslHost = uslHost
                    this@RtkFragment.post = post
                    this@RtkFragment.mAccount = userName
                    this@RtkFragment.mPassWord = passWord
                    this@RtkFragment.mMountPoint = pointMount
                    SDKStorage.setStringValue(SP_NTRIP_RTK_HOST, uslHost)
                    SDKStorage.setIntValue(SP_NTRIP_RTK_PORT, post)
                    SDKStorage.setStringValue(SP_NTRIP_RTK_ACCOUNT, userName)
                    SDKStorage.setStringValue(SP_NTRIP_RTK_PWD, passWord)
                    SDKStorage.setStringValue(SP_NTRIP_RTK_MOUNT_POINT, pointMount)
                    when (DeviceManager.getFirstDroneDevice()?.getRtkManager()?.rtkSignalEnum ?: RTKSignalEnum.NETWORK) {
                        RTKSignalEnum.NETWORK -> {
                            DeviceManager.getFirstDroneDevice()?.getRtkManager()
                                ?.updateNetRtkType(binding.switchNetRtk.isChecked, uslHost, post, object : IRTKManager.ChangeRTKConfigCallback {
                                    override fun onNeedAuterInfo(singnalEnum: RTKSignalEnum, isQianxun: Boolean) {
                                        autorNetRtk(singnalEnum, isQianxun)
                                    }

                                    override fun onUpdateConfigSuccess() {
                                        onUpdateConfigFininsh()
                                    }

                                    override fun onUpdateConfigFailure(error: IAutelCode, msg: String?) {
                                        onUpdateConfigFininsh()
                                        binding.rtkReportInfo.text = appendLogMessageRecord("\nonUpdateConfigFailure,error:$error,msg:$msg\n")
                                    }

                                })
                        }
                        RTKSignalEnum.MOBILE_NETWORK_SERVICES -> {
                            autorNetRtk(RTKSignalEnum.MOBILE_NETWORK_SERVICES, false)
                        }
                        else -> {

                        }
                    }

                })
                show()
            }
        }

        binding.btnCalibrationMode.setOnClickListener {
            if (TextUtils.isEmpty(binding.etLat.text.toString())) {
                ToastUtils.showToast(binding.etLat.hint.toString())
                return@setOnClickListener
            }
            if (TextUtils.isEmpty(binding.etLng.text.toString())) {
                ToastUtils.showToast(binding.etLng.hint.toString())
                return@setOnClickListener
            }
            if (TextUtils.isEmpty(binding.etAttitude.text.toString())) {
                ToastUtils.showToast(binding.etAttitude.hint.toString())
                return@setOnClickListener
            }
            DeviceManager.getFirstDroneDevice()?.getRtkManager()?.switchNestRtkMode(
                binding.etLat.text.toString().toDouble(),
                binding.etLng.text.toString().toDouble(),
                binding.etAttitude.text.toString().toDouble()
            )
        }

        binding.btnSelfPositioningMode.setOnClickListener {
            DeviceManager.getFirstDroneDevice()?.getRtkManager()?.switchNestRtkMode(
                0.0, 0.0, 0.0
            )
        }
    }

    private fun initSingleType() {
        rtkSignalEnumsList.clear()
        rtkSignalEnumsList.addAll(
            listOf(
                getString(R.string.debug_text_rtk_self_net_tag),
                getString(R.string.debug_text_rtk_net_tag),
                getString(R.string.debug_text_rtk_mobile_tag),
            )
        )
        binding.rtkTvService.dataList = rtkSignalEnumsList
        /**
         * 用户根据需要设置网络RTK类型，国内建议采用千寻SDK账号体系，否则网络RTK采用Ntrip方式实现
         */
        binding.switchNetRtk.setOnCheckedChangeListener { _, p1 ->
            run {
                SDKStorage.setStringValue(SP_NTRIP_RTK_HOST, uslHost)
                SDKStorage.setIntValue(SP_NTRIP_RTK_PORT, post)
                DeviceManager.getFirstDroneDevice()?.getRtkManager()
                    ?.updateNetRtkType(p1, uslHost, post, object : IRTKManager.ChangeRTKConfigCallback {
                        override fun onNeedAuterInfo(singnalEnum: RTKSignalEnum, isQianxun: Boolean) {
                            autorNetRtk(singnalEnum, isQianxun)
                        }

                        override fun onUpdateConfigSuccess() {
                            onUpdateConfigFininsh()
                        }

                        override fun onUpdateConfigFailure(error: IAutelCode, msg: String?) {
                            onUpdateConfigFininsh()
                            binding.rtkReportInfo.text = appendLogMessageRecord("\nonUpdateConfigFailure,error:$error,msg:$msg\n")

                        }

                    })
            }
        }
    }

    private fun iniRtkSingleMode() {
        rtkSignalModeList.clear()
        rtkSignalModeList.addAll(
            listOf(
                getString(R.string.debug_text_rtk_all_signal_mode),
                getString(R.string.debug_text_rtk_bds_signal_mode),
            )
        )
        binding.rtkTvMode.dataList = rtkSignalModeList

        binding.rtkTvMode.setSpinnerViewListener { position ->
            signalModeIndex = rtkSignalModeList[position]
            binding.rtkTvService.setDefaultText(signalModeIndex)
            DeviceManager.getFirstDroneDevice()?.getRtkManager()
                ?.switchRTKSignalMode(RTKSignalModeEnum.findEnum(position), object : IRTKManager.ChangeRTKConfigCallback {
                    override fun onNeedAuterInfo(singnalEnum: RTKSignalEnum, isQianxun: Boolean) {
                        autorNetRtk(singnalEnum, isQianxun)
                    }

                    override fun onUpdateConfigSuccess() {
                        onUpdateConfigFininsh()
                    }

                    override fun onUpdateConfigFailure(error: IAutelCode, msg: String?) {
                        onUpdateConfigFininsh()
                        binding.rtkReportInfo.text = appendLogMessageRecord("\nonUpdateConfigFailure,error:$error,msg:$msg\n")

                    }

                })
        }

        var rtkSignalModeEnum = DeviceManager.getFirstDroneDevice()?.getRtkManager()?.rtkSwitchModeEnum ?: RTKSignalModeEnum.ALL_SINGLE_MODE
        signalModeIndex = rtkSignalModeList[rtkSignalModeEnum.value]
        binding.rtkTvService.setDefaultText(signalModeIndex)

    }

    private fun initData() {

        /**
         * RTK状态回调，差分数据上报及挂载点（只有Ntrip会返回挂载点列表）返回，注意：调用方法前保证已经连接飞机
         */
        DeviceManager.getFirstDroneDevice()?.getRtkManager()?.registerRtkInfoCallBack(this)
        /**
         * 初始化缓存域名，端口及账号信息
         */
        RTKConstans.NTRIP_RTK_HOST = SDKStorage.getStringValue(
            SP_NTRIP_RTK_HOST,
            RTKConstans.NTRIP_RTK_HOST
        ).toString()
        RTKConstans.NTRIP_RTK_PORT = SDKStorage.getIntValue(SP_NTRIP_RTK_PORT, RTKConstans.NTRIP_RTK_PORT)
        RTKConstans.NTRIP_RTK_ACCOUNT = SDKStorage.getStringValue(
            SP_NTRIP_RTK_ACCOUNT,
            RTKConstans.NTRIP_RTK_ACCOUNT
        ).toString()
        RTKConstans.NTRIP_RTK_PWD = SDKStorage.getStringValue(SP_NTRIP_RTK_PWD, RTKConstans.NTRIP_RTK_PWD).toString()
        RTKConstans.NTRIP_RTK_MOUNT_POINT = SDKStorage.getStringValue(
            SP_NTRIP_RTK_MOUNT_POINT,
            RTKConstans.NTRIP_RTK_MOUNT_POINT
        ).toString()

        uslHost = RTKConstans.NTRIP_RTK_HOST
        post = RTKConstans.NTRIP_RTK_PORT
        mAccount = RTKConstans.NTRIP_RTK_ACCOUNT
        mPassWord = RTKConstans.NTRIP_RTK_PWD
        mMountPoint = RTKConstans.NTRIP_RTK_MOUNT_POINT
        onUpdateConfigFininsh()
        binding.switchRtkEnable.isChecked =
            DeviceManager.getFirstDroneDevice()?.getRtkManager()?.isenableRTKLocation()
                ?: false
        binding.tvReportInfo.text = getString(R.string.rtk_initial_status)



        DeviceManager.getFirstDroneDevice()?.getKeyManager()?.listen(KeyTools.createKey(
            CommonKey.KeyDroneSystemStatusLFNtfy
        ), object : CommonCallbacks.KeyListener<DroneSystemStateLFNtfyBean> {
            override fun onValueChange(oldValue: DroneSystemStateLFNtfyBean?, newValue: DroneSystemStateLFNtfyBean) {
                this@RtkFragment.droneSystemStateLFNtfyBean = newValue
            }

        })
    }


    private fun autorNetRtk(
        singnalEnum: RTKSignalEnum,
        isQianxun: Boolean
    ) {
        lifecycleScope.launch {
            when (singnalEnum) {
                RTKSignalEnum.NETWORK -> {
                    if (isQianxun) {
                        DeviceManager.getFirstDroneDevice()?.getRtkManager()?.autherQianxunRtk(
                            "D2k4942jg6br6c",
                            "2498fac96aff70b6",
                            "Evo2_RTK",
                            "AU1660705691", object : IRTKManager.RTKAuthoCallback {

                                override fun onRtkAuthorSuccess() {
                                    binding.rtkReportInfo.text = appendLogMessageRecord("\nautherQianxunRtk:onSuccess\n")
                                    Log.d(TAG, "autherQianxunRtk:onSuccess")
                                }

                                override fun onFailure(code: IAutelCode, msg: String?) {
                                    binding.rtkReportInfo.text = appendLogMessageRecord("\nautherQianxunRtk:onFailure\n, code: $code ,msg :$msg")
                                    Log.d(TAG, "autherQianxunRtk:onFailure ,code: $code ,msg :$msg")
                                }

                            })
                    } else {
                        DeviceManager.getFirstDroneDevice()?.getRtkManager()?.autherNetRtk(
                            mAccount,
                            mPassWord,
                            mMountPoint,// 该值使用AUTO或null,用户可以输入自己获取的挂载点，但不建议这样
                            object : IRTKManager.RTKAuthoCallback {

                                override fun onRtkAuthorSuccess() {
                                    binding.rtkReportInfo.text = appendLogMessageRecord("\nautherQianxunRtk:onSuccess\n")
                                    Log.d(TAG, "autherQianxunRtk:onSuccess")
                                }


                                override fun onFailure(code: IAutelCode, msg: String?) {
                                    binding.rtkReportInfo.text = appendLogMessageRecord("\nautherQianxunRtk:onFailure\n, code: $code ,msg :$msg")
                                    Log.d(TAG, "autherNetRtk:onFailure, code: $code ,msg :$msg")
                                }
                            })
                    }
                }
                RTKSignalEnum.MOBILE_NETWORK_SERVICES -> {
                    DeviceManager.getFirstDroneDevice()?.getRtkManager()?.autherMobileServiceRtk(
                        uslHost,
                        post,
                        mAccount,
                        mPassWord,
                        mMountPoint,// 该值使用AUTO或null,用户可以输入自己获取的挂载点，但不建议这样
                        object : IRTKManager.RTKAuthoCallback {

                            override fun onRtkAuthorSuccess() {
                                binding.rtkReportInfo.text = appendLogMessageRecord("\nautherQianxunRtk:onSuccess\n")
                                Log.d(TAG, "autherQianxunRtk:onSuccess")
                            }


                            override fun onFailure(code: IAutelCode, msg: String?) {
                                binding.rtkReportInfo.text = appendLogMessageRecord("\nautherQianxunRtk:onFailure\n, code: $code ,msg :$msg")
                                Log.d(TAG, "autherNetRtk:onFailure, code: $code ,msg :$msg")
                            }
                        })
                }
                /**
                 *暂时只有网络RTK与移动网络需要执行授权
                 */
                else -> {

                }
            }
        }
    }

    // 更新配置成功
    fun onUpdateConfigFininsh() {
        var rtkSignal = DeviceManager.getFirstDroneDevice()?.getRtkManager()?.rtkSignalEnum
            ?: RTKSignalEnum.UNKNOWN

        if (isAdded) {
            lifecycleScope.launch {
                when (rtkSignal) {
                    RTKSignalEnum.SELF_NETWORK -> {
                        binding.nestRtkView.visibility = View.VISIBLE
                    }
                    else -> {
                        binding.nestRtkView.visibility = View.GONE
                    }
                }

                if (rtkSignal.value > 0) {
                    signalIndex = rtkSignalEnumsList[rtkSignal.value - 1]
                    binding.rtkTvService.setDefaultText(signalIndex)
                } else {
                    binding.rtkTvService.setDefaultText("")
                }


            }
        }
    }


    override fun onRtkIniComplement() {
        Log.d(TAG, "onRtkIniComplement")
    }

    override fun onRtkUnConnected(rtkSignalEnum: RTKSignalEnum) {
        Log.d(TAG, "onRtkUnConnected")
        lifecycleScope.launch {
            binding.rtkReportInfo.text = appendLogMessageRecord("\nonRtkUnConnected:${rtkSignalEnum.name}\n")
            binding.rtkStatus.text = getString(R.string.rtk_unconnected)
        }
    }

    /**
     * Ntrip网络挂载点列表，用户可以缓存列表用于网络RTK授权
     */
    override fun onRtkMountPointList(list: ArrayList<String>?) {
        Log.d(TAG, "onRtkMountPointList:$list")
    }

    /**
     * RTK上报信息，详细参考RtkReportBean类
     */
    override fun onRtkReportInfo(reportInfo: RtkReportBean) {
        Log.d(TAG, "reportInfo:$reportInfo")
        lifecycleScope.launch {
            _rtkReportBean.value = reportInfo
        }
    }

    override fun onNestRtkReportInfo(nestRtkReportInfo: NestRtkStatusNotifyBean) {
        Log.d(TAG, "nestRtkReportInfo:$nestRtkReportInfo")
        lifecycleScope.launch {
            binding.rtkNestReportInfo.text = appendLogNestMessageRecord(nestRtkReportInfo.toString())
            scrollToBottom()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        DeviceManager.getFirstDroneDevice()?.getRtkManager()?.unRegisterRtkInfoCallBack(this)
    }

    private val logMessage = StringBuilder()
    private fun appendLogMessageRecord(appendStr: String?): String {
        val curTime = SimpleDateFormat("HH:mm:ss").format(Date())
        logMessage.append(curTime)
            .append(":")
            .append(appendStr)
            .append("\n")

        //长度限制
        var result = logMessage.toString()
        if (result.length > LISTEN_RECORD_MAX_LENGTH) {
            result = result.substring(result.length - LISTEN_RECORD_MAX_LENGTH)
        }
        return result
    }

    private val logNestMessage = StringBuilder()
    private fun appendLogNestMessageRecord(appendStr: String?): String {
        val curTime = SimpleDateFormat("HH:mm:ss").format(Date())
        logNestMessage.append(curTime)
            .append(":")
            .append(appendStr)
            .append("\n")

        //长度限制
        var result = logNestMessage.toString()
        if (result.length > LISTEN_RECORD_MAX_LENGTH) {
            result = result.substring(result.length - LISTEN_RECORD_MAX_LENGTH)
        }
        return result
    }

    private fun scrollToBottom() {
        with(binding.rtkReportInfo) {
            if (layout != null) {
                val scrollOffset = (this.layout.getLineTop(this.lineCount)
                        - this.height)
                if (scrollOffset > 0) {
                    this.scrollTo(0, scrollOffset)
                } else {
                    this.scrollTo(0, 0)
                }
            }
        }
    }

    private fun getFloatNoMoreThanTwoDigits(number: Double): String {
        val format = DecimalFormat("#.####")
        //舍弃规则，RoundingMode.FLOOR表示直接舍弃。
        format.roundingMode = RoundingMode.FLOOR
        return format.format(number)
    }

    override fun onDroneChangedListener(connected: Boolean, drone: IAutelDroneDevice) {
        connectDroneTime = System.currentTimeMillis()
    }

    override fun onCameraAbilityFetchListener(fetched: Boolean) {

    }

}