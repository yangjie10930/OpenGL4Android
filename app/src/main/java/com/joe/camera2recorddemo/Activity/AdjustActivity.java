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
import android.view.ViewTreeObserver;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Toast;

import com.joe.camera2recorddemo.OpenGL.MP4Edior;
import com.joe.camera2recorddemo.OpenGL.MhFilter.AdjustFilter;
import com.joe.camera2recorddemo.OpenGL.Mp4Processor;
import com.joe.camera2recorddemo.OpenGL.Renderer;
import com.joe.camera2recorddemo.OpenGL.Transformation;
import com.joe.camera2recorddemo.R;
import com.joe.camera2recorddemo.Utils.FormatUtils;
import com.joe.camera2recorddemo.Utils.MatrixUtils;
import com.joe.camera2recorddemo.Utils.UriUtils;

import java.io.IOException;

public class AdjustActivity extends AppCompatActivity implements View.OnClickListener, TextureView.SurfaceTextureListener, SeekBar.OnSeekBarChangeListener {

	private TextureView textureView;
	private String videoPath;
	private Surface mSurface;

	//滤镜部分
	private MP4Edior mp4Edior;
	private AdjustFilter adjustFilter;//滤镜组合
	//处理模块
	private Mp4Processor mProcessor;

	//处理类的参数
	private Transformation mTransformation;
	private int rotation = 0;//旋转角度
	private Size mSize, mPreSize;//视频尺寸,预览尺寸

	//视频信息
	FormatUtils.VideoFormat videoFormat;

	//设置的值
	float fC = 1.0f, fB = 0.0f, fS = 1.0f, fV = 0.75f;
	private String path;//视频地址

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_pic);
		initView();
		openFile();
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	private void initView() {
		//初始化滤镜
		mp4Edior = new MP4Edior();
		adjustFilter = new AdjustFilter(getResources());
		mTransformation = new Transformation();

		//其他
		textureView = (TextureView) findViewById(R.id.mp4_ttv);
		mProcessor = new Mp4Processor();

		//SeekBar
		((SeekBar) findViewById(R.id.pic_sk_contrast)).setOnSeekBarChangeListener(this);
		((SeekBar) findViewById(R.id.pic_sk_brightness)).setOnSeekBarChangeListener(this);
		((SeekBar) findViewById(R.id.pic_sk_saturation)).setOnSeekBarChangeListener(this);
		((SeekBar) findViewById(R.id.pic_sk_vignette)).setOnSeekBarChangeListener(this);
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
				path = UriUtils.getRealFilePath(getApplicationContext(), data.getData());
				if (path != null) {
					videoPath = path;
					videoFormat = FormatUtils.getVideoFormat(path);
					//等待绘制完成
					ViewTreeObserver vto2 = textureView.getViewTreeObserver();
					vto2.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
						@Override
						public void onGlobalLayout() {
							textureView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
							//设置控件大小
							RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) textureView.getLayoutParams();
							if (videoFormat.width > videoFormat.height) {
								layoutParams.height = textureView.getWidth() * videoFormat.height / videoFormat.width;
							} else {
								layoutParams.width = textureView.getHeight() * videoFormat.width / videoFormat.height;
							}
							textureView.setLayoutParams(layoutParams);
							//开始解码
							mp4Edior.stop();
							new Thread(new Runnable() {
								@Override
								public void run() {
									mp4Edior.start();
									textureView.setSurfaceTextureListener(AdjustActivity.this);
									mp4Edior.setLoop(true);
									mp4Edior.decodePrepare(videoPath);
									mSize = mp4Edior.getSize();
									mTransformation.setScale(mSize, mPreSize, MatrixUtils.TYPE_CENTERINSIDE);
									mp4Edior.setTransformation(mTransformation);
									mp4Edior.excuate();
								}
							}).start();
						}
					});
				}
			}
		}
	}

	@Override
	public void onSurfaceTextureAvailable(final SurfaceTexture surface, final int width, final int height) {
		mPreSize = new Size(width, height);
		mSurface = new Surface(surface);
		mp4Edior.setOutputSurface(mSurface, width, height);
		mp4Edior.setRenderer(new Renderer() {
			@Override
			public void create() {
				adjustFilter.create();
			}

			@Override
			public void sizeChanged(int width2, int height2) {
				adjustFilter.sizeChanged(width2, height2);
				MatrixUtils.flip(adjustFilter.getVertexMatrix(), true, false);
				MatrixUtils.rotation(adjustFilter.getVertexMatrix(), 90);
			}

			@Override
			public void draw(int texture) {
				adjustFilter.draw(texture);
			}

			@Override
			public void destroy() {
				adjustFilter.destroy();
			}
		});
		mp4Edior.startPreview();
	}

	@Override
	public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
		mPreSize = new Size(width, height);
		mp4Edior.setOutputSurface(mSurface, width, height);
	}

	@Override
	public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
		try {
			mp4Edior.stopPreview();
			adjustFilter.destroy();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return true;
	}

	@Override
	public void onSurfaceTextureUpdated(SurfaceTexture surface) {
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
			mProcessor.setFilterRotation(rotation);
			mProcessor.setRenderer(new Renderer() {

				AdjustFilter adFilter;

				@Override
				public void create() {
					adFilter = new AdjustFilter(getResources());
					adFilter.getContrastFilter().setContrastCode(fC);
					adFilter.getBrightnessFilter().setBrightnessCode(fB);
					adFilter.getSaturationFilter().setSaturationCode(fS);
					adFilter.getVignetteFilter().setmVignetteStart(fV);
					Transformation transformation = new Transformation();
					transformation.setRotation(rotation);
					adFilter.getDistortionFilter().setTransformation(transformation);
					adFilter.create();
				}

				@Override
				public void sizeChanged(int width, int height) {
					adFilter.sizeChanged(width, height);
				}

				@Override
				public void draw(int texture) {
					adFilter.draw(texture);
				}

				@Override
				public void destroy() {
					adFilter.destroy();
				}
			});
			mProcessor.setOnCompleteListener(new Mp4Processor.OnProgressListener() {
				@Override
				public void onProgress(long max, long current) {
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
		intent.setType("video/mp4"); //选择视频
		intent.addCategory(Intent.CATEGORY_OPENABLE);
		startActivityForResult(intent, 1);
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		switch (seekBar.getId()) {
			case R.id.pic_sk_contrast:
				fC = (float) progress / 10;
				adjustFilter.getContrastFilter().setContrastCode(fC);
				break;
			case R.id.pic_sk_brightness:
				fB = (float) (progress - 10) / 10;
				adjustFilter.getBrightnessFilter().setBrightnessCode(fB);
				break;
			case R.id.pic_sk_saturation:
				fS = (float) progress / 10;
				adjustFilter.getSaturationFilter().setSaturationCode(fS);
				break;
			case R.id.pic_sk_vignette:
				fV = (float) (75 - progress) /100;
				adjustFilter.getVignetteFilter().setmVignetteStart(fV);
				break;
		}
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {

	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {

	}
}
