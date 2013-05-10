package com.ssttevee.cloudapp.widget;

import com.ssttevee.cloudapp.R;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.view.View;
import android.view.View.OnClickListener;

public class ListFooter extends RelativeLayout implements OnClickListener {
	
	private TextView more;
	private ProgressBar pb;

	public ListFooter(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected void onFinishInflate() {
		this.setOnClickListener(this);
		this.more = (TextView) findViewById(R.id.indicator_text);
		this.pb = (ProgressBar) findViewById(R.id.progress_bar);
		super.onFinishInflate();
	}

	@Override
	public void onClick(View v) {
		this.more.setVisibility(GONE);
		this.pb.setVisibility(VISIBLE);
	}
	
	public void hideProgress() {
		this.more.setVisibility(VISIBLE);
		this.pb.setVisibility(GONE);
	}
	
	
	
}
