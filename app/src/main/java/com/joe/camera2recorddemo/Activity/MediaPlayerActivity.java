package com.joe.camera2recorddemo.Activity;

import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;

import com.joe.camera2recorddemo.MediaCodecUtil.VideoDecode;
import com.joe.camera2recorddemo.R;
import com.joe.camera2recorddemo.Utils.UriUtils;

import java.io.IOException;

public class MediaPlayerActivity extends AppCompatActivity implements View.OnClickListener {

	private TextureView textureView;
	private MediaPlayer mediaPlayer;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_media_player);
		initView();
	}

	private void initView() {
		textureView = (TextureView) findViewById(R.id.mediaplayer_ttv);
		mediaPlayer = new MediaPlayer();
		mediaPlayer.setLooping(true);
		textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
			@Override
			public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
					mediaPlayer.setSurface(new Surface(surface));
			}

			@Override
			public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

			}

			@Override
			public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
				return false;
			}

			@Override
			public void onSurfaceTextureUpdated(SurfaceTexture surface) {

			}
		});
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.mediaplayer_open_bt:
				Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
				intent.setType("video/mp4"); //选择视频 （mp4 3gp 是android支持的视频格式）
				intent.addCategory(Intent.CATEGORY_OPENABLE);
				startActivityForResult(intent, 1);
				break;
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_OK) {
			final String path = UriUtils.getRealFilePath(getApplicationContext(), data.getData());
			if (path != null) {
				Log.v("MP4Activity", "path:" + path);
				try {
					mediaPlayer.reset();
					mediaPlayer.setDataSource(path);
					mediaPlayer.prepare();
				} catch (IOException e) {
					e.printStackTrace();
				}
				mediaPlayer.start();
			}
		}
	}
}
