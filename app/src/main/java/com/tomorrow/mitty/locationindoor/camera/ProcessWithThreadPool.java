package com.tomorrow.mitty.locationindoor.camera;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.hardware.Camera;

import com.tomorrow.mitty.locationindoor.common.FileUtils;
import com.tomorrow.mitty.locationindoor.common.ImageRecognition;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.tomorrow.mitty.locationindoor.common.myUtils.ByteToBitmap;
import static org.opencv.imgproc.Imgproc.cvtColor;

/**
 * Created by zhantong on 16/6/15.
 */
public class ProcessWithThreadPool {
    private static final String TAG = "ThreadPool";
    private static final int KEEP_ALIVE_TIME = 10;
    private static final TimeUnit TIME_UNIT = TimeUnit.SECONDS;
    private BlockingQueue<Runnable> workQueue;
    private ThreadPoolExecutor mThreadPool;

    private CustomImageButton cimbt;//显示缩小的logo

    public ProcessWithThreadPool() {
        int corePoolSize = Runtime.getRuntime().availableProcessors();
        int maximumPoolSize = corePoolSize * 2;
        workQueue = new LinkedBlockingQueue<>();
        mThreadPool = new ThreadPoolExecutor(corePoolSize, maximumPoolSize, KEEP_ALIVE_TIME, TIME_UNIT, workQueue);
    }

    public synchronized void post(final byte[] frameData, final Camera camera) {
        mThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                processFrame(frameData,camera);
            }
        });
    }

    private synchronized void processFrame(byte[] frameData, Camera camera) {
        //synchronized使写写，写读互斥

//        Log.i(TAG, byte.class.getName());
        Camera.Size previewSize = camera.getParameters().getPreviewSize();
//        Log.i("previewSize", previewSize.toString());
        Bitmap bitmap = ByteToBitmap(frameData, previewSize);
//        bitmap=BitmapRgbToBgr(bitmap);
//        Log.i("bitmap", bitmap.toString());
        //Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);//将data byte型数组转换成bitmap文件

        final Matrix matrix = new Matrix();//转换成矩阵旋转90度
//        if (cameraPosition == 1) {//position为1时为后置摄像头
        matrix.setRotate(90);
//        } else {
//            matrix.setRotate(-90);
//        }
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);//旋转图片

//        Mat grayscaleImage = new Mat(previewSize.height, previewSize.width, CvType.CV_8UC4);//changed：CV_8UC4

        if (bitmap != null) {
            Mat inputFrame = new Mat(previewSize.height, previewSize.width, CvType.CV_64F);
            Utils.bitmapToMat(bitmap, inputFrame);
            Mat mBgr= new Mat(previewSize.height, previewSize.width, CvType.CV_64F);
            cvtColor(inputFrame, mBgr, Imgproc.COLOR_RGBA2BGRA);
            Imgcodecs.imwrite(FileUtils.resultpath+"原图.jpg", mBgr);//added
            if (!bitmap.isRecycled()) {
                bitmap.recycle();
            }

            // Create a grayscale image
//            Imgproc.cvtColor(inputFrame, grayscaleImage, Imgproc.COLOR_RGBA2RGB);

            // 检测目标
            new ImageRecognition().getInfoRect(inputFrame);
//            Rect[] object = (Rect[]) new ImageRecognition().getInfoRect(inputFrame).toArray();
//            Log.e(TAG, object. + "Rect[] object.length");

//
//            MatOfRect mRect = new MatOfRect();
//
//            int maxRectArea = 0 * 0;
//            Rect maxRect = null;
//
//            int logonum = 0;
//
//            for (Rect rect : object) {
//                ++logonum;
//                // 找出最大的面积
//                int tmp = rect.width * rect.height;
//                if (tmp >= maxRectArea) {
//                    maxRectArea = tmp;
//                    maxRect = rect;
//                }
//            }


//            Bitmap rectBitmap = null;
//            if (logonum != 0) {
//                // 剪切最大的头像
//                //Log.e("剪切的长宽", String.format("高:%s,宽:%s", maxRect.width, maxRect.height));
//                Rect rect = new Rect(maxRect.x, maxRect.y, maxRect.width, maxRect.height);
//                Mat rectMat = new Mat(inputFrame, rect);  // 从原始图像拿
//                rectBitmap = Bitmap.createBitmap(rectMat.cols(), rectMat.rows(), Bitmap.Config.ARGB_8888);
//                Utils.matToBitmap(rectMat, rectBitmap);
//
//                Bitmap resizeBmp = cimbt.resizeBitmap(rectBitmap, cimbt.getWidth(), cimbt.getHeight());
//                cimbt.setBitmap(resizeBmp);
//            } else {
//                cimbt.clearnImage();
//                cimbt.setText("没有检测到人脸");
//            }
        }
    }
    public Bitmap BitmapRgbToBgr(Bitmap bitmap){
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
        int indx = 0;
        int a = 0, r = 0, g = 0, b = 0;
        for (int row = 0; row < height; row++) {
            indx = row * width;
            for (int col = 0; col < width; col++) {
                int pixel = pixels[indx];
                r = (pixel >> 16) & 0xff;
                g = (pixel >> 8) & 0xff;
                b = pixel & 0xff;
                pixel = ((a & 0xff) << 24) | ((b & 0xff) << 16) | ((g & 0xff) << 8) | (r & 0xff);
                pixels[indx] = pixel;
                indx++;
            }
        }
        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return bitmap;
    }

}
