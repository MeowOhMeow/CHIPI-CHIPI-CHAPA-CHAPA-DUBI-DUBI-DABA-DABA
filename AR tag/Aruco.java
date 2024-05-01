package artag;
import java.awt.Point;
import java.util.List;

import org.opencv.*;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;



public class Aruco {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Mat image=Imgcodecs.imread("1.png");
		distortImage(image);
		
	}
	
	
	
	
	
	public static void distortImage(Mat img) {

        double[] distData = { -0.215168,0.044354, 0.003615, 0.005093, 0.000000 };
        MatOfDouble dist = new MatOfDouble();
        dist.fromArray(distData);

        double[][] mtxData = { {61.783002, 0.000000,595.212041},
                                {0.000000,671.508662,489.094196},
                                {0.000000, 0.000000, 1.000000} };
        Mat mtx = new Mat(3, 3, CvType.CV_64FC1);
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                mtx.put(i, j, mtxData[i][j]);
            }
        }

        //Mat  = Imgcodecs.imread("Area_1_test.png");
        int w = img.cols();
        int h = img.rows();

        Mat newCameraMatrix = Calib3d.getOptimalNewCameraMatrix(mtx, dist, new Size(w, h), 1, new Size(w, h));
        Mat undistortedImg = new Mat();

        

        Calib3d.undistort(img, undistortedImg, mtx, dist, newCameraMatrix);
        Imgcodecs.imwrite("distort.png",undistortedImg);

    }	
	
	public static void findArucoAndCut(Mat img , Mat mtx , MatOfDouble dist , int w , int h) {


		/**
        Dictionary arucoDict = Aruco.getPredefinedDictionary(5);
        DetectorParameters parameters = DetectorParameters.create();
        List<Mat> corners = new ArrayList<Mat>();
        MatOfDouble rvec = new MatOfDouble();
        MatOfDouble tvec = new MatOfDouble();
        Mat ids = new Mat();
        
        //ArucoDetector detector= new ArucoDetector(dictionary,parameters);
        Aruco.detectMarkers(img, arucoDict, corners, ids);


        Aruco.estimatePoseSingleMarkers(corners, 0.05, mtx, dist, rvec, tvec); //unit=cm

		**/
		
    }
	
	public static double[][] quaternionRotationMatrix(double[] Q) {
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
        double[][] rotMatrix = {{r00, r01, r02}, {r10, r11, r12}, {r20, r21, r22}};

        return rotMatrix;
    }
	
	public static double[] cameraToWorld(double[] pos, double[] quaternion, double[] point) {
        double[][] R = quaternionRotationMatrix(quaternion);
        double[][] Rt01 = {
            {R[0][0], R[0][1], R[0][2], pos[0]},
            {R[1][0], R[1][1], R[1][2], pos[1]},
            {R[2][0], R[2][1], R[2][2], pos[2]},
            {0, 0, 0, 1}
        };

        double[] result = new double[3];
        for (int i = 0; i < 3; i++) {
            double sum = 0;
            for (int j = 0; j < 4; j++) {
                sum += Rt01[i][j] * point[j];
            }
            result[i] = sum;
        }

        return result;
    }
	
	
	public static double cal_dist(Point a , Point b) {
		
		double d= Math.sqrt((a.x-b.x)*(a.x-b.x)+(a.y-b.y)*(a.y-b.y));
		
		return d;
	}

	
	public static double[] ctw_transform() {
		
		//center=api.center
		double[] center= {0,0,0};
		double[] Q= {0.5,0.5,0.5,0.5};
		
		double[] where_item= {0,0,0};
		double snap_distance=0.5;
		double[] snap_point= {where_item[0]-snap_distance,where_item[1],where_item[2]};
		double[] point = {snap_point[0],snap_point[1],snap_point[2],1};
		
		
		return cameraToWorld(center,Q,point);
	}
	
	
}