#KSYMediaPlayer-Android-SDK
---
##SDK支持
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

##功能及API说明
###DRM说明
客户如果采用直播的形式推流到金山云流媒体服务器，那么可以选择是否采用我们的DRM加密服务。播放器端基于自定义对称加解密算法对关键帧进行解密，播放器支持三种形式获取解密Key，更加安全可靠。

由于请求DRM解密Key的URL参数中存在我们的鉴权签名信息（signature），出于安全性考虑，用户可以自行选择以下三种模型之一来获取Key。
无论使用哪种模型，app开发者都需要实现IDRMRetriverRequest两个接口⽅法，这些接⼝⽅法都在线程池执行，用户无需开启新的线程。

1.app提供AK，sk完成签名,并且提供完整取Key URL的方式（不安全，建议仅测试时使用）
![](http://who.kssws.ks-cdn.com/drm01.png)


```
	//这种本地签名，⽣生成完整url的⽅方式 ，需要APP 保存AK ，SK ，计算签名⽆无需⽤用户实现,也不需要通过⽤用户server
	//接⼝口调⽤用⽅方法：
	public DRMKey retriveDRMKeyFromAppServer(String cekVersion,String cekUrl){
	return null;
	}
	
	//返回null就可以
	public DRMFullURL retriveDRMFullUrl(String cekVersion,String cekUrl) throwsException{
	DRMFullURLfullURL= new DRMFullURL("@AK" , "@SK" ,cekUrl,
	cekVersion);
	return fullURL;
	}

```

2.app提供不存放AK,SK，app从appserver获取drm完整url，之后再从ksyserver获取drm
![](http://who.kssws.ks-cdn.com/drm02.png)

```

	//app携带cekURL和cekVersion信息去appserver获取完整drm路劲之后，再请求ksyserver获取DRM，开发者只需要实现
	public DRMFullURL retriveDRMFullUrl(String cekVersion,String cekUrl)throws Exception{
	//发送http请求，从appserver获取drmurl，之后将url拆解成DRMFullURL对象
	returnfullURL;
	}

	public DRMKey retriveDRMKeyFromAppServer(String cekVersion,String cekUrl){
	return null;
	}

```

3.app提供不存放AK,SK，由appserver访问ksyserver获取drm后返回给APP
![](http://who.kssws.ks-cdn.com/drm03.png)

```

	//app携带cekURL和cekVersion信息发送给appserver，appserver生成完整url之后去ksy服务器获取完整drm之后返回给app，开发者只需要实现
	public DRMFullURL retriveDRMFullUrl(String cekVersion,String cekUrl)throws Exception{
	DRMFullURL fullURL= new DRMFullURL(cekUrl,cekVersion)
	return fullURL;
	//直接将参数返回即可
	}
	
	public DRMKey retriveDRMKeyFromAppServer(String cekVersion,String cekUrl){
	//开发者需要在这⾥里构建
	http请求appserver获取drm
	return DRMKey;
	}

```

KSY Server获取解密KEY的接口如下所示：

####请求解密Key接口
**GET NewCek**

*此GET为cek-url创建一个cek密钥串(16进制), 并返回此cek*

**HTTP/1.1  GET /xiaoyi/NewCek?signature=Wq4VjoEnqbldJe6HfRyTkRavcRg=&accesskeyid=8oN7siZgTOSFHft0cXTg&expire=1710333224&nonce=466cc944cdd58b9d&cekurl=rtmp://live.ksyun.com/xiaoyi/ipc1
Host: drm.ksyun.com </br>
Date: Wed, 28 Oct 2009 22:32:00 GMT**


*请求参数描述:</br>
signature:使用ks3颁发的AK,SK对数据进⾏行签名</br>
accesskeyid:ks3颁发的AK.</br>
expire:对应于本次请求的超时时间. 本地计算签名超时时间当前UTC时间 + 3600秒</br>
nonce:相当于本次请求的UID.本地计算签名随机数为expire</br>
resource:camera对应的URL</br>
version:DRM版本号，播放器提供</br>*


**成功响应示例:</br>
HTTP/1.1 200 OK </br>
Content-Length: length </br>
Content-Type: text/plain </br>
Date: Wed, 28 Oct 2009 22:32:00 GMT </br>
Server: Nginx </br>**

```

	<?xml version="1.0" encoding="UTF-8"?> 
	<Result> 
	<Cek>c1cbf122374d55bba69595f0f58d5c80</Cek> 
	</Result>

```


**错误响应示例:
HTTP/1.1 400 Bad Request </br>
Content-Length: length </br>
Content-Type: text/plain </br>
Date: Wed, 28 Oct 2009 22:32:00 GMT </br>
Server: Nginx </br>**

```

	<?xml version="1.0" encoding="UTF-8"?>
	<Error> 
	<Code>BadParams</Code>
	<Message>Expire must be a number</Message>
	</Error>

```

app开发者需要实现IDRMRetriverRequest两个接口方法，两个接口方法都在线程池执行，无需开启新的线程

```

	public abstract DRMFullURL retriveDRMFullUrl(String cekVersion,StringcekUrl)throws Exception;
	//获取drm的url完整路径，如上url实例
	//方案1：如果直接返回
	  DRMFullURLfullURL=newDRMFullURL("@AK" ,"@SK" ,cekUrl,cekVersion);
    //默认采⽤本地⽣成url⽅式直接从ksy务器获取drm 	
	//retriveDRMKeyFromAppServer无需实现
	
	//方案2：如果AKSK存放在appserver，那么这个接口方法开发者需要执行http请求访问appserver获取drm的完整路径，并且将url的各项参数拆解成DRMFullURL，returnDRMFullURLfullURL=newDRMFullURL(kSCDRMHostPort,customerName,
	  drmMethod,signature,accessKey,expire,noce,cekUrl,version);
	//retriveDRMKeyFromAppServer接口方法就无需实现。

	//方案3：如果直接返回DRMFullURLfullURL= new DRMFullURL(cekUrl,cekVersion);那么就采用第三种方式，在appserver生成完整url，并且由appserver访问ksyserver获取drm之后直接返回给app，retriveDRMKeyFromAppServer
	接口就需要开发者实现从appserver获取drm接口

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
