package com.joe.camera2recorddemo.OpenGL.MhFilter;

import android.content.res.Resources;
import android.opengl.GLES20;

import com.joe.camera2recorddemo.OpenGL.Filter.Filter;

/**
 * 饱和度滤镜
 */
public class SaturationFilter extends Filter {

    private int saturationType;
    private float saturationCode = 1.0f;

    public SaturationFilter(Resources resource) {
        super(resource,"shader/base.vert","shader/mh/saturation.frag");
    }

    @Override
    protected void onCreate() {
        super.onCreate();
        saturationType = GLES20.glGetUniformLocation(mGLProgram, "saturation");
    }

    @Override
    protected void onSetExpandData() {
        super.onSetExpandData();
        GLES20.glUniform1f(saturationType, saturationCode);
    }

    public void setSaturationCode(float saturationCode) {
        this.saturationCode = saturationCode;
    }
}
