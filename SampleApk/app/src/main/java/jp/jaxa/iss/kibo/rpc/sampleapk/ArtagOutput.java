package jp.jaxa.iss.kibo.rpc.sampleapk;

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

public class ArtagOutput {

	
	public Mat undistort_img;
	public Mat yolo_img;
	public double[] snap_point;
	
	Artag_output(Mat img1,Mat img2,double[] point){
		this.undistort_img=img1;
		this.yolo_img=img2;
		this.snap_point=point;
	}
	

}
