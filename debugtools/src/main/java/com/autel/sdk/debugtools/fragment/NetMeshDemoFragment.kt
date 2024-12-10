package com.autel.sdk.debugtools.fragment


import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.autel.drone.sdk.SDKConstants
import com.autel.drone.sdk.libbase.error.IAutelCode
import com.autel.drone.sdk.log.SDKLog
import com.autel.drone.sdk.vmodelx.interfaces.IAutelDroneDevice
import com.autel.drone.sdk.vmodelx.interfaces.IAutelRemoteDevice
import com.autel.drone.sdk.vmodelx.interfaces.IControlDroneListener
import com.autel.drone.sdk.vmodelx.interfaces.IMeshDeviceChangedListener
import com.autel.drone.sdk.vmodelx.interfaces.IMultiDeviceOperator
import com.autel.drone.sdk.vmodelx.interfaces.INetMeshManager
import com.autel.drone.sdk.vmodelx.interfaces.IWatchDroneListener
import com.autel.drone.sdk.vmodelx.manager.DeviceManager
import com.autel.drone.sdk.vmodelx.manager.data.ControlMode
import com.autel.drone.sdk.vmodelx.manager.data.MeshModeEnum
import com.autel.drone.sdk.vmodelx.manager.keyvalue.callback.CommonCallbacks
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.AirLinkKey
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.RemoteControllerKey
import com.autel.drone.sdk.vmodelx.manager.keyvalue.key.base.KeyTools
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.nest.enums.ModemModeEnum
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.netmesh.CreateDeviceNetworkReq
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.netmesh.CreateDeviceNetworkResp
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.netmesh.DeviceNetworkInfo
import com.autel.drone.sdk.vmodelx.manager.keyvalue.value.netmesh.DeviceNetworkStatusType
import com.autel.drone.sdk.vmodelx.utils.ToastUtils
import com.autel.sdk.debugtools.R
import com.autel.sdk.debugtools.adapter.NetMeshDemoAdapter
import com.autel.sdk.debugtools.databinding.FragmentNetMeshDemoBinding

/**
 * 演示如何组网
 */
@SuppressLint("SetTextI18n")
class NetMeshDemoFragment : AutelFragment(){
    companion object{

        private const val TAG = "NetMeshDemoFragment"
    }
    private lateinit var binding: FragmentNetMeshDemoBinding

    private var dataList: MutableList<Any> = mutableListOf()

    private lateinit var adapter: NetMeshDemoAdapter

    private var netMeshResponse: CreateDeviceNetworkResp? = null

    private fun getMultiDeviceOperator():IMultiDeviceOperator{ return  DeviceManager.getMultiDeviceOperator() }

    //组网接口
    private fun getNetMeshManger():INetMeshManager{ return getMultiDeviceOperator().getNetMeshManager() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentNetMeshDemoBinding.inflate(layoutInflater)
        setAdapter()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViewListener()
        initNetMeshListener()
        initInfoContent()
        updateData()
    }

    private fun initViewListener(){
        binding.btnNetMeshStart.setOnClickListener{ startNetMesh(false) }
        binding.btnNetMeshComplete.setOnClickListener{ finishNetMesh() }
        binding.btnNetMeshDisband.setOnClickListener{ disbandNetMesh{} }
        binding.btnControlMode.setOnClickListener { setControlMode(ControlMode.ALL, 0) }
        binding.btnChangeWatch.setOnClickListener {}
        binding.btnChangeCenterDrone.setOnClickListener {}

        binding.btnNetSingleStart.setOnClickListener { startSingleMatch() }
    }


    private fun initInfoContent() {
        binding.controlText.text = "Control Model: ${DeviceManager.getMultiDeviceOperator().getControlMode()}"

        val controlDrones = DeviceManager.getMultiDeviceOperator().getControlledDroneList()
        var msg = ""
        controlDrones.forEach { msg +="(${it.deviceNumber()}, ${it.getName()}}" }
        binding.controlDrone.text = "Control Drones: $msg"

        val watchDrone = DeviceManager.getMultiDeviceOperator().getDroneDevices().firstOrNull()
        binding.watchDrone.text = "Watched Drones: (${watchDrone?.deviceNumber()}, ${watchDrone?.getName()})"

        if(DeviceManager.getDeviceManager().getModemMode()== ModemModeEnum.MESH_MODE) {
            binding.modernMode.text = "Modem Mode: A-Mesh Link"
        } else {
            binding.modernMode.text = "Modem Mode: Single Link"
        }
    }

    /**
     * 组网监听
     */
    private fun initNetMeshListener(){
        //Watch drone change report, Autel player will change channel ID to watch video stream
        DeviceManager.getMultiDeviceOperator().addWatchChangeListener(object: IWatchDroneListener{
            override fun onWatchChange(droneList: List<IAutelDroneDevice>) {
                val droneDevice = droneList.firstOrNull()
                //Now only support watch one drone
                if(droneList.size == 1 && droneDevice != null){
                    val lenList = droneDevice.getCameraAbilitySetManger().getLensList()
                    SDKLog.i(TAG, "lenList=$lenList")
                    val channelId = if(droneDevice.isCenter()){
                         SDKConstants.getZoomChancelId()
                    } else {
                         SDKConstants.getLeafZoomChancelId()
                    }
                    ToastUtils.showToast("Current watch drone=${droneDevice.toSampleString()}, channelId=$channelId")
                }

            }
        })

        //Control mode change report
        DeviceManager.getMultiDeviceOperator().addControlChangeListener(object : IControlDroneListener{
            override fun onControlChange(mode: ControlMode, droneList: List<IAutelDroneDevice>) {
                ToastUtils.showToast("Control mode change:${mode}： ${droneList.map { it.deviceNumber()}}")
                updateData()
            }
        })

        //组网状态上报
        getMultiDeviceOperator().addNetMeshChangeListener(object : IMeshDeviceChangedListener{
            override fun onDroneDeviceChanged(newDroneList: MutableList<IAutelDroneDevice>?, oldDroneList: MutableList<IAutelDroneDevice>?) {
                updateData() //飞机更新

                //当前有无中继飞机
                if(newDroneList?.none { it.isCenter() } == true){
                    newDroneList.firstOrNull()?.let { setCenterDrone(it.deviceNumber()) }
                }

                //当前有无Watch飞机
                if(newDroneList?.none { it.isWatched() } == true){
                    newDroneList.firstOrNull()?.let { setWatchDrone(it.deviceNumber()) }
                }
            }

            override fun onRemoteDeviceChanged(newRCList: MutableList<IAutelRemoteDevice>?, oldRCList: MutableList<IAutelRemoteDevice>?) {
                updateData() //遥控器更新
            }

            override fun onMeshStateChanged(status: DeviceNetworkStatusType) {
                when(status){
                    DeviceNetworkStatusType.STARTED->{}
                    DeviceNetworkStatusType.SUCCESS->{}
                    DeviceNetworkStatusType.DISMISS->{
                        ToastUtils.showToast(getString(R.string.netmesh_disband))
                        updateData()
                    }
                    DeviceNetworkStatusType.TIMEOUT->{
                        ToastUtils.showToast(getString(R.string.netmesh_timeout))
                        startNetMesh(true)
                    }
                    else -> {}
                }
            }
        })
    }

    /**
     * 设置中继飞机
     */
    private fun setCenterDrone(deviceId: Int){
        getNetMeshManger().setCenterNode(deviceId, object : CommonCallbacks.CompletionCallbackWithParam<Int>{
            override fun onSuccess(t: Int?) { 
                ToastUtils.showToast(getString(R.string.center_set_success, deviceId)) 
            }
            override fun onFailure(error: IAutelCode, msg: String?) { 
                ToastUtils.showToast(getString(R.string.center_set_failed, msg)) 
            }
        })
    }

    /**
     * 设置watch码流的飞机
     */
    private fun setWatchDrone(deviceId: Int){
        getNetMeshManger().setWatchDevice(arrayListOf(deviceId), object: CommonCallbacks.CompletionCallbackWithParam<Int>{
            override fun onSuccess(t: Int?) { 
                ToastUtils.showToast(getString(R.string.stream_set_success, deviceId)) 
            }
            override fun onFailure(error: IAutelCode, msg: String?) { 
                ToastUtils.showToast(getString(R.string.stream_set_failed, msg)) 
            }
        })
    }

    /**
     * set control mode
     * @param mode ALL, GROUP, SINGLE
     * @param id ALL(ignore id), GROUP(groupId), SINGLE(device node id)
     */
    private fun setControlMode(mode: ControlMode, id:Int){
        val log = when(mode) {
            ControlMode.ALL -> "ALL"
            ControlMode.GROUP -> "GROUP(groupId=$id)"
            ControlMode.SINGLE -> "SINGLE(nodeId=$id)"
            ControlMode.UNKNOWN -> "UNKNOWN"
        }

        getNetMeshManger().switchControlMode(mode,id,object : CommonCallbacks.CompletionCallbackWithParam<Void>{
            override fun onSuccess(t: Void?) {
                ToastUtils.showToast("$log set control mode success")
            }

            override fun onFailure(error: IAutelCode, msg: String?) {
                ToastUtils.showToast("$log set control mode failed：$error, $msg")
            }
        })
    }

    private fun startSingleMatch(){
        if(DeviceManager.getDeviceManager().getModemMode() == ModemModeEnum.MESH_MODE){
            disbandNetMesh { startAirLinkMatch() }
        } else {
            startAirLinkMatch()
        }
    }

    private fun startAirLinkMatch() {
        val matchKey = KeyTools.createKey(AirLinkKey.KeyALinkStartMatching)
        DeviceManager.getDeviceManager().getLocalRemoteDevice().getKeyManager().performAction(matchKey, null,
            object : CommonCallbacks.CompletionCallbackWithParam<Void> {
                override fun onSuccess(t: Void?) {
                    ToastUtils.showToast("startAirLinkMatch success")
                }

                override fun onFailure(error: IAutelCode, msg: String?) {
                    ToastUtils.showToast("startAirLinkMatch failed:$error $msg")
                }
            })
    }

    /**
     * Start Net Mesh
     */
    private fun startNetMesh(continues: Boolean){
        val timeOut = 5 * 60  //set timeout
        val continueFlag  = if(continues ) 1 else 0 //continues flag, if timeout
        val netWorkInfo = DeviceNetworkInfo(System.currentTimeMillis(), "demo")
        val bean = CreateDeviceNetworkReq(timeOut, continueFlag, netWorkInfo)
        getNetMeshManger().startNetMeshMatching(bean, MeshModeEnum.STANDARD, object: CommonCallbacks.CompletionCallbackWithParam<CreateDeviceNetworkResp>{
            override fun onSuccess(t: CreateDeviceNetworkResp?) {
                netMeshResponse = t
                ToastUtils.showToast("start net mesh success")
            }

            override fun onFailure(error: IAutelCode, msg: String?) {
                ToastUtils.showToast("start net mesh failed:$error $msg")
            }
        })
    }

    /**
     * Finish Net Mesh
     */
    private fun finishNetMesh(){
        getNetMeshManger().completeNetMeshMatching(object : CommonCallbacks.CompletionCallbackWithParam<Int>{
            override fun onSuccess(t: Int?) {
                ToastUtils.showToast("finish net mesh success")
            }
            override fun onFailure(error: IAutelCode, msg: String?) {
                ToastUtils.showToast("finish net mesh failed:$error $msg")
            }
        })
    }

    /**
     * Disband Net Mesh
     */
    private fun disbandNetMesh(callback : (Boolean)-> Unit){
        val groupId: Long = netMeshResponse?.info?.groupId ?: 0
        getNetMeshManger().disbandNetMesh(groupId, object : CommonCallbacks.CompletionCallbackWithParam<Int>{
            override fun onSuccess(t: Int?) {
                ToastUtils.showToast("disband net mesh success")
                callback.invoke(true)
            }

            override fun onFailure(error: IAutelCode, msg: String?) {
                ToastUtils.showToast("disband net mesh failed:$error $msg")
                callback.invoke(false)
            }
        })
    }

    private fun setAdapter() {
        adapter = NetMeshDemoAdapter()
        adapter.setOnClickListener {
            val nodeId = getMultiDeviceOperator().getDroneDeviceById(it)?.getNodeId()
            nodeId?.let { nId -> setControlMode(ControlMode.SINGLE, nId) }
        }
        adapter.dataList = dataList
        binding.listView.adapter = adapter
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun updateData() {
        dataList.clear()

        //飞机列表
        val drones = getMultiDeviceOperator().getDroneDevices()
        //本地遥控器
        val localRemoteDevice = getMultiDeviceOperator().getLocalRemoteDevice()
        //非本地遥控器
        val remotes =  getMultiDeviceOperator().getRemoteInfoList()

        dataList.addAll(drones)
        //dataList.add(localRemoteDevice)
        dataList.addAll(remotes)

        adapter.dataList = dataList
        adapter.notifyDataSetChanged()

        initInfoContent()
    }

}