package com.joe.camera2recorddemo.OpenGL.MhFilter;

import android.content.res.Resources;
import android.opengl.GLES20;

import com.joe.camera2recorddemo.OpenGL.Filter.Filter;

/**
 * 对比度滤镜
 */
public class ContrastFilter extends Filter {

    private int contrastType;
    private float contrastCode = 1.0f;

    public ContrastFilter(Resources resource) {
        super(resource,"shader/base.vert","shader/mh/contrast.frag");
    }

    @Override
    protected void onCreate() {
        super.onCreate();
        contrastType = GLES20.glGetUniformLocation(mGLProgram, "stepcv");
    }

    @Override
    protected void onSetExpandData() {
        super.onSetExpandData();
        GLES20.glUniform1f(contrastType, contrastCode);
    }

    public void setContrastCode(float contrastCode) {
        this.contrastCode = contrastCode;
    }
}
