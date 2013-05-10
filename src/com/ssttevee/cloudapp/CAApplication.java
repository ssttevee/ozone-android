package com.ssttevee.cloudapp;

import com.cloudapp.api.CloudApp;
import com.cloudapp.api.model.CloudAppItem;
import com.example.android.trivialdrivesample.util.Purchase;

import android.app.Application;

public class CAApplication extends Application {
	public CloudApp api;
	public int itemsPerPage = 20;
	public String payload = "";
	public CloudAppItem itemForViewing = null;

	public CAApplication() {
		// TODO Auto-generated constructor stub
	}
    public boolean verifyDeveloperPayload(Purchase p) {
        return payload.equals(p.getDeveloperPayload());
    }

}
