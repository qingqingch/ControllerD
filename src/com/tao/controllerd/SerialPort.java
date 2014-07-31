package com.tao.controllerd;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.http.util.ByteArrayBuffer;

public class SerialPort 
{
	private FileDescriptor mFd;
	private FileInputStream mFileInputStream;
	private FileOutputStream mFileOutputStream;
	private int packId = 0;
	public enum CmdType{VISIT, OPERATE};
	
	public SerialPort(File device, int baudrate) throws IOException 
	{
		mFd = open(device.getAbsolutePath(), baudrate);
		if (mFd == null) throw new IOException();
		mFileInputStream = new FileInputStream(mFd);
		mFileOutputStream = new FileOutputStream(mFd);
	}
	
	public int sendCmd(CmdType cmdType, String path, int lightness)
	{
		String[] nodes = path.split(",");
		int nodeCount =	nodes.length;
		ByteArrayBuffer buf = new ByteArrayBuffer(20);
		buf.append(0xc0);
		buf.append(2);
		buf.append(5 + 2 * nodeCount);
		buf.append(nodeCount << 4);
		if (cmdType == CmdType.VISIT)
		{
			packId += 1;
			packId = packId == 256 ? 1 : packId;
			buf.append(packId);
		}
		else
		{
			buf.append(1);
		}
		buf.append(lightness); //0x80 for visit node
		
		for (String n : nodes) 
		{
			int data = (Integer.parseInt(n) >> 8);
			switch (data)
			{
			case 0xc0:
				buf.append(0xdb);
				buf.append(0xdc);
				break;
			case 0xc2:
				buf.append(0xdb);
				buf.append(0xde);
				break;
			case 0xdb:
				buf.append(0xdb);
				buf.append(0xdd);
				break;
			default:
				buf.append(data);
			}
	
			data = Integer.parseInt(n) & 0x00ff;
			switch (data)
			{
			case 0xc0:
				buf.append(0xdb);
				buf.append(0xdc);
				break;
			case 0xc2:
				buf.append(0xdb);
				buf.append(0xde);
				break;
			case 0xdb:
				buf.append(0xdb);
				buf.append(0xdd);
				break;
			default:
				buf.append(data);
			}
		}
		buf.append(0xc2);
		try 
		{
			mFileOutputStream.write(buf.toByteArray());
			if (cmdType != CmdType.VISIT) return 0;
			if (waitForRead(2) == 0) return -1;
			int item = mFileInputStream.read();			
			if (item != 0xc0) return -1;
			while (mFileInputStream.read() != 0xc2);
			return 0;
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
			return -1;
		}
	}
	private native static FileDescriptor open(String path, int baudrate);
	public native void close();
	public native int waitForRead(int second);
	static 
	{
		System.loadLibrary("serial_port");
	}
}
