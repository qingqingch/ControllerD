package com.tao.controllerd;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootBroadcastReceiver extends BroadcastReceiver
{
	public void onReceive(Context context, Intent intent) 
	{
		if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) 
		{
			Intent bootIntent = new Intent(context, MainActivity.class);
			bootIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startActivity(bootIntent);
		}
	}
}
