package com.joe.camera2recorddemo.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;

import com.joe.camera2recorddemo.R;

public class MainActivity extends FragmentActivity implements View.OnClickListener{

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
//		getFragmentManager().beginTransaction()
//				.add(R.id.ff_ll, Camera2Fragment.newInstance())
//				.commitAllowingStateLoss();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()){
			case R.id.go_camera:
				startActivity(new Intent(MainActivity.this,CameraActivity.class));
				break;
			case R.id.go_mp4:
				startActivity(new Intent(MainActivity.this,MP4Activity.class));
				break;
			case R.id.go_pic:
				startActivity(new Intent(MainActivity.this,AdjustActivity.class));
				break;
		}
	}
}
