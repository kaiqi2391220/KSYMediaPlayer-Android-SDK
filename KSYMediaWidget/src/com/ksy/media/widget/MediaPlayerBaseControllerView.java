package com.ksy.media.widget;

import android.content.Context;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.OnDoubleTapListener;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.ksy.media.widget.data.MediaPlayerScreenSize;
import com.ksy.media.widget.data.MediaPlayerUtils;
import com.ksy.media.widget.data.MediaPlayerVideoQuality;

public abstract class MediaPlayerBaseControllerView extends FrameLayout {

	private volatile boolean mNeedGesture = false;
	private volatile boolean mNeedGestureLight = false;
	private volatile boolean mNeedGestureVolume = false;
	private volatile boolean mNeedGestureSeek = false;

	private volatile boolean mEnnableTicker = true;
	private volatile boolean mIsTickerStarted = false;

	protected static final int HIDE_TIMEOUT_DEFAULT = 3000;
	protected static final int TICKER_INTERVAL_DEFAULT = 1000;
	protected static final int MAX_VIDEO_PROGRESS = 1000;

	protected static final int MSG_SHOW = 0x10;
	protected static final int MSG_HIDE = 0x11;
	protected static final int MSG_TICKE = 0x12;

	private static final double RADIUS_SLOP = Math.PI * 1 / 4;
	private static final int GESTURE_NONE = 0x00;
	private static final int GESTURE_LIGHT = 0x01;
	private static final int GESTURE_VOLUME = 0x02;
	private static final int GESTURE_SEEK = 0x03;
	private volatile int mCurrentGesture = GESTURE_NONE;

	protected MediaPlayerVideoQuality mCurrentQuality = MediaPlayerVideoQuality.HD;// 默认
	protected MediaPlayerScreenSize mCurrentScreenSize = MediaPlayerScreenSize.BIG;

	protected LayoutInflater mLayoutInflater;
	protected Window mHostWindow; // TODO 亮度调节
	protected WindowManager.LayoutParams mHostWindowLayoutParams;

	protected MediaPlayerController mMediaPlayerController;
	protected GestureDetector mGestureDetector;

	protected volatile boolean mVideoProgressTrackingTouch = false;

	protected boolean mDeviceNavigationBarExist = false;

	protected volatile boolean mScreenLock = false;
	protected MediaPlayerScreenSizePopupView mScreenPopup; //屏幕尺寸
	
	// TODO 亮度调节
//	protected MediaPlayerBrightView mWidgetLightView;
	protected MediaPlayerControllerBrightView mControllerBrightView; // 新添加亮度调节
	// protected MediaPlayerVolumeView mWidgetVolumeView; //声音调节
	protected MediaPlayerControllerVolumeView mWidgetVolumeControl; // 最新的
	protected MediaPlayerSeekView mWidgetSeekView;

	public MediaPlayerBaseControllerView(Context context, AttributeSet attrs,
			int defStyle) {

		super(context, attrs, defStyle);
		init();
	}

	public MediaPlayerBaseControllerView(Context context, AttributeSet attrs) {

		super(context, attrs);
		init();
	}

	public MediaPlayerBaseControllerView(Context context) {

		super(context);
		init();
	}

	private void startTimerTicker() {

		if (mIsTickerStarted)
			return;
		mIsTickerStarted = true;
		mHandler.removeMessages(MSG_TICKE);
		mHandler.sendEmptyMessage(MSG_TICKE);
	}

	private void stopTimerTicker() {

		if (!mIsTickerStarted)
			return;
		mIsTickerStarted = false;
		mHandler.removeMessages(MSG_TICKE);
	}

	@Override
	protected void onFinishInflate() {

		super.onFinishInflate();
		initViews();
		initListeners();
	}

	private void hideGestureView() {

		if (mNeedGesture) {
			/*if (mWidgetLightView != null && mWidgetLightView.isShowing()) {
				mWidgetLightView.hide(true);
			}*/
			
			/*
			 * if (mWidgetVolumeView != null && mWidgetVolumeView.isShowing()) {
			 * mWidgetVolumeView.hide(true); }
			 */
			if (mWidgetSeekView != null && mWidgetSeekView.isShowing())
				mWidgetSeekView.hide(true);
		}
	}

	public void show() {

		show(HIDE_TIMEOUT_DEFAULT);
	}

	public void show(int timeout) {

		mHandler.sendEmptyMessage(MSG_SHOW);
		mHandler.removeMessages(MSG_HIDE);
		if (timeout > 0) {
			Message msgHide = mHandler.obtainMessage(MSG_HIDE);
			mHandler.sendMessageDelayed(msgHide, timeout);
		}
	}

	public void hide() {

		mHandler.sendEmptyMessage(MSG_HIDE);
	}

	public void toggle() {

		if (isShowing()) {
			hide();
		} else {
			if (!mMediaPlayerController.isPlaying()) {
				show(0);
			} else {
				show();
			}
		}

	}

	public boolean isShowing() {

		if (getVisibility() == View.VISIBLE)
			return true;
		return false;
	}

	public void setNeedGestureDetector(boolean need) {

		this.mNeedGesture = need;
	}

	public void setNeedGestureAction(boolean needLightGesture,
			boolean needVolumeGesture, boolean needSeekGesture) {

		this.mNeedGestureLight = needLightGesture;
		this.mNeedGestureVolume = needVolumeGesture;
		this.mNeedGestureSeek = needSeekGesture;
	}

	public void setNeedTicker(boolean need) {

		this.mEnnableTicker = need;
	}

	//TODO
	public void setMediaPlayerController(MediaPlayerController mediaPlayerController) {
       
		mMediaPlayerController = mediaPlayerController;
		
		mScreenPopup = new MediaPlayerScreenSizePopupView(getContext(), mMediaPlayerController);
	}
	
	// TODO
	public void setHostWindow(Window window) {

		if (window != null) {
			mHostWindow = window;
			mHostWindowLayoutParams = window.getAttributes();
		}
	}

	public void setDeviceNavigationBarExist(boolean deviceNavigationBarExist) {

		mDeviceNavigationBarExist = deviceNavigationBarExist;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {

		final int action = event.getAction();

		switch (action) {
		case MotionEvent.ACTION_DOWN:
			if (mNeedGesture && !mScreenLock) {
				if (mNeedGestureSeek) {
					if (mWidgetSeekView != null)
						mWidgetSeekView.onGestureSeekBegin(
								mMediaPlayerController.getCurrentPosition(),
								mMediaPlayerController.getDuration());
				}
			}
			break;
		case MotionEvent.ACTION_MOVE:
			if (isShowing() && !mScreenLock) {
				show();
			}
			break;
		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_CANCEL:
			if (mNeedGesture && !mScreenLock) {
				if (mCurrentGesture == GESTURE_LIGHT) {
					if (mNeedGestureLight) {
						/*if (mWidgetLightView != null)
							mWidgetLightView.onGestureLightFinish();*/
					}
				} else if (mCurrentGesture == GESTURE_VOLUME) {
					if (mNeedGestureVolume) {
						/*
						 * if (mWidgetVolumeView != null)
						 * mWidgetVolumeView.onGestureVolumeFinish();
						 */

						if (mWidgetVolumeControl != null) {

						}
					}
				} else if (mCurrentGesture == GESTURE_SEEK) {
					if (mNeedGestureSeek) {
						if (mWidgetSeekView != null) {
							long seekPosition = mWidgetSeekView
									.onGestureSeekFinish();
							if (seekPosition >= 0
									&& seekPosition <= mMediaPlayerController
											.getDuration()) {
								mMediaPlayerController.seekTo(seekPosition);
								// mMediaPlayerController.start();
							}
						}
					}
				}
				mCurrentGesture = GESTURE_NONE;
			}
			break;
		default:
			break;
		}

		// 是否需要执行手势操作
		if (mNeedGesture) {
			if (mGestureDetector != null)
				mGestureDetector.onTouchEvent(event);
		}

		return true;
	}
			

	private void init() {

		mLayoutInflater = LayoutInflater.from(getContext());
		mGestureDetector = new GestureDetector(getContext(),
				new GestureDetector.OnGestureListener() {

					@Override
					public boolean onSingleTapUp(MotionEvent e) {

						if (mCurrentGesture == GESTURE_NONE) {
							toggle();
						}
						return false;
					}

					@Override
					public void onShowPress(MotionEvent e) {

					}

					@Override
					public boolean onScroll(MotionEvent e1, MotionEvent e2,
							float distanceX, float distanceY) {

						if (e1 == null || e2 == null || mScreenLock) {
							return false;
						}

						float oldX = e1.getX();
						final double distance = Math.sqrt(Math
								.pow(distanceX, 2) + Math.pow(distanceY, 2));
						int selfWidth = getMeasuredWidth();
						final double radius = distanceY / distance;

						// 当角度值大于设置值时,当做垂直方向处理,反之当做水平方向处理 TODO
						if (Math.abs(radius) > RADIUS_SLOP) {
							// Log.d("lixp",
							// "MediaPlayerBaseControllerView oldX =" + oldX +
							// ">>mCurrentGesture =" + mCurrentGesture +
							// ">>>mNeedGestureVolume=" + mNeedGestureVolume);
							// 处理声音
							if (oldX > selfWidth / 2) {
								if (!mNeedGestureVolume)
									return false;
								if (mCurrentGesture == GESTURE_NONE
										|| mCurrentGesture == GESTURE_VOLUME) {
									mCurrentGesture = GESTURE_VOLUME;
									if (!isShowing())
										show();
									/*if (mWidgetLightView != null)
										mWidgetLightView.hide(true);*/
									if (mWidgetSeekView != null)
										mWidgetSeekView.hide(true);
									AudioManager audioManager = (AudioManager) getContext()
											.getSystemService(
													Context.AUDIO_SERVICE);
									float totalVolumeDistance = getMeasuredHeight();
									if (totalVolumeDistance <= 0)
										totalVolumeDistance = MediaPlayerUtils
												.getRealDisplayHeight(mHostWindow);
									/*
									 * if (mWidgetVolumeView != null) { //TODO
									 * 声音控制
									 * mWidgetVolumeView.onGestureVolumeChange
									 * (distanceY, totalVolumeDistance / 4,
									 * audioManager); }
									 */

									if (mWidgetVolumeControl != null) { // 声音控制
//										Log.d("lixp", "351 basecontrol mWidgetVolumeControl .......");
										mWidgetVolumeControl
												.onGestureVolumeChange(
														distanceY,
														totalVolumeDistance / 4,
														audioManager);
									}

								}
							}
							// 处理亮度
							else {
								if (!mNeedGestureLight)
									return false;
								if (mCurrentGesture == GESTURE_NONE
										|| mCurrentGesture == GESTURE_LIGHT) {
									mCurrentGesture = GESTURE_LIGHT;
									if (!isShowing())
										show();
									// if (mWidgetVolumeView != null) {
									// mWidgetVolumeView.hide(true);
									// }
									if (mWidgetSeekView != null)
										mWidgetSeekView.hide(true);
									float totalLightDistance = getMeasuredHeight();
									if (totalLightDistance <= 0) {
										totalLightDistance = MediaPlayerUtils
												.getRealDisplayHeight(mHostWindow);
									}
									if (mControllerBrightView != null) {
										// mWidgetLightView.onGestureLightChange(distanceY,
										// totalLightDistance / 4, mHostWindow);

										mControllerBrightView
												.onGestureLightChange(distanceY, mHostWindow);
									}
								}
							}
						}
						// 处理视频进度
						else {
							if (!mNeedGestureSeek)
								return false;
							if (mCurrentGesture == GESTURE_NONE
									|| mCurrentGesture == GESTURE_SEEK) {
								mCurrentGesture = GESTURE_SEEK;
								if (!isShowing())
									show();
								// if (mWidgetVolumeView != null) {
								// mWidgetVolumeView.hide(true);
								// }
								/*if (mWidgetLightView != null)
									mWidgetLightView.hide(true);*/
								
								float totalSeekDistance = getMeasuredWidth();
								if (totalSeekDistance <= 0)
									totalSeekDistance = MediaPlayerUtils
											.getRealDisplayWidth(mHostWindow);
								if (mWidgetSeekView != null)
									mWidgetSeekView.onGestureSeekChange(
											-distanceX, totalSeekDistance);
							}
						}
						return false;
					}

					@Override
					public void onLongPress(MotionEvent e) {

					}

					@Override
					public boolean onFling(MotionEvent e1, MotionEvent e2,
							float velocityX, float velocityY) {

						return false;
					}

					@Override
					public boolean onDown(MotionEvent e) {

						return false;
					}
				});

		mGestureDetector.setOnDoubleTapListener(new OnDoubleTapListener() {

			@Override
			public boolean onSingleTapConfirmed(MotionEvent e) {

				return false;
			}

			@Override
			public boolean onDoubleTapEvent(MotionEvent e) {

				return false;
			}

			@Override
			public boolean onDoubleTap(MotionEvent e) {

				if (mScreenLock)
					return false;
				if (mMediaPlayerController.isPlaying()) {
					mMediaPlayerController.pause();
				} else {
					mMediaPlayerController.start();
				}
				return true;
			}
		});
		
	}

	// TODO
	protected Handler mHandler = new Handler() {
		@Override
		public void handleMessage(android.os.Message msg) {

			switch (msg.what) {
			case MSG_SHOW:
				startTimerTicker();
				setVisibility(View.VISIBLE);
				onShow();
				break;
			case MSG_HIDE:
				stopTimerTicker();
				hideGestureView();
				setVisibility(View.GONE);
				// onHide();
				break;
			case MSG_TICKE:
				if (mEnnableTicker) {
					onTimerTicker();
				}
				sendEmptyMessageDelayed(MSG_TICKE, TICKER_INTERVAL_DEFAULT);
				break;
			default:
				break;
			}

		};
	};

	//TODO
	public void setMediaQuality(MediaPlayerVideoQuality quality) {

		this.mCurrentQuality = quality;
	}

	public MediaPlayerVideoQuality getQuality() {

		return this.mCurrentQuality;
	}
	
	public void setScreenSize(MediaPlayerScreenSize screensize) {
		this.mCurrentScreenSize = screensize;
	}
	
	public MediaPlayerScreenSize getScreenSize() {
		
		return this.mCurrentScreenSize;
	}

	abstract void initViews();

	abstract void initListeners();

	abstract void onShow();

	abstract void onHide();

	abstract void onTimerTicker();

	public interface MediaPlayerController extends IMediaPlayerControl {

		boolean supportQuality();

		boolean supportVolume();

		boolean playVideo(String url);

		int getPlayMode();

		void onRequestPlayMode(int requestPlayMode);

		void onBackPress(int playMode);

		void onControllerShow(int playMode);

		void onControllerHide(int playMode);

		void onRequestLockMode(boolean lockMode);

		void onVideoPreparing();

		void onMovieRatioChange(int screenSize);//屏幕切换

		void onMoviePlayRatioUp();

		void onMoviePlayRatioDown();

		void onMovieCrop();

		void onVolumeDown();

		void onVolumeUp();
	}

	public interface OnGuestureChangeListener {

		void onLightChanged();

		void onVolumeChanged();

		void onPlayProgressChanged();
	}
}
