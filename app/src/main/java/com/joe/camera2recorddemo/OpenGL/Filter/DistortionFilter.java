package com.joe.camera2recorddemo.OpenGL.Filter;

import android.content.res.Resources;

import com.joe.camera2recorddemo.OpenGL.TransUtil;
import com.joe.camera2recorddemo.OpenGL.Transformation;
import com.joe.camera2recorddemo.Utils.MatrixUtils;

/**
 * 旋转，翻转，裁剪类滤镜
 * Created by Yj on 2017/10/31.
 */

public class DistortionFilter extends Filter {

	//旋转，翻转，裁剪变换类
	private Transformation mTransformation;
	private float[] mTextureCo;

	public DistortionFilter(Resources resource) {
		super(resource, "shader/base.vert","shader/base.frag");
		initTransformation();
	}

	/**
	 * 设置旋转
	 *
	 * @param rotation
	 */
	public void setRotation(int rotation) {
		initTransformation();
		mTransformation.setRotation(rotation);
		setTextureCo(TransUtil.getTransformationCo(mTextureCo, mTransformation));
	}

	/**
	 * 设置翻转
	 *
	 * @param flip
	 */
	public void setFlip(int flip) {
		initTransformation();
		mTransformation.setFlip(flip);
		setTextureCo(TransUtil.getTransformationCo(mTextureCo, mTransformation));
	}

	/**
	 * 设置裁剪
	 *
	 * @param rect
	 */
	public void setCrop(Transformation.Rect rect) {
		initTransformation();
		mTransformation.setCrop(rect);
		setTextureCo(TransUtil.getTransformationCo(mTextureCo, mTransformation));
	}


	/**
	 * 初始化变化类
	 */
	private void initTransformation() {
		mTextureCo = MatrixUtils.getOriginalTextureCo();
		if (mTransformation == null) {
			mTransformation = new Transformation();
		}
	}

	/**
	 * 设置变化类
	 * @param transformation
	 */
	public void setTransformation(Transformation transformation){
		mTransformation = transformation;
		setTextureCo(TransUtil.getTransformationCo(mTextureCo, mTransformation));
	}
}
