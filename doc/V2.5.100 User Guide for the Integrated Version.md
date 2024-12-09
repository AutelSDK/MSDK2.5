# V2.5.100 Integrated SDK Usage Instructions

## Ⅰ. General Description

The MSDK **V2.5.100** is developed based on the **V2.5.2** (Mesh Edition) and adds compatibility support for the **V2.0** (Standalone Edition). However, due to differences in aircraft and remote controller firmware versions, as well as adjustments to MSDK interfaces and continuous iterations, this may lead to interface changes and compatibility issues. This document details these differences to serve as a reference for developers, aiding them in understanding and addressing potential problems.

#### 1. Essential Differences Between V2.5 and V2.0

- **2.5 Version**:
  
  - **Multi-Aircraft Mesh Support**: V2.5 supports networking operations with multiple flight devices. Devices are dynamically created after reporting through a device discovery service, allowing flexible network configuration and management.
  - **Device Lifecycle Management**: Aircraft devices in a network have a clear lifecycle, including creation, operation, and destruction phases. This enables better management and monitoring of each device's status within the system.

- **2.0 Version**:
  
  - **Single Aircraft Connection**: V2.0 only supports connections to a single flight device. The device is created during SDK initialization and directly connects to the remote controller after frequency pairing.
  - **No Device Lifecycle Management**: Since it supports only a single device, V2.0 lacks complex lifecycle management concepts. Once initialized, the device remains either connected or disconnected.

#### 2. Multi-Aircraft Firmware vs Single-Aircraft Firmware

Multi-aircraft firmware and single-aircraft firmware primarily refer to the firmware versions of the remote controller and aircraft:

- **A-Mesh Firmware**:
  
  - **Version Requirement**: Firmware versions 1.8.x and above support multi-aircraft networking features.
  - **Characteristics**: These firmware versions allow multiple flight devices and remote controllers to join the same network, enabling more complex operational scenarios. For example, one can set up a master remote controller and a relay aircraft, with other remote controllers acting as slave controllers and aircraft as ordinary node aircraft.

- **Single-Aircraft Firmware**:
  
  - **Version Requirement**: Firmware versions below 1.8.x are considered single-aircraft versions.
  - **Characteristics**: Only supports connection to a single flight device. The device is created during SDK initialization and directly connects to the remote controller after frequency pairing, without complex lifecycle management.

#### 3. A-Mesh Functionality

A-Mesh includes two modes: multi-aircraft (multi-aircraft multi-control) networking mode and single-aircraft (one-to-one control) mode. Both modes rely on device discovery ([Reference Documentation](https://developer.autelrobotics.cn/doc/v2.5/mobile_sdk/en/50/8), [Demo](https://tongyi.aliyun.com/qianwen/?spm=5176.2810346&code=sw31xf0id8&utm_content=se_1017929066&sessionId=5e0b82fa1ef44c6db4556b2e9f465468)).

#### 4. Aircraft Firmware Information Reference

| Model                       | Firmware Version | Description                 |
|:---------------------------:|:----------------:|:---------------------------:|
| Evo Max 4T/4N/4T Pro Series | 1.8.x and above  | Multi-Aircraft Mesh Version |
| Evo Max 4T/4N/4T Pro Series | Below 1.8.x      | Single-Aircraft Version     |
| EVO Lite                    | --               | Single-Aircraft Version     |
| Autel Alpha/Titan           | 1.8.x and above  | Multi-Aircraft Mesh Version |
| Autel Alpha/Titan           | Below 1.8.x      | Single-Aircraft Version     |

## Ⅱ. Development Guidelines

#### Scenario 1: Developing an APP Using MSDK V2.5.100 to Support Both Single-Aircraft and Multi-Aircraft Mesh Firmware

- **Development Reference**: Developers should refer to the [MSDK V2.5 Documentation](https://developer.autelrobotics.cn/doc/v2.5/mobile_sdk/en/00/1) for development.

- **Important Notes**: For certain interfaces, implementation details should follow the guidelines provided in the [MSDK V2.0 Documentation](https://developer.autelrobotics.cn/doc/v2/android_api_reference/en/10/1).

| Module                       | Single-Aircraft Firmware                                                                                | A-Mesh Firmware                                                                                                            | Description                            |
|:----------------------------:|:-------------------------------------------------------------------------------------------------------:|:--------------------------------------------------------------------------------------------------------------------------:|:--------------------------------------:|
| Waypoint Mission             | Supports only [Autel Binary Files](https://developer.autelrobotics.cn/doc/v2/mobile_sdk/en/50/5)        | Uses [KMZ Mission Files](https://developer.autelrobotics.cn/doc/v2.5/mobile_sdk/en/50/5)                                   | Compatible with others                 |
| Obstacle Avoidance Interface | **KeyObstacleAvoidance**                                                                                | **KeyObstacleAvoidActionSet**                                                                                              | Extended obstacle <br/>avoidance modes |
| AI Tracking Interface        | Separate interfaces for each camera, such as:<br/>**KeyTrackingEnter**<br/>**KeyInfraredTrackingEnter** | Unified interface with lens parameter specified<br/>**KeyIntelligentLockEnter**<br/>Added lensId in **AIDetectConfigBean** | Interface optimization                 |
| Tracking Target              | **TrackTargetRectBean**                                                                                 | Added **lensId** to **TrackTargetRectBean**                                                                                | Enhanced interface                     |
| Device Pairing               | Single-point pairing                                                                                    | Single-point pairing and multi-aircraft mesh                                                                               | Extended functionality                 |

    

#### Scenario 2: Upgrading MSDK from V2.0 to V2.5.100 in the APP

- **For Multi-Aircraft Mesh Firmware**: If your application uses multi-aircraft networking firmware, you should refer to the [MSDK V2.5 Development Documentation](https://developer.autelrobotics.cn/doc/v2.5/mobile_sdk/en/00/1) for development.

- **For Single-Aircraft Firmware**: Even with single-aircraft firmware, the following aspects still require adjustments:

| Module                                             | Single-Aircraft Firmware                                                                                                                                                         | A-Mesh Firmware                                                                                                                                                                    | Description                                                      |
|:--------------------------------------------------:|:--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------:|:----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------:|:----------------------------------------------------------------:|
| Upgrade Module                                     | Deprecated Interface                                                                                                                                                             | Use [New Interface](https://developer.autelrobotics.cn/doc/v2.5/mobile_sdk/en/50/12)                                                                                               | Multi-Aircraft Upgrade Support                                   |
| Camera：**KeyCameraDeviceInfo**                     | Parameter：**DeviceInfoBean**                                                                                                                                                     | Parameter：**CameraDeviceInfoBean**                                                                                                                                                 | Name Change                                                      |
| Download Listener                                  | **FileTransmissionListener**<br/>**FileTransmitListener**                                                                                                                        | **FileTransmitListener**                                                                                                                                                           | Interface Integration                                            |
| Aircraft Reporting<br/>**FlightControlStatusInfo** | **batteryNotInPlaceFlag** ：Int                                                                                                                                                   | **batteryNotInPlaceFlag**: **BatteryInPlaceEnum**                                                                                                                                  | Type Change                                                      |
| Player                                             | 1. com.autel.module_player.xxx<br/>2. com.autel.rtmp.publisher.IPublishListener<br/>3. com.autel.gb28181.IGB28181PublishListener<br/>4. com.autel.rtspserver.IRtspServerCallBack | 1. com.autel.player.xxx<br/>2. com.autel.publisher.IPublishListener<br/>3. com.autel.publisher.gb28181.IGB28181PublishListener<br/>4. com.autel.publisher.rtsp.IRtspServerCallBack | Package Name Change, Spelling Errors, and Exception Improvements |

## Ⅲ. Additional Notes

1. During the initialization of MSDK, the system automatically detects and determines whether the current firmware is for single-aircraft operation or multi-aircraft Mesh based on predefined firmware rules.

2. If the firmware version cannot be accurately determined, the working mode can be set during SDK initialization.

```Kotlin
val sdkInitCfg = SDKInitConfig().apply { 
    debug = false    
    storage = null   
    log = null       
    single = true  //force single mode
}
SDKManager.get().init(this.applicationContext, sdkInitCfg)
```

3. A-Mesh Interface [Reference Tutorial](https://developer.autelrobotics.cn/doc/v2.5/mobile_sdk/en/50/8)
- **Point-to-Point Pairing and Single-Aircraft Pairing**: Both use the same `AirLinkKey` pairing interface.
- **Entering Pairing Mode**: Double-click the battery switch to enter pairing mode. To enter Mesh mode, first press the aircraft button briefly, then hold it down for an extended period.
4. For any other questions regarding interface changes, you can consult at any time.
