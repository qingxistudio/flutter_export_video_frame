/** 
MIT License

Copyright (c) 2019 mengtnt

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/
package com.mengtnt.export_video_frame;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;

class FileStorage {

    private String cacheDirectory;
    private Context context;
    private boolean external = false;

    private static FileStorage instance = new FileStorage();

    static FileStorage share() {
        return instance;
    }

    static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    static boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
    }

    private FileStorage() {
    }

    public FileStorage(String directory) {
        cacheDirectory = directory;
    }

    void setContext(Context context) {
        this.context = context;
        cacheDirectory = context.getDir("ExportImage", Context.MODE_PRIVATE).getAbsolutePath();
    }

    void createFileByKey(String key, Bitmap bitmapImage) {
        createFileByPath(getFileByName(fileName(key)).getAbsolutePath(), bitmapImage);
    }

    void createFileByPath(String path, Bitmap bitmapImage) {
        FileOutputStream fileOutputStream = null;
        try {
            File file = new File(path);
            if (file.exists()) {
                return;
            }
            fileOutputStream = new FileOutputStream(file);
            bitmapImage.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (fileOutputStream != null) {
                    fileOutputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    String filePathByKey(String key) {
        File file = getFileByName(fileName(key));
        return file.getAbsolutePath();
    }

    String filePathByName(String fileName) {
        File file = getFileByName(fileName);
        return file.getAbsolutePath();
    }

    Boolean cleanCache() {
        File directory = new File(cacheDirectory);
        File[] files = directory.listFiles();
        if (files == null) {
            return true;
        }
        Boolean success = true;
        for (File file : files){
            Boolean result = file.delete();
            if (!result) {
                success = false;
            }
        }
        return success;
    }

    private String fileName(String key) {
        return  MD5.getStr(key);
    }

    private File getFileByName(String fileName) {
        File directory = new File(cacheDirectory);
        if(!directory.exists() && !directory.mkdirs()){
            Log.e("FileStorage","Error creating directory " + directory);
        }
        return new File(directory, fileName);
    }

    private File getAlbumStorageDir(String albumName) {
        return new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), albumName);
    }

}
