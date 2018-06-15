package com.joe.camera2recorddemo.Activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.joe.camera2recorddemo.Adapter.FilterAdapter;
import com.joe.camera2recorddemo.Entity.FilterChoose;
import com.joe.camera2recorddemo.OpenGL.CameraRecorder;
import com.joe.camera2recorddemo.OpenGL.Filter.ChooseFilter;
import com.joe.camera2recorddemo.OpenGL.Filter.Mp4EditFilter;
import com.joe.camera2recorddemo.OpenGL.Renderer;
import com.joe.camera2recorddemo.OpenGL.Transformation;
import com.joe.camera2recorddemo.R;
import com.joe.camera2recorddemo.Utils.MatrixUtils;
import com.joe.camera2recorddemo.View.WheelView.WheelView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Yj on 2017/9/12.
 */

public class CameraActivity extends AppCompatActivity implements Renderer {

	private CameraRecorder mCameraRecord; //GP录像类
	private TextureView mTextureView;//TextureView显示
	private Camera mCamera; //相机
	private Button mTvStart;
	private boolean isStart = false; //是否开始录像
	private Mp4EditFilter mFilter; //基础滤镜
	private int mCameraWidth, mCameraHeight;
	private Camera.Parameters parameters; //相机设置
	private int mUserCamera = 0;//当前使用的摄像头
	private String[] mCameraList;//摄像头个数

	private int filterIndex = 0;//当前选择滤镜
	private Size mSize = new Size(720,1080);
	private Size mOneSize = new Size(720,720);

	/**
	 * 延时发送通知开始自动对焦
	 */
	Handler mCameraAutoFocusCallbackHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			mCamera.autoFocus(new Camera.AutoFocusCallback() {
				@Override
				public void onAutoFocus(boolean success, Camera camera) {
					if (success) {
						camera.cancelAutoFocus();
						doAutoFocus();
					}
				}
			});
		}
	};

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_camera);
		getCameraCount();//获取摄像头个数
		initWheel();//初始化滤镜选择空间
		initView();
	}

	/**
	 * 初始化
	 */
	private void initView(){
		mTextureView = (TextureView) findViewById(R.id.mTexture);
		mTvStart = (Button) findViewById(R.id.mTvStart);
		//设置滤镜
		mFilter = new Mp4EditFilter(getResources());
		mCameraRecord = new CameraRecorder();

		mCameraRecord.setOutputPath(Environment.getExternalStorageDirectory().getAbsolutePath() + "/temp_cam.mp4");
		mTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
			@Override
			public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
				mCamera = Camera.open(0);
				mCameraRecord.setOutputSurface(new Surface(surface));
				mCameraRecord.setOutputSize(mSize);
				mCameraRecord.setRenderer(CameraActivity.this);
				mCameraRecord.setPreviewSize(width, height);
				mCameraRecord.startPreview();
			}

			@Override
			public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
				mCameraRecord.setPreviewSize(width, height);
			}

			@Override
			public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
				if (isStart) {
					isStart = false;
					try {
						mCameraRecord.stopRecord();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					mTvStart.setText("开始");
				}
				stopPreview();
				return true;
			}

			@Override
			public void onSurfaceTextureUpdated(SurfaceTexture surface) {

			}
		});
	}


	public void onClick(View view) {
		switch (view.getId()) {
			case R.id.mTvStart:
				isStart = !isStart;
				mTvStart.setText(isStart ? "停止" : "开始");
				mTvStart.setBackgroundResource(isStart ? R.drawable.record_stop : R.drawable.record_start);
				if (isStart) {
					try {
						mCameraRecord.startRecord();
					} catch (IOException e) {
						e.printStackTrace();
					}
				} else {
					try {
						mCameraRecord.stopRecord();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					Intent v = new Intent(Intent.ACTION_VIEW);
					v.setDataAndType(Uri.parse(Environment.getExternalStorageDirectory().getAbsolutePath() + "/temp_cam.mp4"), "video/mp4");
					startActivity(v);
				}
				break;
			case R.id.mChange:
				changeCamera();
				break;
			case R.id.mOne:
				changeScale(true);
				break;
		}
	}

	@Override
	public void create() {
		try {
			mCamera.setPreviewTexture(mCameraRecord.createInputSurfaceTexture());
		} catch (IOException e) {
			e.printStackTrace();
		}
		Camera.Size mSize = mCamera.getParameters().getPreviewSize();
		mCameraWidth = mSize.height;
		mCameraHeight = mSize.width;
		mCamera.startPreview();
		if (mUserCamera == 0) {
			mCameraAutoFocusCallbackHandler.sendMessageDelayed(new Message(), 1000);
		}
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

	/**
	 * 设置相机自动对焦
	 */
	private void doAutoFocus() {
		parameters = mCamera.getParameters();
		parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
		mCamera.setParameters(parameters);
		mCamera.autoFocus(new Camera.AutoFocusCallback() {
			@Override
			public void onAutoFocus(boolean success, Camera camera) {
				if (success) {
					camera.cancelAutoFocus();// 只有加上了这一句，才会自动对焦。
					if (!Build.MODEL.equals("KORIDY H30")) {
						parameters = camera.getParameters();
						parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
						camera.setParameters(parameters);
					} else {
						parameters = camera.getParameters();
						parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
						camera.setParameters(parameters);
					}
				}
			}
		});
	}

	/**
	 * 获取摄像头个数
	 */
	private void getCameraCount() {
		CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
		if (mCameraList == null) {
			try {
				mCameraList = manager.getCameraIdList();
			} catch (CameraAccessException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 切换摄像头
	 */
	private void changeCamera() {
		if (mCameraList.length > 1) {
			stopPreview();
			mUserCamera += 1;
			if (mUserCamera > mCameraList.length - 1) mUserCamera = 0;
			mCamera = Camera.open(mUserCamera);
			mCameraRecord.startPreview();
		}
	}

	/**
	 * 设置显示比例
	 */
	private void changeScale(boolean one){
		if(one){
			stopPreview();
			int mTextureViewWidth = mTextureView.getWidth();
			RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(mTextureViewWidth,mTextureViewWidth);
			layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT,RelativeLayout.TRUE);
			mTextureView.setLayoutParams(layoutParams);
			mCameraRecord.setOutputSize(mOneSize);
			mCamera = Camera.open(mUserCamera);
			mCameraRecord.startPreview();
		}
	}

	/**
	 * 切换滤镜
	 */
	private void changeFilter(int chooseIndex) {
		if (chooseIndex != filterIndex) {
			mFilter.getChooseFilter().setChangeType(chooseIndex);
		}
	}

	/**
	 * 摄像头停止预览
	 */
	private void stopPreview() {
		try {
			mCameraRecord.stopPreview();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		if (mCamera != null) {
			mCamera.stopPreview();
			mCamera.release();
			mCamera = null;
		}
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
		WheelView wheelView = (WheelView) findViewById(R.id.change_fliter);
		wheelView.setAdapter(new FilterAdapter(filterChooses));
		wheelView.setOnItemSelectedListener(new WheelView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(int index) {
				changeFilter(filterChooses.get(index).getIndex());
				filterIndex = filterChooses.get(index).getIndex();
				Toast.makeText(CameraActivity.this, "选择滤镜:" + filterChooses.get(index).getName(), Toast.LENGTH_SHORT).show();
			}
		});
	}

}