package com.joe.camera2recorddemo.OpenGL;

import android.annotation.TargetApi;
import android.graphics.SurfaceTexture;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.MediaRecorder;
import android.opengl.EGLSurface;
import android.opengl.GLES20;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Surface;

import com.joe.camera2recorddemo.OpenGL.Filter.BaseFilter;
import com.joe.camera2recorddemo.OpenGL.Filter.Filter;
import com.joe.camera2recorddemo.Utils.MatrixUtils;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.Semaphore;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class MP4Edior {

	private SurfaceTexture mInputTexture;
	private Surface mOutputSurface;
	//    private EGLHelper mEncodeEGLHelper;
	private EGLHelper mShowEGLHelper;

	private boolean mGLThreadFlag = false;
	private Thread mGLThread;
	private WrapRenderer mRenderer;
	private Semaphore mSem;
	private int mInputTextureId;

	private int mPreviewWidth = 0;
	private int mPreviewHeight = 0;

	private final Object VIDEO_LOCK = new Object();
	private final Object REC_LOCK = new Object();

	public MP4Edior() {
		mShowEGLHelper = new EGLHelper();
		mSem = new Semaphore(0);
	}

	public void setPreviewSize(int width, int height) {
		this.mPreviewWidth = width;
		this.mPreviewHeight = height;
	}

	public SurfaceTexture createInputSurfaceTexture() {
		mInputTextureId = mShowEGLHelper.createTextureID();
		mInputTexture = new SurfaceTexture(mInputTextureId);
		new Handler(Looper.getMainLooper()).post(new Runnable() {
			@Override
			public void run() {
				mInputTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
					@Override
					public void onFrameAvailable(SurfaceTexture surfaceTexture) {
						mSem.release();
					}
				});
			}
		});
		return mInputTexture;
	}

	public void setOutputSurface(Surface surface) {
		this.mOutputSurface = surface;
	}

	public void setRenderer(Renderer renderer) {
		mRenderer = new WrapRenderer(renderer);
	}

	public void startPreview() {
		synchronized (REC_LOCK) {
			Log.d("C2D", "CameraRecorder startPreview");
			mSem.drainPermits();
			mGLThreadFlag = true;
			mGLThread = new Thread(mGLRunnable);
			mGLThread.start();
		}
	}

	public void stopPreview() throws InterruptedException {
		synchronized (REC_LOCK) {
			mGLThreadFlag = false;
			mSem.release();
			if (mGLThread != null && mGLThread.isAlive()) {
				mGLThread.join();
				mGLThread = null;
			}
			Log.d("C2D", "CameraRecorder stopPreview");
		}
	}

	private Runnable mGLRunnable = new Runnable() {
		@Override
		public void run() {
			if (mOutputSurface == null) {
				Log.e("C2D", "CameraRecorder GLThread exit : outputSurface==null");
				return;
			}
			if (mPreviewWidth <= 0 || mPreviewHeight <= 0) {
				Log.e("C2D", "CameraRecorder GLThread exit : Preview Size==0");
				return;
			}
			mShowEGLHelper.setSurface(mOutputSurface);
			boolean ret = mShowEGLHelper.createGLES(mPreviewWidth, mPreviewHeight);
			if (!ret) {
				Log.e("C2D", "CameraRecorder GLThread exit : createGLES failed");
				return;
			}
			if (mRenderer == null) {
				mRenderer = new WrapRenderer(null);
			}
			mRenderer.setFlag(WrapRenderer.TYPE_SURFACE);
			mRenderer.create();
			int[] t = new int[1];
			GLES20.glGetIntegerv(GLES20.GL_FRAMEBUFFER_BINDING, t, 0);
			mRenderer.sizeChanged(mPreviewWidth, mPreviewHeight);
			GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, t[0]);
			while (mGLThreadFlag) {
				try {
					mSem.acquire();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				if (mGLThreadFlag) {
					mInputTexture.updateTexImage();
					mInputTexture.getTransformMatrix(mRenderer.getTextureMatrix());
					synchronized (VIDEO_LOCK) {
						GLES20.glViewport(0, 0, mPreviewWidth, mPreviewHeight);
						mRenderer.draw(mInputTextureId);
						mShowEGLHelper.swapBuffers();
					}
				}
			}
			mShowEGLHelper.destroyGLES();
		}
	};
}
