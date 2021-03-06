package com.nathan96169.toolshelper.fileoperate;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;

import androidx.core.content.FileProvider;

import com.nathan96169.toolshelper.GlobalInitBase;

import java.io.File;

public class FilePickerSys {
    public static final int FILE_SELECT_REQUESTCODE = 8806;
    /***
     * 1.执行方法
     FilePickerSys.selectFile(resultActivity.getApplicationContext().getPackageName(),DBBackupRestore.BACKUP_DIC, resultActivity);
     ODispatcher.addEventListener(OEventName.FILE_PICK_RESULT_BACK,this);
     2.Activity

     @Override
     protected void onActivityResult(int requestCode, int resultCode, Intent data) {
     super.onActivityResult(requestCode, resultCode, data);
     if (resultCode == Activity.RESULT_OK && requestCode == FilePickerSys.FILE_SELECT_REQUESTCODE) {
     Uri uri = data.getData();
     ODispatcher.dispatchEvent(OEventName.FILE_PICK_RESULT_BACK,uri.getPath());
     LogMe.showInDebug("文件路径："+uri.getPath());
     }
     }
     3.处理事件

     if (OEventName.FILE_PICK_RESULT_BACK.equals(eventName)) {
     String filePath = (String) paramObj;
     if(filePath.contains("/root"))filePath = filePath.replace("/root","");
     try {
     DBBackupRestore.doRestore(ODBHelper.getInstance().getDBName()+".db",filePath);
     ToastTextShow.show("数据恢复成功!", GlobalInitBase.getCurrentActivity());
     if(onRestoreFinListener!=null)onRestoreFinListener.onRestoreFin();
     } catch (Exception e) {
     LogMe.showInDebug(e.toString());
     }
     ODispatcher.removeEventListener(OEventName.FILE_PICK_RESULT_BACK,this);
     }
     */
    public static void selectFile(String providerName,String dicPath,Activity resultActivity){
        //getUrl()获取文件目录，例如返回值为/storage/sdcard1/MIUI/music/mp3_hd/单色冰淇凌_单色凌.mp3
        File dicFile = new File(dicPath);
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        //intent.setType("*/*");//无类型限制 image/* audio/* video/*

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            //记得修改com.xxx.fileprovider与androidmanifest相同
            Uri uri = FileProvider.getUriForFile(resultActivity,providerName,dicFile);
//            intent.setDataAndType(uri,"*/*");
            intent.setDataAndType(uri,"application/vnd.android.package-archive");
//            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI,uri);
        }else{
            intent.setDataAndType(Uri.fromFile(dicFile), "*/*");
        }
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        resultActivity.startActivityForResult(intent, FILE_SELECT_REQUESTCODE);
    }
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        if (resultCode == Activity.RESULT_OK && requestCode == 1) {
//            Uri uri = data.getData();
//            LogMe.showInDebug("文件路径："+uri.getPath());
//        }
//    }

    public String getRealPathFromURI(Uri contentUri) {
        String res = null;
        String[] proj = { MediaStore.Images.Media.DATA };
        Cursor cursor = GlobalInitBase.getCurrentActivity().getContentResolver().query(contentUri, proj, null, null, null);
        if(null!=cursor&&cursor.moveToFirst()){;
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            res = cursor.getString(column_index);
            cursor.close();
        }
        return res;
    }

    /**
     * 专为Android4.4设计的从Uri获取文件绝对路径，以前的方法已不好使
     */
    @SuppressLint("NewApi")
    public String getPath(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{split[1]};

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context       The context.
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public String getDataColumn(Context context, Uri uri, String selection,
                                String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {column};

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

}
