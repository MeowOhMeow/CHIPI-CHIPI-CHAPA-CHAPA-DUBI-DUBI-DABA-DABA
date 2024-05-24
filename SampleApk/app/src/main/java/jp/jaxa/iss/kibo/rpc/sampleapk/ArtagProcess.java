package jp.jaxa.iss.kibo.rpc.sampleapk;

import android.content.res.AssetManager;
import android.content.res.Resources;
import android.util.Log;

import gov.nasa.arc.astrobee.Kinematics;
import gov.nasa.arc.astrobee.types.Point;
import gov.nasa.arc.astrobee.types.Quaternion;
import jp.jaxa.iss.kibo.rpc.api.KiboRpcService;

import java.util.List;
import java.util.ArrayList;

import org.opencv.*;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.*;
import org.opencv.aruco.*;

public class ArtagProcess extends KiboRpcService {

	// ---------------------------------------------

	public static double[] process(Mat img, int index) {
		// parameter initialization
		Mat mtx = new Mat();
		MatOfDouble dist = new MatOfDouble();
		double[] distData = { -0.215168, 0.044354, 0.003615, 0.005093, 0.000000 };

		dist.fromArray(distData);

		double[] mtxData = { 661.783002, 0.000000, 595.212041,
				0.000000, 671.508662, 489.094196,
				0.000000, 0.000000, 1.000000 };
		mtx = new Mat(3, 3, CvType.CV_64FC1);

		mtx.put(0, 0, mtxData);

		int w = 0;
		int h = 0;
		double cz = 0;
		double cy = 0;
		double[] item_camera = { 0, 0, 0 };
		Log.i("artag", "Start process"); //////////

		// img processing
		Mat undistort = distortImage(img, mtx, dist, w, h);

		Log.i("artag", "Distort image complete"); ////////////

		Mat yolo_img = findArucoAndCut(undistort, mtx, dist, w, h, item_camera);
		Log.i("artag", "Aruco finding image complete"); ////////////
		// api.saveMatImage(yolo_img, "yolo" + index + ".jpg");

		// coordinates transformation
		/*
		 * index--;
		 * item[0]=0.5;
		 * double[][] center
		 * ={{10.9078,-10.0293,5.1124},{10.925,-8.875,4.272676419},{10.41031163,-6.8525,
		 * 4.945},{10.925,-7.925,4.291426151}};
		 * double[][] Q=
		 * {{0.707,-0.707,0,0},{0.5,-0.5,0.5,0.5},{0,0,0.71,0.71},{0.5,-0.5,0.5,0.5}};
		 * double snap_distance=0.6;
		 * double[] snap_point={item[0]-snap_distance,item[1],item[2],1};
		 * Log.i("coo","camera coordinate: "+String.valueOf(snap_point[0])+","+String.
		 * valueOf(snap_point[1])+","+String.valueOf(snap_point[2]));
		 * double[] point={-1*snap_point[2],-1*snap_point[1],-1*snap_point[0],1};
		 * double[] ans=cameraToWorld(center[index],Q[index],point);
		 * Log.i("coo","world coordinate: "+String.valueOf(ans[0])+","+String.valueOf(
		 * ans[1])+","+String.valueOf(ans[2]));
		 * return ans;
		 */
		// Artag_output output=new Artag_output(undistort,yolo_img,ans);

		double[][] center = { { 10.9078, -10.0293, 5.1124 }, { 10.925, -8.875, 4.272676419 },
				{ 10.41031163, -6.8525, 4.945 }, { 10.925, -7.925, 4.291426151 } };
		double[][] Q = { { 0.707, -0.707, 0, 0 }, { 0.5, -0.5, 0.5, 0.5 }, { 0, 0, 0.71, 0.71 },
				{ 0.5, -0.5, 0.5, 0.5 } };
		// camera to world
		double[] item_camera_t={item_camera[2],item_camera[0],item_camera[1]};

		double[] snap_camera = { item_camera_t[0] - 0.6, item_camera_t[1], item_camera_t[2], 1 };
		index--;
		double[] snap_world = cameraToWorld(center[index], Q[index], snap_camera);

		Log.i("tf","item_camera_t:"+item_camera_t);
		Log.i("tf","snap_camera:"+snap_camera);
		Log.i("tf","snap_world:"+snap_world);

		return snap_world;


		// return output;
	}

	// ---------------------------------------------
	public static Mat distortImage(Mat img, Mat mtx, MatOfDouble dist, int w, int h) {

		double[] distData = { -0.215168, 0.044354, 0.003615, 0.005093, 0.000000 };

		dist.fromArray(distData);

		double[] mtxData = { 661.783002, 0.000000, 595.212041,
				0.000000, 671.508662, 489.094196,
				0.000000, 0.000000, 1.000000 };
		mtx = new Mat(3, 3, CvType.CV_64FC1);

		mtx.put(0, 0, mtxData);

		// Mat = Imgcodecs.imread("Area_1_test.png");
		w = img.cols();
		h = img.rows();

		Mat newCameraMatrix = Calib3d.getOptimalNewCameraMatrix(mtx, dist, new Size(w, h), 1, new Size(w, h));
		Mat undistortedImg = new Mat();

		Calib3d.undistort(img, undistortedImg, mtx, dist, newCameraMatrix);
		// Imgcodecs.imwrite("C:\\Users\\USER\\Downloads\\distort.png",undistortedImg);
		// api.saveMatImage(undistortImg,"undistortImg.png")
		return undistortedImg;
	}

	// ----------------------------------------
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
		double[][] rotMatrix = { { r00, r01, r02 }, { r10, r11, r12 }, { r20, r21, r22 } };

		return rotMatrix;
	}

	// ---------------------------------------
	public static double[] cameraToWorld(double[] pos, double[] quaternion, double[] point) {
		double[][] R = quaternionRotationMatrix(quaternion);
		double[][] Rt01 = {
				{ R[0][0], R[0][1], R[0][2], pos[0] },
				{ R[1][0], R[1][1], R[1][2], pos[1] },
				{ R[2][0], R[2][1], R[2][2], pos[2] },
				{ 0, 0, 0, 1 }
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

	// -------------------------------------
	public static double cal_dist(double[] a, double[] b) {

		double d = Math.sqrt((a[0] - b[0]) * (a[0] - b[0]) + (a[1] - b[1]) * (a[1] - b[1]));

		return d;
	}

	// -------------------------------------
	public static double cal_len(double[] a) {

		double d = Math.sqrt(a[0] * a[0] + a[1] * a[1]);

		return d;
	}

	// ------------------------------------
	public static double[] ctw_transform() {

		// center=api.center
		double[] center = { 0, 0, 0 };
		double[] Q = { 0.5, 0.5, 0.5, 0.5 };

		double[] where_item = { 0, 0, 0 };
		double snap_distance = 0.5;
		double[] snap_point = { where_item[0] - snap_distance, where_item[1], where_item[2] };
		double[] point = { snap_point[0], snap_point[1], snap_point[2], 1 };

		return cameraToWorld(center, Q, point);
	}
	// ---------------------------------

	public static Mat findArucoAndCut(Mat img , Mat mtx , MatOfDouble dist , int w , int h , double[] item) {


		
        Dictionary arucoDict = Aruco.getPredefinedDictionary(Aruco.DICT_5X5_250);
        DetectorParameters parameters = DetectorParameters.create();
        List<Mat> corners = new ArrayList<Mat>();
        Mat rvec = new Mat();
        Mat tvec = new Mat();
        Mat ids = new Mat();
        
        //ArucoDetector detector= new ArucoDetector(dictionary,parameters);
        Aruco.detectMarkers(img, arucoDict, corners, ids,parameters);
		Log.i("artag","corners size:"+ String.valueOf(corners.size()));
		Log.i("artag","detect complete");

        Aruco.estimatePoseSingleMarkers(corners, 5, mtx, dist, rvec, tvec); //unit=cm
		Log.i("artag","corners size:"+ String.valueOf(corners.size()));

		Log.i("artag","coerners cols:"+(corners.get(0)).cols()+"coerners rows:"+(corners.get(0)).rows());
		double[] vert={0,0};
		double[] hoz={0,0};
		//我不知道coreners的資料型態= =
		hoz[0]=(corners.get(0)).get(0,0)[0]-(corners.get(0)).get(0,1)[0];
		hoz[1]=(corners.get(0)).get(0,0)[1]-(corners.get(0)).get(0,1)[1];
		
		vert[0]=(corners.get(0)).get(0,0)[0]-(corners.get(0)).get(0,3)[0];
		vert[1]=(corners.get(0)).get(0,0)[1]-(corners.get(0)).get(0,3)[1];
		

		double[] lt={0,0};
		double[] rt={0,0};
		double[] lb={0,0};
		double[] rb={0,0};
		double[] rt_dst={0,0};
		double[] rb_dst={0,0};
		double[] lb_dst={0,0};
		
		lt[0]=(corners.get(0)).get(0,0)[0]+(20.75/5)*hoz[0]+(1.25/5)*vert[0];
		lt[1]=(corners.get(0)).get(0,0)[1]+(20.75/5)*hoz[1]+(1.25/5)*vert[1];
		
		rt[0]=(corners.get(0)).get(0,0)[0]+(0.75/5)*hoz[0]+(1.25/5)*vert[0];
		rt[1]=(corners.get(0)).get(0,0)[1]+(0.75/5)*hoz[1]+(1.25/5)*vert[1];
		
		lb[0]=(corners.get(0)).get(0,0)[0]+(20.75/5)*hoz[0]+(-13.75/5)*vert[0];
		lb[1]=(corners.get(0)).get(0,0)[1]+(20.75/5)*hoz[1]+(-13.75/5)*vert[1];
		
		rb[0]=(corners.get(0)).get(0,0)[0]+(0.75/5)*hoz[0]+(-13.75/5)*vert[0];
		rb[1]=(corners.get(0)).get(0,0)[1]+(0.75/5)*hoz[1]+(-13.75/5)*vert[1];
		

		rt_dst[0]=lt[0]+cal_dist(lt,rt);
		rt_dst[1]=lt[1];
		
		rb_dst[0]=rt_dst[0];
		rb_dst[1]=rt_dst[1]+cal_dist(rb,rt);
		
		lb_dst[0]=lt[0];
		lb_dst[1]=lt[1]+cal_dist(lt,lb);

		Log.i("artag","calculation complete");

		Mat src_point= new Mat(4,1,CvType.CV_32FC2);
		Mat dst_point= new Mat(4,1,CvType.CV_32FC2);
		double[] src= {lt[0],lt[1],rt[0],rt[1],lb[0],lb[1],rb[0],rb[1]};
		double[] dst= {0,0,rt_dst[0]-lt[0],rt_dst[1]-lt[1],lb_dst[0]-lt[0],lb_dst[1]-lt[1],rb_dst[0]-lt[0],rb_dst[1]-lt[1]};
		src_point.put(0,0,lt);
		src_point.put(1,0,rt);
		src_point.put(2,0,lb);
		src_point.put(3,0,rb);

		double[] zero={0,0};
		double[] dst_2={rt_dst[0]-lt[0],rt_dst[1]-lt[1]};
		double[] dst_3={lb_dst[0]-lt[0],lb_dst[1]-lt[1]};
		double[] dst_4={rb_dst[0]-lt[0],rb_dst[1]-lt[1]};
		dst_point.put(0,0,zero);
		dst_point.put(1,0,dst_2);
		dst_point.put(2,0,dst_3);
		dst_point.put(3,0,dst_4);






		Mat M=Imgproc.getPerspectiveTransform(src_point,dst_point);
		Mat perspective=new Mat();
		
		Size dsize=new Size();
		dsize.width=cal_dist(lt,rt);
		dsize.height=cal_dist(rt,rb);
		Imgproc.warpPerspective(img,perspective,M,dsize);

		/* 
		Log.i("coo","y:"+String.valueOf((lt[0]+rt[0])/2));
		Log.i("coo","x:"+String.valueOf((rt[1]+rb[1])/2));
		item[2]=(-640+(lt[0]+rt[0]+lb[0]+rb[0])/4)*(0.05/cal_len(hoz));
		item[1]=(480-(rt[1]+rb[1]+lt[1]+lb[1])/4)*(0.05/cal_len(hoz));
		*/

		//get tvec and rotation matrix
		Mat t=tvec.row(0);
		double [] pos= {t.get(0,0)[0],t.get(0,0)[1],t.get(0,0)[2]};
		Mat rr=new Mat();
		Calib3d.Rodrigues(rvec.row(0),rr);
		double [][] R={{rr.get(0,0)[0],rr.get(0,1)[0],rr.get(0,2)[0]} , {rr.get(1,0)[0],rr.get(1,1)[0],rr.get(1,2)[0]},{rr.get(2,0)[0],rr.get(2,1)[0],rr.get(2,2)[0]}};

		Log.i("tf","tvec:"+ pos);
		Log.i("tf","R:"+ R);
		//artag to camera
		double[] item_artag={ 0.0375, -0.135, 1};
		double[][] Rt01 = {
            {R[0][0], R[0][1], R[0][2], pos[0]},
            {R[1][0], R[1][1], R[1][2], pos[1]},
            {R[2][0], R[2][1], R[2][2], pos[2]},
            {0, 0, 0, 1}
        };

        //double[] item_camera = new double[3];
        for (int i = 0; i < 3; i++) {
            double sum = 0;
            for (int j = 0; j < 4; j++) {
                sum += Rt01[i][j] * item_artag[j];
            }
            item[i] = sum;
        }



		return perspective;
    }

	// ------------------------

}
