package com.example.bottlenex;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "UserDB";
    public static final String TABLE_NAME = "users";
    public static final String COL_1 = "ID";
    public static final String COL_2 = "username";
    public static final String COL_3 = "password";

    // Route History Table
    public static final String ROUTE_HISTORY_TABLE = "route_history";
    public static final String ROUTE_ID = "id";
    public static final String ROUTE_START_LAT = "start_lat";
    public static final String ROUTE_START_LON = "start_lon";
    public static final String ROUTE_END_LAT = "end_lat";
    public static final String ROUTE_END_LON = "end_lon";
    public static final String ROUTE_START_ADDRESS = "start_address";
    public static final String ROUTE_END_ADDRESS = "end_address";
    public static final String ROUTE_DISTANCE = "distance";
    public static final String ROUTE_DURATION = "duration";
    public static final String ROUTE_START_TIME = "start_time";
    public static final String ROUTE_END_TIME = "end_time";
    public static final String ROUTE_DATE = "date";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, 2); // Increment version for new table
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_NAME + " (ID INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT, password TEXT)");

        db.execSQL("CREATE TABLE " + ROUTE_HISTORY_TABLE + " (" +
                ROUTE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                ROUTE_START_LAT + " REAL, " +
                ROUTE_START_LON + " REAL, " +
                ROUTE_END_LAT + " REAL, " +
                ROUTE_END_LON + " REAL, " +
                ROUTE_START_ADDRESS + " TEXT, " +
                ROUTE_END_ADDRESS + " TEXT, " +
                ROUTE_DISTANCE + " REAL, " +
                ROUTE_DURATION + " REAL, " +
                ROUTE_START_TIME + " TEXT, " +
                ROUTE_END_TIME + " TEXT, " +
                ROUTE_DATE + " TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("CREATE TABLE " + ROUTE_HISTORY_TABLE + " (" +
                    ROUTE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    ROUTE_START_LAT + " REAL, " +
                    ROUTE_START_LON + " REAL, " +
                    ROUTE_END_LAT + " REAL, " +
                    ROUTE_END_LON + " REAL, " +
                    ROUTE_START_ADDRESS + " TEXT, " +
                    ROUTE_END_ADDRESS + " TEXT, " +
                    ROUTE_DISTANCE + " REAL, " +
                    ROUTE_DURATION + " REAL, " +
                    ROUTE_START_TIME + " TEXT, " +
                    ROUTE_END_TIME + " TEXT, " +
                    ROUTE_DATE + " TEXT)");
        }
    }

    public Boolean insertData(String username, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_2, username);
        contentValues.put(COL_3, password);
        long result = db.insert(TABLE_NAME, null, contentValues);
        return result != -1;
    }

    public Boolean checkUsernamePassword(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME + " WHERE username = ? AND password = ?", new String[]{username, password});
        return cursor.getCount() > 0;
    }

    public Boolean checkUsername(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME + " WHERE username = ?", new String[]{username});
        return cursor.getCount() > 0;
    }

    // Route History Methods
    public long insertRouteHistory(RouteHistory route) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(ROUTE_START_LAT, route.getStartLat());
        contentValues.put(ROUTE_START_LON, route.getStartLon());
        contentValues.put(ROUTE_END_LAT, route.getEndLat());
        contentValues.put(ROUTE_END_LON, route.getEndLon());
        contentValues.put(ROUTE_START_ADDRESS, route.getStartAddress());
        contentValues.put(ROUTE_END_ADDRESS, route.getEndAddress());
        contentValues.put(ROUTE_DISTANCE, route.getDistance());
        contentValues.put(ROUTE_DURATION, route.getDuration());
        contentValues.put(ROUTE_START_TIME, route.getStartTime());
        contentValues.put(ROUTE_END_TIME, route.getEndTime());
        contentValues.put(ROUTE_DATE, route.getDate());
        
        return db.insert(ROUTE_HISTORY_TABLE, null, contentValues);
    }

    public List<RouteHistory> getAllRouteHistory() {
        List<RouteHistory> routeList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + ROUTE_HISTORY_TABLE + " ORDER BY " + ROUTE_ID + " DESC", null);

        if (cursor.moveToFirst()) {
            do {
                RouteHistory route = new RouteHistory();
                route.setId(cursor.getInt(cursor.getColumnIndexOrThrow(ROUTE_ID)));
                route.setStartLat(cursor.getDouble(cursor.getColumnIndexOrThrow(ROUTE_START_LAT)));
                route.setStartLon(cursor.getDouble(cursor.getColumnIndexOrThrow(ROUTE_START_LON)));
                route.setEndLat(cursor.getDouble(cursor.getColumnIndexOrThrow(ROUTE_END_LAT)));
                route.setEndLon(cursor.getDouble(cursor.getColumnIndexOrThrow(ROUTE_END_LON)));
                route.setStartAddress(cursor.getString(cursor.getColumnIndexOrThrow(ROUTE_START_ADDRESS)));
                route.setEndAddress(cursor.getString(cursor.getColumnIndexOrThrow(ROUTE_END_ADDRESS)));
                route.setDistance(cursor.getDouble(cursor.getColumnIndexOrThrow(ROUTE_DISTANCE)));
                route.setDuration(cursor.getDouble(cursor.getColumnIndexOrThrow(ROUTE_DURATION)));
                route.setStartTime(cursor.getString(cursor.getColumnIndexOrThrow(ROUTE_START_TIME)));
                route.setEndTime(cursor.getString(cursor.getColumnIndexOrThrow(ROUTE_END_TIME)));
                route.setDate(cursor.getString(cursor.getColumnIndexOrThrow(ROUTE_DATE)));
                
                routeList.add(route);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return routeList;
    }

    public void deleteRouteHistory(int routeId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(ROUTE_HISTORY_TABLE, ROUTE_ID + " = ?", new String[]{String.valueOf(routeId)});
    }

    public void clearAllRouteHistory() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(ROUTE_HISTORY_TABLE, null, null);
    }
} 