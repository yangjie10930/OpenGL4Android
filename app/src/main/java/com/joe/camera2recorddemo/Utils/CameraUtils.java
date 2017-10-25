package com.joe.camera2recorddemo.Utils;

import android.app.Activity;
import android.hardware.Camera;
import android.view.Surface;

/**
 * Created by Administrator on 2017/10/10.
 */

public class CameraUtils {
	/**
	 * 解决前置摄像头上下颠倒的问题
	 * @param cameraId
	 * @param camera
	 */
	public static void setCameraDisplayOrientation(Activity activity,int cameraId, Camera camera) {
		Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
		Camera.getCameraInfo(cameraId, info);
		int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
		int degrees = 0;
		switch (rotation) {
			case Surface.ROTATION_0:
				degrees = 90;
				break;
			case Surface.ROTATION_90:
				degrees = 180;
				break;
			case Surface.ROTATION_180:
				degrees = 270;
				break;
			case Surface.ROTATION_270:
				degrees = 0;
				break;
		}

		int result;
		if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
			result = (info.orientation + degrees) % 360;
			result = (360 - result) % 360;  // compensate the mirror
		} else {  // back-facing
			result = (info.orientation - degrees + 360) % 360;
		}
		camera.setDisplayOrientation(result);
	}
}
