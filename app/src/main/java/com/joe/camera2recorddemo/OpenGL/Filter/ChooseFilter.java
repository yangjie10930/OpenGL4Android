/*
 *
 * NoFilter.java
 * 
 * Created by Wuwang on 2016/10/17
 */
package com.joe.camera2recorddemo.OpenGL.Filter;

import android.content.res.Resources;
import android.opengl.GLES20;

/**
 * 滤镜集合
 */
public class ChooseFilter extends Filter {

	//滤镜选择代码
	private int hChangeType;
	private int hFilterCode = 0;

	//复杂
	private int width;
	private int height;
	private boolean needGLWH = false;
	private int mGLWidth;
	private int mGLHeight;
	private boolean needTexelWH = false;
	private float mTexelWidth;
	private float mTexelHeight;
	private int mUniformTexelWidthLocation;
	private int mUniformTexelHeightLocation;

	public ChooseFilter(Resources resource) {
		super(resource, "shader/choose/choose.vert", "shader/choose/choose.frag");
	}

	@Override
	protected void onCreate() {
		super.onCreate();
		hChangeType = GLES20.glGetUniformLocation(mGLProgram, "vChangeType");
		mGLWidth = GLES20.glGetUniformLocation(mGLProgram, "uWidth");
		mGLHeight = GLES20.glGetUniformLocation(mGLProgram, "uHeight");
		mUniformTexelWidthLocation = GLES20.glGetUniformLocation(mGLProgram, "texelWidth");
		mUniformTexelHeightLocation = GLES20.glGetUniformLocation(mGLProgram, "texelHeight");
	}

	@Override
	protected void onSizeChanged(int width, int height) {
		super.onSizeChanged(width, height);
		this.width = width;
		this.height = height;
		setTextlSize(5.0f);
	}

	@Override
	protected void onSetExpandData() {
		super.onSetExpandData();
		GLES20.glUniform1i(hChangeType, hFilterCode);
		if (needGLWH) {
			GLES20.glUniform1f(mGLWidth, width);
			GLES20.glUniform1f(mGLHeight, height);
		}
		if (needTexelWH) {
			GLES20.glUniform1f(mUniformTexelWidthLocation, mTexelWidth);
			GLES20.glUniform1f(mUniformTexelHeightLocation, mTexelHeight);
		}
	}

	/**
	 * 设置滤镜类型
	 *
	 * @param code
	 */
	public void setChangeType(int code) {
		this.hFilterCode = code;
		switch (code) {
			case FilterType.TOON:
				needTexelWH = true;
				setTextlSize(4.2f);
				break;
			case FilterType.CONVOLUTION:
				needTexelWH = true;
				setTextlSize(1.3f);
				break;
			case FilterType.SOBEL:
				needGLWH = true;
				break;
			case FilterType.SKETCH:
				needTexelWH = true;
				setTextlSize(3.0f);
				break;
			default:
				needTexelWH = false;
				needGLWH = false;
				break;
		}
	}

	private void setTextlSize(float size) {
		mTexelWidth = size / width;
		mTexelHeight = size / height;
	}

	/**
	 * 滤镜类型
	 */
	public static class FilterType {
		public static final int NORMAL = 0;
		public static final int COOL = 1;
		public static final int WARM = 2;
		public static final int GRAY = 3;
		public static final int CAMEO = 4;
		public static final int INVERT = 5;
		public static final int SEPIA = 6;
		public static final int TOON = 7;
		public static final int CONVOLUTION = 8;
		public static final int SOBEL = 9;
		public static final int SKETCH = 10;
	}
}
