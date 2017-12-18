package com.joe.camera2recorddemo.Utils;

import android.opengl.Matrix;

public enum MatrixUtils {
	;
	public static final int TYPE_FITXY = 0;
	public static final int TYPE_CENTERCROP = 1;
	public static final int TYPE_CENTERINSIDE = 2;
	public static final int TYPE_FITSTART = 3;
	public static final int TYPE_FITEND = 4;

	/**
	 * @return the original texture coordinate
	 */
	public static float[] getOriginalTextureCo() {
		return new float[]{
				0.0f, 0.0f,
				0.0f, 1.0f,
				1.0f, 0.0f,
				1.0f, 1.0f
		};
	}

	/**
	 * @return the original vertex coordinate
	 */
	public static float[] getOriginalVertexCo() {
		return new float[]{
				-1.0f, 1.0f,
				-1.0f, -1.0f,
				1.0f, 1.0f,
				1.0f, -1.0f
		};
	}

	/**
	 * @return the original matrix
	 */
	public static float[] getOriginalMatrix() {
		return new float[]{
				1, 0, 0, 0,
				0, 1, 0, 0,
				0, 0, 1, 0,
				0, 0, 0, 1
		};
	}

	/**
	 * calculate appointed matrix by image size and view size
	 *
	 * @param matrix     returns the result
	 * @param type       one of TYPE_FITEND,TYPE_CENTERCROP,TYPE_CENTERINSIDE,TYPE_FITSTART,TYPE_FITXY
	 * @param imgWidth   image width
	 * @param imgHeight  image height
	 * @param viewWidth  view width
	 * @param viewHeight view height
	 */
	public static void getMatrix(float[] matrix, int type, int imgWidth, int imgHeight, int viewWidth,
								 int viewHeight) {
		if (imgHeight > 0 && imgWidth > 0 && viewWidth > 0 && viewHeight > 0) {
			float[] projection = new float[16];
			float[] camera = new float[16];
			if (type == TYPE_FITXY) {
				Matrix.orthoM(projection, 0, -1, 1, -1, 1, 1, 3);
				Matrix.setLookAtM(camera, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0);
				Matrix.multiplyMM(matrix, 0, projection, 0, camera, 0);
			}
			float sWhView = (float) viewWidth / viewHeight;
			float sWhImg = (float) imgWidth / imgHeight;
			if (sWhImg > sWhView) {
				switch (type) {
					case TYPE_CENTERCROP:
						Matrix.orthoM(projection, 0, -sWhView / sWhImg, sWhView / sWhImg, -1, 1, 1, 3);
						break;
					case TYPE_CENTERINSIDE:
						Matrix.orthoM(projection, 0, -1, 1, -sWhImg / sWhView, sWhImg / sWhView, 1, 3);
						break;
					case TYPE_FITSTART:
						Matrix.orthoM(projection, 0, -1, 1, 1 - 2 * sWhImg / sWhView, 1, 1, 3);
						break;
					case TYPE_FITEND:
						Matrix.orthoM(projection, 0, -1, 1, -1, 2 * sWhImg / sWhView - 1, 1, 3);
						break;
				}
			} else {
				switch (type) {
					case TYPE_CENTERCROP:
						Matrix.orthoM(projection, 0, -1, 1, -sWhImg / sWhView, sWhImg / sWhView, 1, 3);
						break;
					case TYPE_CENTERINSIDE:
						Matrix.orthoM(projection, 0, -sWhView / sWhImg, sWhView / sWhImg, -1, 1, 1, 3);
						break;
					case TYPE_FITSTART:
						Matrix.orthoM(projection, 0, -1, 2 * sWhView / sWhImg - 1, -1, 1, 1, 3);
						break;
					case TYPE_FITEND:
						Matrix.orthoM(projection, 0, 1 - 2 * sWhView / sWhImg, 1, -1, 1, 1, 3);
						break;
				}
			}
			Matrix.setLookAtM(camera, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0);
			Matrix.multiplyMM(matrix, 0, projection, 0, camera, 0);
		}
	}

	/**
	 * @param m 待翻转的4X4矩阵
	 * @param x X轴翻转
	 * @param y Y轴翻转
	 * @return
	 */
	public static float[] flip(float[] m, boolean x, boolean y) {
		if (x || y) {
			Matrix.scaleM(m, 0, x ? -1 : 1, y ? -1 : 1, 1);
		}
		return m;
	}

	/**
	 * 旋转矩阵
	 *
	 * @param m 待旋转的4X4矩阵
	 * @param r 旋转角度
	 * @return
	 */
	public static float[] rotation(float[] m, float r) {
		Matrix.rotateM(m, 0, r, 0.0f, 0.0f, 1.0f);
		return m;
	}

	public static float[] crop(float[] m, float x, float y, float width, float height) {
		float minX = x;
		float minY = y;
		float maxX = minX + width;
		float maxY = minY + height;

		// left bottom
		m[0] = minX;
		m[1] = minY;
		// right bottom
		m[2] = maxX;
		m[3] = minY;
		// left top
		m[4] = minX;
		m[5] = maxY;
		// right top
		m[6] = maxX;
		m[7] = maxY;

		return m;
	}

	/**
	 * 获取Surface的顶点坐标系
	 *
	 * @return
	 */
	public static float[] getSurfaceVertexCo() {
		return new float[]{
				-1.0f, -1.0f,
				-1.0f, 1.0f,
				1.0f, -1.0f,
				1.0f, 1.0f,
		};
	}

	/**
	 * 获取Camera的顶点坐标系
	 *
	 * @return
	 */
	public static float[] getCameraVertexCo() {
		return new float[]{
				-1.0f, 1.0f,
				1.0f, 1.0f,
				-1.0f, -1.0f,
				1.0f, -1.0f,
		};
	}

	/**
	 * 获取本地视频处理的顶点坐标系
	 *
	 * @return
	 */
	public static float[] getMoveVertexCo() {
		return new float[]{
				1.0f, -1.0f,
				-1.0f, -1.0f,
				1.0f, 1.0f,
				-1.0f, 1.0f,
		};
	}
}
