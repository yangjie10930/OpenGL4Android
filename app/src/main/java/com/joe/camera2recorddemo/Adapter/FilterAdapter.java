package com.joe.camera2recorddemo.Adapter;

import com.joe.camera2recorddemo.Entity.FilterChoose;
import com.joe.camera2recorddemo.View.WheelView.WheelView;

import java.util.List;

/**
 * Created by Administrator on 2017/10/20.
 */

public class FilterAdapter extends WheelView.WheelAdapter {

	private List<FilterChoose> strs;

	public FilterAdapter(List<FilterChoose> filterChooses) {
		strs = filterChooses;
	}

	@Override
	protected int getItemCount() {
		return strs.size();
	}

	@Override
	protected String getItem(int index) {
		return strs.get(index).getName();
	}
}
