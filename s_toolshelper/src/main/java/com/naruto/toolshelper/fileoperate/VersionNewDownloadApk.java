package com.naruto.toolshelper.fileoperate;

import android.app.DownloadManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.content.FileProvider;

import com.naruto.toolshelper.GlobalInitBase;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * new VersionNewDownloadApk(context, appUrl, "***.apk");
 * <!--网络通信权限-->
 * <uses-permission android:name="android.permission.INTERNET"/>
 * <!--SD卡写入数据权限-->
 * <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
 * <!--SD卡创建与删除权限-->
 * <uses-permission android:name="android.permission.MOUNT_UNMOUNT_FILESYSTEMS"/>
 * <!--VISIBILITY_HIDDEN表示不显示任何通知栏提示的权限-->
 * <uses-permission android:name="android.permission.DOWNLOAD_WITHOUT_NOTIFICATION"/>
 * <!--DownloadManager-->
 * <uses-permission android:name="android.permission.ACCESS_DOWNLOAD_MANAGER"/>
 *
 * 在清单文件中注册Service
 *
 * <!--版本更新服务-->
 *
 * <service
 * android:name="com.github.phoenix.service.DownloadService"></service>
 *
 *
 *<!--使用方式-->
 txt_update_change.setText("正在下载...");
 if(VersionNewDownloadApk.checkInstallPermission(getContext())) {
 new VersionNewDownloadApk(getContext(), downLoadUrl, UrlHelper.getFileName(downLoadUrl) + ".apk");
 }else{
 txt_update_change.setText("请重新打开App更新!");
 }
 */
public class VersionNewDownloadApk {
    public static boolean isDownloading = false;
    //下载器
    private DownloadManager downloadManager;
    private Context mContext;
    //下载的ID
    private long downloadId;
    private String name;
    private String pathstr;

    public VersionNewDownloadApk(Context context, String url, String name) {
        this.mContext = context;
        this.name = name;
        //先删除
        File file = new File(mContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), name);
        UtilFileSave.RecursionDeleteFile(file);
        LogMe.showInDebug("升级软件：先清除");
        if(VersionNewDownloadApk.checkInstallPermission(context))
        downloadAPK(url, name);
    }
    /**1.先查有没权限*/
    public static boolean checkInstallPermission(Context context){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){//8.0以上安装要权限
//    <uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES"/>
            if(!GlobalInitBase.getCurrentActivity().getPackageManager().canRequestPackageInstalls()) {
                Uri packageUri = Uri.parse("package:" + context.getPackageName());
                Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, packageUri);
                GlobalInitBase.getCurrentActivity().startActivityForResult(intent, 998);
                return false;
            }else return true;
        }else return true;
    }

    //下载apk
    private void downloadAPK(String url, String name) {
        //创建下载任务
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        //移动网络情况下是否允许漫游
        request.setAllowedOverRoaming(true);
        //在通知栏中显示，默认就是显示的
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
        request.setTitle("更新软件");
        request.setDescription("下载中...");
        request.setVisibleInDownloadsUi(true);
        LogMe.showInDebug("升级软件：下载中...");

        //设置下载的路径
        File file = new File(mContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), name);
        request.setDestinationUri(Uri.fromFile(file));
        pathstr = file.getAbsolutePath();
        if(file.exists()){
            LogMe.showInDebug("升级软件：直接安装...");
            installAPK();
            return;
        }
        //获取DownloadManager
        if (downloadManager == null)
            downloadManager = (DownloadManager) mContext.getSystemService(Context.DOWNLOAD_SERVICE);
        //将下载请求加入下载队列，加入下载队列后会给该任务返回一个long型的id，通过该id可以取消任务，重启任务、获取下载的文件等等
        if (downloadManager != null) {
            downloadId = downloadManager.enqueue(request);
        }

        //注册广播接收者，监听下载状态
        mContext.registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        isDownloading = true;
    }

    //广播监听下载的各个状态
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            checkStatus();/**此处可以定时check*/
        }
    };

    //检查下载状态
    private void checkStatus() {
        DownloadManager.Query query = new DownloadManager.Query();
        //通过下载的id查找
        query.setFilterById(downloadId);
        Cursor cursor = downloadManager.query(query);
        if (cursor.moveToFirst()) {
            //下载状态
            int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
            //已经下载文件大小
            int byteDownd = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
            //下载文件的总大小
            int byteTotal = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
            switch (status) {
                //下载暂停
                case DownloadManager.STATUS_PAUSED:
                    Log.e("更新apk","STATUS_PAUSED");
                    break;
                //下载延迟
                case DownloadManager.STATUS_PENDING:
                    Log.e("更新apk","STATUS_PENDING");
                    break;
                //正在下载
                case DownloadManager.STATUS_RUNNING:
                    Log.e("更新apk","STATUS_RUNNING");
                    break;
                //下载完成
                case DownloadManager.STATUS_SUCCESSFUL:
                    Log.e("更新apk","STATUS_SUCCESSFUL");
                    //下载完成安装APK
                    installAPK();
                    cursor.close();
                    mContext.unregisterReceiver(receiver);
                    isDownloading = false;
                    break;
                //下载失败
                case DownloadManager.STATUS_FAILED:
                    Log.e("更新apk","STATUS_FAILED");
                    Toast.makeText(mContext, "下载失败", Toast.LENGTH_SHORT).show();
                    cursor.close();
                    mContext.unregisterReceiver(receiver);
                    isDownloading = false;
                    break;
            }
        }
    }

    private void installAPK() {
        LogMe.showInDebug("升级软件：开始安装");
        setPermission(pathstr);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        //Android 7.0以上要使用FileProvider
        if (Build.VERSION.SDK_INT >= 24) {
            File file = (new File(pathstr));
            Uri apkUri = FileProvider.getUriForFile(mContext, GlobalInitBase.getContext().getPackageName(), file);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);//临时授权
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            Log.e("更新apk","安装>=24:"+pathstr);
        } else {
//            intent.setDataAndType(Uri.fromFile(new File(Environment.DIRECTORY_DOWNLOADS, name)), "application/vnd.android.package-archive");
            intent.setDataAndType(Uri.fromFile(new File(pathstr)), "application/vnd.android.package-archive");
            Log.e("更新apk","安装<24:"+pathstr);
        }
        GlobalInitBase.getCurrentActivity().startActivity(intent);
    }

    //修改文件权限
    private void setPermission(String absolutePath) {
        String command = "chmod " + "777" + " " + absolutePath;
        Runtime runtime = Runtime.getRuntime();
        try {
            runtime.exec(command);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



}
