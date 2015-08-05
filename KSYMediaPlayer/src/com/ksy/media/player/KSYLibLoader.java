package com.ksy.media.player;

public interface KSYLibLoader {
    public void loadLibrary(String libName) throws UnsatisfiedLinkError,
            SecurityException;
}
