package com.joe.camera2recorddemo.OpenGL.MhFilter;

import android.content.res.Resources;

import com.joe.camera2recorddemo.OpenGL.Filter.ChooseFilter;
import com.joe.camera2recorddemo.OpenGL.Filter.DistortionFilter;
import com.joe.camera2recorddemo.OpenGL.Filter.GroupFilter;

/**
 * 调整滤镜
 * Created by Yj on 2018/6/14.
 */

public class AdjustFilter extends GroupFilter {
	private ContrastFilter contrastFilter;
	private BrightnessFilter brightnessFilter;
	private SaturationFilter saturationFilter;
	private VignetteFilter vignetteFilter;
	private DistortionFilter distortionFilter;

	public AdjustFilter(Resources resource) {
		super(resource);
	}

	@Override
	protected void initBuffer() {
		super.initBuffer();
		contrastFilter = new ContrastFilter(mRes);
		brightnessFilter = new BrightnessFilter(mRes);
		saturationFilter = new SaturationFilter(mRes);
		distortionFilter = new DistortionFilter(mRes);
		vignetteFilter = new VignetteFilter(mRes);
		addFilter(contrastFilter);
		addFilter(brightnessFilter);
		addFilter(saturationFilter);
		addFilter(vignetteFilter);
		addFilter(distortionFilter);
	}

	public ContrastFilter getContrastFilter() {
		return contrastFilter;
	}

	public BrightnessFilter getBrightnessFilter() {
		return brightnessFilter;
	}

	public SaturationFilter getSaturationFilter() {
		return saturationFilter;
	}

	public DistortionFilter getDistortionFilter() {
		return distortionFilter;
	}

	public VignetteFilter getVignetteFilter() {
		return vignetteFilter;
	}
}
