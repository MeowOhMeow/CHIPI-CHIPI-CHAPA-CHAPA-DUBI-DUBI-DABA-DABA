package jp.jaxa.iss.kibo.rpc.taiwan;

import android.util.Log;

import gov.nasa.arc.astrobee.types.Point;
import gov.nasa.arc.astrobee.types.Quaternion;

import java.util.List;
import java.util.ArrayList;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.Size;
import org.opencv.imgproc.*;
import org.opencv.aruco.*;

/**
 * Process the image and return the snap point
 *
 * Usage:
 * 1. setDistortCoefficient(double[] distortCoefficient)
 * 2. setCameraMatrix(double[] cameraMatrix)
 * 3. setSnapDistance(double snapDistance) (optional, default is 0.6)
 * 4. process(Point center, Quaternion orientation, Mat img)
 */
public class ARTagProcess {

    private static final String TAG = "ARTagProcess";

    private static double snapDistance = 0.6d;
    private static Mat distortCoefficient = null;
    private static Mat cameraMatrix = null;
    private static Mat newCameraMatrix = new Mat();
    private static Dictionary ArUcoDict = Aruco.getPredefinedDictionary(Aruco.DICT_5X5_250);
    private static DetectorParameters parameters = DetectorParameters.create();
    private static Point[] areaCenter = { new Point(10.95d, -10.58d, 5.195d), new Point(10.925d, -8.875d, 3.76203d),
            new Point(10.925d, -7.925d, 3.76093d), new Point(9.866984d, -6.8525d, 4.945d),
            new Point(11.143d, -6.7607d, 4.9654d) };

    private static class DetectionResult {

        private Mat resultImage;
        private Boolean valid;
        private Mat rvec, tvec;

        DetectionResult(Mat resultImage, Boolean valid, Mat rvec, Mat tvec) {
            this.resultImage = resultImage;
            this.valid = valid;
            this.rvec = rvec;
            this.tvec = tvec;
        }

        Mat getResultImage() {
            return resultImage;
        }

        Boolean getValid() {
            return valid;
        }

        Mat getRvec() {
            return rvec;
        }

        Mat getTvec() {
            return tvec;
        }
    }

    /**
     * Set the distortion coefficient
     *
     * @param distortCoefficient: distortion coefficient
     */
    public static void setDistortCoefficient(double[] distortCoefficient) {
        MatOfDouble matOfDouble = new MatOfDouble();
        matOfDouble.fromArray(distortCoefficient);
        ARTagProcess.distortCoefficient = matOfDouble.reshape(1, 1);
        Log.i(TAG, "dist: " + distortCoefficient[0] + " " + distortCoefficient[1] + " " + distortCoefficient[2] + " "
                + distortCoefficient[3] + " " + distortCoefficient[4]);
    }

    /**
     * Set the camera matrix
     *
     * @param cameraMatrix: camera matrix
     */
    public static void setCameraMatrix(double[] cameraMatrix) {
        MatOfDouble matOfDouble = new MatOfDouble();
        matOfDouble.fromArray(cameraMatrix);
        ARTagProcess.cameraMatrix = matOfDouble.reshape(1, 3);
        Log.i(TAG,
                "camera matrix: " + cameraMatrix[0] + " " + cameraMatrix[1] + " " + cameraMatrix[2] + " "
                        + cameraMatrix[3]
                        + " " + cameraMatrix[4] + " " + cameraMatrix[5] + " " + cameraMatrix[6] + " " + cameraMatrix[7]
                        + " " + cameraMatrix[8]);
    }

    /**
     * Set the snap distance
     *
     * @param snapDistance: snap distance
     */
    public static void setSnapDistance(double snapDistance) {
        ARTagProcess.snapDistance = snapDistance;
    }

    public static ARTagOutput[] process(Point center, Quaternion orientation, Mat img) {
        if (distortCoefficient == null || cameraMatrix == null) {
            Log.e(TAG, "Camera matrix or distortion coefficient is not set");
            return new ARTagOutput[0];
        }

        Log.i(TAG, "Start process");
        // img processing
        Mat undistortedImage = undistortImage(img);
        Log.i(TAG, "Image undistorted");
        List<Mat> corners = new ArrayList<>();
        Aruco.detectMarkers(undistortedImage, ArUcoDict, corners, new Mat(), parameters);
        ARTagOutput[] output = new ARTagOutput[corners.size()];

        Log.i(TAG, "corners size:" + corners.size());
        if (corners.size() == 0) {
            Log.i(TAG, "no aruco tag detected");
            return new ARTagOutput[0];
        }

        for (int ARTagIdx = 0; ARTagIdx < corners.size(); ARTagIdx++) {
            List<Mat> singleCorner = new ArrayList<>();
            singleCorner.add(corners.get(ARTagIdx));
            DetectionResult result = cutImageByCorner(undistortedImage, singleCorner);

            Point snapWorld = getCameraWorldPoint(center, orientation, result.getRvec(),
                    result.getTvec());
            Point ARTagWorld = getARTagWorldPoint(center, orientation, result.getRvec(),
                    result.getTvec());

            int whereARTagIs = -1;
            double leastBoxDistance = Double.MAX_VALUE;
            for (int areaIdx = 0; areaIdx < areaCenter.length; areaIdx++) {
                double boxDistance = Utility.calEuclideanDistance(ARTagWorld, areaCenter[areaIdx]);
                if (boxDistance < leastBoxDistance) {
                    leastBoxDistance = boxDistance;
                    whereARTagIs = areaIdx;
                }
            }
            Log.i(TAG, "ARTagWorld: (" + ARTagWorld.getX() + "," + ARTagWorld.getY() + "," + ARTagWorld.getZ() + ")");
            Log.i(TAG, "areaIdx: " + whereARTagIs);
            output[ARTagIdx] = new ARTagOutput(snapWorld, ARTagWorld, result.getResultImage(), result.getValid(),
                    whereARTagIs);
        }

        Log.i(TAG, "End process");

        return output;
    }

    /**
     * Get the world point from the camera point
     *
     * @param center:      astrobee center point
     * @param orientation: astrobee orientation
     * @param rvec:        rvec from aruco
     * @param tvec:        tvec from aruco
     * @return Point: world point
     */
    private static Point getCameraWorldPoint(Point center, Quaternion orientation, Mat rvec, Mat tvec) {
        // get tvec and rotation matrix
        double[] arTagInCamera = { tvec.get(0, 0)[0], tvec.get(0, 0)[1], tvec.get(0, 0)[2] };
        Mat rotationMatrix = new Mat();
        Calib3d.Rodrigues(rvec, rotationMatrix);
        // Mat rotationMatrix= rotation.inv();
        double[][] R = {
                { rotationMatrix.get(0, 0)[0], rotationMatrix.get(0, 1)[0], rotationMatrix.get(0, 2)[0] },
                { rotationMatrix.get(1, 0)[0], rotationMatrix.get(1, 1)[0], rotationMatrix.get(1, 2)[0] },
                { rotationMatrix.get(2, 0)[0], rotationMatrix.get(2, 1)[0], rotationMatrix.get(2, 2)[0] } };

        Log.i(TAG, "tvec: " + arTagInCamera[0] + " " + arTagInCamera[1] + " " + arTagInCamera[2]);
        Log.i(TAG, "R: " + R[0][0] + " " + R[0][1] + " " + R[0][2]);
        Log.i(TAG, "   " + R[1][0] + " " + R[1][1] + " " + R[1][2]);
        Log.i(TAG, "   " + R[2][0] + " " + R[2][1] + " " + R[2][2]);

        // artag to camera
        double[] item_artag = { -0.135, 0.0375, 0, 1 };
        double[][] Rt01 = {
                { R[0][0], R[0][1], R[0][2], arTagInCamera[0] },
                { R[1][0], R[1][1], R[1][2], arTagInCamera[1] },
                { R[2][0], R[2][1], R[2][2], arTagInCamera[2] },
                { 0, 0, 0, 1 }
        };

        double[] itemPointInCamera = new double[4];
        // dot product, z to front, x to right, y to down
        for (int i = 0; i < 4; i++) {
            double sum = 0;
            for (int j = 0; j < 4; j++) {
                sum += Rt01[i][j] * item_artag[j];
            }
            itemPointInCamera[i] = sum;
        }
        // convert to x to front, y to right, z to down
        double[] item_camera = { itemPointInCamera[2], itemPointInCamera[0], itemPointInCamera[1] };
        double[] snapPoint = { item_camera[0] - snapDistance, item_camera[1], item_camera[2], 1 };

        double[] Q = { orientation.getW(), orientation.getX(), orientation.getY(), orientation.getZ() };
        double[] centerPoint = { center.getX(), center.getY(), center.getZ() };
        double[] worldPoint = cameraToWorld(centerPoint, Q, snapPoint);

        return new Point(worldPoint[0], worldPoint[1], worldPoint[2]);
    }

    /**
     * Get the world point from the artag point
     *
     * @param center:      astrobee center point
     * @param orientation: astrobee orientation
     * @param rvec:        rvec from aruco
     * @param tvec:        tvec from aruco
     * @return Point: world point
     */
    private static Point getARTagWorldPoint(Point center, Quaternion orientation, Mat rvec, Mat tvec) {
        // get tvec and rotation matrix
        double[] arTagInCamera = { tvec.get(0, 0)[0], tvec.get(0, 0)[1], tvec.get(0, 0)[2] };
        Mat rotationMatrix = new Mat();
        Calib3d.Rodrigues(rvec, rotationMatrix);
        // Mat rotationMatrix= rotation.inv();
        double[][] R = {
                { rotationMatrix.get(0, 0)[0], rotationMatrix.get(0, 1)[0], rotationMatrix.get(0, 2)[0] },
                { rotationMatrix.get(1, 0)[0], rotationMatrix.get(1, 1)[0], rotationMatrix.get(1, 2)[0] },
                { rotationMatrix.get(2, 0)[0], rotationMatrix.get(2, 1)[0], rotationMatrix.get(2, 2)[0] } };

        // artag to camera
        double[] item_artag = { -0.135, 0.0375, 0, 1 };
        double[][] Rt01 = {
                { R[0][0], R[0][1], R[0][2], arTagInCamera[0] },
                { R[1][0], R[1][1], R[1][2], arTagInCamera[1] },
                { R[2][0], R[2][1], R[2][2], arTagInCamera[2] },
                { 0, 0, 0, 1 }
        };

        double[] itemPointInCamera = new double[4];
        // dot product, z to front, x to right, y to down
        for (int i = 0; i < 4; i++) {
            double sum = 0;
            for (int j = 0; j < 4; j++) {
                sum += Rt01[i][j] * item_artag[j];
            }
            itemPointInCamera[i] = sum;
        }
        // convert to x to front, y to right, z to down
        double[] item_camera = { itemPointInCamera[2], itemPointInCamera[0], itemPointInCamera[1] };
        double[] ARTagPoint = { item_camera[0], item_camera[1], item_camera[2], 1 };

        double[] Q = { orientation.getW(), orientation.getX(), orientation.getY(), orientation.getZ() };
        double[] centerPoint = { center.getX(), center.getY(), center.getZ() };
        double[] worldPoint = cameraToWorld(centerPoint, Q, ARTagPoint);

        return new Point(worldPoint[0], worldPoint[1], worldPoint[2]);
    }

    /**
     * Undistort the image
     *
     * @param img: image from astrobee camera
     * @return Mat
     */
    private static Mat undistortImage(Mat img) {
        int w = img.cols();
        int h = img.rows();

        newCameraMatrix = Calib3d.getOptimalNewCameraMatrix(cameraMatrix, distortCoefficient, new Size(w, h), 1,
                new Size(w, h));
        Mat undistortedImg = new Mat();

        Calib3d.undistort(img, undistortedImg, cameraMatrix, distortCoefficient, newCameraMatrix);
        return undistortedImg;
    }

    /**
     * Convert a quaternion to a 3x3 rotation matrix
     *
     * @param Q: quaternion in (w, x, y, z)
     * @return 3x3 rotation matrix
     */
    private static double[][] quaternionToRotationMatrix(double[] Q) {
        // Extract the values from Q
        double q0 = Q[0];
        double q1 = Q[1];
        double q2 = Q[2];
        double q3 = Q[3];

        // First row of the rotation matrix
        double r00 = 2 * (q0 * q0 + q1 * q1) - 1;
        double r01 = 2 * (q1 * q2 - q0 * q3);
        double r02 = 2 * (q1 * q3 + q0 * q2);

        // Second row of the rotation matrix
        double r10 = 2 * (q1 * q2 + q0 * q3);
        double r11 = 2 * (q0 * q0 + q2 * q2) - 1;
        double r12 = 2 * (q2 * q3 - q0 * q1);

        // Third row of the rotation matrix
        double r20 = 2 * (q1 * q3 - q0 * q2);
        double r21 = 2 * (q2 * q3 + q0 * q1);
        double r22 = 2 * (q0 * q0 + q3 * q3) - 1;

        // 3x3 rotation matrix
        return new double[][] { { r00, r01, r02 }, { r10, r11, r12 }, { r20, r21, r22 } };
    }

    /**
     * Transform a point from camera to world coordinates
     *
     * @param pos:        position of the camera
     * @param quaternion: quaternion of the camera
     * @param point:      point in camera coordinates
     * @return point in world coordinates
     */
    private static double[] cameraToWorld(double[] pos, double[] quaternion, double[] point) {
        double[][] R = quaternionToRotationMatrix(quaternion);
        double[][] Rt01 = {
                { R[0][0], R[0][1], R[0][2], pos[0] },
                { R[1][0], R[1][1], R[1][2], pos[1] },
                { R[2][0], R[2][1], R[2][2], pos[2] },
                { 0, 0, 0, 1 }
        };

        double[] result = new double[4];
        for (int i = 0; i < 4; i++) {
            double sum = 0;
            for (int j = 0; j < 4; j++) {
                sum += Rt01[i][j] * point[j];
            }
            result[i] = sum;
        }

        return result;
    }

    /**
     * Calculate the distance between two points
     *
     * @param a: point a
     * @param b: point b
     * @return distance between a and b
     */
    private static double calDist(double[] a, double[] b) {
        return Math.sqrt((a[0] - b[0]) * (a[0] - b[0]) + (a[1] - b[1]) * (a[1] - b[1]));
    }

    /**
     * Cut the image by the given corner
     *
     * @param img:     image from astrobee camera
     * @param corners: list of corners
     * @return DetectionResult
     */
    private static DetectionResult cutImageByCorner(Mat img, List<Mat> corners) {

        Mat rvecs = new Mat();
        Mat tvecs = new Mat();

        Aruco.estimatePoseSingleMarkers(corners, 0.05f, newCameraMatrix, distortCoefficient, rvecs, tvecs); // unit=cm

        int idx = 0;

        double[] vertical = { 0, 0 };
        double[] horizontal = { 0, 0 };

        horizontal[0] = (corners.get(idx)).get(0, 0)[0] - (corners.get(idx)).get(0, 1)[0];
        horizontal[1] = (corners.get(idx)).get(0, 0)[1] - (corners.get(idx)).get(0, 1)[1];

        vertical[0] = (corners.get(idx)).get(0, 0)[0] - (corners.get(idx)).get(0, 3)[0];
        vertical[1] = (corners.get(idx)).get(0, 0)[1] - (corners.get(idx)).get(0, 3)[1];

        double[] leftTopCorner = { 0, 0 };
        double[] rightTopCorner = { 0, 0 };
        double[] leftBottomCorner = { 0, 0 };
        double[] rightBottomCorner = { 0, 0 };

        float scalingFactor = 0.2f;

        leftTopCorner[0] = (corners.get(idx)).get(0, 0)[0] + ((20.75f / 5f) * horizontal[0]
                + (1.25 / 5f) * vertical[0]) * (1 + scalingFactor);
        leftTopCorner[1] = (corners.get(idx)).get(0, 0)[1] + ((20.75f / 5f) * horizontal[1]
                + (1.25 / 5f) * vertical[1]) * (1 + scalingFactor);

        rightTopCorner[0] = (corners.get(idx)).get(0, 0)[0] + ((0.75 / 5f) * horizontal[0]
                + (1.25 / 5f) * vertical[0]) * (1 + scalingFactor);
        rightTopCorner[1] = (corners.get(idx)).get(0, 0)[1] + ((0.75 / 5f) * horizontal[1]
                + (1.25 / 5f) * vertical[1]) * (1 + scalingFactor);

        leftBottomCorner[0] = (corners.get(idx)).get(0, 0)[0] + ((20.75f / 5f) * horizontal[0]
                + (-13.75 / 5f) * vertical[0]) * (1 + scalingFactor);
        leftBottomCorner[1] = (corners.get(idx)).get(0, 0)[1] + ((20.75f / 5f) * horizontal[1]
                + (-13.75 / 5f) * vertical[1]) * (1 + scalingFactor);

        rightBottomCorner[0] = (corners.get(idx)).get(0, 0)[0] + ((0.75 / 5f) * horizontal[0]
                + (-13.75 / 5f) * vertical[0]) * (1 + scalingFactor);
        rightBottomCorner[1] = (corners.get(idx)).get(0, 0)[1] + ((0.75 / 5f) * horizontal[1]
                + (-13.75 / 5f) * vertical[1]) * (1 + scalingFactor);

        double width = calDist(leftTopCorner, rightTopCorner);
        double height = calDist(leftTopCorner, leftBottomCorner);

        double[] rightTopDst = { width, 0 };
        double[] rightBottomDst = { width, height };
        double[] leftBottomDst = { 0, height };

        Log.i(TAG, "calculation complete");

        Mat srcPoint = new Mat(4, 1, CvType.CV_32FC2);
        Mat dstPoint = new Mat(4, 1, CvType.CV_32FC2);

        srcPoint.put(0, 0, leftTopCorner);
        srcPoint.put(1, 0, rightTopCorner);
        srcPoint.put(2, 0, leftBottomCorner);
        srcPoint.put(3, 0, rightBottomCorner);

        dstPoint.put(0, 0, 0, 0);
        dstPoint.put(1, 0, rightTopDst);
        dstPoint.put(2, 0, leftBottomDst);
        dstPoint.put(3, 0, rightBottomDst);

        // check whether it's outside or not
        boolean valid = true;
        for (int srcIdx = 0; srcIdx < 4; srcIdx++) {
            double[] checkPoint = srcPoint.get(srcIdx, 0);
            if (checkPoint[0] > img.width() || checkPoint[0] < 0 || checkPoint[1] > img.height() || checkPoint[1] < 0) {
                valid = false;
                break;
            }
        }
        Log.i(TAG, "Validation : " + valid);

        Mat M = Imgproc.getPerspectiveTransform(srcPoint, dstPoint);
        Mat resultImage = new Mat();

        Size dsize = new Size();
        dsize.width = calDist(leftTopCorner, rightTopCorner);
        dsize.height = calDist(rightTopCorner, rightBottomCorner);
        Imgproc.warpPerspective(img, resultImage, M, dsize);

        return new DetectionResult(resultImage, valid, rvecs.row(idx), tvecs.row(idx));
    }
}
