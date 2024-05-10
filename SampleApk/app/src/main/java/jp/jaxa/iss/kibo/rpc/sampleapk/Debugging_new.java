package jp.jaxa.iss.kibo.rpc.sampleapk;
import android.util.Log;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;


import org.opencv.core.*;
import org.opencv.core.Mat;
import org.opencv.dnn.Dnn;
//import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;


import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
//import java.util.HashMap;



public class Debugging_new {
//	static {
//		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
//	}

	public static OrtEnvironment env;
	public static OrtSession session;
	public static long count;
	public static long channels;
	public static long netHeight;
	public static long netWidth;
	public static  float srcw;
	public static  float srch;
	public static float confThreshold = 0.25f;
	public static float nmsThreshold = 0.5f;
	static Mat src;

	public static OnnxTensor transferTensor(Mat dst) {
		Imgproc.cvtColor(dst, dst, Imgproc.COLOR_BGR2RGB);
		dst.convertTo(dst, CvType.CV_32FC1, 1. / 255);
		int rows = dst.rows();
		int cols = dst.cols();
		int channels = dst.channels();
		//System.out.println("Shape: [" + rows + ", " + cols + ", " + channels + "]");

		count = 1;
		netWidth = 640;
		netHeight = 640;
		
		float[] whc = new float[Long.valueOf(channels).intValue() * Long.valueOf(netWidth).intValue() * Long.valueOf(netHeight).intValue()];
		//System.out.println("Shape_whc: " + whc.length);
		dst.get(0, 0, whc);
		float[] chw = whc2cwh(whc);
		OnnxTensor tensor = null;
		
		env = OrtEnvironment.getEnvironment();
		try {
			tensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(chw), new long[] {count, channels, netWidth, netHeight});
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
		return tensor;
	}

	//宽 高 类型 to 类 宽 高
	public static float[] whc2cwh(float[] src) {
		float[] chw = new float[src.length];
		int j = 0;
		for (int ch = 0; ch < 3; ++ch) {
			for (int i = ch; i < src.length; i += 3) {
				chw[j] = src[i];
				j++;
			}
		}
		return chw;
	}



	public  int[] image(  Mat img)
	{    int[] count = new int[10];
		OrtEnvironment env = OrtEnvironment.getEnvironment();
		//OrtSession session = null;
		try {
			getClass().getResourceAsStream("best.onnx");
			//session = env.createSession("C:/SampleApk/app/src/main/java/jp/jaxa/iss/kibo/rpc/sampleapk/best.onnx");

			if(session==null){
				Log.i("CHIPI-CHIPI", "session is null " );
				return  count;
			}


//			Mat img = Imgcodecs.imread("source folder/45fb2e43-0564-41c0-8fb9-51f92c226a2e.png"); //kapton_tape: 5
//			Mat img = Imgcodecs.imread("source folder/902d1d66-65d9-4069-a2ec-bf8443a07fd0.png"); //pipette: 1
//			Mat img = Imgcodecs.imread("source folder/63651b40-583c-4a42-a474-8bbc51e523f1.png"); //beaker: 1，top: 1
			//Mat img = Imgcodecs.imread("source folder/ac6f6197-1a3a-43f2-827d-0d1000dcc292.png"); //screwdriver: 1
			
			Mat image = img.clone();

			int[] original_shape = { img.rows(), img.cols() };
			//System.out.println("original_shape: [" + original_shape[0] + ", " + original_shape[1] + "]");

			int _max = Math.max(img.rows(), img.cols());
			int new_height, new_width;
			double ratio;
			if (_max == img.rows()) {
				new_height = 640;
				ratio = (double)new_height / img.rows();
				new_width = (int)(img.cols() * ratio);
			}
			else {
				new_width = 640;
				ratio = (double)new_width / img.cols();
				new_height = (int)(img.rows() * ratio);
			}
			//System.out.println("Dimensions of the new image will be: \n height: " + new_height + "\n width: " + new_width);

			Mat resized = new Mat();
			Imgproc.resize(image, resized, new org.opencv.core.Size(new_width, new_height));
			//            Imgproc.imshow("Resized Image", resized);
			//            Imgproc.waitKey(0);

			Mat blank_image = Mat.zeros(640, 640, resized.type());
			int start_x = (640 - resized.cols()) / 2;
			int start_y = (640 - resized.rows()) / 2;
			int end_x = start_x + resized.cols();
			int end_y = start_y + resized.rows();
			resized.copyTo(blank_image.submat(start_y, end_y, start_x, end_x));

			//印blank_size
			int rows = blank_image.rows();
			int cols = blank_image.cols();
			int channels = blank_image.channels();
			//System.out.println("Shape: [" + rows + ", " + cols + ", " + channels + "]");

			OnnxTensor tensor = transferTensor(blank_image);
			//System.out.println("Tensor size info: " + tensor.getInfo());
			OrtSession.Result result = session.run(Collections.singletonMap("images", tensor)); //"images"是輸入名稱
			OnnxTensor res = (OnnxTensor)result.get(0);
			//System.out.println("Tensor size info: " + res.getInfo());
			float[][][] dataRes = (float[][][])res.getValue();
			float[][] output0 = dataRes[0];

			//System.out.println("output_shape: [" + output0.length + ", " + output0[0].length + "]");
			//System.out.println("input_name: " + session.getInputNames().toArray(new String[0])[0]);
			//System.out.println("output_name: " + session.getOutputNames().toArray(new String[0])[0]);

			List<Integer> classIds = new ArrayList<>();
			List<Float> scores = new ArrayList<>();
			List<Rect2d> boxes_list = new ArrayList<>();
			List<double[]> boxes_double = new ArrayList<>();
			List<Integer> index_list = new ArrayList<>();

			//output，transpose
			float[][] output_trans = new float[output0[0].length][output0.length];

			for (int i = 0; i < output0.length; i++) {
			    for (int j = 0; j < output0[0].length; j++) {
			        output_trans[j][i] = output0[i][j];
			    }
			}
			//System.out.println("output_shape: [" + output_trans.length + ", " + output_trans[0].length + "]");
			
			//test
//			for (int i = 0; i < output_trans.length; i++) {
//			    System.out.print("Row " + i + ": ");
//			    for (int j = 0; j < output_trans[0].length; j++) {
//			        System.out.print(output_trans[i][j] + " ");
//			    }
//			    System.out.println();
//			}
			
			for (int i = 0; i < output_trans.length; ++i) {
				//float max_score = output_trans[i][4];
				float[] classScores = new float[output_trans[i].length - 4];
				for (int j = 4; j < output_trans[i].length; j++) {
			        classScores[j - 4] = output_trans[i][j];
			    }
				float max_score = Float.MIN_VALUE;
			    for (float score : classScores) {
			        if (score > max_score) {
			            max_score = score;
			        }
			    }
				
				if (max_score >= 0.5) {
					int class_id = 0;
					float max_class_score = 0;
					for (int j = 4; j < output_trans[0].length; j++) {
						if (output_trans[i][j] > max_class_score) {
							max_class_score = output_trans[i][j];
							class_id = j - 4;
						}
					}
					double x = output_trans[i][0];
					double y = output_trans[i][1];
					double w = output_trans[i][2];
					double h = output_trans[i][3];

					int left = (int)((x - w / 2) * original_shape[1] / 640);
					int top = (int)((y - h / 2) * original_shape[0] / 640);
					int width = (int)(w * original_shape[1] / 640);
					int height = (int)(h * original_shape[0] / 640);

					classIds.add(class_id);
					scores.add(max_score);
					boxes_list.add(new Rect2d (left, top, width, height));
					boxes_double.add(new double[] {left, top, width, height});
					index_list.add(i);
				}
			}
			

			// Non-maximum suppression
			MatOfInt indexs = new MatOfInt();
			MatOfRect2d boxes = new MatOfRect2d(boxes_list.toArray(new Rect2d[0]));
			float[] confArr = new float[scores.size()];
			for(int i=0;i<scores.size();i++){
			    confArr[i]=scores.get(i);
			}
			MatOfFloat con =new MatOfFloat(confArr);
			Dnn.NMSBoxes(boxes,con,0.5F,0.5F,indexs);
			if(indexs.empty()){
			    System.out.println("indexs is empty");
			    return count;
			}
			// Use your own implementation or a library like OpenCV for NMS

			// Draw detections
//			for (int i = 0; i < classIds.size(); i++) {
//				System.out.println("box: " + boxes_double.get(i)[0] + ", " + boxes.get(i)[1] + ", " + boxes.get(i)[2] + ", " + boxes.get(i)[3]);
//				System.out.println("score: " + scores.get(i));
//				System.out.println("class_id: " + classIds.get(i));
//			}
			
			int[] ints = indexs.toArray();
			//int[] count = new int[10];
			for (int i : ints) {
				//System.out.println("index: " + index_list.get(i));
				//System.out.println("box: " + boxes_double.get(i)[0] + ", " + boxes_double.get(i)[1] + ", " + boxes_double.get(i)[2] + ", " + boxes_double.get(i)[3]);
				//System.out.println("score: " + scores.get(i));
				//System.out.println("class_id: " + classIds.get(i));
				count[classIds.get(i)]++;
			}
			//System.out.println('\n');
			
			for(int i = 0; i < count.length; ++i) {
				 String itemName = "";
				 if(count[i]!=0) {
				    switch (i) {
				        case 0:
				            itemName = "beaker";
				            break;
				        case 1:
				            itemName = "goggle";
				            break;
				        case 2:
				            itemName = "hammer";
				            break;
				        case 3:
				            itemName = "kapton_tape";
				            break;
				        case 4:
				            itemName = "pipette";
				            break;
				        case 5:
				            itemName = "screwdriver";
				            break;
				        case 6:
				            itemName = "thermometer";
				            break;
				        case 7:
				            itemName = "top";
				            break;
				        case 8:
				            itemName = "watch";
				            break;
				        case 9:
				            itemName = "wrench";
				            break;
				        default:
				            itemName = "Unknown";
				            break;
				    }
				   // System.out.println(itemName + ": " + count[i]);
				 }	
			}

			
	
		}
		catch (OrtException e) {
			e.printStackTrace();
		}
		finally{
			try {
				 if (session != null) session.close();
			}
			catch (OrtException e) {
			 e.printStackTrace();
			}
			env.close();
		}

		return count;
	}
}
