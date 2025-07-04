# MSDK

### Changelog

### Version V2.5.100

- **Release Notes**: this version is a backward-compatible release, capable of supporting both 1.7 and 1.8 firmware simultaneously.
  Additionally, it supports the Evo Lite Enterprise, Evo MAX, Evo Alpha, and others.
- **Special Notes on Compatible Versions**:  [Refer to](https://developer.autelrobotics.cn/doc/v2.5/mobile_sdk/en/50/17)
- **PSDK Sample**: View PSDK related example code for the branch [develop_payload](https://github.com/AutelSDK/MSDK2.5/tree/develop_payload)

### Version V2.5.100_no_webrtc

Version V2.5.100_no_webrtc is tailored for customers whose apps depend on WebRTC and encounter conflicts with the WebRTC included in version V2.5.100.  
For these users, it is necessary to depend on WebRTC in the app's build.gradle file as follows: implementation rootProject.ext.deps.external.webrtc_sdk.

For detailed update information, please refer to the [official documentation](https://developer.autelrobotics.com/doc/v2.5/mobile_sdk/en/50/17).

=============================================================================================================================

### Version V2.5.8.4_with_webrtc

- **Release Notes**: Fixed issues where some users encountered errors due to missing WebRTC dependencies. Also resolved conflicts arising from existing WebRTC dependencies in applications.

```js
//need the below dependencies
implementation 'com.squareup.okhttp3:logging-interceptor:4.10.0'
api 'com.squareup.retrofit2:retrofit:2.9.0'
api 'com.squareup.retrofit2:adapter-rxjava3:2.9.0'
api 'com.squareup.retrofit2:adapter-rxjava3:2.9.0'
api 'com.squareup.retrofit2:converter-moshi:2.9.0'
api 'me.jessyan:retrofit-url-manager:1.4.0'
implementation 'com.squareup.okhttp3:okhttp-dnsoverhttps:4.9.3'
implementation 'com.squareup.retrofit2:converter-gson:2.9.0'

//SDKConstants path changed to path com.autel.sdk
SDKConstants changes to path com.autel.drone.sdk.SDKConstants
```

# Overview

The new version of the MSDK introduces networking capabilities, supporting the addition of multiple controllers (primary and secondary controllers), and multiple drones into a single network. After successful networking, there exists only one primary controller and one relay drone among all devices, while other controllers are secondary ones, and other drones are ordinary node drones.
A group is a business construct based on networking, where drones that join the same group are considered part of a group, allowing for group control. Drones not in a group operate independently, with the choice of control modes (single control, full control, group control) to perform various operations on the drones.

## Terminology

### Concepts

- **Networking**: Devices joining a network.
- **Group**: Drones already networked joining a group.
- **DeviceId**: Unique ID of a drone.
- **NodeId**: Node ID for this networking session.
- **Relay Drone**: Central node drone.
- **Primary Controller**: Controller initiating networking.
- **Secondary Controller**: Controller joining networking.
- **Single Control**: Select and control one drone.
- **Group Control**: Select and control a group.
- **Full Control**: Control all drones.
- **Watch Device**: Device capable of viewing video streams.

## Networking Roles and Permissions

### Role

- **Primary Controller**: Full permissions; cannot switch roles; can view the stream of controlled drones.
- **Secondary Controller**: Joystick disabled; cannot switch roles; can view the stream of controlled drones.
- **Relay Drone**: No role switching; can be replaced through a relay replacement process; can push streams to all controllers.
- **Leaf Drone**: Can switch to controlled drone; cannot push streams.
- **Controlled Drone**: Can switch to leaf drone; can push streams to all controllers.

## MSDK Networking Interfaces

### 1. DeviceManager

- **IMultiDeviceOperator Interface**
  - `getGroupMeshApi()`: Group operation interface.
  - `getNetMeshManager()`: Networking operation interface.
  - `isSingleControl()`: Whether in single control mode.
  - `isNetMesh()`: Whether it's a networking version.
  - `addControlChangeListener()`, `removeControlChangeListener()`: Control change listeners.
  - `addWatchChangeListener()`, `removeWatchChangeListener()`: Watch change listeners.
  - `getDroneDevices()`: List of drone devices.
  - `getControlMode()`: Control mode.
  - `hasGroupMesh()`: Existence of group information.
  - `getRemoteDevices()`: List of remote devices.
  - `getDroneDeviceById()`, `getDroneDeviceByNodeId()`: Get drone by ID or node ID.
  - `getLocalRemoteDevice()`: Local remote device info.
  - `getConnectedDeviceIds()`: IDs of connected devices.
  - `getDroneUpgradeDevices()`: Drone upgrade device list.
  - `generateKeyManagerList()`: Key manager list based on device ID list.
  - `performActionList()`: Execute multi-drone action interface.
  - `addDroneDevicesListener()`, `removeDroneDevicesListener()`: Drone device listeners.
  - `getCenterDroneDevice()`: Relay drone.
  - `getControlledDroneList()`, `getWatchedDroneList()`: Controlled and watched drone lists.
  - `isMainRC()`: Whether it's the primary RC.
  - `addNetMeshChangeListener()`, `removeNetMeshChangeListener()`: Networking change listeners.
  - `getGroupList()`: Group list.
  - `getSingleDeviceList()`: Single device list (not in a group).
  - `getActiveDroneAlbumPort()`, `getActiveFileServicePort()`: Ports for active drone album and file service.
  - `getActiveAlbumBaseUrl()`, `getActiveFileBaseUrl()`: Base URLs for active drone album and file service.

### 2. Networking Related Interface

- **INetMeshManager Interface**
  - `getAllMeshDeviceList()`: List of all mesh devices.
  - `getLocalRCName()`, `getMainRCName()`: Names of local and main RC.
  - `getMainRcWatchDrone()`: Main RC's watch drone.
  - `isNetMeshing()`, `isMeshDisband()`: Networking status.
  - `startNetMeshMatching()`, `completeNetMeshMatching()`: Start and complete networking.
  - `delNetMeshDevice()`: Remove mesh device.
  - `setCenterNode()`: Set center node.
  - `joinDeviceNetMesh()`, `disbandNetMesh()`, `quitNetMeshMatching()`: Join, disband, and quit networking.
  - `nameDeviceNetMeshMatching()`: Rename device in networking team.
  - `setWatchDevice()`: Switch stream in multi-control mode.
  - `setNetMeshStreamControl()`: Networking stream control settings.

### 3. Group Related Interface

- **IGroupMeshApi Interface**
  - `createGroup()`, `addDroneToGroup()`, `delDroneFromGroup()`: Create, add, and delete drones from groups.
  - `disbandGroup()`: Disband a group.
  - `switchControlMode()`: Switch control mode.
  - `changeGroupName()`: Change group name.
  - `setGroupDroneLeader()`: Set leader drone.

## Device Listeners

- **Drone Event Listener**
  - `addDroneDevicesListener()`, `removeDroneDevicesListener()`: Add and remove drone device listeners.

## Drone Capabilities Provided

- **IAutelDroneDevice Interface**
  - Provides access to various drone capabilities such as Album Manager, Waypoint Mission Manager, Track Mission Manager, Camera Ability Set Manager, Gimbal type, Player Manager, File Service Manager, RTK Manager, connection status, flight readiness, drone type, control status, node ID, group ID, IP address, and ports for services.

## MSDK Networking Function Example

- **Main Controller**
  - `startNetMeshMatching()`: Begin pairing.
  - `delNetMeshDevice()`: Remove device.
  - `setCenterNode()`: Set relay.
  - `completeNetMeshMatching()`: Complete networking.
  - `disbandNetMesh()`: Disband networking.
  - `nameDeviceNetMeshMatching()`: Rename networking device.
- **Secondary Controller**
  - `joinDeviceNetMesh()`: Join networking.
  - `quitNetMeshMatching()`: Quit networking.

## Networking Listeners

- **Device Status Listener**
  - `onDroneCreate()`, `onDroneChangedListener()`, `onMainServiceValid()`, `onCameraAbilityFetchListener()`, `onSDKErrorListener()`, `onDroneDestroy()`: Device lifecycle change listeners.
- **Control Change Listener**
  - `onControlChange()`: Control change listener during networking.
- **Watch Change Listener**
  - `onWatchChange()`: Watch change listener; supports up to two drones' videos simultaneously playing using AutelPlayerManager.
