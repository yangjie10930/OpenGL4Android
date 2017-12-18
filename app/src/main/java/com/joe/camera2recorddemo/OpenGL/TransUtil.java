package com.joe.camera2recorddemo.OpenGL;


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
		if (transformation.getCropRect() != null) {
			resolveCrop(transformation.getCropRect().x, transformation.getCropRect().y,
					transformation.getCropRect().width, transformation.getCropRect().height);
		} else {
			resolveCrop(Transformation.FULL_RECT.x, Transformation.FULL_RECT.y,
					Transformation.FULL_RECT.width, Transformation.FULL_RECT.height);
		}
		resolveFlip(transformation.getFlip());
		resolveRotate(transformation.getRotation());
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
			case 90:
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
			case 180:
				swap(textureCoords, 0, 6);
				swap(textureCoords, 1, 7);
				swap(textureCoords, 2, 4);
				swap(textureCoords, 3, 5);
				break;
			case 270:
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
			case 0:
			default:
				break;
		}
	}

//	/**
//	 * 缩放变换
//	 * @param vertices 		顶点坐标系
//	 * @param inputWidth 	输入宽度
//	 * @param inputHeight 	输入高度
//	 * @param outputWidth	输出宽度
//	 * @param outputHeight	输出高度
//	 * @param scaleType		缩放类型
//	 * @return
//	 */
//	public static float[] resolveScale(float[] vertices,int inputWidth, int inputHeight, int outputWidth, int outputHeight,
//							  int scaleType) {
//		if (scaleType == Transformation.SCALE_TYPE_FIT_XY) {
//			// The default is FIT_XY
//			return vertices;
//		}
//
//		// Note: scale type need to be implemented by adjusting
//		// the vertices (not textureCoords).
//		if (inputWidth * outputHeight == inputHeight * outputWidth) {
//			// Optional optimization: If input w/h aspect is the same as output's,
//			// there is no need to adjust vertices at all.
//			return vertices;
//		}
//
//		float inputAspect = inputWidth / (float) inputHeight;
//		float outputAspect = outputWidth / (float) outputHeight;
//
//		if (scaleType == Transformation.SCALE_TYPE_CENTER_CROP) {
//			if (inputAspect < outputAspect) {
//				float heightRatio = outputAspect / inputAspect;
//				vertices[1] *= heightRatio;
//				vertices[3] *= heightRatio;
//				vertices[5] *= heightRatio;
//				vertices[7] *= heightRatio;
//			} else {
//				float widthRatio = inputAspect / outputAspect;
//				vertices[0] *= widthRatio;
//				vertices[2] *= widthRatio;
//				vertices[4] *= widthRatio;
//				vertices[6] *= widthRatio;
//			}
//		} else if (scaleType == Transformation.SCALE_TYPE_CENTER_INSIDE) {
//			if (inputAspect < outputAspect) {
//				float widthRatio = inputAspect / outputAspect;
//				vertices[0] *= widthRatio;
//				vertices[2] *= widthRatio;
//				vertices[4] *= widthRatio;
//				vertices[6] *= widthRatio;
//			} else {
//				float heightRatio = outputAspect / inputAspect;
//				vertices[1] *= heightRatio;
//				vertices[3] *= heightRatio;
//				vertices[5] *= heightRatio;
//				vertices[7] *= heightRatio;
//			}
//		}
//		return vertices;
//	}


	private static void swap(float[] arr, int index1, int index2) {
		float temp = arr[index1];
		arr[index1] = arr[index2];
		arr[index2] = temp;
	}
}
