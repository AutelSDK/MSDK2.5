# V2.5.100融合SDK使用说明

## 一. 总体说明

      MSDK **V2.5.100** 是在  **V2.5.2**（组网版）的基础上开发的，新增了对 **V2.0**（单机版）的兼容性支持。然而，由于飞机和遥控器固件版本的不同，以及 MSDK 接口的调整和持续迭代，可能会导致接口变化和兼容性问题。本文档详细列出了这些差异，旨在为不同开发者提供参考，帮助他们更好地理解和应对可能出现的问题。

#### 1. V2.5与V2.0本质区别

-  **V2.5 版本**：
  
  - **多机组网支持**：V2.5 支持多个飞行设备的组网操作。设备通过设备发现服务上报并动态创建，动态网络配置和管理。
  - **设备生命周期管理**：组网中的飞机设备具有明确的生命周期，包括创建、运行和销毁等阶段。这使得系统能够更好地管理和监控每个设备的状态。

- **V2.0 版本**：
  
  - **单一飞机连接**：V2.0 仅支持单个飞行设备的连接。设备在 SDK 初始化时即创建，并在对频后直接与遥控器建立连接。
  - **无设备生命周期管理**：由于只支持单一设备，V2.0 中的设备没有复杂的生命周期管理概念，设备一旦初始化要么连接状态，要么断开连接。

#### 2. 多机固件和单机固件

    多机固件和单机固件主要指的是遥控器和飞机配套的固件版本：

- **多机组网固件**：
  
  - **版本要求**：版本1.8.x及以上的固件版本支持多机组网功能。
  - **特性**：这些固件版本允许多个飞行设备和多个遥控器加入同一个网络，实现更复杂的操作场景。例如，可以设置一个主控遥控器和一个中继飞机，并且其他遥控器作为从控遥控器，飞机作为普通节点飞机。

- **单机固件**：
  
  - **版本要求**：低于1.8.x的固件版本为单机版本。
  - **特性**：仅支持单一飞行设备连接，设备在SDK初始化时创建，并在对频后直接与遥控器建立连接，没有复杂的生命周期管理。

#### 3. 组网功能

    组网包括多机（多机多控）组网模式和单机（一机一控）模式，者两种模式都是基于设备发现([参考文档](https://developer.autelrobotics.cn/doc/v2.5/mobile_sdk/en/50/8)、[Demo]())。

#### 4. 机型固件信息参考

| 机型                     | 固件版本    | 说明     |
|:----------------------:|:-------:|:------:|
| Evo Max 4T/4N/4T Pro系列 | 1.8及以上  | 多机组网版本 |
| Evo Max 4T/4N/4T Pro系列 | 1.8以下版本 | 单机版本   |
| EVO Lite               | --      | 单机版本   |
| Autel Alpha/Titan      | 1.8及以上  | 多机组网版本 |
| 1.8及以上                 | 1.8以下版本 | 单机版本   |

## 二. 开发细则

  **场景一：使用 MSDK V2.5.100 开发APP同时支持单机和多机固件**

- 需要参考 MSDK V2.5的[开发文档](https://developer.autelrobotics.cn/doc/v2.5/mobile_sdk/en/00/1)进行开发
- 注意事项：以下接口需要按照MSDK V2.0[开发文档](https://developer.autelrobotics.cn/doc/v2/android_api_reference/en/10/1)的实现

| 模块     | 单机固件                                                                          | 多机固件                                                                        | 说明      |
|:------:|:-----------------------------------------------------------------------------:|:---------------------------------------------------------------------------:|:-------:|
| 航线任务   | 仅支持[autel二进制文件](https://developer.autelrobotics.cn/doc/v2/mobile_sdk/en/50/5) | 使用KMZ[任务文件](https://developer.autelrobotics.cn/doc/v2.5/mobile_sdk/en/50/5) | 兼容主流文件  |
| 避障接口   | KeyObstacleAvoidance                                                          | KeyObstacleAvoidActionSet                                                   | 扩展了避障模式 |
| AI追踪接口 | 各镜头独立接口 ，如：<br/>KeyTrackingEnter<br/>KeyInfraredTrackingEnter                 | 统一接口，参数指定镜头<br/>KeyIntelligentLockEnter<br/>AIDetectConfigBean增加lensId      | 接口优化    |
| 追踪目标   | TrackTargetRectBean                                                           | TrackTargetRectBean增加lensId                                                 | 接口完善    |
| 对频     | 对频                                                                            | 单点对频，多机组网                                                                   | 扩展      |

    

**场景二：APP将MSDK从 V2.0 升级到V2.5.100**

- 如果是多机固件需要参考 MSDK V2.5 的[开发文档](https://developer.autelrobotics.cn/doc/v2.5/mobile_sdk/en/00/1)进行开发

- 如果是单机固件，以下内容仍需要调整

| 模块                               | 单机固件                                                                                                                                                                             | 多机固件                                                                                                                                                                               | 说明                     |
|:--------------------------------:|:--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------:|:----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------:|:----------------------:|
| 升级模块                             | 就接口已不支持                                                                                                                                                                          | 使用[新接口](https://developer.autelrobotics.cn/doc/v2.5/mobile_sdk/en/50/12)                                                                                                           | 扩展多机升级                 |
| 相机：KeyCameraDeviceInfo           | 参数：DeviceInfoBean                                                                                                                                                                | 参数：CameraDeviceInfoBean                                                                                                                                                            | 名称变更                   |
| 下载监听                             | FileTransmissionListener<br/>FileTransmitListener                                                                                                                                | FileTransmitListener                                                                                                                                                               | 接口整合                   |
| 飞机上报<br/>FlightControlStatusInfo | batteryNotInPlaceFlag ：Int                                                                                                                                                       | batteryNotInPlaceFlag: BatteryInPlaceEnum                                                                                                                                          | 类型变更                   |
| 播放器                              | 1. com.autel.module_player.xxx<br/>2. com.autel.rtmp.publisher.IPublishListener<br/>3. com.autel.gb28181.IGB28181PublishListener<br/>4. com.autel.rtspserver.IRtspServerCallBack | 1. com.autel.player.xxx<br/>2. com.autel.publisher.IPublishListener<br/>3. com.autel.publisher.gb28181.IGB28181PublishListener<br/>4. com.autel.publisher.rtsp.IRtspServerCallBack | 包名变更<br/>拼写错误<br/>异常完善 |

## 三. 附加说明

1. MSDK会在初始化的时候会根据固件规则，自动判断当前是单机固件还是组网固件

2. 如果无法准确判断固件版本，SDK初始化可以设置其工作模式

```Kotlin
val sdkInitCfg = SDKInitConfig().apply { 
    debug = false    
    storage = null   
    log = null       
    single = true  //force single mode
}
SDKManager.get().init(this.applicationContext, sdkInitCfg)
```

3. 组网接口[参考教程](https://developer.autelrobotics.cn/doc/v2.5/mobile_sdk/en/50/8)，点对点对频和单机对频使用同一AirLinkKey对频接口；双击电池开关进入对频，先短按后长按飞机进入组网模式

4. 其它接口变更问题，可以随时咨询
