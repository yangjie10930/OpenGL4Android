package com.joe.camera2recorddemo.OpenGL;


import java.util.Arrays;

/**
 * Created by Yj on 2017/10/30.
 * 变换的帮助类
 */

public class TransUtil {

	public static float[] textureCoords;

	/**
	 * 获得变换后的数据
	 * @param tc 				原始数据
	 * @param transformation	变化类型
	 * @return
	 */
	public static float[] getTransformationCo(float[] tc,final Transformation transformation) {
		textureCoords = tc;
		if (transformation.cropRect != null) {
			resolveCrop(transformation.cropRect.x, transformation.cropRect.y,
					transformation.cropRect.width, transformation.cropRect.height);
		} else {
			resolveCrop(Transformation.FULL_RECT.x, Transformation.FULL_RECT.y,
					Transformation.FULL_RECT.width, Transformation.FULL_RECT.height);
		}
		resolveFlip(transformation.flip);
		resolveRotate(transformation.rotation);
		return textureCoords;
	}

	private static void resolveCrop(float x, float y, float width, float height) {
		float minX = x;
		float minY = y;
		float maxX = minX + width;
		float maxY = minY + height;

		// left bottom
		textureCoords[0] = minX;
		textureCoords[1] = minY;
		// right bottom
		textureCoords[2] = maxX;
		textureCoords[3] = minY;
		// left top
		textureCoords[4] = minX;
		textureCoords[5] = maxY;
		// right top
		textureCoords[6] = maxX;
		textureCoords[7] = maxY;
	}

	private static void resolveFlip(int flip) {
		switch (flip) {
			case Transformation.FLIP_HORIZONTAL:
				swap(textureCoords, 0, 2);
				swap(textureCoords, 4, 6);
				break;
			case Transformation.FLIP_VERTICAL:
				swap(textureCoords, 1, 5);
				swap(textureCoords, 3, 7);
				break;
			case Transformation.FLIP_HORIZONTAL_VERTICAL:
				swap(textureCoords, 0, 2);
				swap(textureCoords, 4, 6);

				swap(textureCoords, 1, 5);
				swap(textureCoords, 3, 7);
				break;
			case Transformation.FLIP_NONE:
			default:
				break;
		}
	}

	private static void resolveRotate(int rotation) {
		float x, y;
		switch (rotation) {
			case Transformation.ROTATION_90:
				x = textureCoords[0];
				y = textureCoords[1];
				textureCoords[0] = textureCoords[4];
				textureCoords[1] = textureCoords[5];
				textureCoords[4] = textureCoords[6];
				textureCoords[5] = textureCoords[7];
				textureCoords[6] = textureCoords[2];
				textureCoords[7] = textureCoords[3];
				textureCoords[2] = x;
				textureCoords[3] = y;
				break;
			case Transformation.ROTATION_180:
				swap(textureCoords, 0, 6);
				swap(textureCoords, 1, 7);
				swap(textureCoords, 2, 4);
				swap(textureCoords, 3, 5);
				break;
			case Transformation.ROTATION_270:
				x = textureCoords[0];
				y = textureCoords[1];
				textureCoords[0] = textureCoords[2];
				textureCoords[1] = textureCoords[3];
				textureCoords[2] = textureCoords[6];
				textureCoords[3] = textureCoords[7];
				textureCoords[6] = textureCoords[4];
				textureCoords[7] = textureCoords[5];
				textureCoords[4] = x;
				textureCoords[5] = y;
				break;
			case Transformation.ROTATION_0:
			default:
				break;
		}
	}

	private static void swap(float[] arr, int index1, int index2) {
		float temp = arr[index1];
		arr[index1] = arr[index2];
		arr[index2] = temp;
	}
}
