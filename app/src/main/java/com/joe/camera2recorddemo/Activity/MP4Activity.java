package com.joe.camera2recorddemo.Activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Toast;

import com.joe.camera2recorddemo.Adapter.FilterAdapter;
import com.joe.camera2recorddemo.Entity.FilterChoose;
import com.joe.camera2recorddemo.MediaCodecUtil.VideoDecode;
import com.joe.camera2recorddemo.OpenGL.Filter.ChooseFilter;
import com.joe.camera2recorddemo.OpenGL.MP4Edior;
import com.joe.camera2recorddemo.OpenGL.Mp4Processor;
import com.joe.camera2recorddemo.OpenGL.Renderer;
import com.joe.camera2recorddemo.R;
import com.joe.camera2recorddemo.Utils.MatrixUtils;
import com.joe.camera2recorddemo.Utils.UriUtils;
import com.joe.camera2recorddemo.View.WheelView.WheelView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MP4Activity extends AppCompatActivity implements View.OnClickListener, TextureView.SurfaceTextureListener {

	private TextureView textureView;
	private VideoDecode mVideoDecode;
	private String videoPath;
	private Surface mSurface;

	//滤镜部分
	private MP4Edior mp4Edior;
	private ChooseFilter mFilter; //基础滤镜
	private int mCameraWidth, mCameraHeight;
	private int filterIndex = 0;//当前选择滤镜

	//处理模块
	private Mp4Processor mProcessor;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_mp4);
		initView();
		initWheel();
	}

	private void initView() {
		//设置滤镜
		mp4Edior = new MP4Edior();
		mFilter = new ChooseFilter(getResources());
		mFilter.setChangeType(0);
		textureView = (TextureView) findViewById(R.id.mp4_ttv);
//		textureView.setRotation(270);
		mVideoDecode = new VideoDecode();
		mProcessor = new Mp4Processor();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.mp4_open_bt:
				Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
				intent.setType("video/mp4"); //选择视频 （mp4 3gp 是android支持的视频格式）
				intent.addCategory(Intent.CATEGORY_OPENABLE);
				startActivityForResult(intent, 1);
				break;
			case R.id.mp4_exec_bt:
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
					Log.v("MP4Activity", "path:" + path);
					videoPath = path;
					mVideoDecode.stop();
					new Thread(new Runnable() {
						@Override
						public void run() {
							mVideoDecode.start();
							textureView.setSurfaceTextureListener(MP4Activity.this);
							mVideoDecode.setLoop(true);
							mVideoDecode.decodePrepare(videoPath);
							mVideoDecode.excuate();
						}
					}).start();
				}
			}
		}
	}

	@Override
	public void onSurfaceTextureAvailable(final SurfaceTexture surface, int width, int height) {
		Log.v("MP4Activity", "+++onSurfaceTextureAvailable+++:" + width + "," + height);
		mCameraWidth = width;
		mCameraHeight = height;
		mSurface = new Surface(surface);
		mp4Edior.setOutputSurface(mSurface);
		mp4Edior.setRenderer(new Renderer() {
			@Override
			public void create() {
				mVideoDecode.setSurface(new Surface(mp4Edior.createInputSurfaceTexture()));
				mFilter.create();
			}

			@Override
			public void sizeChanged(int width, int height) {
				mFilter.sizeChanged(width, height);
				MatrixUtils.getMatrix(mFilter.getVertexMatrix(), MatrixUtils.TYPE_CENTERCROP, mCameraWidth, mCameraHeight, width, height);
				MatrixUtils.flip(mFilter.getVertexMatrix(), false, true);
			}

			@Override
			public void draw(int texture) {
				mFilter.draw(texture);
			}

			@Override
			public void destroy() {
				mFilter.destroy();
			}
		});
		mp4Edior.setPreviewSize(width, height);
		mp4Edior.startPreview();
	}

	@Override
	public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
		Log.v("MP4Activity", "+++onSurfaceTextureSizeChanged+++" + width + "," + height);
		mp4Edior.setPreviewSize(width, height);
	}

	@Override
	public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
		Log.v("MP4Activity", "+++onSurfaceTextureDestroyed+++");
		try {
			mp4Edior.stopPreview();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return true;
	}

	@Override
	public void onSurfaceTextureUpdated(SurfaceTexture surface) {
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
				Toast.makeText(MP4Activity.this, "选择滤镜:" + filterChooses.get(index).getName(), Toast.LENGTH_SHORT).show();
			}
		});
	}

	/**
	 * 切换滤镜
	 */
	private void changeFilter(int chooseIndex) {
		if (chooseIndex != filterIndex) {
			mFilter.setChangeType(chooseIndex);
		}
	}

	@Override
	protected void onDestroy() {
		mVideoDecode.stop();
		super.onDestroy();
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
				ChooseFilter filter;

				@Override
				public void create() {
					filter = new ChooseFilter(getResources());
					filter.setChangeType(filterIndex);
					filter.create();
				}

				@Override
				public void sizeChanged(int width, int height) {
					filter.sizeChanged(width, height);
				}

				@Override
				public void draw(int texture) {
					filter.draw(texture);
				}

				@Override
				public void destroy() {
					filter.destroy();
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
		}else{
			Toast.makeText(this, "请先选择一个视频文件", Toast.LENGTH_SHORT).show();
		}
	}
}
