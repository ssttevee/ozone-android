/**
 * 
 */
package com.ssttevee.cloudapp.widget;

import com.cloudapp.api.CloudAppException;
import com.cloudapp.api.model.CloudAppItem;
import com.cloudapp.api.model.CloudAppItem.Type;
import com.ssttevee.cloudapp.CAApplication;
import com.ssttevee.cloudapp.MainActivity;
import com.ssttevee.cloudapp.R;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class CheckableRelativeLayout extends RelativeLayout {

	public boolean isChecked;
	public CloudAppItem data;
	
	private Context context;
	private Handler handler;
	
	private CheckBox checkBox;
	private TextView itemCounter;
	private TextView itemName;
	private ImageView itemType;
	private ImageView itemPrivate;

	public CheckableRelativeLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.context = context;
		this.handler = new Handler();
	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();

		checkBox = (CheckBox) findViewById(R.id.item_checkbox);
		itemType = (ImageView) findViewById(R.id.item_type);
		itemName = (TextView) findViewById(R.id.item_name);
		itemCounter = (TextView) findViewById(R.id.item_counter);
		itemPrivate = (ImageView) findViewById(R.id.item_private);
		
		checkBox.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				isChecked = !isChecked;
			}
		});
		checkBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {}
		});
	}
	
	public void setData(final CloudAppItem item) {
		this.data = item;
		try {
			itemType.setImageResource(getTypeDrawable(item.getItemType()));
		} catch (CloudAppException e) {
			itemType.setImageResource(R.drawable.type_unknown);
		}
		try {
			itemName.setText(item.getName());
		} catch (CloudAppException e) {
			itemName.setText("Error: File name not found...");
		}
		try {
			itemCounter.setText(item.getViewCounter() + " Views");
		} catch (CloudAppException e) {
			itemName.setText("0 Views");
		}
		try {
			if(item.isPrivate()) {
				itemPrivate.setImageDrawable(getResources().getDrawable(R.drawable.lv_icon_lock));
				itemPrivate.setTag(true);
			} else {
				itemPrivate.setImageDrawable(getResources().getDrawable(R.drawable.lv_icon_unlock));
				itemPrivate.setTag(false);
			}
		} catch (CloudAppException e) {
		}
		itemPrivate.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if(context instanceof MainActivity) {
					new Thread(new Runnable() {
						public void run() {
							try {
								if(((Boolean) itemPrivate.getTag())) {
									((CAApplication) ((MainActivity) context).getApplication()).api.setSecurity(item, false);
									handler.post(new Runnable() {
										public void run() {
											itemPrivate.setImageDrawable(getResources().getDrawable(R.drawable.lv_icon_unlock));
											itemPrivate.setTag(false);
											try {
												Toast.makeText(context, item.getName() + " set to public", Toast.LENGTH_SHORT).show();
											} catch (CloudAppException e) {
												// TODO Auto-generated catch block
												e.printStackTrace();
											}
										}
									});
								} else {
									((CAApplication) ((MainActivity) context).getApplication()).api.setSecurity(item, true);
									handler.post(new Runnable() {
										public void run() {
											itemPrivate.setImageDrawable(getResources().getDrawable(R.drawable.lv_icon_lock));
											itemPrivate.setTag(true);
											try {
												Toast.makeText(context, item.getName() + " set to private", Toast.LENGTH_SHORT).show();
											} catch (CloudAppException e) {
												// TODO Auto-generated catch block
												e.printStackTrace();
											}
										}
									});
								}
							} catch (CloudAppException e) {
								e.printStackTrace();
							}
						}
					}).start();
				}
			}
		});
	}
	
	private int getTypeDrawable(Type type) {
		switch (type) {
		case ARCHIVE:
			return R.drawable.type_archive;
			
		case AUDIO:
			return R.drawable.type_audio;
			
		case BOOKMARK:
			return R.drawable.type_bookmark;
			
		case IMAGE:
			return R.drawable.type_image;
			
		case TEXT:
			return R.drawable.type_text;
			
		case UNKNOWN:
			return R.drawable.type_unknown;

		case VIDEO:
			return R.drawable.type_video;
		}
		
		return R.drawable.type_unknown;
	}
	
	public void setChecked() {
		checkBox.setChecked(true);
		isChecked = true;
	}
	
	public void setUnchecked() {
		checkBox.setChecked(false);
		isChecked = false;
	}
	
	public void setListener(final OnClickListener listener) {
		checkBox.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				isChecked = !isChecked;
				listener.onClick(view);
			}
		});
	}
	
}
