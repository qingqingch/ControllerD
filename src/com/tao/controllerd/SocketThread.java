package com.tao.controllerd;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;


public class SocketThread implements Runnable
{
	private Socket socket = null;
	private BufferedReader bufferedReader = null;
	public SocketThread(Socket socket) throws IOException 
	{
		this.socket = socket;
		bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
	}
	public void run() 
	{
		try 
		{
			PrintStream printStream = new PrintStream(socket.getOutputStream());
			for (String content = null; (content = bufferedReader.readLine()) != null; ) 
			{
				SQLiteDatabase db = ServerThread.dbHelper.getWritableDatabase();
				Map<String, String> args = splitQueryString(content);
				String cmdType = args.get("cmd_type");
				if (cmdType.equals("update_device")) 
				{
					String response = "cmd_type=update_device&data=";
					printStream.println(response+getDevices(db));
				} 
				else if (cmdType.equals("change_name")) 
				{
					String id = args.get("id");
					String name = args.get("name");
					ContentValues values = new ContentValues();
					values.put("name", name);
					db.update("devices", values, "id=?", new String[]{id});
					
					String response = "cmd_type=update_device&data=";
					response += getDevices(db);
					ServerThread.sendResponse(response);
					
				} 
				else if (cmdType.equals("change_lightness")) 
				{
					String path = args.get("device_path");
					String lightness = args.get("lightness");
					ContentValues values = new ContentValues();
					values.put("lightness", lightness);
					int dotIndex = path.indexOf(",");
					int endIndex = dotIndex == -1 ? path.length() : dotIndex;
					db.update("devices", values, "id=?", new String[]{path.substring(0, endIndex)});
					
					ServerThread.serialPort.sendCmd(SerialPort.CmdType.OPERATE, path, Integer.parseInt(lightness));
					Cursor cursor = db.query("devices",new String[]{"lightness"},
							null,null,null,null,null);
					String response = "cmd_type=update_status&data=";
					while (cursor.moveToNext()) 
					{
						response += cursor.getString(cursor.getColumnIndex("lightness")) + "|";
					}
					
					ServerThread.sendResponse(response);
					
				} 
				else if (cmdType.equals("update_status")) 
				{
					Cursor cursor = db.query("devices",new String[]{"lightness"},
							null,null,null,null,null);
					String response = "cmd_type=update_status&data=";
					while (cursor.moveToNext()) 
					{
						response += cursor.getString(cursor.getColumnIndex("lightness")) + "|";
					}
					printStream.println(response);
				} 
				else if (cmdType.equals("routing_device")) 
				{
					ServerThread.serialPort.close();
					ServerThread.serialPort = new SerialPort(new File("/dev/ttyS2"), 9600);
					int maxId = Integer.parseInt(args.get("device_number"));
					db.execSQL("delete from devices");
					HashSet<Integer> nodes = new HashSet<Integer>();
					ArrayList<ArrayList<Node>> readyNodes = new ArrayList<ArrayList<Node>>();
					for (int i = 1; i <= maxId; i ++) nodes.add(i);

					readyNodes.add(new ArrayList<Node>());
					readyNodes.get(0).add(null);
					int count = 0;
					for (int i = 1; i < 15 && nodes.isEmpty() == false; i ++) 
					{
						readyNodes.add(new ArrayList<Node>());
						HashSet<Integer> foundNodes = new HashSet<Integer>();
						for (int id : nodes) 
						{
							for (Node node : readyNodes.get(i-1)) 
							{
								String path = String.valueOf(id);
								for (Node tempNode = node; tempNode != null; tempNode = tempNode.parent) 
								{
									path += ",";
									path += tempNode.id;
								}
								//visit node twice
								if (ServerThread.serialPort.sendCmd(SerialPort.CmdType.VISIT, path, 0x80) != -1) 
								{
									readyNodes.get(i).add(new Node(id, node));
									foundNodes.add(id);
									noticeAndStore((++count)*100/maxId, path, db);
									break;
								}
								if (ServerThread.serialPort.sendCmd(SerialPort.CmdType.VISIT, path, 0x80) != -1) 
								{
									readyNodes.get(i).add(new Node(id, node));
									foundNodes.add(id);
									noticeAndStore((++count)*100/maxId, path, db);
									break;
								}
							}
						}
						nodes.removeAll(foundNodes);
					}
					if (!nodes.isEmpty())
					{
						String failedIds = "";
						for (int id : nodes) failedIds += "," + id;
						PrintStream ps = new PrintStream(socket.getOutputStream());
						ps.println("cmd_type=debug_info&data=" + failedIds);
					}
					String response = "cmd_type=update_device&data=";
					ServerThread.sendResponse(response + getDevices(db));
				} 
				else if (cmdType.equals("add_task")) 
				{
					String ids = args.get("ids");
					String time = args.get("time");
					String dow = args.get("dow");
					String lightness = args.get("lightness");
					ContentValues cvs = new ContentValues();
					cvs.put("ids", ids);
					cvs.put("time", time);
					cvs.put("dow", dow);
					cvs.put("lightness", Integer.valueOf(lightness));
					cvs.put("ispaused", 0);
					db.insert("tasks", null, cvs);
					
					String result = getTasks(db);
					int rowid = getLastRowid(result);
					ServerThread.alarmSettingHelper.registAlarm(rowid, time, dow, ids, Integer.parseInt(lightness));
					
					String response = "cmd_type=update_task&data=" + result;
					ServerThread.sendResponse(response);
				} 
				else if (cmdType.equals("delete_task")) 
				{
					String rowid = args.get("rowid");
					db.delete("tasks", "rowid=?", new String[]{rowid});
					ServerThread.alarmSettingHelper.unregistAlarm(Integer.parseInt(rowid));
					String response = "cmd_type=update_task&data=";
					response += getTasks(db);
					ServerThread.sendResponse(response);
				} 
				else if (cmdType.equals("update_task")) 
				{
					String response = "cmd_type=update_task&data=";
					printStream.println(response + getTasks(db));
				} 
				else if (cmdType.equals("modify_task")) 
				{
					String rowid = args.get("rowid");
					String ids = args.get("ids");
					String time = args.get("time");
					String dow = args.get("dow");
					String lightness = args.get("lightness");
					ContentValues cvs = new ContentValues();
					cvs.put("ids", ids);
					cvs.put("time", time);
					cvs.put("dow", dow);
					cvs.put("lightness", Integer.valueOf(lightness));
					cvs.put("ispaused", 0);
					db.update("tasks", cvs, "rowid=?", new String[]{rowid});
					ServerThread.alarmSettingHelper.unregistAlarm(Integer.parseInt(rowid));
					ServerThread.alarmSettingHelper.registAlarm(Integer.valueOf(rowid), time, dow, ids, Integer.parseInt(lightness));
					
					String result = getTasks(db);
					String response = "cmd_type=update_task&data=" + result;
					ServerThread.sendResponse(response);
				} 
				else if (cmdType.equals("commandAll")) 
				{
					int lightness = Integer.parseInt(args.get("lightness"));
					Cursor cursor = db.query("devices",new String[]{"path"},
							null,null,null,null,null);
					
					Set<String> pathSet = new HashSet<>();
					while (cursor.moveToNext()) 
					{
						String path = cursor.getString(0);
						int i = path.indexOf(",");
						if (i != -1) pathSet.add("65535," + path.substring(i + 1, path.length() - 1));
					}
					pathSet.add("65535");// 0xffff
					
					for (int i = 0; i < 2; i ++) 
					{
						for (String p : pathSet)
						{
							ServerThread.serialPort.sendCmd(SerialPort.CmdType.OPERATE, p, lightness);
							Thread.sleep(800);
						}
					}
					
					ContentValues cvs = new ContentValues();
					cvs.put("lightness", lightness);
					db.update("devices", cvs, null, null);
					String response = "cmd_type=update_device&data=";
					response += getDevices(db);
					ServerThread.sendResponse(response);
				} 
				else if (cmdType.equals("toggle_task")) 
				{
					String rowid = args.get("rowid");
					int ispaused = Integer.parseInt(args.get("ispaused"));
					ContentValues cvs = new ContentValues();
					cvs.put("ispaused", ispaused);
					db.update("tasks", cvs, "rowid=?", new String[]{rowid});
					if (ispaused == 1)
						ServerThread.alarmSettingHelper.unregistAlarm(Integer.parseInt(rowid));
					else 
					{
						String time = args.get("time");
						String dow = args.get("dow");
						String ids = args.get("ids");
						String lightness = args.get("lightness");
						ServerThread.alarmSettingHelper.registAlarm(Integer.valueOf(rowid), time, dow, ids, Integer.parseInt(lightness));
					}
					
					String result = getTasks(db);
					String response = "cmd_type=update_task&data=" + result;
					ServerThread.sendResponse(response);
				} 
				else if (cmdType.equals("find_server")) 
				{
					String response = "cmd_type=echo_server";
					printStream.println(response);
				}
			}
			socket.close();
			ServerThread.connectedClients.remove(socket);
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}
	}
	
	private void noticeAndStore(int completedRate, String path, SQLiteDatabase db) throws IOException
	{
		String response = "cmd_type=routing_device&data=";
		for (Socket sock : ServerThread.connectedClients) 
		{
			PrintStream printStream = new PrintStream(sock.getOutputStream());
			printStream.println(response + completedRate);
		}
		//insert device info into database
		ContentValues cvs = new ContentValues();
		String[] ids = path.split(",");
		cvs.put("id", Integer.parseInt(ids[0]));
		cvs.put("name", "device"+ids[0]);
		cvs.put("path", "["+path+"]");
		cvs.put("lightness", 0);
		db.insert("devices", null, cvs);
	}
	private int getLastRowid(String result) 
	{
		String[] tasks = result.split("\\(");
		String task = tasks[tasks.length - 1];
		return Integer.parseInt(task.substring(0, task.indexOf("|")));
	}
	public static String getTasks(SQLiteDatabase db) 
	{
		StringBuilder content = new StringBuilder();
		Cursor cursor = db.query("tasks",new String[]{"rowid","ids","time","dow","lightness","ispaused"},
				null,null,null,null,null);
		while (cursor.moveToNext()) 
		{
			content.append(cursor.getInt(cursor.getColumnIndex("rowid"))+"|");
			content.append(cursor.getString(cursor.getColumnIndex("ids"))+"|");
			content.append(cursor.getString(cursor.getColumnIndex("time"))+"|");
			content.append(cursor.getString(cursor.getColumnIndex("dow"))+"|");
			content.append(cursor.getInt(cursor.getColumnIndex("lightness"))+"|");
			content.append(cursor.getInt(cursor.getColumnIndex("ispaused"))+"(");
		}
		return content.toString();
	}
	public static String getDevices(SQLiteDatabase db) 
	{
		StringBuilder content = new StringBuilder();
		Cursor cursor = db.query("devices",new String[]{"name","path","lightness"},
				null,null,null,null,null);
		while (cursor.moveToNext()) 
		{
			content.append(cursor.getString(cursor.getColumnIndex("name")));
			content.append(cursor.getString(cursor.getColumnIndex("path")));
			content.append("(" + cursor.getInt(cursor.getColumnIndex("lightness")) + ")");
		}
		return content.toString();
	}
	public static Map<String, String> splitQueryString(String param) 
	{
		Map<String, String> queryPairs = new LinkedHashMap<String, String>();
		String[] pairs = param.split("&");
		for (String pair : pairs) 
		{
			int idx = pair.indexOf("=");
			queryPairs.put(pair.substring(0, idx), pair.substring(idx + 1));
		}
		return queryPairs;
	}
	private class Node 
	{
		public int id;
		public Node parent;
		public Node(int id, Node parent) 
		{
			this.id = id;
			this.parent = parent;
		}
	}
}
