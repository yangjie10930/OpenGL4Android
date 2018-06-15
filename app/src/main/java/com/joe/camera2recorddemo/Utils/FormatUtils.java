package com.joe.camera2recorddemo.Utils;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;
import android.util.Size;

import com.joe.camera2recorddemo.MediaCodecUtil.TrackUtils;

import java.io.File;

/**
 * 获取视频信息类
 * Created by Administrator on 2017/12/6.
 */

public class FormatUtils {

	public static class VideoFormat{
		public int width;
		public int height;
		public int rotation;
	}

	/**
	 * 获取视频尺寸
	 *
	 * @param url 视频地址
	 * @return 返回视频尺寸
	 */
	public static Size getVideoSize(String url) {
		int mInputHeight = 0, mInputWidth = 0;
		MediaExtractor extractor = new MediaExtractor();
		try {
			extractor.setDataSource(url);
			int trackIndex = TrackUtils.selectVideoTrack(extractor);
			if (trackIndex < 0) {
				throw new RuntimeException("No video track found in " + url);
			}
			extractor.selectTrack(trackIndex);
			MediaFormat mediaFormat = extractor.getTrackFormat(trackIndex);
			//获取宽高信息
			int rotation = mediaFormat.containsKey(MediaFormat.KEY_ROTATION) ? mediaFormat.getInteger(MediaFormat.KEY_ROTATION) : 0;
			if (rotation == 90 || rotation == 270) {
				mInputHeight = mediaFormat.getInteger(MediaFormat.KEY_WIDTH);
				mInputWidth = mediaFormat.getInteger(MediaFormat.KEY_HEIGHT);
			} else {
				mInputWidth = mediaFormat.getInteger(MediaFormat.KEY_WIDTH);
				mInputHeight = mediaFormat.getInteger(MediaFormat.KEY_HEIGHT);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new Size(mInputWidth, mInputHeight);
	}

	/**
	 * 获取视频信息
	 *
	 * @param url
	 * @return
	 */
	public static VideoFormat getVideoFormat(String url){
		VideoFormat videoFormat = new VideoFormat();
		videoFormat.height = 0;
		videoFormat.width = 0;
		MediaExtractor extractor = new MediaExtractor();
		try {
			extractor.setDataSource(url);
			int trackIndex = TrackUtils.selectVideoTrack(extractor);
			if (trackIndex < 0) {
				throw new RuntimeException("No video track found in " + url);
			}
			extractor.selectTrack(trackIndex);
			MediaFormat mediaFormat = extractor.getTrackFormat(trackIndex);
			//获取宽高信息
			videoFormat.rotation = mediaFormat.containsKey(MediaFormat.KEY_ROTATION) ? mediaFormat.getInteger(MediaFormat.KEY_ROTATION) : 0;
			if (videoFormat.rotation == 90 || videoFormat.rotation == 270) {
				videoFormat.height = mediaFormat.getInteger(MediaFormat.KEY_WIDTH);
				videoFormat.width = mediaFormat.getInteger(MediaFormat.KEY_HEIGHT);
			} else {
				videoFormat.width = mediaFormat.getInteger(MediaFormat.KEY_WIDTH);
				videoFormat.height = mediaFormat.getInteger(MediaFormat.KEY_HEIGHT);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return videoFormat;
	}
}
