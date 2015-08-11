#KSYMediaPlayer-Android-SDK
---
##SDK支持说明
目前播放器SDK支持的流媒体传输协议有：

* RTMP，HTTP，HLS及RTSP(RTP,SDP)

解码基于FFMPEG，音视频格式支持列表如下（以下仅列出常见格式）

* MP4，3GP，FLV，TS/TP，RMVB ，MKV，M4V，AVI，WMV ，MKV

##SDK使用说明 

###结构
SDK包含三个工程，其中

* KSYMediaPlayer- 播放器核心Library库
* KSYMediaWidget- 播放器UI-Library库
* KSYMediaDemo- SDK demo app

其中KSYMediaPlayer/libs/目录下，是不同指令集CPU对应的播放器底层so包，分为：

* libksyffmpeg.so
* libksyplayer.so
* libksyrtmp.so
* libksyutil.so

###集成

根据用户的需求，可以选择两种方式集成：

* 如果仅需要播放器核心库，不需要UI及上层逻辑，那么仅需引入播放器核心库KsyMediaPlayer，其使用方式类似原生Android的MediaPlayer，具体接口文档请参考[javadoc](http://ks3.ksyun.com/doc/index.html)

* 如果需要播放器及对应UI，用户需要引入播放器核心库KsyMediaPlayer以及播放器UI库KsyMediaWidget两个library project。在自己的Acitivity中，使用com.ksy.media.widget.MediaPlayerView，实现MediaPlayerView.PlayerViewCallback回调即可，具体代码请参考KSYMediaDemo

###Manifest权限申明

```

	<uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.WAKE_LOCK" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/> 

```

###错误码对应表
<table>
  <tr>
    <th>错误码</th>
    <th>错误类型</th>
    <th>描述</th>
  </tr>
  <tr>
    <td>10000</td>
    <td>ERROR_UNKNOWN</td>
    <td>未知错误</td>
  </tr>
  <tr>
    <td>10001</td>
    <td>ERROR_IO</td>
    <td>IO错误</td>
  </tr>
  <tr>
    <td>10002</td>
    <td>ERROR_TIMEOUT</td>
    <td>请求超时</td>
  </tr>
 <tr>
    <td>10003</td>
    <td>ERROR_UNSUPPORT</td>
    <td>不支持的格式</td>
  </tr>
 <tr>
    <td>10004</td>
    <td>ERROR_NOFILE</td>
    <td>文件不存在</td>
  </tr>
 <tr>
    <td>10005</td>
    <td>ERROR_SEEKUNSUPPORT</td>
    <td>当前不支持seek</td>
  </tr>
 <tr>
    <td>10006</td>
    <td>ERROR_SEEKUNREACHABLE</td>
    <td>当前seek不可达</td>
  </tr>
 <tr>
    <td>10007</td>
    <td>ERROR_DRM</td>
    <td>DRM出错</td>
  </tr>
 <tr>
    <td>10008</td>
    <td>ERROR_MEM</td>
    <td>内存溢出</td>
  </tr> 
<tr>
    <td>10009</td>
    <td>ERROR_WRONGPARAM</td>
    <td>参数错误</td>
  </tr>
</table>
