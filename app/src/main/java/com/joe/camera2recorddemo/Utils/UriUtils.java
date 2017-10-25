package com.joe.camera2recorddemo.Utils;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

/**
 * Created by Yj on 2017/10/16.
 */

public class UriUtils {

	/**
	 * 获取URI的绝对路径
	 * @param context
	 * @param uri
	 * @return
	 */
	public static String getRealFilePath(Context context,final Uri uri) {
		if (null == uri) return null;
		final String scheme = uri.getScheme();
		String data = null;
		if (scheme == null) {
			Log.e("UriUtils", "scheme is null");
			data = uri.getPath();
		} else if (ContentResolver.SCHEME_FILE.equals(scheme)) {
			data = uri.getPath();
			Log.e("UriUtils", "SCHEME_FILE");
		} else if (ContentResolver.SCHEME_CONTENT.equals(scheme)) {
			data = GetPathFromUri4kitkat.getPath(context, uri);
		}
		return data;
	}
}
