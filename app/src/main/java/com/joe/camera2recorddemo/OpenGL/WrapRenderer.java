package com.joe.camera2recorddemo.OpenGL;


import com.joe.camera2recorddemo.OpenGL.Filter.OesFilter;
import com.joe.camera2recorddemo.Utils.MatrixUtils;

/**
 * Created by aiya on 2017/9/12.
 */

class WrapRenderer implements Renderer{

    private Renderer mRenderer;
    private OesFilter mFilter;
    private FrameBuffer mFrameBuffer;

    public static final int TYPE_MOVE=0;
    public static final int TYPE_CAMERA=1;
    public static final int TYPE_SURFACE=2;

    public WrapRenderer(Renderer renderer){
        this.mRenderer=renderer;
        mFrameBuffer=new FrameBuffer();
        mFilter=new OesFilter();
        if(renderer!=null){
            MatrixUtils.flip(mFilter.getVertexMatrix(),false,true);
        }
    }

    public void setFlag(int flag){
        if(flag==TYPE_SURFACE){
            mFilter.setVertexCo(new float[]{
                    -1.0f, -1.0f,
                    -1.0f, 1.0f,
                    1.0f, -1.0f,
                    1.0f, 1.0f,
            });
        }else if(flag==TYPE_CAMERA){
            mFilter.setVertexCo(new float[]{
                    -1.0f, 1.0f,
                    1.0f, 1.0f,
                    -1.0f, -1.0f,
                    1.0f, -1.0f,
            });
        }else if(flag==TYPE_MOVE){
            mFilter.setVertexCo(new float[]{
                    1.0f, -1.0f,
                    -1.0f, -1.0f,
                    1.0f, 1.0f,
                    -1.0f, 1.0f,
            });
        }
    }

    public float[] getTextureMatrix(){
        return mFilter.getTextureMatrix();
    }

    @Override
    public void create() {
        mFilter.create();
        if(mRenderer!=null){
            mRenderer.create();
        }
    }

    @Override
    public void sizeChanged(int width, int height) {
        mFilter.sizeChanged(width, height);
        if(mRenderer!=null){
            mRenderer.sizeChanged(width, height);
        }
    }

    @Override
    public void draw(int texture) {
        if(mRenderer!=null){
            mRenderer.draw(mFilter.drawToTexture(texture));
        }else{
            mFilter.draw(texture);
        }
    }

    @Override
    public void destroy() {
        if(mRenderer!=null){
            mRenderer.destroy();
        }
        mFilter.destroy();
    }
}
