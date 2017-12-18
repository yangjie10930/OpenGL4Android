package com.joe.camera2recorddemo.OpenGL;

import android.util.Size;

/**
 * Created by Yj on 2017/10/30.
 * 图形旋转，翻转，缩放，裁剪类
 */

public class Transformation {
    public static final Rect FULL_RECT = new Rect(0, 0, 1, 1);

    public static final int FLIP_NONE = 2001;
    public static final int FLIP_HORIZONTAL = 2002;
    public static final int FLIP_VERTICAL = 2003;
    public static final int FLIP_HORIZONTAL_VERTICAL = 2004;

    private Rect cropRect = FULL_RECT;
    private int flip = FLIP_NONE;
    private int rotation = 0;
    private Size inputSize;
    private Size outputSize;
    private int scaleType = 0;

    public void setCrop(Rect cropRect) {
        this.cropRect = cropRect;
    }

    public void setFlip(int flip) {
        this.flip = flip;
    }

    public void setRotation(int rotation) {
        this.rotation = rotation;
    }

    public void setInputSize(Size inputSize) {
        this.inputSize = inputSize;
    }

    public void setOutputSize(Size outputSize) {
        this.outputSize = outputSize;
    }

    public Rect getCropRect() {
        return cropRect;
    }

    public int getFlip() {
        return flip;
    }

    public int getRotation() {
        return rotation;
    }

    public Size getInputSize() {
        return inputSize;
    }

    public Size getOutputSize() {
        return outputSize;
    }

    public int getScaleType() {
        return scaleType;
    }

    public void setScale(Size inputSize, Size outputSize, int scaleType) {
        this.inputSize = inputSize;
        this.outputSize = outputSize;
        this.scaleType = scaleType;
    }

    public static class Rect {
        final float x;
        final float y;
        final float width;
        final float height;

        public Rect(final float x, final float y, final float width, final float height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }
}
