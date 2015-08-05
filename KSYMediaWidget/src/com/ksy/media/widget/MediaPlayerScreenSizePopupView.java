package com.ksy.media.widget;

import java.util.List;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.ksy.media.widget.MediaPlayerBaseControllerView.MediaPlayerController;
import com.ksy.media.widget.data.KsyConstants;
import com.ksy.media.widget.data.MediaPlayerScreenSize;
import com.ksy.mediaPlayer.widget.R;

/**
 * @description 屏幕尺寸弹出框
 * @author LIXIAOPENG
 *
 */
public class MediaPlayerScreenSizePopupView {

	private Context mContext;
	private PopupWindow mPopupWindow;
	private ListView mListView;

	private ScreenSizeAdapter mAdapter;
	private List<MediaPlayerScreenSize> mData;
	private Callback mCallback;

	private boolean isShowing = false;
	private MediaPlayerScreenSize mCurrentSeletedQuality;
	
	private MediaPlayerController mediaPlayerController;
	
	public MediaPlayerScreenSizePopupView(Context context) {
		this.mContext = context;
		
		init();
	}
	
	public MediaPlayerScreenSizePopupView(Context context, MediaPlayerController mMediaPlayerController) {
		this.mContext = context;
		this.mediaPlayerController = mMediaPlayerController;
		
		init();
	}

	private void init() {

		LayoutInflater inflater = LayoutInflater.from(mContext);
		View root ;
		
		if (KsyConstants.SCREEN_VIEW_COLOR == 1) { //蓝色  
			root = inflater.inflate(R.layout.blue_media_player_quality_popup_view,
					null);
		} else if (KsyConstants.SCREEN_VIEW_COLOR == 2) { //红色
			root = inflater.inflate(R.layout.red_media_player_quality_popup_view,
					null);
		} else if (KsyConstants.SCREEN_VIEW_COLOR == 3) { //黄色
			root = inflater.inflate(R.layout.orange_media_player_quality_popup_view,
					null);
		} else if (KsyConstants.SCREEN_VIEW_COLOR == 4) { //绿色
			root = inflater.inflate(R.layout.green_media_player_quality_popup_view,
					null);
		} else if (KsyConstants.SCREEN_VIEW_COLOR == 5) { //粉色
			root = inflater.inflate(R.layout.pink_media_player_quality_popup_view,
					null);
		}
		
		mListView = (ListView) root.findViewById(R.id.quality_list_view);
		mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				
				if (position == 0) {
					mediaPlayerController.onMovieRatioChange(0);
				} else if (position == 1) {
					mediaPlayerController.onMovieRatioChange(1);
				}
				
				mPopupWindow.dismiss();
				
				if (mCallback != null) {
					if (mData != null && mData.size() > 0) {
						MediaPlayerScreenSize quality = mData.get(position);
						if (quality != null) {
							mCallback.onQualitySelected(quality); //TODO callback
							
						}
					}
				}
			}
		});

		mAdapter = new ScreenSizeAdapter();
		mListView.setAdapter(mAdapter);

		mPopupWindow = new PopupWindow(mContext);
		mPopupWindow.setFocusable(true);
		mPopupWindow.setTouchable(true);
		mPopupWindow.setOutsideTouchable(true);
		mPopupWindow.setTouchInterceptor(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
					return true;
				}
				return false;
			}
		});
		mPopupWindow.setOnDismissListener(new OnDismissListener() {

			@Override
			public void onDismiss() {
				isShowing = false;
				if (mCallback != null)
					mCallback.onPopupViewDismiss();
			}
		});
		mPopupWindow.setContentView(root);

	}

	public void show(View anchor, List<MediaPlayerScreenSize> qualityList,
			MediaPlayerScreenSize curQuality, int x, int y, int width,
			int height) {

		this.mData = qualityList;
		this.mCurrentSeletedQuality = curQuality;
		mAdapter.notifyDataSetChanged();
		mPopupWindow.setWidth(width);
		mPopupWindow.setHeight(height);
		mPopupWindow.showAtLocation(anchor, Gravity.NO_GRAVITY, x, y);
		isShowing = true;

	}

	public void hide() {
		mPopupWindow.dismiss();
	}

	public boolean isShowing() {
		return isShowing;
	}

	public void setCallback(Callback callback) {
		mCallback = callback;
	}

	public Callback getCallback() {
		return mCallback;
	}

	public interface Callback {
		void onQualitySelected(MediaPlayerScreenSize screensize);

		void onPopupViewDismiss();
	}

	//TODO
	class ScreenSizeAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			if (mData != null)
				return mData.size();
			return 0;
		}

		@Override
		public Object getItem(int position) {
			return position;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			if (convertView == null) {
				convertView = new ScreenItemView(mContext);
			}

			ScreenItemView itemView = (ScreenItemView) convertView;

			MediaPlayerScreenSize quality = mData.get(position);
			itemView.initData(quality);

			return itemView;
		}
	}

	//TODO
	class ScreenItemView extends RelativeLayout {

		private TextView mScreenSizeTextView;

		public ScreenItemView(Context context, AttributeSet attrs, int defStyle) {
			super(context, attrs, defStyle);
			init(context);
		}

		public ScreenItemView(Context context, AttributeSet attrs) {
			super(context, attrs);
			init(context);
		}

		public ScreenItemView(Context context) {
			super(context);
			init(context);
		}

		private void init(Context context) {
			if (KsyConstants.SCREEN_VIEW_COLOR == 1) { //蓝色  
				inflate(context, R.layout.blue_media_player_screen_size_item, this);
			} else if (KsyConstants.SCREEN_VIEW_COLOR == 2) { //红色
				inflate(context, R.layout.red_media_player_screen_size_item, this);
			} else if (KsyConstants.SCREEN_VIEW_COLOR == 3) { //黄色
				inflate(context, R.layout.orange_media_player_screen_size_item, this);
			} else if (KsyConstants.SCREEN_VIEW_COLOR == 4) { //绿色
				inflate(context, R.layout.green_media_player_screen_size_item, this);
			} else if (KsyConstants.SCREEN_VIEW_COLOR == 5) { //粉色
				inflate(context, R.layout.pink_media_player_screen_size_item, this);
			}
			
//			inflate(context, R.layout.blue_media_player_screen_size_item, this);
			mScreenSizeTextView = (TextView) findViewById(R.id.screen_size_text_view);
		}

		public void initData(MediaPlayerScreenSize screensize) {
			mScreenSizeTextView.setText(screensize.getName());
			if (null != mCurrentSeletedQuality
					&& screensize == mCurrentSeletedQuality) {
				setEnabled(false);
				/*mQualityTextView.setTextColor(getResources().getColor(
						R.color.player_quality_text_selector));*/
				
			} else {
				setEnabled(true);
				// mQualityTextView.setTextColor(getResources().getColor(R.color.controller_base_textcolor));
			}
		}
	}

}



