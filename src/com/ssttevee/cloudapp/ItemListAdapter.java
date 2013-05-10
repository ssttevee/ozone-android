package com.ssttevee.cloudapp;

import java.util.ArrayList;
import java.util.List;

import com.cloudapp.api.CloudAppException;
import com.cloudapp.api.model.CloudAppItem;
import com.ssttevee.cloudapp.widget.CheckableRelativeLayout;

import android.content.Context;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

public class ItemListAdapter extends BaseAdapter {

    private Context context;
	private List<CloudAppItem> fullList;
	private List<CloudAppItem> caiList;
	private CloudAppItem.Type filter;
	
	private List<CloudAppItem> checkedItems;
	private CBListener listener;

	public ItemListAdapter(Context context, List<CloudAppItem> fullList) {
        this.context = context;
        this.fullList = fullList;
        this.caiList = fullList;
        this.checkedItems = new ArrayList<CloudAppItem>();
	}
    
	@Override
	public int getCount() {
		return caiList.size();
	}

	@Override
	public CloudAppItem getItem(int pos) {
		return caiList.get(pos);
	}

	@Override
	public long getItemId(int pos) {
		return pos;
	}

	@Override
	public View getView(final int pos, View convertView, ViewGroup parent) {
    	
		CheckableRelativeLayout crl;
		if(convertView == null) crl = (CheckableRelativeLayout) View.inflate(context, R.layout.listview_cloudapp_item, null);
		else crl = (CheckableRelativeLayout) convertView;
		
		final CheckableRelativeLayout fcrl = crl;
		fcrl.setData(caiList.get(pos));
		if(checkedItems.contains(caiList.get(pos))) fcrl.setChecked();
		else fcrl.setUnchecked();
		fcrl.setListener(new OnClickListener() {
			
			@Override
			public void onClick(View view) {
				if(fcrl.isChecked) {
					checkedItems.add(getItem(pos));
				} else {
					checkedItems.remove(getItem(pos));
				}
				listener.onCheckBoxChanged(!checkedItems.isEmpty());
			}
		});
		return fcrl;
	}
	
	public void setListener(CBListener listener) {
		this.listener = listener;
	}
	
	public List<CloudAppItem> getCheckedItems() {
		return checkedItems;
	}
	
	public void uncheckAllItems() {
		checkedItems.clear();
	}
	
	public void setFilter(CloudAppItem.Type type) {
		filter = type;
		System.out.println("Setting Filter to: " + filter);
		if(filter == null) {
			caiList = fullList;
		} else {
			caiList = new ArrayList<CloudAppItem>();
			for (CloudAppItem cloudAppItem : fullList) {
				try {
					if(cloudAppItem.getItemType() == filter)
						caiList.add(cloudAppItem);
				} catch (CloudAppException e) {
					e.printStackTrace();
				}
			}
		}
        uncheckAllItems();
	}
	
	public void setItems(List<CloudAppItem> fullList) {
        this.fullList = fullList;
        setFilter(filter);
        uncheckAllItems();
	}
	
	interface CBListener {
	    void onCheckBoxChanged(boolean checked);
	}
}
