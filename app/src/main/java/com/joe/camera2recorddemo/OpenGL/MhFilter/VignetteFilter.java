package com.joe.camera2recorddemo.OpenGL.MhFilter;

import android.content.res.Resources;
import android.graphics.PointF;
import android.opengl.GLES20;

import com.joe.camera2recorddemo.OpenGL.Filter.Filter;

import java.nio.FloatBuffer;

/**
 * 暗角滤镜
 */
public class VignetteFilter extends Filter {

    private int mVignetteCenterLocation;
    private PointF mVignetteCenter = new PointF();
    private int mVignetteColorLocation;
    private float[] mVignetteColor = new float[] {0.0f, 0.0f, 0.0f};
    private int mVignetteStartLocation;
    private float mVignetteStart = 0.75f;
    private int mVignetteEndLocation;
    private float mVignetteEnd = 0.75f;
    private float[] vec2 = new float[2];

    public VignetteFilter(Resources resource) {
        super(resource,"shader/base.vert","shader/mh/vignette.frag");
        vec2[0] = mVignetteCenter.x;
        vec2[1] = mVignetteCenter.y;
    }

    @Override
    protected void onCreate() {
        super.onCreate();
        mVignetteCenterLocation = GLES20.glGetUniformLocation(mGLProgram, "vignetteCenter");
        mVignetteColorLocation = GLES20.glGetUniformLocation(mGLProgram, "vignetteColor");
        mVignetteStartLocation = GLES20.glGetUniformLocation(mGLProgram, "vignetteStart");
        mVignetteEndLocation = GLES20.glGetUniformLocation(mGLProgram, "vignetteEnd");
    }

    @Override
    protected void onSetExpandData() {
        super.onSetExpandData();
        GLES20.glUniform2fv(mVignetteCenterLocation,1,vec2,0);
        GLES20.glUniform3fv(mVignetteColorLocation,1, FloatBuffer.wrap(mVignetteColor));
        GLES20.glUniform1f(mVignetteStartLocation, mVignetteStart);
        GLES20.glUniform1f(mVignetteEndLocation, mVignetteEnd);
    }

    public void setmVignetteStart(float mVignetteStart) {
        this.mVignetteStart = mVignetteStart;
    }

    public void setmVignetteEnd(float mVignetteEnd) {
        this.mVignetteEnd = mVignetteEnd;
    }
}
