package com.joe.camera2recorddemo.OpenGL.Filter;

import android.content.res.Resources;

/**
 * 综合滤镜
 * Created by Yj on 2017/11/1.
 */

public class Mp4EditFilter extends GroupFilter {
	private ChooseFilter chooseFilter;
	private DistortionFilter distortionFilter;

	public Mp4EditFilter(Resources resource) {
		super(resource);
	}

	@Override
	protected void initBuffer() {
		super.initBuffer();
		chooseFilter = new ChooseFilter(mRes);
		distortionFilter = new DistortionFilter(mRes);
		addFilter(chooseFilter);
		addFilter(distortionFilter);
	}

	public ChooseFilter getChooseFilter() {
		return chooseFilter;
	}

	public DistortionFilter getDistortionFilter() {
		return distortionFilter;
	}
}
