package com.tao.controllerd;

import java.util.Calendar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class AlarmSettingHelper 
{
	private Context context;
	public AlarmSettingHelper(Context context)
	{
		this.context = context;
	}
	public void setupAlarms()
	{
		SQLiteDatabase db = ServerThread.dbHelper.getReadableDatabase();
		Cursor cursor = db.query("tasks",new String[]{"rowid","ids","time","dow","lightness","ispaused"},
				null,null,null,null,null);
		while (cursor.moveToNext()) 
		{
			int ispaused = cursor.getInt(cursor.getColumnIndex("ispaused"));
			if (ispaused == 1) continue;
			int rowid = cursor.getInt(cursor.getColumnIndex("rowid"));
			String ids = cursor.getString(cursor.getColumnIndex("ids"));
			String time = cursor.getString(cursor.getColumnIndex("time"));
			String dow = cursor.getString(cursor.getColumnIndex("dow"));
			int lightness = cursor.getInt(cursor.getColumnIndex("lightness"));
			registAlarm(rowid, time, dow, ids, lightness);
		}
	}
	public void registAlarm(int requestId, String time, String dow, String ids, int lightness)
	{
		Calendar cal = Calendar.getInstance();
		String[] t = time.split(":");
		cal.set(Calendar.HOUR_OF_DAY, Integer.valueOf(t[0]));
		cal.set(Calendar.MINUTE, Integer.valueOf(t[1]));
		cal.set(Calendar.SECOND, 0);
		Intent intent = new Intent(context, AlarmBroadcastReceiver.class);
		intent.putExtra("hour", t[0]);
		intent.putExtra("minute", t[1]);
		intent.putExtra("dow", dow);
		intent.putExtra("ids", ids);
		intent.putExtra("lightness", lightness);
		
		PendingIntent pi = PendingIntent.getBroadcast(context, requestId, intent, 0);
		AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		am.setRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pi);
	}
	public void unregistAlarm(int requestId)
	{
		Intent intent = new Intent(context, AlarmBroadcastReceiver.class);
		PendingIntent pi = PendingIntent.getBroadcast(context, requestId, intent, 0);
		AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		am.cancel(pi);
	}
}
