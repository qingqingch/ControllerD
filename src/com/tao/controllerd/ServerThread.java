package com.tao.controllerd;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Locale;

import android.content.Context;
import android.content.Intent;

public class ServerThread implements Runnable 
{
	private static final int SOCKET_PORT = 55000;
	public static Boolean continue_running = true;
	private Context context;
	public static LinkedList<Socket> connectedClients = new LinkedList<Socket>();
	public static DatabaseHelper dbHelper;
	public static SerialPort serialPort;
	public static AlarmSettingHelper alarmSettingHelper;
	
	public ServerThread (Context context) 
	{
		this.context = context;
	}
	public void run() 
	{
		dbHelper = new DatabaseHelper(context, "data.db", null, 2);
		alarmSettingHelper = new AlarmSettingHelper(context);
		alarmSettingHelper.setupAlarms();
		try 
		{
			serialPort = new SerialPort(new File("/dev/ttyS2"), 9600);
			ServerSocket serverSocket = new ServerSocket(SOCKET_PORT);
			while (continue_running) 
			{
				Socket socket = serverSocket.accept();
				connectedClients.add(socket);
				sendConnectMsg(socket.getInetAddress().getHostAddress());
				new Thread(new SocketThread(socket)).start();
			}
			serverSocket.close();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}
	private void sendConnectMsg(String addr)
	{
		Intent intent = new Intent("connected");
		SimpleDateFormat dateFormat = new SimpleDateFormat("hh:mm:ss", Locale.CHINA);
		intent.putExtra("connect_info", String.format("From:%s At:%s", addr, dateFormat.format(new Date())));
		context.sendBroadcast(intent);
	}
	public static void sendResponse(String response)
	{
		try
		{
			for (Socket sock : connectedClients) 
			{
				PrintStream printStream = new PrintStream(sock.getOutputStream());
				printStream.println(response);
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
}
