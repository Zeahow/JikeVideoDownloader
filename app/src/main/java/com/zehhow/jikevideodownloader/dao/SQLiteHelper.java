package com.zehhow.jikevideodownloader.dao;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.Vector;

public class SQLiteHelper extends SQLiteOpenHelper {
    private static SQLiteHelper instance;
    private static SQLiteDatabase db;

    private static final String CREATE_TASK =
            "CREATE TABLE Task ("
            + "urlHashCode INTEGER PRIMARY KEY, "
            + "url TEXT, "
            + "name TEXT, "
            + "path TEXT, "
            + "totalLength INTEGER, "
            + "downloadedLength INTEGER);";

    /**
     * 获取实例
     * @return 数据库实例对象
     */
    public static SQLiteHelper getInstance() {
        return instance;
    }

    /**
     * 初始化数据库
     */
    public static void init(Context context) {
        if(instance != null) return;
        instance = new SQLiteHelper(context, "task", null, 1);
        db = instance.getReadableDatabase();
    }

    private SQLiteHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    /**
     * 创建数据库
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TASK);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    /**
     * 返回任务已下载的文件长度
     * @param urlHashCode 任务的下载地址的hashCode
     * @return 任务已下载的文件长度。-1表示任务不存在
     */
    public long getDownloadedLength(int urlHashCode) {
        if(urlHashCode == 0) return -1;
        long downloadedLength = -1;
        Cursor cursor = db.rawQuery("SELECT downloadedLength FROM Task WHERE urlHashCode = ?;", new String[]{urlHashCode + ""});
        if(cursor.moveToFirst())
            downloadedLength = cursor.getLong(cursor.getColumnIndex("downloadedLength"));

        cursor.close();
        return downloadedLength;
    }

    /**
     * 返回任务文件的总长度
     * @param urlHashCode 任务的下载地址的hashCode
     * @return 任务文件的总长度。-1表示任务不存在
     */
    public long getTotalLength(int urlHashCode) {
        if(urlHashCode == 0) return -1;
        long totalLength = -1;
        Cursor cursor = db.rawQuery("SELECT totalLength FROM Task WHERE urlHashCode = ?;", new String[]{urlHashCode + ""});
        if(cursor.moveToFirst())
            totalLength = cursor.getLong(cursor.getColumnIndex("totalLength"));

        cursor.close();
        return totalLength;
    }

    /**
     * 添加一条新的任务数据至数据库
     * @param task 任务类，包含任务地址等待
     */
    public void addTask(TaskBean task) {
        if(task == null) return;

        db.execSQL("INSERT INTO Task (urlHashCode, url, name, path, totalLength, downloadedLength) VALUES(?,?,?,?,?,?);",
                task.toStringArray());
    }

    /**
     * 从数据库删除一条任务记录
     * @param urlHashCode 任务的下载地址的hashCode
     */
    public void deleteTask(int urlHashCode) {
        db.execSQL("DELETE FROM Task WHERE urlHashCode = ?;", new String[]{urlHashCode + ""});
    }

    /**
     * 获取所有任务记录
     * @return 所有任务记录
     */
    public Vector<TaskBean> queryAllTasks() {
        Cursor cursor = db.rawQuery("SELECT * FROM Task;", null);
        Vector<TaskBean> tasks = new Vector<>();

        if(cursor.moveToFirst()) {
            do {
                TaskBean task = new TaskBean();
                task.urlHashCode = cursor.getInt(cursor.getColumnIndex("urlHashCode"));
                task.url = cursor.getString(cursor.getColumnIndex("url"));
                task.name = cursor.getString(cursor.getColumnIndex("name"));
                task.path = cursor.getString(cursor.getColumnIndex("path"));
                task.totalLength = cursor.getLong(cursor.getColumnIndex("totalLength"));
                task.downloadedLength = cursor.getLong(cursor.getColumnIndex("downloadedLength"));
                tasks.add(task);
            } while(cursor.moveToNext());
        }

        cursor.close();
        return tasks;
    }

    /**
     * 更新下载进度
     * @param urlHashCode 任务的下载地址的hashCode
     * @param downloadedLength 最新的文件已下载长度
     */
    public void updateProgress(int urlHashCode, long downloadedLength) {
        db.execSQL("UPDATE Task SET downloadedLength = ? WHERE urlHashCode = ?;",
                new String[]{downloadedLength + "", urlHashCode + ""});
    }
}
