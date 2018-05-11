package com.tomorrow.mitty.locationindoor.common;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.DMatch;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.Features2d;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

//import org.opencv.highgui.Highgui;//old

/**
 * Created by niwei on 2017/4/28.
 */
public class ImageRecognition {

    private float nndrRatio = 0.7f;//这里设置既定值为0.7，该值可自行调整

    private int matchesPointCount = 0;

    private Mat cameraOriginalImage;//从相机中提取的图片帧

    public List<Rect> getInfoRect(Mat originalImage){
        originalImage=Imgcodecs.imread(FileUtils.resultpath+"原图.jpg",Imgcodecs.CV_LOAD_IMAGE_COLOR);
        this.cameraOriginalImage=originalImage;
//        String sdDir = getSDCardBaseDir();
//        String filePath = sdDir + "/Pictures/OpenCV/" + "nake" + ".png";
        //模板// 图片列表
        List<String> imagePathList = FileUtils.getImagePathFromSD();
        List<Rect> locInfos = new ArrayList<Rect>();
        for (String templateFilePath:imagePathList){
            Mat templateImage = Imgcodecs.imread(templateFilePath, Imgcodecs.CV_LOAD_IMAGE_COLOR);
            ImageRecognition imageRecognition = new ImageRecognition();
            Rect locInfo=imageRecognition.matchImage(templateImage, originalImage);
            if(locInfo!=null){
                locInfos.add(locInfo);
            }
            System.out.println("匹配的像素点总数：" + imageRecognition.getMatchesPointCount());
        }
        return locInfos;
    }



    public float getNndrRatio() {
        return nndrRatio;
    }

    public void setNndrRatio(float nndrRatio) {
        this.nndrRatio = nndrRatio;
    }

    public int getMatchesPointCount() {
        return matchesPointCount;
    }

    public void setMatchesPointCount(int matchesPointCount) {
        this.matchesPointCount = matchesPointCount;
    }

    public Rect matchImage(Mat templateImage, Mat originalImage) {
        Rect locationInfomation=null;
        MatOfKeyPoint templateKeyPoints = new MatOfKeyPoint();
        //指定特征点算法SURF
//        OpenCV 中针对特征点匹配问题已经提供了很多算法，包括 FAST 、SIFT 、SURF 、ORB 等，这里不赘述这些算法之间的区别，直接以 SURF 为例，看下 OpenCV 里面如何应用的
        FeatureDetector featureDetector = FeatureDetector.create(FeatureDetector.SURF);
        //获取模板图的特征点
        featureDetector.detect(templateImage, templateKeyPoints);
        //提取模板图的特征点
        MatOfKeyPoint templateDescriptors = new MatOfKeyPoint();
        DescriptorExtractor descriptorExtractor = DescriptorExtractor.create(DescriptorExtractor.SURF);

        System.out.println("提取模板图的特征点");
        descriptorExtractor.compute(templateImage, templateKeyPoints, templateDescriptors);

        //显示模板图的特征点图片
//        Imgcodecs.imread(fileName, Imgcodecs.CV_LOAD_IMAGE_GRAYSCALE)//new example
//        Mat outputImage = new Mat(templateImage.rows(), templateImage.cols(), Highgui.CV_LOAD_IMAGE_COLOR);//old
        Mat outputImage = new Mat(templateImage.rows(), templateImage.cols(), Imgcodecs.CV_LOAD_IMAGE_COLOR);
        System.out.println("在模板图片上显示提取的特征点");
        Features2d.drawKeypoints(templateImage, templateKeyPoints, outputImage, new Scalar(255, 0, 0), 0);

        //获取原图的特征点
        MatOfKeyPoint originalKeyPoints = new MatOfKeyPoint();
        MatOfKeyPoint originalDescriptors = new MatOfKeyPoint();
        featureDetector.detect(originalImage, originalKeyPoints);
        System.out.println("提取原图的特征点");
        descriptorExtractor.compute(originalImage, originalKeyPoints, originalDescriptors);

        //显示原图的特征点图片
        Mat outputImage1 = new Mat(originalImage.rows(), originalImage.cols(), Imgcodecs.CV_LOAD_IMAGE_COLOR);
        System.out.println("在原图片上显示提取的特征点");
        Features2d.drawKeypoints(originalImage, originalKeyPoints, outputImage1, new Scalar(255, 0, 0), 0);

        List<MatOfDMatch> matches = new LinkedList();
        DescriptorMatcher descriptorMatcher = DescriptorMatcher.create(DescriptorMatcher.FLANNBASED);
        System.out.println("寻找最佳匹配");
        /**
         * knnMatch方法的作用就是在给定特征描述集合中寻找最佳匹配
         * 使用KNN-matching算法，令K=2，则每个match得到两个最接近的descriptor，然后计算最接近距离和次接近距离之间的比值，当比值大于既定值时，才作为最终match。
         */
        //changed:add{
//        templateDescriptors.convertTo(templateDescriptors,CvType.CV_32F);
//        originalDescriptors.convertTo(originalDescriptors,CvType.CV_32F);
        //changed:add}
        descriptorMatcher.knnMatch(templateDescriptors, originalDescriptors, matches, 2);

        System.out.println("计算匹配结果");
        LinkedList<DMatch> goodMatchesList = new LinkedList();

        //对匹配结果进行筛选，依据distance进行筛选
        matches.forEach(match -> {
            DMatch[] dmatcharray = match.toArray();
            DMatch m1 = dmatcharray[0];
            DMatch m2 = dmatcharray[1];

            if (m1.distance <= m2.distance * nndrRatio) {
                goodMatchesList.addLast(m1);
            }
        });

        matchesPointCount = goodMatchesList.size();
        //当匹配后的特征点大于等于 4 个，则认为模板图在原图中，该值可以自行调整
        if (matchesPointCount >= 20) {//changed:4
            System.out.println("模板图在原图匹配成功！");

            List<KeyPoint> templateKeyPointList = templateKeyPoints.toList();
            List<KeyPoint> originalKeyPointList = originalKeyPoints.toList();
            LinkedList<Point> objectPoints = new LinkedList();
            LinkedList<Point> scenePoints = new LinkedList();
            goodMatchesList.forEach(goodMatch -> {
                objectPoints.addLast(templateKeyPointList.get(goodMatch.queryIdx).pt);
                scenePoints.addLast(originalKeyPointList.get(goodMatch.trainIdx).pt);
            });
            MatOfPoint2f objMatOfPoint2f = new MatOfPoint2f();
            objMatOfPoint2f.fromList(objectPoints);
            MatOfPoint2f scnMatOfPoint2f = new MatOfPoint2f();
            scnMatOfPoint2f.fromList(scenePoints);
            //使用 findHomography 寻找匹配上的关键点的变换
            Mat homography = Calib3d.findHomography(objMatOfPoint2f, scnMatOfPoint2f, Calib3d.RANSAC, 3);

            /**
             * 透视变换(Perspective Transformation)是将图片投影到一个新的视平面(Viewing Plane)，也称作投影映射(Projective Mapping)。
             */
            Mat templateCorners = new Mat(4, 1, CvType.CV_32FC2);//changed:CV_32FC2
            Mat templateTransformResult = new Mat(4, 1, CvType.CV_32FC2);//changed:CV_32FC2
            templateCorners.put(0, 0, new double[]{0, 0});
            templateCorners.put(1, 0, new double[]{templateImage.cols(), 0});
            templateCorners.put(2, 0, new double[]{templateImage.cols(), templateImage.rows()});
            templateCorners.put(3, 0, new double[]{0, templateImage.rows()});
            //使用 perspectiveTransform 将模板图进行透视变以矫正图象得到标准图片
            Core.perspectiveTransform(templateCorners, templateTransformResult, homography);

            //矩形四个顶点
            double[] pointA = templateTransformResult.get(0, 0);
            double[] pointB = templateTransformResult.get(1, 0);
            double[] pointC = templateTransformResult.get(2, 0);
            double[] pointD = templateTransformResult.get(3, 0);

            //指定取得数组子集的范围
            int rowStart = (int) pointA[1];
            int rowEnd = (int) pointC[1];
            int colStart = (int) pointD[0];
            int colEnd = (int) pointB[0];
            if(rowStart>=0 && colStart>=0 &&
                    (colEnd-colStart)>=0 && (rowEnd-rowStart)>=0){
//                System.out.printf("%d  %d  %d  %d ",rowStart,rowEnd,colStart,colEnd);
                Mat subMat = originalImage.submat(rowStart, rowEnd, colStart, colEnd);
                Imgcodecs.imwrite(FileUtils.resultpath+"原图中的匹配图.jpg", subMat);
            }


            //获取识别出的logo的位置信息
            locationInfomation=new Rect(new Point(pointA),new Point(pointD));
            //将匹配的图像用用四条线框出来
            Imgproc.line(originalImage, new Point(pointA), new Point(pointB), new Scalar(0, 255, 0), 4);//上 A->B
            Imgproc.line(originalImage, new Point(pointB), new Point(pointC), new Scalar(0, 255, 0), 4);//右 B->C
            Imgproc.line(originalImage, new Point(pointC), new Point(pointD), new Scalar(0, 255, 0), 4);//下 C->D
            Imgproc.line(originalImage, new Point(pointD), new Point(pointA), new Scalar(0, 255, 0), 4);//左 D->A

            MatOfDMatch goodMatches = new MatOfDMatch();
            goodMatches.fromList(goodMatchesList);
            Mat matchOutput = new Mat(originalImage.rows() * 2, originalImage.cols() * 2, Imgcodecs.CV_LOAD_IMAGE_COLOR);
            Features2d.drawMatches(templateImage, templateKeyPoints, originalImage, originalKeyPoints, goodMatches, matchOutput, new Scalar(0, 255, 0), new Scalar(255, 0, 0), new MatOfByte(), 2);

            Imgcodecs.imwrite(FileUtils.resultpath+"特征点匹配过程.jpg", matchOutput);
            Imgcodecs.imwrite(FileUtils.resultpath+"模板图在原图中的位置.jpg", originalImage);
        } else {
            System.out.println("模板图不在原图中！");
        }

        Imgcodecs.imwrite(FileUtils.resultpath+"模板特征点.jpg", outputImage);
        Imgcodecs.imwrite(FileUtils.resultpath+"原图特征点.jpg", outputImage1);

        return locationInfomation;
    }

//    public static void main(String[] args) {
//        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
//
//        String templateFilePath = "/Users/apple/Desktop/openCV/模板.jpeg";
//        String originalFilePath = "/Users/apple/Desktop/openCV/原图.jpeg";
//        //读取图片文件
//        Mat templateImage = Imgcodecs.imread(templateFilePath, Imgcodecs.CV_LOAD_IMAGE_COLOR);
//        Mat originalImage = Imgcodecs.imread(originalFilePath, Imgcodecs.CV_LOAD_IMAGE_COLOR);
//
//        ImageRecognition imageRecognition = new ImageRecognition();
//        imageRecognition.matchImage(templateImage, originalImage);
//
//        System.out.println("匹配的像素点总数：" + imageRecognition.getMatchesPointCount());
//    }


}