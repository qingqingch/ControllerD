package com.tao.controllerd;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends Activity 
{
	private TextView connectMsg;
	private static Thread listenThread = null;
	private ConnectMsgReceiver connectMsgReceiver;
	
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_page);
		connectMsg = (TextView) findViewById(R.id.textView);
		registMsgReciever();
		if (listenThread == null)
		{
			listenThread = new Thread(new ServerThread(this));
			listenThread.start();
		}
	}
	
	void registMsgReciever()
	{
		connectMsgReceiver = new ConnectMsgReceiver();
		registerReceiver(connectMsgReceiver, new IntentFilter("connected"));
	}

	protected void onDestroy() 
	{
		super.onDestroy();
		unregisterReceiver(connectMsgReceiver);
	}
	
	private class ConnectMsgReceiver extends BroadcastReceiver
	{
		public void onReceive(Context context, Intent intent) 
		{
			String msgs = connectMsg.getText().toString() + "\n" + intent.getStringExtra("connect_info");
			connectMsg.setText(msgs);
		}
	}
}
