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

# =============== 基础保留规则 ===============
-keepattributes *Annotation*, Signature, InnerClasses, SourceFile, LineNumberTable

# =============== SDK核心API保护 ===============
-keep class com.autel.** { *; }
-keep class autel.** { *; }
-keep class com.autel.AutelMediaKit
-keep class com.autel.CodecType
-keep class com.autel.RawFrame

# =============== 模块特定保护 ===============
# 媒体中心
-keep class com.autel.video.VideoSource { *; }
-keep class com.autel.video.VideoSource$NativeDataListener { *; }
-keep class com.autel.gb28181.** { *; }
-keep class com.autel.player.** { *; }
-keep class com.autel.videorecord.** { *; }

# 协议
-keep class autel.protocol.** { *; }
-keep class com.autel.drone.sdk.pbprotocol.** { *; }
-keep class com.autel.drone.sdk.pbprotocol.interaction.msg.BaseProtoMsg { *; }

# 基础库
-keep class com.autel.internal.mission.** { *; }
-keep class com.autel.lib.jniHelper.** { *; }
-keep class com.autel.drone.sdk.libbase.error.** { *; }
-keep class com.autel.drone.sdk.SDKConfig { *; }
-keep class com.autel.drone.sdk.SDKConfig$Companion { *; }

# 云服务
-keep class com.autel.cloud.aiservice.bean.** { *; }
-keep class com.autel.cloud.http.base.** { *; }

# 暴露接口
-keep class com.autel.drone.sdk.vmodelx.** { *; }
-keep class com.autel.drone.sdk.vmodelx.device.IAutelDroneListener { *; }

# =============== JNI/NDK相关 ===============
-keepclasseswithmembernames class * {
    native <methods>;
}

# =============== 序列化类保护 ===============
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# =============== 枚举保护 ===============
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# =============== 资源保护 ===============
-keep class **.R$* { *; }
-keepclassmembers class **.R$* {
    public static <fields>;
}

# =============== 第三方库规则 ===============
# Gson
-keep class com.google.gson.** { *; }
-keep class com.google.gson.stream.** { *; }

# Retrofit
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# OkHttp
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Netty
-keep class io.netty.** { *; }

# Protobuf
-keep class com.google.protobuf.** { *; }

# WebRTC
-keep class org.webrtc.** { *; }

# XStream
-keep class com.thoughtworks.xstream.** { *; }

# Room
-keep class androidx.room.** { *; }

# MQTT
-keep class org.eclipse.paho.client.mqttv3.** { *; }

# =============== 模块重打包规则 ===============
# 防止模块间混淆后类名冲突
-repackageclasses com.autel.sdk.proguard