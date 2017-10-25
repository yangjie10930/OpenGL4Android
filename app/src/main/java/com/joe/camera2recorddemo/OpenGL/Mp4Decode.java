package com.joe.camera2recorddemo.OpenGL;

import android.annotation.TargetApi;
import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.media.MediaMuxer;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.Semaphore;


/**
 * MP4处理工具，暂时只用于处理图像。
 * 4.4的手机不支持video/mp4v-es格式的视频流，MediaMuxer混合无法stop，5.0以上可以
 */

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class Mp4Decode {

	private final int TIME_OUT = 1000;

	private String mInputPath;                  //输入路径

	private MediaCodec mVideoDecoder;           //视频解码器
	private MediaExtractor mExtractor;          //音视频分离器
	private MediaMuxer mMuxer;                  //音视频混合器
	private EGLHelper mEGLHelper;               //GL环境创建的帮助类
	private MediaCodec.BufferInfo mVideoDecoderBufferInfo;  //用于存储当前帧的视频解码信息

	private int mVideoEncoderTrack = -1;     //解码视轨
	private int mVideoDecoderTrack = -1;     //编码视轨

	//private String mAudioMime;
	//private String mVideoMime;

	private int mInputVideoWidth = 0;     //输入视频的宽度
	private int mInputVideoHeight = 0;    //输入视频的高度

	private int mOutputVideoWidth = 0;    //输出视频的宽度
	private int mOutputVideoHeight = 0;   //输出视频的高度
	private int mVideoTextureId;        //原始视频图像的纹理
	private SurfaceTexture mVideoSurfaceTexture;    //用于接收原始视频的解码的图像流

	private Surface mOutputSurface;                 //视频输出的Surface

	private Thread mDecodeThread;
	private Thread mGLThread;
	private boolean mCodecFlag = false;
	private boolean isVideoExtractorEnd = false;
	private boolean isStarted = false;
	private WrapRenderer mRenderer;
	private boolean mGLThreadFlag = false;
	private Semaphore mSem;
	private Semaphore mDecodeSem;

	private final Object Extractor_LOCK = new Object();
	private final Object PROCESS_LOCK = new Object();

	private OnProgressListener mProgressListener;

	private boolean isUserWantToStop = false;
	private long mVideoStopTimeStamp = 0;     //视频停止时的时间戳，用于外部主动停止处理时，音频截取

	private long mTotalVideoTime = 0;     //视频的总时长

	public Mp4Decode() {
		mEGLHelper = new EGLHelper();
		mVideoDecoderBufferInfo = new MediaCodec.BufferInfo();
	}


	/**
	 * 设置用于处理的MP4文件
	 *
	 * @param path 文件路径
	 */
	public void setInputPath(String path) {
		this.mInputPath = path;
	}

	/**
	 * 设置直接渲染到指定的Surface上,测试用
	 *
	 * @param surface 渲染的位置
	 */
	public void setOutputSurface(Surface surface) {
		this.mOutputSurface = surface;
	}

	/**
	 * 设置用户处理接口
	 *
	 * @param renderer 处理接口
	 */
	public void setRenderer(Renderer renderer) {
		mRenderer = new WrapRenderer(renderer);
	}

	public int getVideoSurfaceTextureId() {
		return mVideoTextureId;
	}

	public SurfaceTexture getVideoSurfaceTexture() {
		return mVideoSurfaceTexture;
	}

	/**
	 * 设置输出Mp4的图像大小，默认为输出大小
	 *
	 * @param width  视频图像宽度
	 * @param height 视频图像高度
	 */
	public void setOutputSize(int width, int height) {
		this.mOutputVideoWidth = width;
		this.mOutputVideoHeight = height;
	}

	public void setOnCompleteListener(OnProgressListener listener) {
		this.mProgressListener = listener;
	}

	private boolean prepare() throws IOException {
		//todo 获取视频旋转信息，并做出相应处理
		synchronized (PROCESS_LOCK) {
			int videoRotation = 0;
			MediaMetadataRetriever mMetRet = new MediaMetadataRetriever();
			mMetRet.setDataSource(mInputPath);
			mExtractor = new MediaExtractor();
			mExtractor.setDataSource(mInputPath);
			int count = mExtractor.getTrackCount();
			//解析Mp4
			for (int i = 0; i < count; i++) {
				MediaFormat format = mExtractor.getTrackFormat(i);
				String mime = format.getString(MediaFormat.KEY_MIME);
				Log.d("Mp4Pro", "extractor format-->" + mExtractor.getTrackFormat(i));
				if (mime.startsWith("video")) {
					//5.0以下，不能解析mp4v-es //todo 5.0以上也可能存在问题，目前还不知道原因
					mVideoDecoderTrack = i;
					mTotalVideoTime = Long.valueOf(mMetRet.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
					String rotation = mMetRet.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
					if (rotation != null) {
						videoRotation = Integer.valueOf(rotation);
					}
					if (videoRotation == 90 || videoRotation == 270) {
						mInputVideoHeight = format.getInteger(MediaFormat.KEY_WIDTH);
						mInputVideoWidth = format.getInteger(MediaFormat.KEY_HEIGHT);
					} else {
						mInputVideoWidth = format.getInteger(MediaFormat.KEY_WIDTH);
						mInputVideoHeight = format.getInteger(MediaFormat.KEY_HEIGHT);
					}
					Log.e("Mp4Pro", "createDecoder");
					mVideoDecoder = MediaCodec.createDecoderByType(mime);
					Log.e("Mp4Pro", "createDecoder end");
					mVideoTextureId = mEGLHelper.createTextureID();
					mVideoSurfaceTexture = new SurfaceTexture(mVideoTextureId);
					mVideoSurfaceTexture.setOnFrameAvailableListener(mFrameAvaListener);
					mVideoDecoder.configure(format, new Surface(mVideoSurfaceTexture), null, 0);
				}
			}
		}
		return true;
	}

	public boolean start() throws IOException {
		synchronized (PROCESS_LOCK) {
			if (!isStarted) {
				if (!prepare()) {
					Log.e("Mp4Pro", "prepare failed");
					return false;
				}

				isUserWantToStop = false;

				isVideoExtractorEnd = false;
				isVideoExtractorEnd = false;
				mGLThreadFlag = true;
				mVideoDecoder.start();
				mGLThread = new Thread(new Runnable() {
					@Override
					public void run() {
						glRunnable();
					}
				});
				mGLThread.start();

				mCodecFlag = true;
				mDecodeThread = new Thread(new Runnable() {
					@Override
					public void run() {
						//视频处理
						if (mVideoDecoderTrack >= 0) {
							Log.d("Mp4Pro", "videoDecodeStep start");
							while (mCodecFlag && !videoDecodeStep()) ;
							Log.d("Mp4Pro", "videoDecodeStep end");
							mGLThreadFlag = false;
							try {
								mSem.release();
								mGLThread.join();
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}

						Log.d("Mp4Pro", "codec thread_finish");
						mCodecFlag = false;
						avStop();
						//todo 判断是用户取消了的情况
					}
				});
				mDecodeThread.start();
				isStarted = true;
			}
		}
		return true;
	}

	/**
	 * 等待解码线程执行完毕，异步线程同步等待
	 */
	public void waitProcessFinish() throws InterruptedException {
		if (mDecodeThread != null && mDecodeThread.isAlive()) {
			mDecodeThread.join();
		}
	}

	//视频解码到SurfaceTexture上，以供后续处理。返回值为是否是最后一帧视频
	private boolean videoDecodeStep() {
		int mInputIndex = mVideoDecoder.dequeueInputBuffer(TIME_OUT);
		if (mInputIndex >= 0) {
			ByteBuffer buffer = getInputBuffer(mVideoDecoder, mInputIndex);
			buffer.clear();
			synchronized (Extractor_LOCK) {
				mExtractor.selectTrack(mVideoDecoderTrack);
				int ret = mExtractor.readSampleData(buffer, 0);
				if (ret != -1) {
					mVideoStopTimeStamp = mExtractor.getSampleTime();
					Log.d("Mp4Pro", "mVideoStopTimeStamp:" + mVideoStopTimeStamp);
					mVideoDecoder.queueInputBuffer(mInputIndex, 0, ret, mVideoStopTimeStamp, mExtractor.getSampleFlags());
				}
				isVideoExtractorEnd = !mExtractor.advance();
			}
		}
		while (true) {
			int mOutputIndex = mVideoDecoder.dequeueOutputBuffer(mVideoDecoderBufferInfo, TIME_OUT);
			if (mOutputIndex >= 0) {
				try {
					Log.d("Mp4Pro", " mDecodeSem.acquire ");
					mSem.release();
					if (!isUserWantToStop) {
						mDecodeSem.acquire();
					}
					Log.d("Mp4Pro", " mDecodeSem.acquire end ");
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				mVideoDecoder.releaseOutputBuffer(mOutputIndex, true);
			} else if (mOutputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
				//MediaFormat format=mVideoDecoder.getOutputFormat();
			} else if (mOutputIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
				break;
			}
		}
		return isVideoExtractorEnd || isUserWantToStop;
	}

	private void glRunnable() {
		mSem = new Semaphore(0);
		mDecodeSem = new Semaphore(0);
		mEGLHelper.setSurface(mOutputSurface);
		boolean ret = mEGLHelper.createGLES(mOutputVideoWidth, mOutputVideoHeight);
		if (!ret) return;
		if (mRenderer == null) {
			mRenderer = new WrapRenderer(null);
		}
		mRenderer.create();
		mRenderer.sizeChanged(mOutputVideoWidth, mOutputVideoHeight);
		while (mGLThreadFlag) {
			try {
				Log.d("Mp4Pro", " mSem.acquire ");
				mSem.acquire();
				Log.d("Mp4Pro", " mSem.acquire end");
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if (mGLThreadFlag) {
				mVideoSurfaceTexture.updateTexImage();
				//todo 带有rotation的视频，还需要处理
				mVideoSurfaceTexture.getTransformMatrix(mRenderer.getTextureMatrix());
				mRenderer.draw(mVideoTextureId);
				mEGLHelper.setPresentationTime(mVideoDecoderBufferInfo.presentationTimeUs * 1000);
				mEGLHelper.swapBuffers();
			}
			if (mProgressListener != null) {
				mProgressListener.onProgress(getTotalVideoTime() * 1000L, mVideoDecoderBufferInfo.presentationTimeUs);
			}
			mDecodeSem.release();
		}
		mRenderer.destroy();
		mEGLHelper.destroyGLES();
	}

	public long getPresentationTime() {
		return mVideoDecoderBufferInfo.presentationTimeUs * 1000;
	}

	public long getTotalVideoTime() {
		return mTotalVideoTime;
	}

	private SurfaceTexture.OnFrameAvailableListener mFrameAvaListener = new SurfaceTexture.OnFrameAvailableListener() {
		@Override
		public void onFrameAvailable(SurfaceTexture surfaceTexture) {
			Log.e("Mp4Pro", "mSem.release ");
//            mSem.release();
		}
	};

	private void avStop() {
		if (isStarted) {
			if (mVideoDecoder != null) {
				mVideoDecoder.stop();
				mVideoDecoder.release();
				mVideoDecoder = null;
			}
			if (mExtractor != null) {
				mExtractor.release();
			}
			isStarted = false;
			mVideoEncoderTrack = -1;
			mVideoDecoderTrack = -1;
		}
	}

	public boolean stop() throws InterruptedException {
		synchronized (PROCESS_LOCK) {
			if (isStarted) {
				if (mCodecFlag) {
					mDecodeSem.release();
					isUserWantToStop = true;
					if (mDecodeThread != null && mDecodeThread.isAlive()) {
						Log.d("Mp4Pro", "try to stop decode thread");
						mDecodeThread.join();
						Log.d("Mp4Pro", "decode thread stoped");
					}
					isUserWantToStop = false;
				}
			}
		}
		return true;
	}

	public boolean release() throws InterruptedException {
		synchronized (PROCESS_LOCK) {
			if (mCodecFlag) {
				stop();
			}
		}
		return true;
	}

	private ByteBuffer getInputBuffer(MediaCodec codec, int index) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			return codec.getInputBuffer(index);
		} else {
			return codec.getInputBuffers()[index];
		}
	}

	public interface OnProgressListener {
		void onProgress(long max, long current);

		void onComplete(String path);
	}

}
