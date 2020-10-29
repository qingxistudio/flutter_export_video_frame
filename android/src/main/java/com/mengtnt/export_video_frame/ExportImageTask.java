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
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.MediaMetadataRetriever;
import android.os.AsyncTask;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;

interface Callback {
    void exportPath(ArrayList<String> list);
}

final class ExportImageTask extends AsyncTask<Object,Void,ArrayList<String>> {

    private Callback callBack;

    void setCallBack(Callback callBack) {
        this.callBack = callBack;
    }

    @Override
    protected ArrayList<String> doInBackground(Object... objects) {
        String filePath = (String) objects[0];
        Object param = objects[1];
        if (param instanceof Integer) {
            int number = (int)param;
            if (number > 0) {
                Number third = (Number) objects[2];
                float quality = third.floatValue();
                String exportDir = (String) objects[3];
                String exportPrefix = (String)objects[4];
                return  exportImageList(filePath,number,quality,exportDir,exportPrefix);
            }
        } else if (param instanceof Long) {
            Long duration = (Long)param;
            Number third = (Number)objects[2];
            float radian = third.floatValue();
            ArrayList result = new ArrayList(1);
            result.add(exportImageByDuration(filePath,duration,radian));
            return result;
        } else if (param instanceof Number) {
            Number second = (Number)param;
            return exportGifImageList(filePath,second.floatValue());
        }

        return null;
    }

    protected ArrayList<String> exportGifImageList(String filePath,float quality) {
        ArrayList result = new ArrayList();
        GifDecoder decoder = new GifDecoder();
        try {
            InputStream input = new FileInputStream(filePath);
            decoder.read(input);
            decoder.decodeImageData();
            GifDecoder.GifFrame[] frames = decoder.getFrames();
            int index = 0;
            for (GifDecoder.GifFrame frame:frames) {
                String key = String.format("%s%d%.1f", filePath, index,quality);
                FileStorage.share().createFileByKey(key,frame.image, Bitmap.CompressFormat.PNG);
                result.add(FileStorage.share().filePathByKey(key));
                index ++;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return result;
    }

    protected  String exportImageByDuration(String filePath,Long duration,float radian) {
        String result = new String();
        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
        try {
            mediaMetadataRetriever.setDataSource(filePath);
            Bitmap bmpOriginal = mediaMetadataRetriever.getFrameAtTime(duration * 1000, MediaMetadataRetriever.OPTION_CLOSEST);
            if (bmpOriginal == null) {
                throw new Exception("bmpOriginal is null");
            }
            int bmpVideoHeight = bmpOriginal.getHeight();
            int bmpVideoWidth = bmpOriginal.getWidth();

            Bitmap bitmap = bmpOriginal;

            if (radian != 0) {
                Matrix m = new Matrix();
                float degrees = (float) (radian * 180 / Math.PI);
                m.postRotate(degrees);
                bitmap = Bitmap.createBitmap(bmpOriginal, 0,0,bmpVideoWidth, bmpVideoHeight, m,false);
            }
            String key = String.format("%s%d%.4f", filePath, duration,radian);
            FileStorage.share().createFileByKey(key,bitmap,Bitmap.CompressFormat.JPEG);
            result = FileStorage.share().filePathByKey(key);
        } catch (Exception e) {
            Log.e("Media read error",e.toString());
        }
        mediaMetadataRetriever.release();
        return result;
    }

    protected ArrayList<String> exportImageList(String filePath,int number,float quality, String exportDir, String exportPrefix) {
        ArrayList result = new ArrayList(number);
        float scale = (float)0.1;
        if (quality > 0.1) {
            scale = quality;
        }

        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
        try {
            mediaMetadataRetriever.setDataSource(filePath);
            String METADATA_KEY_DURATION = mediaMetadataRetriever
                    .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            String metaRotation = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
            int rotation = metaRotation == null ? 0 : Integer.parseInt(metaRotation);
            int rawVideoWidth = Integer.valueOf(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
            int rawVideoHeight = Integer.valueOf(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));

            int rotatedVideoWidth;
            int rotatedVideoHeight;
            if (rotation == 90 || rotation == 270) {
                rotatedVideoWidth = rawVideoHeight;
                rotatedVideoHeight = rawVideoWidth;
            } else {
                rotatedVideoWidth = rawVideoWidth;
                rotatedVideoHeight = rawVideoHeight;
            }
//            int videoHeight = ;
            int max = (int) Long.parseLong(METADATA_KEY_DURATION);
            int step = max / number;

            for ( int index = 0 ; index < max ; index = index+step ) {
                Bitmap bitmap;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1 && rotatedVideoWidth > 0) {
                    int dstWidth = (int) (rotatedVideoWidth * scale);
                    int dstHeight = (int) (rotatedVideoHeight * scale);
                    bitmap = mediaMetadataRetriever.getScaledFrameAtTime(index * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC,dstWidth,dstHeight);
                } else {
                    Bitmap bmpOriginal = mediaMetadataRetriever.getFrameAtTime(index * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
                    if (bmpOriginal == null) {
                        continue;
                    }
                    int bmpVideoHeight = bmpOriginal.getHeight() ;
                    int bmpVideoWidth = bmpOriginal.getWidth();
                    Matrix m = new Matrix();
                    bitmap = bmpOriginal;
                    if (scale < 1.0) {
                        m.setScale(scale, scale);
                        bitmap = Bitmap.createBitmap(bmpOriginal, 0, 0, bmpVideoWidth, bmpVideoHeight, m, false);
                    }
                }

                FileStorage storage;
                if (exportDir == null) {
                    storage = FileStorage.share();
                } else {
                    storage = new FileStorage(exportDir);
                }

                String imageExportPath;
                if (exportPrefix == null || exportPrefix.length() == 0) {
                    String key = String.format("%s%d", filePath, index);
                    imageExportPath = storage.filePathByKey(key);
                } else {
                    String fileName = String.format("%s%d%s", exportPrefix, index+1, ".jpg");
                    imageExportPath = storage.filePathByName(fileName);
                }
                storage.createFileByPath(imageExportPath,bitmap, Bitmap.CompressFormat.JPEG);
                result.add(imageExportPath);
            }
        } catch (Exception e) {
            Log.e("Media read error",e.toString());
        }
        mediaMetadataRetriever.release();
        return result;
    }

    @Override
    protected void onPostExecute(ArrayList<String> strings) {
        super.onPostExecute(strings);
        this.callBack.exportPath(strings);
    }

}
