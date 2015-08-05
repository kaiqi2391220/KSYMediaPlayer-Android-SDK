package com.ksy.media.player.util;

import java.io.Serializable;

public interface DRMRetrieverResponseHandler extends Serializable {

	void onSuccess(String version, String cek);

	void onFailure(int code, String response, Throwable e);
}
