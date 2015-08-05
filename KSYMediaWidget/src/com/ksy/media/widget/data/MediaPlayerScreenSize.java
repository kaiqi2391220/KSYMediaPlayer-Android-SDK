package com.ksy.media.widget.data;


public enum MediaPlayerScreenSize {

    BIG(MediaPlayerScreenSize.VIDEO_SCREEN_SIZE_BIG,"16:9"), SMALL(MediaPlayerScreenSize.VIDEO_SCREEN_SIZE_SMALL,"4:3");
    
    public static final int VIDEO_SCREEN_SIZE_BIG = 1;
    public static final int VIDEO_SCREEN_SIZE_SMALL = 2;
    
    private MediaPlayerScreenSize(int flag, String name) {
        this.flag = flag;
        this.name = name;
    }
    
    private int flag = VIDEO_SCREEN_SIZE_BIG;
    private String name;
    
    public int getFlag() {
        return flag;
    }
    public String getName() {
        return name;
    }
    
    public static MediaPlayerScreenSize getQualityNameByFlag(int flag){
        switch (flag) {
		case VIDEO_SCREEN_SIZE_BIG:
			return BIG;
		case VIDEO_SCREEN_SIZE_SMALL:
			return SMALL;
		}
        return null;
        
    }
    
}
