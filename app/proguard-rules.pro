# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ===============================================================================
# MSDK (Mobile Software Development Kit) ProGuard Rules
# 汇总所有模块的混淆规则，保护SDK核心功能和API
# ===============================================================================

# =============== 基础保留规则 ===============
# 保护代码中的Annotation不被混淆
-keepattributes *Annotation*
# 避免混淆泛型, 这在JSON实体映射时非常重要
-keepattributes Signature
# 抛出异常时保留代码行号和源文件名
-keepattributes SourceFile,LineNumberTable
# 保护内部类
-keepattributes InnerClasses

# =============== Android系统相关 ===============
# 表示不混淆任何包含native方法的类的类名以及native方法名
-keepclasseswithmembernames class * {
    native <methods>;
}

# 表示不混淆枚举中的values()和valueOf()方法
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# 表示不混淆Parcelable实现类中的CREATOR字段
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# 保护Serializable类
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# 保留R下面的资源
-keep class **.R$* { *; }
-keepclassmembers class **.R$* {
    public static <fields>;
}

# =============== SDK核心API保护 ===============
# 保护所有Autel相关的类
-keep class com.autel.** { *; }
-keep class autel.** { *; }

# 媒体相关核心类
-keep class com.autel.AutelMediaKit
-keep class com.autel.CodecType
-keep class com.autel.RawFrame
-keep class com.autel.eventlog.** { *; }

# SDK配置和常量
-keep class com.autel.drone.sdk.SDKConfig { *; }
-keep class com.autel.drone.sdk.SDKConfig$Companion { *; }
-keepclassmembers class com.autel.drone.sdk.SDKConfig { *; }
-keep class com.autel.drone.sdk.SDKConstants { *; }

# 日志配置
-keep class com.autel.drone.sdk.LogCfg { *; }
-keep class com.autel.drone.sdk.LogCfg$Companion { *; }

# SDK日志模块
-keep class com.autel.sdk.log.** { *; }
-keep class com.autel.sdk.log.AutelLog { *; }

# 设备监听器
-keep class com.autel.drone.sdk.vmodelx.device.IAutelDroneListener

# 基础应用类
-keep class com.autel.AutelBaseApplication
-keepclassmembers class com.autel.AutelBaseApplication {
    public *;
}

# =============== 视频媒体模块 ===============
# 视频源相关
-keep class com.autel.video.VideoSource {
    void *(...);
    public static void StreamDataRecv(int, long, int, int, int, int, int, int, byte[]);
}
-keep class com.autel.video.VideoSource$NativeDataListener { *; }

# 播放器相关
-keep class com.autel.player.** { *; }
-keep class com.autel.player.Constants { *; }
-keep class com.autel.player.Util { *; }
-keep class com.autel.player.VideoDisplayType { *; }
-keep class com.autel.player.VideoType { *; }
-keep class com.autel.player.player.** { *; }
-keep class com.autel.player.codec.** { *; }

# 视频录制
-keep class com.autel.videorecord.** { *; }

# RTMP和RTSP
-keep class com.autel.rtmp.** { *; }
-keep class com.autel.rtspserver.** { *; }

# GB28181
-keep class com.autel.gb28181.** { *; }
-keep class com.autel.publisher.gb28181.IGB28181PublishListener { *; }

# 发布器基类
-keep class * extends com.autel.publisher.BasePublisher { *; }

# =============== 协议模块 ===============
# Protocol Buffer相关
-keep class autel.protocol.** { *; }
-keep class com.autel.drone.sdk.pbprotocol.** { *; }
-keep class com.autel.drone.sdk.pbprotocol.interaction.msg.BaseProtoMsg
-keepclassmembers class com.autel.drone.sdk.pbprotocol.interaction.msg.BaseProtoMsg {
    public *;
}
-keep class com.autel.drone.sdk.pbprotocol.constants.** { *; }
-keep class com.autel.drone.sdk.pbprotocol.convert.** { *; }
-keep class com.autel.drone.sdk.pbprotocol.interaction.** { *; }

# =============== 基础库模块 ===============
# 任务相关
-keep class com.autel.internal.mission.** { *; }
-keep class com.autel.lib.jniHelper.** { *; }
-keep class com.autel.msdk.lib.domain.model.** { *; }
-keep class com.autel.modelblib.lib.domain.model.** { *; }
-keep class com.autel.lib.model.mission.model.** { *; }
-keep class com.autel.sdk.mission.wp.** { *; }
-keep class com.autel.sdk.store.** { *; }

# 错误处理
-keep class com.autel.drone.sdk.libbase.error.** { *; }
-keep class com.autel.drone.sdk.libbase.callback.** { *; }

# DSP相关
-keep class com.autel.drone.sdk.libbase.common.dsp.** { *; }
-keepclassmembers class com.autel.drone.sdk.libbase.common.dsp.FileConstants {
    public void init(android.content.Context);
}

# 日志
-keep class com.autel.drone.sdk.log.** { *; }
-keep class com.autel.drone.sdk.store.** { *; }
-keep class com.autel.drone.sdk.http.** { *; }

# AISO和其他工具
-keep class com.autel.aiso.** { *; }
-keep class com.autel.bean.** { *; }
-keep class com.autel.kmzalgorithm.** { *; }

# Opus工具
-keep class com.autel.plugin.utils.OpusUtils { *; }
-keep class com.autel.plugin.utils.OpusUtils$Companion { *; }
-keepclassmembers class com.autel.plugin.utils.OpusUtils { *; }

# =============== VM模型和业务模块 ===============
# 核心VModel
-keep class com.autel.drone.sdk.vmodelx.** { *; }

# 任务模块
-keep class com.autel.drone.sdk.vmodelx.module.mission.** { *; }
-keep class com.autel.drone.sdk.vmodelx.module.mission.enums.** { *; }

# 升级模块
-keep class com.autel.drone.sdk.vmodelx.module.upgrade.** { *; }
-keep class com.autel.drone.sdk.vmodelx.module.upgrade.bean.** { *; }
-keep class com.autel.drone.sdk.vmodelx.module.upgrade.bean.ota.** { *; }
-keep class com.autel.drone.sdk.vmodelx.module.upgrade.bean.upload.** { *; }

# 权限模块
-keep class com.autel.drone.sdk.vmodelx.module.authority.bean.** { *; }

# 相机模块
-keep class com.autel.drone.sdk.vmodelx.module.camera.bean.** { *; }
-keep class com.autel.drone.sdk.vmodelx.module.camera.bean.Gimbal { *; }
-keep class com.autel.drone.sdk.vmodelx.module.camera.CameraJsonParser { *; }

# 机巢相关
-keep class com.autel.drone.sdk.vmodelx.nest.entity.** { *; }
-keep class com.autel.drone.sdk.vmodelx.external.** { *; }
-keepclassmembers class com.autel.nest.expose.CommandCenterManager {
    public *;
}

# 算法模块
-keep class com.autel.drone.sdk.algor.** { *; }

# 服务管理器
-keep class com.autel.drone.sdk.vmodelx.manager.RtmpServiceManager {
    void *(...);
    public void switchStream(int, java.lang.String);
    public void refershStream();
}
-keep class com.autel.drone.sdk.vmodelx.manager.GB28181ServiceManager {
    void *(...);
}

# =============== 云服务模块 ===============
# AI服务bean
-keep class com.autel.cloud.aiservice.bean.** { *; }

# HTTP相关
-keep class com.autel.cloud.http.base.** { *; }
-keep class com.autel.cloud.http.enum.** { *; }
-keep class com.autel.cloud.http.retrofit.result.** { *; }

# 提供者
-keep class com.autel.cloud.provider.** { *; }

# =============== 网络通信模块 ===============
# 网络相关
-keep class com.autel.AutelNet.** { *; }
-keep class com.autel.utils.** { *; }
-keep class com.autel.webrtc.** { *; }

# =============== 第三方库规则 ===============
# R8 ProGuard工具
-keep class com.android.tools.r8.** { *; }
-keep class com.android.tools.r8.graph.** { *; }
-keep class com.android.tools.r8.shaking.** { *; }

# Google相关
-keep class com.google.protobuf.** { *; }
-keep class com.google.gson.** { *; }
-keep class com.google.gson.stream.** { *; }
-keep class com.google.gson.annotations.SerializedName

# JSON解析
-keep class org.json.** { *; }

# XStream
-keep class com.thoughtworks.xstream.** { *; }

# Netty
-keep class de.** { *; }
-keep class io.netty.** { *; }

# WebRTC
-keep class org.webrtc.** { *; }

# OkHttp
-keep class okhttp.** { *; }
-keep class okhttp.internal.util.** { *; }

# 日志框架
-keep class com.autel.log.** { *; }

# Room数据库
-keep class androidx.room.** { *; }

# MQTT
-keep class org.eclipse.paho.client.mqttv3.internal.* { *; }
-keep class org.eclipse.paho.client.mqttv3.spi.* { *; }
-keep class org.eclipse.paho.client.mqttv3.** { *; }

# 主SDK重打包 (根级别)
-repackageclasses com.autel.sdk.proguard
