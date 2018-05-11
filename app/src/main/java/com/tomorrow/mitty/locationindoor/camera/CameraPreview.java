package com.tomorrow.mitty.locationindoor.camera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.tomorrow.mitty.locationindoor.common.ImageRecognition;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import static com.tomorrow.mitty.locationindoor.common.myUtils.ByteToBitmap;

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback {
    private static final String TAG = "CameraPreview";
    private SurfaceHolder mHolder;
    private Camera mCamera;

    //process frame type
    private static final int PROCESS_WITH_HANDLER_THREAD = 1;
    private static final int PROCESS_WITH_QUEUE = 2;
    private static final int PROCESS_WITH_ASYNC_TASK = 3;
    private static final int PROCESS_WITH_THREAD_POOL = 4;

    private int processType = PROCESS_WITH_THREAD_POOL;

    //HandlerThread
    private ProcessWithHandlerThread processFrameHandlerThread;
    private Handler processFrameHandler;

    //Queue
    private ProcessWithQueue processFrameQueue;
    private LinkedBlockingQueue<byte[]> frameQueue;

    //ThreadPool
    private ProcessWithThreadPool processFrameThreadPool;

    private CustomImageButton cimbt;

    public CameraPreview(Context context,CustomImageButton cimbt) {
        super(context);
        this.cimbt=cimbt;
        mHolder = getHolder();
        mHolder.addCallback(this);
        setZOrderMediaOverlay(true);
        switch (processType) {
            case PROCESS_WITH_HANDLER_THREAD:
                processFrameHandlerThread = new ProcessWithHandlerThread("process frame");
                processFrameHandler = new Handler(processFrameHandlerThread.getLooper(), processFrameHandlerThread);
                break;
            case PROCESS_WITH_QUEUE:
                frameQueue = new LinkedBlockingQueue<>();
                processFrameQueue = new ProcessWithQueue(frameQueue);
                break;
            case PROCESS_WITH_ASYNC_TASK:
                break;
            case PROCESS_WITH_THREAD_POOL:
                processFrameThreadPool = new ProcessWithThreadPool();
        }
    }

    public void show()
    {
//        setZOrderOnTop(true);//覆盖所有窗口
        setZOrderMediaOverlay(true);
    }

    public void hide()
    {
//        setZOrderOnTop(false);
        setZOrderMediaOverlay(false);
    }

    private void openCameraOriginal() {
        try {
            mCamera = Camera.open();
        } catch (Exception e) {
            Log.d(TAG, "camera is not available");
        }
    }

    public Camera getCameraInstance() {
        if (mCamera == null) {
            CameraHandlerThread mThread = new CameraHandlerThread("camera thread");
            synchronized (mThread) {
                mThread.openCamera();
            }
        }
        return mCamera;
    }

    public void surfaceCreated(SurfaceHolder holder) {
        getCameraInstance();
        mCamera.setPreviewCallback(this);
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.setDisplayOrientation(90);//旋转90度
            mCamera.startPreview();
        } catch (IOException e) {
            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        mHolder.removeCallback(this);
        mCamera.setPreviewCallback(null);
        mCamera.stopPreview();
        mCamera.release();
        mCamera = null;
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        switch (processType) {
            case PROCESS_WITH_HANDLER_THREAD:
                processFrameHandler.obtainMessage(ProcessWithHandlerThread.WHAT_PROCESS_FRAME, data).sendToTarget();
                break;
            case PROCESS_WITH_QUEUE:
                try {
                    frameQueue.put(data);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                break;
            case PROCESS_WITH_ASYNC_TASK:
                new ProcessWithAsyncTask().execute(data);
                break;
            case PROCESS_WITH_THREAD_POOL:
                processFrameThreadPool.post(data,camera);
                break;
        }


        Camera.Size previewSize = camera.getParameters().getPreviewSize();
//        Log.i("previewSize", previewSize.toString());
        Bitmap bitmap = ByteToBitmap(data, previewSize);
//        Log.i("bitmap", bitmap.toString());
        //Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);//将data byte型数组转换成bitmap文件

        final Matrix matrix = new Matrix();//转换成矩阵旋转90度
//        if (cameraPosition == 1) {//position为1时为后置摄像头
        matrix.setRotate(90);
//        } else {
//            matrix.setRotate(-90);
//        }
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);//旋转图片

        Mat grayscaleImage = new Mat(previewSize.height, previewSize.width, CvType.CV_64F);//changed：CV_8UC4
        int absoluteLogoSize = (int) (previewSize.height * 0.2);//预览的小窗口大小

        if (bitmap != null) {
            Mat inputFrame = new Mat();
            Utils.bitmapToMat(bitmap, inputFrame);
//            Imgcodecs.imwrite(FileUtils.resultpath+"原图.jpg", inputFrame);//added
            if (!bitmap.isRecycled()) {
                bitmap.recycle();
            }

            // Create a grayscale image
            Imgproc.cvtColor(inputFrame, grayscaleImage, Imgproc.COLOR_RGBA2RGB);

            MatOfRect mRect = new MatOfRect();


            int maxRectArea = 0 * 0;
            Rect maxRect = null;

            int logonum = 0;


            // 检测目标
            List<Rect> object=new ImageRecognition().getInfoRect(inputFrame);
//            Rect[] object = (Rect[]) new ImageRecognition().getInfoRect(inputFrame).toArray();
//            Log.e(TAG, object. + "Rect[] object.length");

            for (Rect rect : object) {
                ++logonum;
                // 找出最大的面积
                int tmp = rect.width * rect.height;
                if (tmp >= maxRectArea) {
                    maxRectArea = tmp;
                    maxRect = rect;
                }
            }


            Bitmap rectBitmap = null;
            if (logonum != 0) {
                // 剪切最大的头像
                //Log.e("剪切的长宽", String.format("高:%s,宽:%s", maxRect.width, maxRect.height));
                Rect rect = new Rect(maxRect.x, maxRect.y, maxRect.width, maxRect.height);
                Mat rectMat = new Mat(inputFrame, rect);  // 从原始图像拿
                rectBitmap = Bitmap.createBitmap(rectMat.cols(), rectMat.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(rectMat, rectBitmap);

                Bitmap resizeBmp = cimbt.resizeBitmap(rectBitmap, cimbt.getWidth(), cimbt.getHeight());
                cimbt.setBitmap(resizeBmp);
            } else {
                cimbt.clearnImage();
                cimbt.setText("没有检测到人脸");
            }
        }

    }

    private class CameraHandlerThread extends HandlerThread {
        Handler mHandler;

        public CameraHandlerThread(String name) {
            super(name);
            start();
            mHandler = new Handler(getLooper());
        }

        synchronized void notifyCameraOpened() {
            notify();
        }

        void openCamera() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    openCameraOriginal();
                    notifyCameraOpened();
                }
            });
            try {
                wait();
            } catch (InterruptedException e) {
                Log.w(TAG, "wait was interrupted");
            }
        }
    }
}
