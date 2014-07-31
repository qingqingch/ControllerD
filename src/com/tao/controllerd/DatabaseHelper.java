package com.tao.controllerd;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper 
{

	public DatabaseHelper(Context context, String name, CursorFactory factory, int version) 
	{
		super(context, name, factory, version);
	}

	@Override
	public void onCreate(SQLiteDatabase db) 
	{
		String sql = "create table devices(id int, name varchar(100), path varchar(100), lightness int)";
		db.execSQL(sql);
		sql = "create table tasks(ids varchar(200), time varchar(20), dow varchar(30), lightness int, ispaused int)";
		db.execSQL(sql);
	}
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) 
	{
		
	}
}
