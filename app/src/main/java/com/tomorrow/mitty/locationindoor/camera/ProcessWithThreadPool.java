package com.tomorrow.mitty.locationindoor.camera;

import android.hardware.Camera;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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

    private void processFrame(byte[] frameData, Camera camera) {

//        Log.i(TAG, byte.class.getName());
//        Camera.Size previewSize = camera.getParameters().getPreviewSize();
////        Log.i("previewSize", previewSize.toString());
//        Bitmap bitmap = ByteToBitmap(frameData, previewSize);
////        Log.i("bitmap", bitmap.toString());
//        //Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);//将data byte型数组转换成bitmap文件
//
//        final Matrix matrix = new Matrix();//转换成矩阵旋转90度
////        if (cameraPosition == 1) {//position为1时为后置摄像头
//            matrix.setRotate(90);
////        } else {
////            matrix.setRotate(-90);
////        }
//        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);//旋转图片
//
//        Mat grayscaleImage = new Mat(previewSize.height, previewSize.width, CvType.CV_64F);//changed：CV_8UC4
//        int absoluteLogoSize = (int) (previewSize.height * 0.2);//预览的小窗口大小
//
//        if (bitmap != null) {
//            Mat inputFrame = new Mat();
//            Utils.bitmapToMat(bitmap, inputFrame);
////            Imgcodecs.imwrite(FileUtils.resultpath+"原图.jpg", inputFrame);//added
//            if (!bitmap.isRecycled()) {
//                bitmap.recycle();
//            }
//
//            // Create a grayscale image
//            Imgproc.cvtColor(inputFrame, grayscaleImage, Imgproc.COLOR_RGBA2RGB);
//
//            MatOfRect mRect = new MatOfRect();
//
//
//            int maxRectArea = 0 * 0;
//            Rect maxRect = null;
//
//            int logonum = 0;
//
//
//            // 检测目标
//            Rect[] object = (Rect[]) new ImageRecognition().getInfoRect(inputFrame).toArray();
//            Log.e(TAG, object.length + "Rect[] object.length");
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
//
////
////            Bitmap rectBitmap = null;
////            if (logonum != 0) {
////                // 剪切最大的头像
////                //Log.e("剪切的长宽", String.format("高:%s,宽:%s", maxRect.width, maxRect.height));
////                Rect rect = new Rect(maxRect.x, maxRect.y, maxRect.width, maxRect.height);
////                Mat rectMat = new Mat(inputFrame, rect);  // 从原始图像拿
////                rectBitmap = Bitmap.createBitmap(rectMat.cols(), rectMat.rows(), Bitmap.Config.ARGB_8888);
////                Utils.matToBitmap(rectMat, rectBitmap);
////
////                Bitmap resizeBmp = cimbt.resizeBitmap(rectBitmap, cimbt.getWidth(), cimbt.getHeight());
////                cimbt.setBitmap(resizeBmp);
////            } else {
////                cimbt.clearnImage();
////                cimbt.setText("没有检测到人脸");
////            }
//        }

    }

}
