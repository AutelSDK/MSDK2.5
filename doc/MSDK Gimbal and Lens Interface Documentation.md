# MSDK Gimbal and Lens Interface Documentation

## Common Interfaces

### 1. Get Current Drone's Gimbal Type
```kotlin
interface IAutelDroneDevice {
  fun getGimbalDeviceType(): GimbalTypeEnum
}
```

### 2. Get Supported Lens List for Gimbal

```kotlin
interface ICameraAbilitySetManager {
  fun getLensList(type: GimbalTypeEnum? = null): List<LensTypeEnum>?
}

enum class LensTypeEnum {
  /** Zoom lens */
  Zoom,
  /** Telephoto zoom */
  TeleZoom,
  /** Thermal */
  Thermal,
  /** Telephoto thermal */
  TeleThermal,
  /** Wide angle */
  WideAngle,
  /** Night vision */
  NightVision;
}
```

### 3. Get Lens ID
```kotlin
interface ICameraAbilitySetManager {
  fun getLenId(lensType: LensTypeEnum, type: GimbalTypeEnum? = null): Int?
}
```

### 4. Get Lens Video Stream Channel Id
```kotlin
object SDKConstants {
  /** 
   * Channel id for infrared infrared lens 
   */ 
  fun getInfraredChannelId(): Int

  /** 
   * Channel id for night vision lens 
   */ 
  fun getNightVisionChannelId(): Int

  /** 
   * Channel id for zoom lens 
   */ 
  fun getZoomChancelId(): Int

  /** 
   * Channel id for wide angle lens 
   */ 
  fun getWideAngleChannelId(): Int

  /** 
   * Channel id for telezoom lens 
   */ 
  fun getTelZoomChancelId(): Int
}
```

## Usage Example

Get video stream channel id based on lens type and play the video
```kotlin
//1. Get drone device
val droneDevice = DeviceManager.getDeviceManager().getFirstDroneDevice()
//2. Get gimbal type
val gimbalType = droneDevice?.getGimbalDeviceType()
//3. Get lens list
val lensList = droneDevice?.getCameraAbilitySetManger()?.getLensList(gimbalType)

//4. Get video channel id based on lens type
val channelId = when(lensType) {
  LensTypeEnum.WideAngle -> SDKConstants.getWideAngleChannelId()
  LensTypeEnum.NightVision -> SDKConstants.getNightVisionChannelId()
  LensTypeEnum.TeleZoom -> SDKConstants.getTelZoomChancelId()
  LensTypeEnum.Thermal, LensTypeEnum.TeleThermal -> SDKConstants.getInfraredChannelId()
  LensTypeEnum.Zoom -> if (lensList?.contains(LensTypeEnum.WideAngle) == true) {
    SDKConstants.getTelZoomChancelId()
  } else {
    SDKConstants.getZoomChancelId()
  }
  else -> SDKConstants.getZoomChancelId()
}

//5. Play video stream
val codecView = createAutelCodecView()
uiBinding.layoutView.addView(codecView)
autelPlayer = AutelPlayer(channelId)
autelPlayer?.addVideoView(codecView)
```

For complete example code, please refer to [MuiltCodecFragment](../debugtools/src/main/java/com/autel/sdk/debugtools/fragment/MuiltCodecFragment.kt) 