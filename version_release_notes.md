# MSDK

### Changelog

### Version V2.5.8.3

- **Release Notes**: Fixed issues where some users encountered errors due to missing WebRTC dependencies. Also resolved conflicts arising from existing WebRTC dependencies in applications.

- **AAR Package**: `autel-msdk2-release_V2.5.8.3_with_webrtc.aar`

  If your app project already includes a dependency on WebRTC (e.g., `implementation 'io.github.webrtc-sdk:android:114.5735.02'`), please use `autel-msdk2-release_V2.5.8_need_add_webrtc.aar` instead.


### Version 2.5.06

Enable the YUV callback for listening to the drone video stream and support live streaming via WebRTC.

```js
app build.gradle 
packagingOptions {
    pickFirst 'lib/arm64-v8a/libjingle_peerconnection_so.so'
}

implementation 'io.github.webrtc-sdk:android:114.5735.02'
```

```js
mAutelPlayer = AutelPlayer(SDKConstants.STREAM_CHANNEL_16110)
mAutelPlayer?.setVideoInfoListener(object : IVideoStreamListener {
    override fun onVideoSizeChanged(p0: Int, p1: Int, p2: Int) {

    }

    override fun onVideoInfoCallback(p0: Int, p1: Int, p2: Int, p3: Int, p4: Int) {

    }

    override fun onFrameYuv(p0: ByteBuffer?, p1: Int, p2: Int, p3: Int) {

    }

    override fun onVideoErrorCallback(p0: Int, p1: Int, p2: String?) {

    }

})
mAutelPlayer!!.addVideoView(codecView)
mAutelPlayer!!.startPlayer()
```

