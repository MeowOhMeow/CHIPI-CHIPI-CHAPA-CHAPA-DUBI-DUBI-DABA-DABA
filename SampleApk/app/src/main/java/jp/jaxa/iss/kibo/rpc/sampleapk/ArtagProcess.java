package jp.jaxa.iss.kibo.rpc.sampleapk;


import android.content.res.AssetManager;
import android.content.res.Resources;
import android.util.Log;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.List;

import org.opencv.*;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.*;



public class ArtagProcess {
    
    //---------------------------------------------
	public static Artag_output artag_process(Mat img,Artag_output a,int index) {
		//parameter initialization
		Mat mtx=new Mat();
		MatOfDouble dist=new MatOfDouble();
		int w=0;
		int h=0;
		double cx=0;
		double cy=0;
		
        //img processing
		Mat undistort=distortImage(img,mtx,dist,w,h);
		Mat yolo_img=findArucoAndCut(undistort,mtx,dist,w,h,cx,cy);

        //coordinates transformation
        index--;
		double[] item= {cx,cy,0};
        double[][] center ={{10.95,-10.14697184,5.195},{10.925,-8.875,4.272676419},{10.41031163,-6.8525,4.945},{10.925,-7.925,4.291426151}}
        double[][] Q= {{0.5,0.5,0.5,0.5},{0.5,-0.5,0.5,0.5},{0,0,0.71,0.71},{0.5,-0.5,0.5,0.5}};
        double snap_distance=0.5;
        double snap_point={item[0]-snap_distance,item[1],item[2]};
        double[] ans=cameraToWorld(center[index],Q[index],snap_point);
		Artag_output output=new Artag_output(undistort,yolo_img,ans);
		
		
		return output;
	}
	//---------------------------------------------
	public static Mat distortImage(Mat img,Mat mtx,MatOfDouble dist,int w,int h) {

        double[] distData = { -0.215168,0.044354, 0.003615, 0.005093, 0.000000 };
        
        dist.fromArray(distData);

        double[] mtxData = { 661.783002, 0.000000,595.212041,
                            0.000000,671.508662,489.094196,
                            0.000000, 0.000000, 1.000000 };
        mtx = new Mat(3, 3, CvType.CV_64FC1);

        mtx.put(0, 0, mtxData);
            
        

        //Mat  = Imgcodecs.imread("Area_1_test.png");
        w = img.cols();
        h = img.rows();

        Mat newCameraMatrix = Calib3d.getOptimalNewCameraMatrix(mtx, dist, new Size(w, h), 1, new Size(w, h));
        Mat undistortedImg = new Mat();

        

        Calib3d.undistort(img, undistortedImg, mtx, dist, newCameraMatrix);
        Imgcodecs.imwrite("C:\\Users\\USER\\Downloads\\distort.png",undistortedImg);
        //api.saveMatImage(undistortImg,"undistortImg.png")
        return undistortedImg;
    }	
	
	//----------------------------------------
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
	//---------------------------------------
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
	
	//-------------------------------------
	public static double cal_dist(Point2D.Double a , Point2D.Double b) {
		
		double d= Math.sqrt((a.x-b.x)*(a.x-b.x)+(a.y-b.y)*(a.y-b.y));
		
		return d;
	}

	//------------------------------------
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
	//---------------------------------
	
	public static Mat findArucoAndCut(Mat img , Mat mtx , MatOfDouble dist , int w , int h , double cx , double cy) {


		
        Dictionary arucoDict = Aruco.getPredefinedDictionary(5);
        DetectorParameters parameters = DetectorParameters.create();
        List<Mat> corners = new ArrayList<Mat>();
        MatOfDouble rvec = new MatOfDouble();
        MatOfDouble tvec = new MatOfDouble();
        Mat ids = new Mat();
        
        //ArucoDetector detector= new ArucoDetector(dictionary,parameters);
        Aruco.detectMarkers(img, arucoDict, corners, ids);


        Aruco.estimatePoseSingleMarkers(corners, 0.05, mtx, dist, rvec, tvec); //unit=cm

		Point2D.Double vert;
		Point2D.Double hoz;
		//我不知道coreners的資料型態= =
		hoz.x=(corners.get(0)).get(0,0)[0]-(corners.get(0)).get(1,0)[0];
		hoz.y=(corners.get(0)).get(0,1)[0]-(corners.get(0)).get(1,1)[0];
		
		vert.x=(corners.get(0)).get(0,0)[0]-(corners.get(0)).get(3,0)[0];
		vert.y=(corners.get(0)).get(0,1)[0]-(corners.get(0)).get(3,1)[0];
		
		Point2D.Double lt,rt,lb,rb,rt_dst,rb_dst,lb_dst;
		
		lt.x=(corners.get(0)).get(0,0)[0]+(20.75/5)*hoz.x+(1.25/5)*vert.x;
		lt.y=(corners.get(0)).get(0,1)[0]+(20.75/5)*hoz.y+(1.25/5)*vert.y;
		
		rt.x=(corners.get(0)).get(0,0)[0]+(0.75/5)*hoz.x+(1.25/5)*vert.x;
		rt.x=(corners.get(0)).get(0,1)[0]+(0.75/5)*hoz.y+(1.25/5)*vert.y;
		
		lb.x=(corners.get(0)).get(0,0)[0]+(20.75/5)*hoz.x+(-13.75/5)*vert.x;
		lb.y=(corners.get(0)).get(0,1)[0]+(20.75/5)*hoz.y+(-13.75/5)*vert.y;
		
		rb.x=(corners.get(0)).get(0,0)[0]+(0.75/5)*hoz.x+(-13.75/5)*vert.x;
		rb.y=(corners.get(0)).get(0,1)[0]+(0.75/5)*hoz.y+(-13.75/5)*vert.y;
		

		rt_dst.x=lt.x+cal_dist(lt,rt);
		rt_dst.y=lt.y;
		
		rb_dst.x=rt_dst.x;
		rb_dst.y=rt_dst.y+cal_dist(rb,rt);
		
		lb_dst.x=lt.x;
		lb_dst.y=lt.y+cal_dist(lt,lb);
		
		Mat src_point= new Mat(4,2,CvType.CV_64FC1);
		Mat dst_point= new Mat(4,2,CvType.CV_64FC1);
		double[] src= {lt.x,lt.y,rt.x,rt.y,lb.x,lb.y,rb.x,rb.y};
		double[] dst= {0,0,rt_dst.x-lt.x,rt_dst.y-lt.y,lb_dst.x-lt.x,lb_dst.y-lt.y,rb_dst.x-lt.x,rb_dst.y-lt.y};
		src_point.put(0,0,src);
		dst_point.put(0,0,dst);
		Mat M=Imgproc.getPerspectiveTransform(src_point,dst_point);
		Mat perspective;
		
		Size dsize;
		dsize.width=cal_dist(lt,rt);
		dsize.height=cal_dist(rt,rb);
		Imgproc.warpPerspective(img,perspective,M,dsize);
		
		cx=(lt.x+rt.x+lb.x+rb.x)/4;
		cy=(lt.y+rt.y+lb.y+rb.y)/4;
		
		return perspective;
    }
	
	//------------------------


}
