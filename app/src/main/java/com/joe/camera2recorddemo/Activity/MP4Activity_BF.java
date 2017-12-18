package com.joe.camera2recorddemo.Activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Toast;

import com.joe.camera2recorddemo.Adapter.FilterAdapter;
import com.joe.camera2recorddemo.Entity.FilterChoose;
import com.joe.camera2recorddemo.MediaCodecUtil.VideoDecode;
import com.joe.camera2recorddemo.OpenGL.Filter.ChooseFilter;
import com.joe.camera2recorddemo.OpenGL.Filter.Mp4EditFilter;
import com.joe.camera2recorddemo.OpenGL.MP4Edior;
import com.joe.camera2recorddemo.OpenGL.Mp4Processor;
import com.joe.camera2recorddemo.OpenGL.Renderer;
import com.joe.camera2recorddemo.OpenGL.Transformation;
import com.joe.camera2recorddemo.R;
import com.joe.camera2recorddemo.Utils.MatrixUtils;
import com.joe.camera2recorddemo.Utils.UriUtils;
import com.joe.camera2recorddemo.View.WheelView.WheelView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MP4Activity_BF extends AppCompatActivity implements View.OnClickListener, TextureView.SurfaceTextureListener {

	private TextureView textureView;
	private String videoPath;
	private Surface mSurface;

	//滤镜部分
	private MP4Edior mp4Edior;
	private ChooseFilter mMp4EditFilter;//滤镜组合
	private int filterIndex = 0;//当前选择滤镜

	//处理模块
	private Mp4Processor mProcessor;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_mp4_bf);
		initView();
		initWheel();
		openFile();
	}

	private void initView() {
		//初始化滤镜
		mp4Edior = new MP4Edior();
		mMp4EditFilter = new ChooseFilter(getResources());
		mMp4EditFilter.setChangeType(0);

		//其他
		textureView = (TextureView) findViewById(R.id.mp4_ttv);
		mProcessor = new Mp4Processor();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.mp4_bt_save:
				saveExec();
				break;
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == 1) {
			if (resultCode == RESULT_OK) {
				final String path = UriUtils.getRealFilePath(getApplicationContext(), data.getData());
				if (path != null) {
					videoPath = path;
					mp4Edior.stop();
					new Thread(new Runnable() {
						@Override
						public void run() {
							mp4Edior.start();
							textureView.setSurfaceTextureListener(MP4Activity_BF.this);
							mp4Edior.setLoop(true);
							mp4Edior.decodePrepare(videoPath);
							mp4Edior.excuate();
						}
					}).start();
				}
			}
		}
	}

	@Override
	public void onSurfaceTextureAvailable(final SurfaceTexture surface, final int width, final int height) {
		Log.v("MP4Sur","++onSurfaceTextureAvailable++");
		mSurface = new Surface(surface);
		mp4Edior.setOutputSurface(mSurface, width, height);
		mp4Edior.setRenderer(new Renderer() {
			@Override
			public void create() {
				mMp4EditFilter.create();
			}

			@Override
			public void sizeChanged(int width2, int height2) {
				mMp4EditFilter.sizeChanged(width2, height2);
				MatrixUtils.flip(mMp4EditFilter.getVertexMatrix(), true, false);
				MatrixUtils.rotation(mMp4EditFilter.getVertexMatrix(),180);
			}

			@Override
			public void draw(int texture) {
				mMp4EditFilter.draw(texture);
			}

			@Override
			public void destroy() {
				Log.v("MP4Sur","++destroy++");
				mMp4EditFilter.destroy();
			}
		});
		mp4Edior.startPreview();
	}

	@Override
	public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
		Log.v("MP4Sur","++onSurfaceTextureSizeChanged++");
		mp4Edior.setOutputSurface(mSurface, width, height);
	}

	@Override
	public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
		Log.v("MP4Sur","++onSurfaceTextureDestroyed++");
		try {
			mp4Edior.stopPreview();
			mMp4EditFilter.destroy();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return true;
	}

	@Override
	public void onSurfaceTextureUpdated(SurfaceTexture surface) {
//		Log.v("MP4Sur","++onSurfaceTextureUpdated++");
	}


	/**
	 * 设置滤镜选择控件
	 */
	private void initWheel() {
		final List<FilterChoose> filterChooses = new ArrayList<>();
		filterChooses.add(new FilterChoose(ChooseFilter.FilterType.NORMAL, "默认"));
		filterChooses.add(new FilterChoose(ChooseFilter.FilterType.COOL, "寒冷"));
		filterChooses.add(new FilterChoose(ChooseFilter.FilterType.WARM, "温暖"));
		filterChooses.add(new FilterChoose(ChooseFilter.FilterType.GRAY, "灰度"));
		filterChooses.add(new FilterChoose(ChooseFilter.FilterType.CAMEO, "浮雕"));
		filterChooses.add(new FilterChoose(ChooseFilter.FilterType.INVERT, "底片"));
		filterChooses.add(new FilterChoose(ChooseFilter.FilterType.SEPIA, "旧照"));
		filterChooses.add(new FilterChoose(ChooseFilter.FilterType.TOON, "动画"));
		filterChooses.add(new FilterChoose(ChooseFilter.FilterType.CONVOLUTION, "卷积"));
		filterChooses.add(new FilterChoose(ChooseFilter.FilterType.SOBEL, "边缘"));
		filterChooses.add(new FilterChoose(ChooseFilter.FilterType.SKETCH, "素描"));
		WheelView wheelView = (WheelView) findViewById(R.id.mp4_change_fliter);
		wheelView.setAdapter(new FilterAdapter(filterChooses));
		wheelView.setOnItemSelectedListener(new WheelView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(int index) {
				changeFilter(filterChooses.get(index).getIndex());
				filterIndex = filterChooses.get(index).getIndex();
				Toast.makeText(MP4Activity_BF.this, "选择滤镜:" + filterChooses.get(index).getName(), Toast.LENGTH_SHORT).show();
			}
		});
	}

	/**
	 * 切换滤镜
	 */
	private void changeFilter(int chooseIndex) {
		if (chooseIndex != filterIndex) {
			mMp4EditFilter.setChangeType(chooseIndex);
		}
	}

	@Override
	protected void onPause() {
		mp4Edior.stop();
		super.onPause();
	}

	/**
	 * 保存滤镜处理文件
	 */
	private void saveExec() {
		if (videoPath != null) {
			final ProgressDialog mProgressDialog = new ProgressDialog(this);
			mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			mProgressDialog.setMax(100);
			mProgressDialog.setCancelable(false);
			mProgressDialog.setCanceledOnTouchOutside(false);
			mProgressDialog.setTitle("正在处理");
			mProgressDialog.setProgress(0);

			mProcessor.setInputPath(videoPath);
			mProcessor.setOutputPath(Environment.getExternalStorageDirectory().getAbsolutePath() + "/temp_loc.mp4");
			mProcessor.setRenderer(new Renderer() {

				Mp4EditFilter mp4EditFilter;

				@Override
				public void create() {
					mp4EditFilter = new Mp4EditFilter(getResources());
					mp4EditFilter.getChooseFilter().setChangeType(filterIndex);
					mp4EditFilter.create();
				}

				@Override
				public void sizeChanged(int width, int height) {
					mp4EditFilter.sizeChanged(width, height);
				}

				@Override
				public void draw(int texture) {
					mp4EditFilter.draw(texture);
				}

				@Override
				public void destroy() {
					mp4EditFilter.destroy();
				}
			});
			mProcessor.setOnCompleteListener(new Mp4Processor.OnProgressListener() {
				@Override
				public void onProgress(long max, long current) {
					Log.e("wuwang", "max/current:" + max + "/" + current);
					float pro = (float) current / max;
					mProgressDialog.setProgress((int) (pro * 100));
				}

				@Override
				public void onComplete(String path) {
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							mProgressDialog.dismiss();
							Toast.makeText(getApplicationContext(), "处理完毕", Toast.LENGTH_SHORT).show();
							Intent v = new Intent(Intent.ACTION_VIEW);
							v.setDataAndType(Uri.parse(Environment.getExternalStorageDirectory().getAbsolutePath() + "/temp_loc.mp4"), "video/mp4");
							startActivity(v);
						}
					});
				}
			});
			try {
				mProgressDialog.show();
				mProcessor.start();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			Toast.makeText(this, "请先选择一个视频文件", Toast.LENGTH_SHORT).show();
		}
	}

	/**
	 * 打开文件
	 */
	private void openFile() {
		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.setType("video/mp4"); //选择视频 （mp4 3gp 是android支持的视频格式）
		intent.addCategory(Intent.CATEGORY_OPENABLE);
		startActivityForResult(intent, 1);
	}
}
