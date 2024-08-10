# MSDK

### Changelog

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

