package com.tao.controllerd;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class AlarmBroadcastReceiver extends BroadcastReceiver 
{
	public void onReceive(Context context, Intent intent) 
	{
		String hour = intent.getStringExtra("hour");
		String minute = intent.getStringExtra("minute");
		String dow = intent.getStringExtra("dow");
		String ids = intent.getStringExtra("ids");
		int lightness = intent.getIntExtra("lightness", 0);
		Calendar cal = Calendar.getInstance();
		String cur_dow = String.valueOf(cal.get(Calendar.DAY_OF_WEEK));
		String cur_hour = String.valueOf(cal.get(Calendar.HOUR_OF_DAY));
		String cur_minute = String.valueOf(cal.get(Calendar.MINUTE));
		
		if (dow.contains(cur_dow) && hour.equals(cur_hour) && minute.equals(cur_minute))
		{
			new Thread(new CommandThread(ids, lightness)).start();
		}
	}
	private class CommandThread implements Runnable
	{
		private String ids;
		private int lightness;
		public CommandThread(String ids, int lightness) 
		{
			this.ids = ids;
			this.lightness = lightness;
		}
		public void run()
		{
			try
			{
				SQLiteDatabase db = ServerThread.dbHelper.getWritableDatabase();
				Cursor cur = db.rawQuery("select path from devices", null);
				String[] devIds = ids.split(",");
				if (cur.getCount() == devIds.length) 
				{
					Set<String> pathSet = new HashSet<>();
					while (cur.moveToNext()) 
					{
						String path = cur.getString(0);
						int i = path.indexOf(",");
						if (i != -1) pathSet.add("65535," + path.substring(i + 1, path.length()-1));
					}
					pathSet.add("65535");
					
					for (int i = 0; i < 3; i ++)
					{
						for (String p : pathSet) 
						{
							ServerThread.serialPort.sendCmd(SerialPort.CmdType.OPERATE_ALL, p, lightness);
							Thread.sleep(500);
						}
					}
					
					for (String devId : devIds)
					{
						ContentValues cvs = new ContentValues();
						cvs.put("lightness", lightness);
						db.update("devices", cvs, "id=?", new String[]{devId});
					}
				}
				else
				{
					for (String devId : devIds) 
					{
						Cursor cursor = db.query("devices", new String[]{"path"}, "id=?", new String[]{devId}, 
								null, null, null);
						while (cursor.moveToNext()) 
						{
							String path = cursor.getString(cursor.getColumnIndex("path"));
							String p = path.substring(1, path.indexOf("]"));
							int success = 0;
							for (int i = 0; i < 3; i++)
							{
								success = ServerThread.serialPort.sendCmd(SerialPort.CmdType.OPERATE, p, lightness);
								if (success == 0) break;
								Thread.sleep(500);
							}
							if (success == 0)
							{
								ContentValues cvs = new ContentValues();
								cvs.put("lightness", lightness);
								db.update("devices", cvs, "id=?", new String[]{devId});
							}
						}
					}
					
				}
				String response = "cmd_type=update_device&data=";
				response += SocketThread.getDevices(db);
				ServerThread.sendResponse(response);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}
	
}