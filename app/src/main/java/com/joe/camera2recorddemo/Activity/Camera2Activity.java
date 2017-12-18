package com.joe.camera2recorddemo.Activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.RequiresApi;
import android.support.v13.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.joe.camera2recorddemo.OpenGL.Filter.BeautyFilter;
import com.joe.camera2recorddemo.OpenGL.CameraRecorder;
import com.joe.camera2recorddemo.OpenGL.Filter.Filter;
import com.joe.camera2recorddemo.OpenGL.Renderer;
import com.joe.camera2recorddemo.R;
import com.joe.camera2recorddemo.Utils.MatrixUtils;

import java.io.IOException;
import java.util.Arrays;

public class Camera2Activity extends AppCompatActivity implements View.OnClickListener, Renderer {

	private SurfaceView mSurfaceView;
	private SurfaceHolder mSurfaceHolder;
	private CameraManager mCameraManager;//摄像头管理器
	private Handler childHandler, mainHandler;
	private String mCameraID;//摄像头Id 0 为后  1 为前
	private CameraCaptureSession mCameraCaptureSession;
	private CameraDevice mCameraDevice;

	private CameraRecorder mCameraRecord;
	private Filter mFilter;
	private int mCameraWidth, mCameraHeight;
	private boolean isStart = false;
	private TextView mTvStart;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_camera2);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		initVIew();
	}

	/**
	 * 初始化
	 */
	private void initVIew() {
		//mSurfaceView
		mSurfaceView = (SurfaceView) findViewById(R.id.surface_view_camera2_activity);
		mTvStart = (TextView) findViewById(R.id.mTvStart);
		//GL
		mSurfaceView.setOnClickListener(this);
		mFilter = new BeautyFilter(getResources()).setBeautyLevel(5);
		mCameraRecord = new CameraRecorder();
		mCameraRecord.setOutputPath(Environment.getExternalStorageDirectory().getAbsolutePath() + "/temp_cam.mp4");
		mSurfaceHolder = mSurfaceView.getHolder();
		mSurfaceHolder.setKeepScreenOn(true);
		// mSurfaceView添加回调
		mSurfaceHolder.addCallback(new SurfaceHolder.Callback() {
			@Override
			public void surfaceCreated(SurfaceHolder holder) { //SurfaceView创建
//				initCamera2();
				// 初始化Camera
				mCameraRecord.setOutputSurface(holder.getSurface());
				mCameraRecord.setOutputSize(new Size(768, 1080));
				mCameraRecord.setRenderer(Camera2Activity.this);
			}

			@Override
			public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
				Log.v("Camera2CC", "surfaceChanged w:" + width + ";h:" + height);
				mCameraRecord.setPreviewSize(width, height);
				mCameraRecord.startPreview();
			}

			@Override
			public void surfaceDestroyed(SurfaceHolder holder) { //SurfaceView销毁
				if (isStart) {
					isStart = false;
					try {
						mCameraRecord.stopRecord();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					mTvStart.setText("开始");
				}
				try {
					mCameraRecord.stopPreview();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				// 释放Camera资源
				if (null != mCameraDevice) {
					mCameraDevice.close();
					Camera2Activity.this.mCameraDevice = null;
				}
			}
		});
	}

	/**
	 * 初始化Camera2
	 */
	@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
	private void initCamera2() {
		HandlerThread handlerThread = new HandlerThread("Camera2");
		handlerThread.start();
		childHandler = new Handler(handlerThread.getLooper());
		mainHandler = new Handler(getMainLooper());
		mCameraID = "" + CameraCharacteristics.LENS_FACING_FRONT;//后摄像头
		//获取摄像头管理
		mCameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
		try {
			if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
				return;
			}
			//打开摄像头
			mCameraManager.openCamera(mCameraID, stateCallback, mainHandler);
		} catch (CameraAccessException e) {
			e.printStackTrace();
		}
	}


	/**
	 * 摄像头创建监听
	 */
	private CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
		@Override
		public void onOpened(CameraDevice camera) {//打开摄像头
			mCameraDevice = camera;
			//开启预览
			takePreview();
		}

		@Override
		public void onDisconnected(CameraDevice camera) {//关闭摄像头
			if (null != mCameraDevice) {
				mCameraDevice.close();
				Camera2Activity.this.mCameraDevice = null;
			}
		}

		@Override
		public void onError(CameraDevice camera, int error) {//发生错误
			Toast.makeText(Camera2Activity.this, "摄像头开启失败", Toast.LENGTH_SHORT).show();
		}
	};

	/**
	 * 开始预览
	 */
	private void takePreview() {
		try {

			// 创建预览需要的CaptureRequest.Builder
			final CaptureRequest.Builder previewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
			// 将SurfaceView的surface作为CaptureRequest.Builder的目标
			SurfaceTexture surfaceTexture = mCameraRecord.createInputSurfaceTexture();
			Surface surface = new Surface(surfaceTexture);
			previewRequestBuilder.addTarget(surface);
			// 创建CameraCaptureSession，该对象负责管理处理预览请求和拍照请求
			mCameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() // ③
			{
				@Override
				public void onConfigured(CameraCaptureSession cameraCaptureSession) {
					if (null == mCameraDevice) return;
					// 当摄像头已经准备好时，开始显示预览
					mCameraCaptureSession = cameraCaptureSession;
					try {
						// 自动对焦
						previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
						// 打开闪光灯
						previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
						// 显示预览
						CaptureRequest previewRequest = previewRequestBuilder.build();
						mCameraCaptureSession.setRepeatingRequest(previewRequest, null, childHandler);
					} catch (CameraAccessException e) {
						e.printStackTrace();
					}
				}

				@Override
				public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
					Toast.makeText(Camera2Activity.this, "配置失败", Toast.LENGTH_SHORT).show();
				}
			}, childHandler);
		} catch (CameraAccessException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 点击事件
	 */
	@Override
	public void onClick(View view) {
		switch (view.getId()) {
			case R.id.mTvStart:
				isStart = !isStart;
				mTvStart.setText(isStart ? "停止" : "开始");
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
		}
	}

	@Override
	public void create() {
		initCamera2();
		mCameraWidth = 768;
		mCameraHeight = 1080;
		mFilter.create();
	}

	private int width, height;

	@Override
	public void sizeChanged(int width, int height) {
		this.width = 768;
		this.height = 1080;
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
}
