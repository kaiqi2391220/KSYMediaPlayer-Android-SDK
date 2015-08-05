package com.ksy.media.widget;

import android.graphics.Bitmap;
import android.view.View;

/**
 * Created by xf on 2014/9/14.
 */
public class SurfaceControl {

    public static Bitmap screenshot(View view) {
        view.setDrawingCacheEnabled(true);
        view.buildDrawingCache();
        Bitmap bmp = view.getDrawingCache();
        return bmp;
    }
}
