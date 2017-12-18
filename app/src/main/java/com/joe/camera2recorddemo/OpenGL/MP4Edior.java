package com.joe.camera2recorddemo.OpenGL;

import android.annotation.TargetApi;
import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.opengl.GLES20;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Size;
import android.view.Surface;

import com.joe.camera2recorddemo.MediaCodecUtil.TrackUtils;
import com.joe.camera2recorddemo.Utils.MatrixUtils;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.Semaphore;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class MP4Edior {

	//============================  OpenGL =========================

	private SurfaceTexture mInputTexture;
	private Surface mOutputSurface;
	//    private EGLHelper mEncodeEGLHelper;
	private EGLHelper mShowEGLHelper;

	private boolean mGLThreadFlag = false;
	private Thread mGLThread;
	private WrapRenderer mRenderer;
	private Semaphore mSem;
	private int mInputTextureId;

	private int mPreviewWidth = -1;
	private int mPreviewHeight = -1;
	private int mInputWidth = -1;
	private int mInputHeight = -1;

	private final Object VIDEO_LOCK = new Object();
	private final Object REC_LOCK = new Object();

	//===========================  MeidaCodec ========================

	private static final String TAG = "VideoToFrames";
	private static final long DEFAULT_TIMEOUT_US = 10000;

	private final int decodeColorFormat = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible;

	MediaExtractor extractor = null;
	MediaCodec decoder = null;
	MediaFormat mediaFormat;

	private boolean isLoop = false;//是否循环播放
	private boolean isStop = false;//是否停止
	private String videoFilePath;

	private Size mSize;//输入视频的尺寸

	public MP4Edior() {
		mShowEGLHelper = new EGLHelper();
		mSem = new Semaphore(0);
	}

	public Surface createInputSurfaceTexture() {
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
		return new Surface(mInputTexture);
	}

	public void setOutputSurface(Surface surface, int width, int height) {
		this.mOutputSurface = surface;
		this.mPreviewWidth = width;
		this.mPreviewHeight = height;
	}

	public void setRenderer(Renderer renderer) {
		mRenderer = new WrapRenderer(renderer);
	}

	/**
	 * 开始预览
	 */
	public void startPreview() {
		synchronized (REC_LOCK) {
			mSem.drainPermits();
			mGLThreadFlag = true;
			mGLThread = new Thread(mGLRunnable);
			mGLThread.start();
		}
	}

	/**
	 * 停止预览
	 *
	 * @throws InterruptedException
	 */
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

	/**
	 * =====================================    MediaCodec   ===============================
	 */

	/**
	 * 解码器初始化
	 *
	 * @param videoFilePath
	 */
	public void decodePrepare(String videoFilePath) {
		this.videoFilePath = videoFilePath;
		extractor = null;
		decoder = null;
		try {
			File videoFile = new File(videoFilePath);
			extractor = new MediaExtractor();
			extractor.setDataSource(videoFile.toString());
			int trackIndex = TrackUtils.selectVideoTrack(extractor);
			if (trackIndex < 0) {
				throw new RuntimeException("No video track found in " + videoFilePath);
			}
			extractor.selectTrack(trackIndex);
			mediaFormat = extractor.getTrackFormat(trackIndex);
			String mime = mediaFormat.getString(MediaFormat.KEY_MIME);
			decoder = MediaCodec.createDecoderByType(mime);
			if (isColorFormatSupported(decodeColorFormat, decoder.getCodecInfo().getCapabilitiesForType(mime))) {
				mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, decodeColorFormat);
				Log.i(TAG, "set decode color format to type " + decodeColorFormat);
			} else {
				Log.i(TAG, "unable to set decode color format, color format type " + decodeColorFormat + " not supported");
			}

			//获取宽高信息
			int rotation = mediaFormat.containsKey(MediaFormat.KEY_ROTATION) ? mediaFormat.getInteger(MediaFormat.KEY_ROTATION) : 0;
			if (rotation == 90 || rotation == 270) {
				mInputHeight = mediaFormat.getInteger(MediaFormat.KEY_WIDTH);
				mInputWidth = mediaFormat.getInteger(MediaFormat.KEY_HEIGHT);
			} else {
				mInputWidth = mediaFormat.getInteger(MediaFormat.KEY_WIDTH);
				mInputHeight = mediaFormat.getInteger(MediaFormat.KEY_HEIGHT);
			}
			mSize = new Size(mInputWidth, mInputHeight);

			//设置
			mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
			mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
			mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 2500000);
			decoder.configure(mediaFormat, createInputSurfaceTexture(), null, 0);
			decoder.start();
		} catch (IOException ioe) {
			throw new RuntimeException("failed init encoder", ioe);
		}
	}

	public void close() {
		try {
			if (decoder != null) {
				decoder.stop();
				decoder.release();
			}

			if (extractor != null) {
				extractor.release();
				extractor = null;
			}
		} catch (IllegalStateException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 外部调用开始解码
	 */
	public void excuate() {
		try {
			decodeFramesToImage(decoder, extractor, mediaFormat);
		} finally {
			close();
			if (isLoop && !isStop) {
				decodePrepare(videoFilePath);
				excuate();
			}
		}

	}

	/**
	 * 设置是否循环
	 *
	 * @param isLoop
	 */
	public void setLoop(boolean isLoop) {
		this.isLoop = isLoop;
	}

	/**
	 * 检查是否支持的色彩格式
	 *
	 * @param colorFormat
	 * @param caps
	 * @return
	 */
	private boolean isColorFormatSupported(int colorFormat, MediaCodecInfo.CodecCapabilities caps) {
		for (int c : caps.colorFormats) {
			if (c == colorFormat) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 开始解码
	 *
	 * @param decoder
	 * @param extractor
	 * @param mediaFormat
	 */
	public void decodeFramesToImage(MediaCodec decoder, MediaExtractor extractor, MediaFormat mediaFormat) {
		MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
		boolean sawInputEOS = false;
		boolean sawOutputEOS = false;

		long startMs = System.currentTimeMillis();
		while (!sawOutputEOS && !isStop) {
			if (!sawInputEOS) {
				int inputBufferId = decoder.dequeueInputBuffer(DEFAULT_TIMEOUT_US);
				if (inputBufferId >= 0) {
					ByteBuffer inputBuffer = decoder.getInputBuffer(inputBufferId);
					int sampleSize = extractor.readSampleData(inputBuffer, 0); //将一部分视频数据读取到inputbuffer中，大小为sampleSize
					if (sampleSize < 0) {
						decoder.queueInputBuffer(inputBufferId, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
						sawInputEOS = true;
					} else {
						long presentationTimeUs = extractor.getSampleTime();
						Log.v(TAG, "presentationTimeUs:" + presentationTimeUs);
						decoder.queueInputBuffer(inputBufferId, 0, sampleSize, presentationTimeUs, 0);
						extractor.advance();  //移动到视频文件的下一个地址
					}
				}
			}
			int outputBufferId = decoder.dequeueOutputBuffer(info, DEFAULT_TIMEOUT_US);
			if (outputBufferId >= 0) {
				if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
					sawOutputEOS = true;
				}
				boolean doRender = (info.size != 0);
				if (doRender) {
					sleepRender(info, startMs);//延迟解码
					decoder.releaseOutputBuffer(outputBufferId, true);
				}
			}
		}
	}

	/**
	 * 停止解码播放
	 */
	public void stop() {
		isStop = true;
	}

	/**
	 * 开始解码播放
	 */
	public void start() {
		isStop = false;
	}

	/**
	 * 获取视频尺寸
	 *
	 * @return 视频尺寸
	 */
	public Size getSize() {
		return mSize;
	}

	/**
	 * 延迟解码，按帧播放
	 */
	private void sleepRender(MediaCodec.BufferInfo audioBufferInfo, long startMs) {
		while (audioBufferInfo.presentationTimeUs / 1000 > System.currentTimeMillis() - startMs) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
				break;
			}
		}
	}

	/**
	 * 设置变换类型
	 *
	 * @param transformation
	 */
	public void setTransformation(Transformation transformation) {
		float[] vms = mRenderer.getmFilter().getVertexMatrix();
		if (transformation.getScaleType() == MatrixUtils.TYPE_CENTERINSIDE) {
			if (transformation.getRotation() == 90 || transformation.getRotation() == 270) {
				MatrixUtils.getMatrix(vms, MatrixUtils.TYPE_CENTERINSIDE, transformation.getInputSize().getWidth(), transformation.getInputSize().getHeight()
						, transformation.getOutputSize().getHeight(), transformation.getOutputSize().getWidth());
			} else {
				MatrixUtils.getMatrix(vms, MatrixUtils.TYPE_CENTERINSIDE, transformation.getInputSize().getHeight(), transformation.getInputSize().getWidth()
						, transformation.getOutputSize().getHeight(), transformation.getOutputSize().getWidth());
			}
		}

		//设置旋转
		if (transformation.getRotation() != 0) {
			MatrixUtils.rotation(vms, transformation.getRotation());
		}

		//设置裁剪
		if (transformation.getCropRect() != null) {
			float[] vtCo = new float[8];
			MatrixUtils.crop(vtCo,transformation.getCropRect().x,transformation.getCropRect().y
					,transformation.getCropRect().width,transformation.getCropRect().height);
			mRenderer.getmFilter().setTextureCo(vtCo);
		}

		//设置翻转
		if (transformation.getFlip() != Transformation.FLIP_NONE) {
			switch (transformation.getFlip()) {
				case Transformation.FLIP_HORIZONTAL:
					MatrixUtils.flip(vms, true, false);
					break;
				case Transformation.FLIP_VERTICAL:
					MatrixUtils.flip(vms, false, true);
					break;
				case Transformation.FLIP_HORIZONTAL_VERTICAL:
					MatrixUtils.flip(vms, true, true);
					break;
				default:
					break;
			}
		}

		//设置投影矩阵
		mRenderer.getmFilter().setVertexMatrix(vms);
	}
}
