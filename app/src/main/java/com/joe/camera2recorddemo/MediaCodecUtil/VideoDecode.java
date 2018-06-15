package com.joe.camera2recorddemo.MediaCodecUtil;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;


public class VideoDecode {
	private static final String TAG = "VideoToFrames";
	private static final long DEFAULT_TIMEOUT_US = 10000;

    private final int decodeColorFormat = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible;


	public int ImageWidth = 0;
	public int ImageHeight = 0;

	MediaExtractor extractor = null;
	MediaCodec decoder = null;
	MediaFormat mediaFormat;

	private boolean isLoop = false;//是否循环播放
	private boolean isStop = false;//是否停止
	private String videoFilePath;

	/**
	 * 解码器初始化
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

			//消除旋转信息，防止拉伸
			mediaFormat.setInteger(MediaFormat.KEY_ROTATION,0);
			//设置
			mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
			mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
			mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 2500000);
			decoder.configure(mediaFormat,mSurface, null, 0);
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
		}catch (IllegalStateException e){
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
			if(isLoop && !isStop){
				decodePrepare(videoFilePath);
				excuate();
			}
		}

	}

	/**
	 * 设置是否循环
	 * @param isLoop
	 */
	public void setLoop(boolean isLoop){
		this.isLoop = isLoop;
	}

	/**
	 * 检查是否支持的色彩格式
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
	 * @param decoder
	 * @param extractor
	 * @param mediaFormat
	 */
	public void decodeFramesToImage(MediaCodec decoder, MediaExtractor extractor, MediaFormat mediaFormat) {
		MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
		boolean sawInputEOS = false;
		boolean sawOutputEOS = false;

		final int width = mediaFormat.getInteger(MediaFormat.KEY_WIDTH);
		final int height = mediaFormat.getInteger(MediaFormat.KEY_HEIGHT);

		ImageWidth = width;
		ImageHeight = height;

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
						Log.v(TAG, "presentationTimeUs:"+presentationTimeUs);
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

	public void stop(){
		isStop = true;
	}

	public void start(){
		isStop = false;
	}

	//======================设置输出Surface==============================

	private Surface mSurface;

	public void setSurface(Surface surface){
		this.mSurface = surface;
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
}