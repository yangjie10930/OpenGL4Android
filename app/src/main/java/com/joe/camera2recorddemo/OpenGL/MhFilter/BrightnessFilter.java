package com.joe.camera2recorddemo.OpenGL.MhFilter;

import android.content.res.Resources;
import android.opengl.GLES20;

import com.joe.camera2recorddemo.OpenGL.Filter.Filter;

/**
 * 亮度滤镜
 */
public class BrightnessFilter extends Filter {

    private int brightnessType;
    private float brightnessCode = 0.0f;

    public BrightnessFilter(Resources resource) {
        super(resource,"shader/base.vert","shader/mh/brightness.frag");
    }

    @Override
    protected void onCreate() {
        super.onCreate();
        brightnessType = GLES20.glGetUniformLocation(mGLProgram, "brightness");
    }

    @Override
    protected void onSetExpandData() {
        super.onSetExpandData();
        GLES20.glUniform1f(brightnessType, brightnessCode);
    }

    public void setBrightnessCode(float brightnessCode) {
        this.brightnessCode = brightnessCode;
    }
}
