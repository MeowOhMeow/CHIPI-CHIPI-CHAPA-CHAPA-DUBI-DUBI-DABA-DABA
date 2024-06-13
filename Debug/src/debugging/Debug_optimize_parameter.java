package Debug_new; 
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;


import org.opencv.core.*;
import org.opencv.core.Mat;
import org.opencv.dnn.Dnn;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
//import java.util.HashMap;



public class Debug_optimize_parameter { 
	static {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
	}

	public static OrtEnvironment env;
	public static OrtSession session;
	public static long count;
	public static long netHeight;
	public static long netWidth;
//	public static float confThreshold = 0.25f;
//	public static float nmsThreshold = 0.5f;
	static Mat src;

	public static OnnxTensor transferTensor(Mat dst) {
		Imgproc.cvtColor(dst, dst, Imgproc.COLOR_BGR2RGB);
		dst.convertTo(dst, CvType.CV_32FC1, 1. / 255);
		int channels = dst.channels();

		count = 1;
		netWidth = 640;
		netHeight = 640;
		
		float[] whc = new float[Long.valueOf(channels).intValue() * Long.valueOf(netWidth).intValue() * Long.valueOf(netHeight).intValue()];
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

	public static void main(String[] args) {  
		OrtEnvironment env = OrtEnvironment.getEnvironment();
		OrtSession session = null;
		try {
			session = env.createSession("./src/main/java/Debug_new/best_0611.onnx"); 
		}catch (OrtException e) {
        	e.printStackTrace();
        }
		Mat img = Imgcodecs.imread("source folder/images/5.jpg"); 
		if (img.empty()) {
            System.out.println("Error: Image not loaded correctly. Check the image path.");
		}	
		
		//锐化图像
//        Mat gray = new Mat();
//        Imgproc.cvtColor(img, gray, Imgproc.COLOR_BGR2GRAY);
//        Mat sharp = new Mat();
//        Imgproc.GaussianBlur(gray, sharp, new Size(0, 0), 3);
//        //Imgproc.GaussianBlur(gray, sharp, new Size(0, 0), 1);
//        Core.addWeighted(gray, 1.5, sharp, -0.5, 0, sharp);
//        //Core.addWeighted(gray, 2.0, sharp, -1.0, 0, sharp);
//        Imgproc.cvtColor(sharp, sharp, Imgproc.COLOR_GRAY2BGR);
//        
//        //保存锐化后的图像
//        String outputPath = "source folder/2024-06-07-1/Area3_result_sharpened.jpg";
//        Imgcodecs.imwrite(outputPath, sharp);
//        System.out.println("Sharpened image saved to " + outputPath);
//		if (img.empty()) {
//	            System.out.println("Error: Image not loaded correctly. Check the image path.");
//	    }			
//		Mat image = sharp.clone();
		Mat image = img.clone();
		
		int[] original_shape = { img.rows(), img.cols() };
		
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
		
		Mat resized = new Mat();
		Imgproc.resize(image, resized, new org.opencv.core.Size(new_width, new_height));
		
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
		
		OnnxTensor tensor = transferTensor(blank_image);
		OrtSession.Result modelOutput = null; // "images"是輸入名稱
        try {
            modelOutput = session.run(Collections.singletonMap("images", tensor));
        } catch (OrtException e) {
        	e.printStackTrace();
        }
		OnnxTensor detectionResult = (OnnxTensor)modelOutput.get(0);
		float[][][] resultInFloat = null;
        try {
            resultInFloat = (float[][][]) detectionResult.getValue();
        } catch (OrtException e) {
        	e.printStackTrace();
        }
        // select batch 0
        float[][] output0 = resultInFloat[0];
		
		List<Integer> classIds = new ArrayList<>();
		List<Float> scores = new ArrayList<>();
		List<Rect2d> boxes_list = new ArrayList<>();
		
		//output，transpose
		float[][] output_trans = new float[output0[0].length][output0.length];
		
		for (int i = 0; i < output0.length; i++) {
		    for (int j = 0; j < output0[0].length; j++) {
		        output_trans[j][i] = output0[i][j];
		    }
		}
//		System.out.println("output_shape: [" + output_trans.length + ", " + output_trans[0].length + "]");
		
		//這邊開始跑迴圈測試--------------------------------------------------------------------------------------
		int target_item_idx = 8;
		int target_count = 1;
		List<Pair<Float, Float>> passed_parameters = new ArrayList<>();
		
		for(double m = 0.1; m < 0.9; m=m+0.01) {
			for(double n = 0.1; n < 0.9; n=n+0.01) {
				float confThreshold = (float) m;
				float nmsThreshold = (float)n;
				
				//original program---------------------------------------------------------------------------
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
				    
//				    //test
//				    if(i == 6862) {
//				    	System.out.println("Pre_Confidence for index " + i + ": " + max_score);
//				    }
//					
					if (max_score >= confThreshold) { //原max_score=0.5
						int class_id = 0;
						float max_class_score = 0;
						for (int j = 4; j < output_trans[0].length; j++) {
							if (output_trans[i][j] > max_class_score) {
								max_class_score = output_trans[i][j];
								class_id = j - 4;
							}
						}
//						//test
//						System.out.println("Pre_Confidence for index " + i + ": " + max_score);
						
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
					}
				}
				
				// Non-maximum suppression
				MatOfInt candidates = new MatOfInt();
				MatOfRect2d boxes = new MatOfRect2d(boxes_list.toArray(new Rect2d[0]));
				float[] confArr = new float[scores.size()];
				for(int i=0;i<scores.size();i++){
				    confArr[i]=scores.get(i);
				}
				MatOfFloat con =new MatOfFloat(confArr);
				Dnn.NMSBoxes(boxes, con, confThreshold, nmsThreshold, candidates); // condidence高的物品選出來，再用IOU(intersection
				if(candidates.empty()){
				    System.out.println("indexs is empty");
				    continue;
				}
				
				int[] indices = candidates.toArray();
				float[] confidences = con.toArray();
				
				float[] itemTotalConfs = new float[10];
				int[] count = new int[10];
				for (int idx : indices) {
				    int classId = classIds.get(idx);
				    itemTotalConfs[classId] += confidences[idx];
				    count[classId]++;
				}
				float maxConf = itemTotalConfs[0];
				int returnIdx = 0;
				for (int idx = 0; idx < 10; idx++) {
				    if (itemTotalConfs[idx] > maxConf) {
				        returnIdx = idx;
				        maxConf = itemTotalConfs[idx];
				    }
				}
//				//test
//				for (int idx : indices) {
//		            System.out.println("Confidence for index " + idx + ": " + confidences[idx]);
//		        }
//				//test
//				System.out.println("\nCounts:");
//		        for (int i = 0; i < count.length; i++) {
//		            System.out.println("Class " + i + ": " + count[i]);
//		        }
//		        if(count[9] == 2)
//		        	break;
				//original program---------------------------------------------------------------------------
				
				//如果是對的答案，就把這組(confThreshold, nmsThreshold)變成pair，存入list後印出來
				if((returnIdx == target_item_idx) && (count[returnIdx] == target_count)) { 
					Pair<Float, Float> pair = new Pair<>(confThreshold, nmsThreshold);
					passed_parameters.add(pair);
				}
			}
		}
		
		//印出來
//		System.out.print("Passed Parameters Results:");
//		System.out.println();
		for (int i = 0; i < passed_parameters.size(); i++) {
			System.out.print("(" + passed_parameters.get(i).getKey() + ", " + passed_parameters.get(i).getValue() + "), ");
		}
	}
}
