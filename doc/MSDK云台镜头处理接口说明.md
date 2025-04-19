# MSDK云台镜头相关接口说明

## 常用接口

### 1. 获取当前无人机的云台类型
```kotlin
interface IAutelDroneDevice {
  fun getGimbalDeviceType(): GimbalTypeEnum
}
```

### 2. 获取云台支持镜头列表

```kotlin
interface ICameraAbilitySetManager {
  fun getLensList(type: GimbalTypeEnum? = null): List<LensTypeEnum>?
}

enum class LensTypeEnum {
  /** 变焦*/
  Zoom,
  /** 长焦*/
  TeleZoom,
  /** 红外*/
  Thermal,
  /**变焦红外*/
  TeleThermal,
  /** 广角*/
  WideAngle,
  /** 夜视*/
  NightVision;
}
```

### 3. 获取镜头的ID
```kotlin
interface ICameraAbilitySetManager {
  fun getLenId(lensType: LensTypeEnum, type: GimbalTypeEnum? = null): Int?
}
```

### 3. 获取镜头视频流的端口
```kotlin
object SDKConstants {
  /** 
   * 红外或者变焦红外镜头的端口 
   */ 
  fun getInfraredChannelId(): Int

  /** 
   * 夜视镜头的端口 
   */ 
  fun getNightVisionChannelId(): Int

  /** 
   * 变焦镜头的端口 
   */ 
  fun getZoomChancelId(): Int

  /** 
   * 广角镜头的端口 
   */ 
  fun getWideAngleChannelId(): Int

  /** 
   * 长焦镜头的端口 
   */ 
  fun getTelZoomChancelId(): Int
}
```

## 使用示例

根据镜头获取视频流的端口，并把视频播放出来
```kotlin
//1. 获取无人机设备
val droneDevice = DeviceManager.getDeviceManager().getFirstDroneDevice()
//2. 获取云台型号
val gimbalType = droneDevice?.getGimbalDeviceType()
//3. 获取镜头列表
val lensList = droneDevice?.getCameraAbilitySetManger()?.getLensList(gimbalType)

//4. 根据镜头类型获取视频的端口
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

//5. 播放视频流
val codecView = createAutelCodecView()
uiBinding.layoutView.addView(codecView)
autelPlayer = AutelPlayer(channelId)
autelPlayer?.addVideoView(codecView)
```

完整的示例代码，可以参考[MuiltCodecFragment](../debugtools/src/main/java/com/autel/sdk/debugtools/fragment/MuiltCodecFragment.kt)