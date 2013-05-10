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
	
	/**
	 * Checks if the payload in the given purchase is the same as 
	 * the payload given, then resets the payload back to nothing
	 * 
	 * @param p
	 * 			the purchase containing the payload to compare
     * @return {@code true} if the payloads are the same,
     *         {@code false} otherwise.
	 */
    public boolean verifyDeveloperPayload(Purchase p) {
    	String payload = this.payload;
    	this.payload = "";
        return payload.equals(p.getDeveloperPayload());
    }

}
