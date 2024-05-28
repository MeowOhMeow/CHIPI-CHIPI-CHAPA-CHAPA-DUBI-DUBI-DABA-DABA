package jp.jaxa.iss.kibo.rpc.sampleapk;

import android.util.Log;

import gov.nasa.arc.astrobee.types.Point;
import gov.nasa.arc.astrobee.types.Quaternion;
import jp.jaxa.iss.kibo.rpc.api.KiboRpcService;

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
 * @brief Process the image and return the snap point
 * 
 * Usage:
 * 1. setDistortCoefficient(double[] distortCoefficient)
 * 2. setCameraMatrix(double[] cameraMatrix)
 * 3. setSnapDistance(double snapDistance) (optional, default is 0.6)
 * 4. process(Point center, Quaternion orientation, Mat img)
 */
public class ArtagProcess extends KiboRpcService {
	private static double snapDistance = 0.6d;
	private static Mat rvec = new Mat();
	private static Mat tvec = new Mat();
	private static Mat distortCoefficient = new Mat();
	private static Mat cameraMatrix = new Mat();
	private static Mat newCameraMatrix = new Mat();

	/**
	 * @brief Set the distortion coefficient
	 * @param distortCoefficient
	 */
	public static void setDistortCoefficient(double[] distortCoefficient) {
		MatOfDouble matOfDouble = new MatOfDouble();
		matOfDouble.fromArray(distortCoefficient);
		ArtagProcess.distortCoefficient = matOfDouble.reshape(1, 1);
		Log.i("ArtagProcess","dist: "+distortCoefficient[0]+" "+distortCoefficient[1]+" "+distortCoefficient[2]+" "+distortCoefficient[3]+" "+distortCoefficient[4]);
	}

	/**
	 * @brief Set the camera matrix
	 * @param cameraMatrix
	 */
	public static void setCameraMatrix(double[] cameraMatrix) {
		MatOfDouble matOfDouble = new MatOfDouble();
		matOfDouble.fromArray(cameraMatrix);
		ArtagProcess.cameraMatrix = matOfDouble.reshape(1, 3);
		Log.i("ArtagProcess","c_mtx: "+cameraMatrix[0]+" "+cameraMatrix[1]+" "+cameraMatrix[2]+" "+cameraMatrix[3]+" "+cameraMatrix[4]+" "+cameraMatrix[5]+" "+cameraMatrix[6]+" "+cameraMatrix[7]+" "+cameraMatrix[8]);
	}

	/**
	 * @brief Set the snap distance
	 * @param snapDistance
	 */
	public static void setSnapDistance(double snapDistance) {
		ArtagProcess.snapDistance = snapDistance;
	}

	/**
	 * @brief Process the image and return the snap point
	 * @param center: astrobee center point
	 * @param orientation: astrobee orientation
	 * @param img: image from astrobee camera
	 * @return ArtagOutput: snap point and result image
	 */
	public static ArtagOutput process(Point center, Quaternion orientation, Mat img) {
		Log.i("ArtagProcess", "Start process");
		// img processing
		Mat undistortedImage = undistortImage(img);
		Log.i("ArtagProcess", "Image undistorted");

		//Mat resultImage = findArucoAndCut(undistortedImage);
		Mat resultImage = findArucoAndCut(undistortedImage);
		Log.i("ArtagProcess", "Image found ar tag");

		Point snapWorld = getWorldPoint(center, orientation, rvec, tvec);
		Log.i("ArtagProcess", "Get world point: " + snapWorld);

		return new ArtagOutput(snapWorld, resultImage);
	}

	/**
	 * @brief Get the world point from the camera point
	 * @param center: astrobee center point
	 * @param orientation: astrobee orientation
	 * @param rvec: rotation vector
	 * @param tvec: translation vector
	 * @return Point: world point
	 */
	private static Point getWorldPoint(Point center, Quaternion orientation, Mat rvec, Mat tvec) {
		// get tvec and rotation matrix
		double[] arTagInCamera = { tvec.get(0, 0)[0], tvec.get(0, 0)[1], tvec.get(0, 0)[2] };
		Mat rotationMatrix = new Mat();
		Calib3d.Rodrigues(rvec, rotationMatrix);
		//Mat rotationMatrix= rotation.inv();
		double[][] R = {
				{ rotationMatrix.get(0, 0)[0], rotationMatrix.get(0, 1)[0], rotationMatrix.get(0, 2)[0] },
				{ rotationMatrix.get(1, 0)[0], rotationMatrix.get(1, 1)[0], rotationMatrix.get(1, 2)[0] },
				{ rotationMatrix.get(2, 0)[0], rotationMatrix.get(2, 1)[0], rotationMatrix.get(2, 2)[0] } };

		Log.i("ArtagProcess", "tvec: " + arTagInCamera[0]+" " + arTagInCamera[1]+" " + arTagInCamera[2]);
		Log.i("ArtagProcess", "R: " + R[0][0]+" " + R[0][1]+" " +R[0][2]);
		Log.i("ArtagProcess", "   " + R[1][0]+" " + R[1][1]+" " +R[1][2]);
		Log.i("ArtagProcess", "   " + R[2][0]+" " + R[2][1]+" " +R[2][2]);
		double[][] nc = {
				{ newCameraMatrix.get(0, 0)[0], newCameraMatrix.get(0, 1)[0], newCameraMatrix.get(0, 2)[0] },
				{ newCameraMatrix.get(1, 0)[0], newCameraMatrix.get(1, 1)[0], newCameraMatrix.get(1, 2)[0] },
				{ newCameraMatrix.get(2, 0)[0], newCameraMatrix.get(2, 1)[0], newCameraMatrix.get(2, 2)[0] } };
		Log.i("ArtagProcess","nc_mtx: "+nc[0][0]+" "+nc[0][1]+" "+nc[0][2]+" "+nc[1][0]+" "+nc[1][1]+" "+nc[1][2]+" "+nc[2][0]+" "+nc[2][1]+" "+nc[2][2]);


		// artag to camera
		double[] item_artag = { 0, 0.0375, -0.135, 1 };
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
		double[] center_offset = { 0.1177 , -0.0422 , -0.0826 };
		double[] snapPoint = { item_camera[0] - snapDistance - center_offset[0] , item_camera[1] - center_offset[1] , item_camera[2] - center_offset[2] , 1 };

		double[] Q = { orientation.getW(), orientation.getX(), orientation.getY(), orientation.getZ() };
		double[] centerPoint = { center.getX(), center.getY(), center.getZ() };
		double[] worldPoint = cameraToWorld(centerPoint, Q, snapPoint);

		return new Point(worldPoint[0], worldPoint[1], worldPoint[2]);
	}

	/**
	 * @brief Undistort the image
	 * @param img: image from astrobee camera
	 * @return Mat: undistorted image
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
	 * @brief Convert a quaternion to a 3x3 rotation matrix
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
		double[][] rotMatrix = { { r00, r01, r02 }, { r10, r11, r12 }, { r20, r21, r22 } };

		return rotMatrix;
	}

	/**
	 * @brief Transform a point from camera to world coordinates
	 * 
	 * @param pos
	 * @param quaternion
	 * @param point
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
	 * @brief Calculate the distance between two points
	 * 
	 * @param a
	 * @param b
	 * @return distance between a and b
	 */
	private static double calDist(double[] a, double[] b) {
		double d = Math.sqrt((a[0] - b[0]) * (a[0] - b[0]) + (a[1] - b[1]) * (a[1] - b[1]));
		return d;
	}

	/**
	 * @brief Calculate the Euclidean distance of a vector
	 * 
	 * @param vec
	 * @return Euclidean distance of vec
	 */
	private static double getEuclideanDistance(double[] vec) {
		double sum = 0;
		for (int i = 0; i < vec.length; i++) {
			sum += vec[i] * vec[i];
		}
		return Math.sqrt(sum);
	}

	/**
	 * @brief Find the Aruco tag and cut the image
	 * 
	 * @param img: image from astrobee camera
	 * @return Mat: result image
	 */
	private static Mat findArucoAndCut(Mat img) {
		Dictionary arucoDict = Aruco.getPredefinedDictionary(Aruco.DICT_5X5_250);
		DetectorParameters parameters = DetectorParameters.create();
		List<Mat> corners = new ArrayList<Mat>();
		Mat rvecs = new Mat();
		Mat tvecs = new Mat();
		Mat ids = new Mat();

		// ArucoDetector detector= new ArucoDetector(dictionary,parameters);
		Aruco.detectMarkers(img, arucoDict, corners, ids, parameters);
		Log.i("ArtagProcess", "corners size:" + String.valueOf(corners.size()));
		Log.i("ArtagProcess", "detect complete");

		Aruco.estimatePoseSingleMarkers(corners, 0.05f, newCameraMatrix, distortCoefficient, rvecs, tvecs); // unit=cm
		Log.i("ArtagProcess", "corners size:" + String.valueOf(corners.size()));
		int closestIndex = 0;
		double closestDistance = getEuclideanDistance(tvecs.get(0, 0));
		for (int index = 1; index < tvecs.rows(); index++) {
			double distance = getEuclideanDistance(tvecs.get(index, 0));
			if (distance < closestDistance) {
				closestDistance = distance;
				closestIndex = index;
			}
		}
		rvec = rvecs.row(closestIndex);
		tvec = tvecs.row(closestIndex);

		Log.i("ArtagProcess", "coerners cols:" + (corners.get(
				closestIndex)).cols() + "coerners rows:" + (corners.get(closestIndex)).rows());
		double[] vert = { 0, 0 };
		double[] hoz = { 0, 0 };

		hoz[0] = (corners.get(closestIndex)).get(0, 0)[0] - (corners.get(closestIndex)).get(0, 1)[0];
		hoz[1] = (corners.get(closestIndex)).get(0, 0)[1] - (corners.get(closestIndex)).get(0, 1)[1];

		vert[0] = (corners.get(closestIndex)).get(0, 0)[0] - (corners.get(closestIndex)).get(0, 3)[0];
		vert[1] = (corners.get(closestIndex)).get(0, 0)[1] - (corners.get(closestIndex)).get(0, 3)[1];

		double[] lt = { 0, 0 };
		double[] rt = { 0, 0 };
		double[] lb = { 0, 0 };
		double[] rb = { 0, 0 };
		double[] rt_dst = { 0, 0 };
		double[] rb_dst = { 0, 0 };
		double[] lb_dst = { 0, 0 };

		lt[0] = (corners.get(closestIndex)).get(0, 0)[0] + (20.75 / 5) * hoz[0] + (1.25 / 5) * vert[0];
		lt[1] = (corners.get(closestIndex)).get(0, 0)[1] + (20.75 / 5) * hoz[1] + (1.25 / 5) * vert[1];

		rt[0] = (corners.get(closestIndex)).get(0, 0)[0] + (0.75 / 5) * hoz[0] + (1.25 / 5) * vert[0];
		rt[1] = (corners.get(closestIndex)).get(0, 0)[1] + (0.75 / 5) * hoz[1] + (1.25 / 5) * vert[1];

		lb[0] = (corners.get(closestIndex)).get(0, 0)[0] + (20.75 / 5) * hoz[0] + (-13.75 / 5) * vert[0];
		lb[1] = (corners.get(closestIndex)).get(0, 0)[1] + (20.75 / 5) * hoz[1] + (-13.75 / 5) * vert[1];

		rb[0] = (corners.get(closestIndex)).get(0, 0)[0] + (0.75 / 5) * hoz[0] + (-13.75 / 5) * vert[0];
		rb[1] = (corners.get(closestIndex)).get(0, 0)[1] + (0.75 / 5) * hoz[1] + (-13.75 / 5) * vert[1];

		rt_dst[0] = lt[0] + calDist(lt, rt);
		rt_dst[1] = lt[1];

		rb_dst[0] = rt_dst[0];
		rb_dst[1] = rt_dst[1] + calDist(rb, rt);

		lb_dst[0] = lt[0];
		lb_dst[1] = lt[1] + calDist(lt, lb);

		Log.i("ArtagProcess", "calculation complete");

		Mat src_point = new Mat(4, 1, CvType.CV_32FC2);
		Mat dst_point = new Mat(4, 1, CvType.CV_32FC2);
		double[] src = { lt[0], lt[1], rt[0], rt[1], lb[0], lb[1], rb[0], rb[1] };
		double[] dst = { 0, 0, rt_dst[0] - lt[0], rt_dst[1] - lt[1], lb_dst[0] - lt[0], lb_dst[1] - lt[1],
				rb_dst[0] - lt[0], rb_dst[1] - lt[1] };
		src_point.put(0, 0, lt);
		src_point.put(1, 0, rt);
		src_point.put(2, 0, lb);
		src_point.put(3, 0, rb);

		double[] zero = { 0, 0 };
		double[] dst_2 = { rt_dst[0] - lt[0], rt_dst[1] - lt[1] };
		double[] dst_3 = { lb_dst[0] - lt[0], lb_dst[1] - lt[1] };
		double[] dst_4 = { rb_dst[0] - lt[0], rb_dst[1] - lt[1] };
		dst_point.put(0, 0, zero);
		dst_point.put(1, 0, dst_2);
		dst_point.put(2, 0, dst_3);
		dst_point.put(3, 0, dst_4);

		Mat M = Imgproc.getPerspectiveTransform(src_point, dst_point);
		Mat resultImage = new Mat();

		Size dsize = new Size();
		dsize.width = calDist(lt, rt);
		dsize.height = calDist(rt, rb);
		Imgproc.warpPerspective(img, resultImage, M, dsize);

		return resultImage;
	}
}
