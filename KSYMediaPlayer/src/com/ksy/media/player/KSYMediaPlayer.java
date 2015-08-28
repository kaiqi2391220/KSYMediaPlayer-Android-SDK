package com.ksy.media.player;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;

import com.ksy.media.player.annotations.AccessedByNative;
import com.ksy.media.player.annotations.CalledByNative;
import com.ksy.media.player.option.AvFormatOption;
import com.ksy.media.player.pragma.DebugLog;
import com.ksy.media.player.util.Constants;
import com.ksy.media.player.util.IOUtils;

public final class KSYMediaPlayer extends BaseMediaPlayer {

	private final static String TAG = KSYMediaPlayer.class.getName();

	private static final int MEDIA_NOP = 0; // interface test message
	private static final int MEDIA_PREPARED = 1;
	private static final int MEDIA_PLAYBACK_COMPLETE = 2;
	private static final int MEDIA_BUFFERING_UPDATE = 3;
	private static final int MEDIA_SEEK_COMPLETE = 4;
	private static final int MEDIA_SET_VIDEO_SIZE = 5;
	private static final int MEDIA_TIMED_TEXT = 99;
	private static final int MEDIA_ERROR = 100;
	private static final int MEDIA_GET_DRM_KEY = 101;
	private static final int MEDIA_INFO = 200;

	protected static final int MEDIA_SET_VIDEO_SAR = 10001;

	@AccessedByNative
	private long mNativeMediaPlayer;

	@AccessedByNative
	private int mNativeSurfaceTexture;

	@AccessedByNative
	private int mListenerContext;

	private SurfaceHolder mSurfaceHolder;
	private EventHandler mEventHandler;
	private PowerManager.WakeLock mWakeLock = null;
	private boolean mScreenOnWhilePlaying;
	private boolean mStayAwake;

	private int mVideoWidth;
	private int mVideoHeight;
	private int mVideoSarNum;
	private int mVideoSarDen;

	private String mDataSource;
	private String mFFConcatContent;

	private static KSYLibLoader sLocalLibLoader = new KSYLibLoader() {

		@Override
		public void loadLibrary(String libName) throws UnsatisfiedLinkError,
				SecurityException {

			System.loadLibrary(libName);
		}
	};

	private static volatile boolean mIsLibLoaded = false;

	public static void loadLibrariesOnce(KSYLibLoader libLoader) {

		synchronized (KSYMediaPlayer.class) {
			if (!mIsLibLoaded) {
				// libLoader.loadLibrary("gnustl_shared");
				libLoader.loadLibrary("ksyffmpeg");
				libLoader.loadLibrary("ksyutil");
				libLoader.loadLibrary("ksysdl");
				libLoader.loadLibrary("ksyplayer");
				mIsLibLoaded = true;
			}
		}
	}

	private static volatile boolean mIsNativeInitialized = false;

	private static void initNativeOnce() {

		synchronized (KSYMediaPlayer.class) {
			if (!mIsNativeInitialized) {
				native_init();
				mIsNativeInitialized = true;
			}
		}
	}

	public KSYMediaPlayer() {

		this(sLocalLibLoader);
	}

	public KSYMediaPlayer(KSYLibLoader libLoader) {

		initPlayer(libLoader);
	}

	private void initPlayer(KSYLibLoader libLoader) {

		loadLibrariesOnce(libLoader);
		initNativeOnce();

		Looper looper;
		if ((looper = Looper.myLooper()) != null) {
			mEventHandler = new EventHandler(this, looper);
		} else if ((looper = Looper.getMainLooper()) != null) {
			mEventHandler = new EventHandler(this, looper);
		} else {
			mEventHandler = null;
		}

		native_setup(new WeakReference<KSYMediaPlayer>(this));
	}

	private native void _setVideoSurface(Surface surface);

	@Override
	public void setDisplay(SurfaceHolder sh) {

		mSurfaceHolder = sh;
		Surface surface;
		if (sh != null) {
			surface = sh.getSurface();
		} else {
			surface = null;
		}
		_setVideoSurface(surface);
		updateSurfaceScreenOn();
	}

	@Override
	public void setSurface(Surface surface) {

		if (mScreenOnWhilePlaying && surface != null) {
			DebugLog.w(TAG,
					"setScreenOnWhilePlaying(true) is ineffective for Surface");
		}
		mSurfaceHolder = null;
		_setVideoSurface(surface);
		updateSurfaceScreenOn();
	}

	@Override
	public void setDataSource(String path) throws IOException,
			IllegalArgumentException, SecurityException, IllegalStateException {

		mDataSource = path;
		_setDataSource(path, null, null);
	}

	private native void _setDataSource(String path, String[] keys,
			String[] values) throws IOException, IllegalArgumentException,
			SecurityException, IllegalStateException;

	@Override
	public void setDataSource(String path, Map<String, String> headers)
			throws IOException, IllegalArgumentException, SecurityException,
			IllegalStateException {
		StringBuffer buffer = new StringBuffer();
		if (headers != null && headers.size() > 0) {
			for (Entry<String, String> entry : headers.entrySet()) {
				buffer.append(entry.getKey().toString()).append(":")
						.append(entry.getValue().toString()).append("\r\n");
			}
		}
		mDataSource = path;
		_setDataSourceAndHeader(path, buffer.toString());
		Log.d("eflake", buffer.toString());
	}

	private native void _setDataSourceAndHeader(String path, String headers)
			throws IOException, IllegalArgumentException, SecurityException,
			IllegalStateException;

	@Override
	public void setCacheInPause(boolean useCacheInPause) throws IOException,
			IllegalArgumentException, SecurityException, IllegalStateException {
		_setCacheInPause(useCacheInPause);
	}

	private native void _setCacheInPause(boolean useCacheInPause)
			throws IOException, IllegalArgumentException, SecurityException,
			IllegalStateException;

	@Override
	public String getDataSource() {
		return mDataSource;
	}

	public void setDataSourceAsFFConcatContent(String ffConcatContent) {
		mFFConcatContent = ffConcatContent;
	}

	@Override
	public void prepareAsync() throws IllegalStateException {
		if (TextUtils.isEmpty(mFFConcatContent)) {
			_prepareAsync();
		} else {
			_prepareAsync();
		}
	}

	public native void _prepareAsync() throws IllegalStateException;

	@Override
	public void start() throws IllegalStateException {

		Log.e(Constants.LOG_TAG, "KSYMediaPlayer start()");
		stayAwake(true);
		_start();
	}

	private native void _start() throws IllegalStateException;

	@Override
	public void stop() throws IllegalStateException {

		stayAwake(false);
		_stop();
	}

	private native void _stop() throws IllegalStateException;

	@Override
	public void pause() throws IllegalStateException {

		stayAwake(false);
		_pause();
	}

	private native void _pause() throws IllegalStateException;

	@SuppressLint("Wakelock")
	@Override
	public void setWakeMode(Context context, int mode) {

		boolean washeld = false;
		if (mWakeLock != null) {
			if (mWakeLock.isHeld()) {
				washeld = true;
				mWakeLock.release();
			}
			mWakeLock = null;
		}

		PowerManager pm = (PowerManager) context
				.getSystemService(Context.POWER_SERVICE);
		mWakeLock = pm.newWakeLock(mode | PowerManager.ON_AFTER_RELEASE,
				KSYMediaPlayer.class.getName());
		mWakeLock.setReferenceCounted(false);
		if (washeld) {
			mWakeLock.acquire();
		}
	}

	@Override
	public void setScreenOnWhilePlaying(boolean screenOn) {

		if (mScreenOnWhilePlaying != screenOn) {
			if (screenOn && mSurfaceHolder == null) {
				DebugLog.w(TAG,
						"setScreenOnWhilePlaying(true) is ineffective without a SurfaceHolder");
			}
			mScreenOnWhilePlaying = screenOn;
			updateSurfaceScreenOn();
		}
	}

	@SuppressLint("Wakelock")
	private void stayAwake(boolean awake) {

		if (mWakeLock != null) {
			if (awake && !mWakeLock.isHeld()) {
				mWakeLock.acquire();
			} else if (!awake && mWakeLock.isHeld()) {
				mWakeLock.release();
			}
		}
		mStayAwake = awake;
		updateSurfaceScreenOn();
	}

	private void updateSurfaceScreenOn() {

		if (mSurfaceHolder != null) {
			mSurfaceHolder.setKeepScreenOn(mScreenOnWhilePlaying && mStayAwake);
		}
	}

	@Override
	public int getVideoWidth() {

		return mVideoWidth;
	}

	@Override
	public int getVideoHeight() {

		return mVideoHeight;
	}

	@Override
	public int getVideoSarNum() {

		return mVideoSarNum;
	}

	@Override
	public int getVideoSarDen() {

		return mVideoSarDen;
	}

	@Override
	public native boolean isPlaying();

	@Override
	public native void seekTo(long msec) throws IllegalStateException;

	@Override
	public native long getCurrentPosition();

	@Override
	public native long getDuration();

	@Override
	public void release() {

		stayAwake(false);
		updateSurfaceScreenOn();
		resetListeners();
		_release();
	}

	private native void _release();

	// TODO
	@Override
	public void reset() {

		stayAwake(false);
		_reset();
		// make sure none of the listeners get called anymore
		mEventHandler.removeCallbacksAndMessages(null);

		mVideoWidth = 0;
		mVideoHeight = 0;
	}

	private native void _reset();

	@Override
	public native void setVolume(float leftVolume, float rightVolume);

	@Override
	public MediaInfo getMediaInfo() {

		MediaInfo mediaInfo = new MediaInfo();
		mediaInfo.mMediaPlayerName = "ksyplayer";

		String videoCodecInfo = _getVideoCodecInfo();
		if (!TextUtils.isEmpty(videoCodecInfo)) {
			String nodes[] = videoCodecInfo.split(",");
			if (nodes.length >= 2) {
				mediaInfo.mVideoDecoder = nodes[0];
				mediaInfo.mVideoDecoderImpl = nodes[1];
			} else if (nodes.length >= 1) {
				mediaInfo.mVideoDecoder = nodes[0];
				mediaInfo.mVideoDecoderImpl = "";
			}
		}

		String audioCodecInfo = _getAudioCodecInfo();
		if (!TextUtils.isEmpty(audioCodecInfo)) {
			String nodes[] = audioCodecInfo.split(",");
			if (nodes.length >= 2) {
				mediaInfo.mAudioDecoder = nodes[0];
				mediaInfo.mAudioDecoderImpl = nodes[1];
			} else if (nodes.length >= 1) {
				mediaInfo.mAudioDecoder = nodes[0];
				mediaInfo.mAudioDecoderImpl = "";
			}
		}

		try {
			mediaInfo.mMeta = KSYMediaMeta.parse(_getMediaMeta());
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return mediaInfo;
	}

	private native String _getVideoCodecInfo();

	private native String _getAudioCodecInfo();

	public void setAvOption(AvFormatOption option) {

		setAvFormatOption(option.getName(), option.getValue());
	}

	public void setAvFormatOption(String name, String value) {

		_setAvFormatOption(name, value);
	}

	public void setAvCodecOption(String name, String value) {

		_setAvCodecOption(name, value);
	}

	public void setSwScaleOption(String name, String value) {

		_setSwScaleOption(name, value);
	}

	public void setOverlayFormat(int chromaFourCC) {

		_setOverlayFormat(chromaFourCC);
	}

	public void setFrameDrop(int frameDrop) {

		_setFrameDrop(frameDrop);
	}

	public void setMediaCodecEnabled(boolean enabled) {

		_setMediaCodecEnabled(enabled);
	}

	public void setOpenSLESEnabled(boolean enabled) {

		_setOpenSLESEnabled(enabled);
	}

	private native void _setAvFormatOption(String name, String value);

	private native void _setAvCodecOption(String name, String value);

	private native void _setSwScaleOption(String name, String value);

	private native void _setOverlayFormat(int chromaFourCC);

	private native void _setFrameDrop(int frameDrop);

	private native void _setMediaCodecEnabled(boolean enabled);

	private native void _setOpenSLESEnabled(boolean enabled);

	@Override
	public Bundle getMediaMeta() {

		return _getMediaMeta();
	}

	private native Bundle _getMediaMeta();

	public static String getColorFormatName(int mediaCodecColorFormat) {

		return _getColorFormatName(mediaCodecColorFormat);
	}

	private static native final String _getColorFormatName(
			int mediaCodecColorFormat);

	@Override
	public void setAudioStreamType(int streamtype) {

		// do nothing
	}

	private static native final void native_init();

	private native final void native_setup(Object IjkMediaPlayer_this);

	private native final void native_finalize();

	private native final void native_message_loop(Object IjkMediaPlayer_this);

	@Override
	protected void finalize() {

		native_finalize();
	}

	private static class EventHandler extends Handler {

		private final WeakReference<KSYMediaPlayer> mWeakPlayer;

		public EventHandler(KSYMediaPlayer mp, Looper looper) {

			super(looper);
			mWeakPlayer = new WeakReference<KSYMediaPlayer>(mp);
		}

		@Override
		public void handleMessage(Message msg) {

			KSYMediaPlayer player = mWeakPlayer.get();
			if (player == null || player.mNativeMediaPlayer == 0) {
				DebugLog.w(TAG,
						"IjkMediaPlayer went away with unhandled events");
				return;
			}

			switch (msg.what) {
			case MEDIA_PREPARED:
				player.notifyOnPrepared();
				return;

			case MEDIA_PLAYBACK_COMPLETE:
				player.notifyOnCompletion();
				player.stayAwake(false);
				return;

			case MEDIA_BUFFERING_UPDATE:
				long bufferPosition = msg.arg1;
				if (bufferPosition < 0) {
					bufferPosition = 0;
				}

				long percent = 0;
				long duration = player.getDuration();
				if (duration > 0) {
					percent = bufferPosition * 100 / duration;
				}
				if (percent >= 100) {
					percent = 100;
				}

				// DebugLog.efmt(TAG, "Buffer (%d%%) %d/%d", percent,
				// bufferPosition, duration);
				player.notifyOnBufferingUpdate((int) percent);
				return;

			case MEDIA_SEEK_COMPLETE:
				player.notifyOnSeekComplete();
				return;

			case MEDIA_SET_VIDEO_SIZE:
				player.mVideoWidth = msg.arg1;
				player.mVideoHeight = msg.arg2;
				player.notifyOnVideoSizeChanged(player.mVideoWidth,
						player.mVideoHeight, player.mVideoSarNum,
						player.mVideoSarDen);
				return;

			case MEDIA_ERROR:

				Log.e(Constants.LOG_TAG, "Error (" + msg.arg1 + "," + msg.arg2
						+ ")");
				if (!player.notifyOnError(msg.arg1, msg.arg2)) {
					player.notifyOnCompletion();
				}
				player.stayAwake(false);
				return;

			case MEDIA_INFO:
				if (msg.arg1 != MEDIA_INFO_VIDEO_TRACK_LAGGING) {
					Log.e(Constants.LOG_TAG, "===============Info (" + msg.arg1
							+ "," + msg.arg2 + ")");
				}
				player.notifyOnInfo(msg.arg1, msg.arg2);
				// No real default action so far.
				return;
			case MEDIA_TIMED_TEXT:
				// do nothing
				break;

			case MEDIA_NOP: // interface test message - ignore
				break;

			case MEDIA_SET_VIDEO_SAR:
				player.mVideoSarNum = msg.arg1;
				player.mVideoSarDen = msg.arg2;
				player.notifyOnVideoSizeChanged(player.mVideoWidth,
						player.mVideoHeight, player.mVideoSarNum,
						player.mVideoSarDen);
				break;

			case MEDIA_GET_DRM_KEY:
				Log.e(TAG, "MEDIA_GET_DRM_KEY");
				String version = (String) msg.obj;
				if (!TextUtils.isEmpty(version)) {
					player.notifyOnDRMRequired(msg.arg1, msg.arg2, version);
				} else {
					DebugLog.e(TAG, "version is null" + version);
				}
				break;

			// TODO 102 不处理，没有问题，状态改变时抛出
			default:
				DebugLog.e(TAG, "Unknown message type " + msg.what);
				return;
			}
		}
	}

	@CalledByNative
	private static void postEventFromNative(Object weakThiz, int what,
			int arg1, int arg2, Object obj) {

		if (weakThiz == null)
			return;

		@SuppressWarnings("rawtypes")
		KSYMediaPlayer mp = (KSYMediaPlayer) ((WeakReference) weakThiz).get();
		if (mp == null) {
			return;
		}

		if (what == MEDIA_INFO && arg1 == MEDIA_INFO_STARTED_AS_NEXT) {
			// this acquires the wakelock if needed, and sets the client side
			// state
			mp.start();
		}
		if (mp.mEventHandler != null) {
			Message m = mp.mEventHandler.obtainMessage(what, arg1, arg2, obj);
			mp.mEventHandler.sendMessage(m);
		}
	}

	private OnControlMessageListener mOnControlMessageListener;

	public void setOnControlMessageListener(OnControlMessageListener listener) {

		mOnControlMessageListener = listener;
	}

	public static interface OnControlMessageListener {

		public int onControlResolveSegmentCount();

		public String onControlResolveSegmentUrl(int segment);

		public String onControlResolveSegmentOfflineMrl(int segment);

		public int onControlResolveSegmentDuration(int segment);
	}

	@CalledByNative
	private static int onControlResolveSegmentCount(Object weakThiz) {

		DebugLog.ifmt(TAG, "onControlResolveSegmentCount");
		if (weakThiz == null || !(weakThiz instanceof WeakReference<?>))
			return -1;

		@SuppressWarnings("unchecked")
		WeakReference<KSYMediaPlayer> weakPlayer = (WeakReference<KSYMediaPlayer>) weakThiz;
		KSYMediaPlayer player = weakPlayer.get();
		if (player == null)
			return -1;

		OnControlMessageListener listener = player.mOnControlMessageListener;
		if (listener == null)
			return -1;

		return listener.onControlResolveSegmentCount();
	}

	@CalledByNative
	private static String onControlResolveSegmentUrl(Object weakThiz,
			int segment) {

		DebugLog.ifmt(TAG, "onControlResolveSegmentUrl %d", segment);
		if (weakThiz == null || !(weakThiz instanceof WeakReference<?>))
			return null;

		@SuppressWarnings("unchecked")
		WeakReference<KSYMediaPlayer> weakPlayer = (WeakReference<KSYMediaPlayer>) weakThiz;
		KSYMediaPlayer player = weakPlayer.get();
		if (player == null)
			return null;

		OnControlMessageListener listener = player.mOnControlMessageListener;
		if (listener == null)
			return null;

		return listener.onControlResolveSegmentUrl(segment);
	}

	@CalledByNative
	private static String onControlResolveSegmentOfflineMrl(Object weakThiz,
			int segment) {

		DebugLog.ifmt(TAG, "onControlResolveSegmentOfflineMrl %d", segment);
		if (weakThiz == null || !(weakThiz instanceof WeakReference<?>))
			return null;

		@SuppressWarnings("unchecked")
		WeakReference<KSYMediaPlayer> weakPlayer = (WeakReference<KSYMediaPlayer>) weakThiz;
		KSYMediaPlayer player = weakPlayer.get();
		if (player == null)
			return null;

		OnControlMessageListener listener = player.mOnControlMessageListener;
		if (listener == null)
			return null;

		return listener.onControlResolveSegmentOfflineMrl(segment);
	}

	@CalledByNative
	private static int onControlResolveSegmentDuration(Object weakThiz,
			int segment) {

		DebugLog.ifmt(TAG, "onControlResolveSegmentDuration %d", segment);
		if (weakThiz == null || !(weakThiz instanceof WeakReference<?>))
			return -1;

		@SuppressWarnings("unchecked")
		WeakReference<KSYMediaPlayer> weakPlayer = (WeakReference<KSYMediaPlayer>) weakThiz;
		KSYMediaPlayer player = weakPlayer.get();
		if (player == null)
			return -1;

		OnControlMessageListener listener = player.mOnControlMessageListener;
		if (listener == null)
			return -1;

		return listener.onControlResolveSegmentDuration(segment);
	}

	public static interface OnMediaCodecSelectListener {

		public String onMediaCodecSelect(IMediaPlayer mp, String mimeType,
				int profile, int level);
	}

	private OnMediaCodecSelectListener mOnMediaCodecSelectListener;

	public void setOnMediaCodecSelectListener(
			OnMediaCodecSelectListener listener) {

		mOnMediaCodecSelectListener = listener;
	}

	@Override
	public void resetListeners() {

		super.resetListeners();
		mOnMediaCodecSelectListener = null;
	}

	@CalledByNative
	private static String onSelectCodec(Object weakThiz, String mimeType,
			int profile, int level) {

		if (weakThiz == null || !(weakThiz instanceof WeakReference<?>))
			return null;

		@SuppressWarnings("unchecked")
		WeakReference<KSYMediaPlayer> weakPlayer = (WeakReference<KSYMediaPlayer>) weakThiz;
		KSYMediaPlayer player = weakPlayer.get();
		if (player == null)
			return null;

		OnMediaCodecSelectListener listener = player.mOnMediaCodecSelectListener;
		if (listener == null)
			listener = DefaultMediaCodecSelector.sInstance;

		return listener.onMediaCodecSelect(player, mimeType, profile, level);
	}

	public static class DefaultMediaCodecSelector implements
			OnMediaCodecSelectListener {

		public static DefaultMediaCodecSelector sInstance = new DefaultMediaCodecSelector();

		@Override
		@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
		public String onMediaCodecSelect(IMediaPlayer mp, String mimeType,
				int profile, int level) {

			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN)
				return null;

			if (TextUtils.isEmpty(mimeType))
				return null;

			Log.i(TAG, String.format(Locale.US,
					"onSelectCodec: mime=%s, profile=%d, level=%d", mimeType,
					profile, level));
			TreeMap<Integer, KSYMediaCodecInfo> candidateCodecList = new TreeMap<Integer, KSYMediaCodecInfo>();
			int numCodecs = MediaCodecList.getCodecCount();
			for (int i = 0; i < numCodecs; i++) {
				MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
				Log.d(TAG,
						String.format(Locale.US, "  found codec: %s",
								codecInfo.getName()));
				if (codecInfo.isEncoder())
					continue;

				String[] types = codecInfo.getSupportedTypes();
				if (types == null)
					continue;

				for (String type : types) {
					if (TextUtils.isEmpty(type))
						continue;

					Log.d(TAG, String.format(Locale.US, "    mime: %s", type));
					if (!type.equalsIgnoreCase(mimeType))
						continue;

					KSYMediaCodecInfo candidate = KSYMediaCodecInfo
							.setupCandidate(codecInfo, mimeType);
					if (candidate == null)
						continue;

					candidateCodecList.put(candidate.mRank, candidate);
					Log.i(TAG, String.format(Locale.US,
							"candidate codec: %s rank=%d", codecInfo.getName(),
							candidate.mRank));
					candidate.dumpProfileLevels(mimeType);
				}
			}

			Entry<Integer, KSYMediaCodecInfo> bestEntry = candidateCodecList
					.lastEntry();
			if (bestEntry == null)
				return null;

			KSYMediaCodecInfo bestCodec = bestEntry.getValue();
			if (bestCodec == null || bestCodec.mCodecInfo == null)
				return null;

			if (bestCodec.mRank < KSYMediaCodecInfo.RANK_LAST_CHANCE) {
				Log.w(TAG, String.format(Locale.US, "unaccetable codec: %s",
						bestCodec.mCodecInfo.getName()));
				return null;
			}

			Log.i(TAG, String.format(Locale.US, "selected codec: %s rank=%d",
					bestCodec.mCodecInfo.getName(), bestCodec.mRank));
			return bestCodec.mCodecInfo.getName();
		}
	}

	// P1 Added Interface
	@Override
	public void setAudioAmplify(float ratio) {

		if (ratio <= 0) {
			ratio = MEDIA_AUDIO_AMPLIFY_DEFAULT;
			Log.w(Constants.LOG_TAG, "unsupported audio amplify ratio :"
					+ ratio + ",replace the default size :"
					+ MEDIA_AUDIO_AMPLIFY_DEFAULT);
		}
		_setAudioAmplify(ratio);
	}

	@Override
	public void setVideoRate(float rate) {

		if (rate <= 0) {
			rate = MEDIA_VIDEO_RATE_DEFAULT;
			Log.w(Constants.LOG_TAG, "unsupported video rate :" + rate
					+ ",replace the default size :" + MEDIA_VIDEO_RATE_DEFAULT);
		}
		_setVideoRate(rate);
	}

	@Override
	public void getCurrentFrame(Bitmap bitmap) {

		if (bitmap != null) {
			_getCurrentFrame(bitmap);
		} else {
			Log.w(Constants.LOG_TAG,
					"get current failed, bitmap can not be null ");
		}
	}

	@Override
	public void setBufferSize(int size) {

		if (size <= 0) {
			size = MEDIA_BUFFERSIZE_DEFAULT;
			Log.w(Constants.LOG_TAG, "unsupported buffer size :" + size
					+ ",replace the default size :" + MEDIA_BUFFERSIZE_DEFAULT);
		}
		_setBufferSize(size);
	};

	@Override
	public void setAnalyseDuration(int duration) {

		if (duration <= 0) {
			Log.w(Constants.LOG_TAG, "unsupported analyse duration :"
					+ duration + ",replace the default size :"
					+ MEDIA_ANALYSE_DURATION_DEFAULT);
			duration = MEDIA_ANALYSE_DURATION_DEFAULT;
		}
		_setAnalyseDuration(duration);
	};

	@Override
	public void setDRMKey(String version, String key) {

		if (checkDRMKey(version, key)) {
			Log.i(Constants.LOG_TAG, " DRM  version :" + version + ",Cek:"
					+ key);
			_setDRMKey(version, key);
		} else {
			Log.w(Constants.LOG_TAG, "DRM failed with error");
		}
	}

	@Override
	public void setTimeout(int timeout) {

		if (timeout <= 0) {
			Log.w(Constants.LOG_TAG, "unsupported time out  :" + timeout
					+ ",replace the default time out :"
					+ IMediaPlayer.MEDIA_TIME_OUT_DEFAULT);
			timeout = IMediaPlayer.MEDIA_TIME_OUT_DEFAULT;
		}
		_setTimeout(timeout);
	}

	// need to implements
	private boolean checkDRMKey(String version, String key) {

		if (TextUtils.isEmpty(version) || TextUtils.isEmpty(key)) {
			Log.w(Constants.LOG_TAG,
					"DRM version & key can not be null or empty");
			return false;
		} else {
			return true;
		}
	}

	/**
	 * 设置缓存目录
	 * 
	 * @param cachedPath
	 *            必须为文件夹路径
	 */
	@Override
	public boolean setCachedDir(String cachedPath) {
		Log.d("lixp", "cachedPath = " + cachedPath);
		if (null == cachedPath || "".equals(cachedPath)) {
			Log.e(Constants.LOG_TAG,
					"the cached path is null , so the streaming cached function failure");
			return false;
		}
		File file = new File(cachedPath);
		if (file.isFile()) {
			Log.e(Constants.LOG_TAG,
					"the cached path must be a forder , so the streaming cached function failure");
			return false;
		}
		Log.d("lixp", "file.isFile() = " + file.isFile() + ">>!file.exists() ="
				+ !file.exists() + ">>!file.mkdirs()=" + !file.mkdirs());
		if (!file.exists()) {
			if (!file.mkdirs()) {// TODO
				Log.e(Constants.LOG_TAG,
						"the cached forder create fail , so the streaming cached function failure");
				return false;
			}
		} else {
			Log.e("lixp", "1064  !file.exists() .......");
		}

		Log.i(Constants.LOG_TAG,
				"the cached forder create success , streaming will cached with path :"
						+ cachedPath);
		_setCachedDir(cachedPath);
		return true;
	}

	/**
	 * 清空缓存文件夹 以及文件夹所有层级目录的文件，缓存文件夹会会重新创建
	 * 
	 * @param cachedPath
	 *            需要清空的缓存目录
	 * 
	 * @exception 文件夹不存在
	 *                ，路劲指向文件
	 */

	@Override
	public boolean clearCachedFiles(String cachedPath) {

		if (null == cachedPath || "".equals(cachedPath)) {
			Log.e(Constants.LOG_TAG, "the cached path is null , clear nothing");
			return false;
		}
		File file = new File(cachedPath);
		if (file.isFile()) {
			Log.e(Constants.LOG_TAG,
					"the cached path must be a forder , clear nothing");
			return false;
		}

		if (!file.exists()) {
			Log.e(Constants.LOG_TAG, "the cached path not exist, clear nothing");
			return false;
		}

		boolean success = IOUtils.deleteDir(new File(cachedPath));

		if (!success) {
			Log.e(Constants.LOG_TAG, "clear the cached path occur an problem !");
			return false;
		}

		if (!file.mkdirs()) {// TODO
			Log.e(Constants.LOG_TAG, "the cached forder recreate failed !");
			return true;
		}

		Log.e(Constants.LOG_TAG,
				"the cached forder clear success , recreate cached forder success");// TODO
		return true;
	}

	@Override
	public void setLowDelayEnabled(boolean ennable) {

		_setLowDelayEnabled(ennable);
	}

	private native void _setAudioAmplify(float ratio);

	private native void _setVideoRate(float rate);

	private native void _setAnalyseDuration(int duration);

	private native void _setBufferSize(int Size);

	private native void _getCurrentFrame(Bitmap bitmap);

	private native void _setDRMKey(String version, String key);

	private native void _setTimeout(int timeout);

	private native void _setCachedDir(String cachedPath);

	private native void _setLowDelayEnabled(boolean enabled);

	@Override
	public void setLogEnabled(boolean enable) {

	}

	@Override
	public boolean isPlayable() {

		return true;
	}

	@Override
	public void setKeepInBackground(boolean keepInBackground) {

	}

}
