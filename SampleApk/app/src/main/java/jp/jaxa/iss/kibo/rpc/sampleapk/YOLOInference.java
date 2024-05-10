package jp.jaxa.iss.kibo.rpc.sampleapk;

import android.util.Log;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;

import org.opencv.core.*;
import org.opencv.core.Mat;
import org.opencv.dnn.Dnn;
import org.opencv.imgproc.Imgproc;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class YOLOInference {
	public static OrtEnvironment env;
	public static OrtSession session;
	public static long count;
	public static long channels;
	public static long netHeight;
	public static long netWidth;
	public static float srcw;
	public static float srch;
	public static float confThreshold = 0.25f;
	public static float nmsThreshold = 0.5f;
	static Mat src;

	static {
		OrtEnvironment env = OrtEnvironment.getEnvironment();
		OrtSession session = null;
		// TODO: load model here
	}

	public static OnnxTensor transfer2Tensor(Mat dst) {
		Imgproc.cvtColor(dst, dst, Imgproc.COLOR_BGR2RGB);
		dst.convertTo(dst, CvType.CV_32FC1, 1. / 255);
		int rows = dst.rows();
		int cols = dst.cols();
		int channels = dst.channels();

		count = 1;
		netWidth = 640;
		netHeight = 640;

		float[] whc = new float[Long.valueOf(channels).intValue() * Long.valueOf(netWidth).intValue()
				* Long.valueOf(netHeight).intValue()];
		// System.out.println("Shape_whc: " + whc.length);
		dst.get(0, 0, whc);
		float[] chw = whc2cwh(whc);
		OnnxTensor tensor = null;

		env = OrtEnvironment.getEnvironment();
		try {
			tensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(chw),
					new long[] { count, channels, netWidth, netHeight });
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
		return tensor;
	}

	// 宽 高 类型 to 类 宽 高
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

	public static int[] getPredictions(Mat img) {
		int[] count = new int[10];

		if (session == null) {
			Log.i("CHIPI-CHIPI", "session is null");
			return count;
		}

		Mat image = img.clone();

		int[] original_shape = { img.rows(), img.cols() };

		int _max = Math.max(img.rows(), img.cols());
		int new_height, new_width;
		double ratio;
		if (_max == img.rows()) {
			new_height = 640;
			ratio = (double) new_height / img.rows();
			new_width = (int) (img.cols() * ratio);
		} else {
			new_width = 640;
			ratio = (double) new_width / img.cols();
			new_height = (int) (img.rows() * ratio);
		}

		Mat resized = new Mat();
		Imgproc.resize(image, resized, new org.opencv.core.Size(new_width, new_height));

		Mat blank_image = Mat.zeros(640, 640, resized.type());
		int start_x = (640 - resized.cols()) / 2;
		int start_y = (640 - resized.rows()) / 2;
		int end_x = start_x + resized.cols();
		int end_y = start_y + resized.rows();
		resized.copyTo(blank_image.submat(start_y, end_y, start_x, end_x));

		int rows = blank_image.rows();
		int cols = blank_image.cols();
		int channels = blank_image.channels();

		OnnxTensor tensor = transfer2Tensor(blank_image);
		OrtSession.Result result = null; // "images"是輸入名稱
		try {
			result = session.run(Collections.singletonMap("images", tensor));
		} catch (OrtException e) {
			// TODO: log and handle error
			e.printStackTrace();
		}
		OnnxTensor res = (OnnxTensor) result.get(0);
		float[][][] dataRes = new float[0][][];
		try {
			dataRes = (float[][][]) res.getValue();
		} catch (OrtException e) {
			// TODO: log and handle error
			e.printStackTrace();
		}
		float[][] output0 = dataRes[0];

		List<Integer> classIds = new ArrayList<>();
		List<Float> scores = new ArrayList<>();
		List<Rect2d> boxes_list = new ArrayList<>();
		List<double[]> boxes_double = new ArrayList<>();
		List<Integer> index_list = new ArrayList<>();

		// output，transpose
		float[][] output_trans = new float[output0[0].length][output0.length];

		for (int i = 0; i < output0.length; i++) {
			for (int j = 0; j < output0[0].length; j++) {
				output_trans[j][i] = output0[i][j];
			}
		}

		for (int i = 0; i < output_trans.length; ++i) {
			// float max_score = output_trans[i][4];
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

				int left = (int) ((x - w / 2) * original_shape[1] / 640);
				int top = (int) ((y - h / 2) * original_shape[0] / 640);
				int width = (int) (w * original_shape[1] / 640);
				int height = (int) (h * original_shape[0] / 640);

				classIds.add(class_id);
				scores.add(max_score);
				boxes_list.add(new Rect2d(left, top, width, height));
				boxes_double.add(new double[] { left, top, width, height });
				index_list.add(i);
			}
		}

		// Non-maximum suppression
		MatOfInt indexs = new MatOfInt();
		MatOfRect2d boxes = new MatOfRect2d(boxes_list.toArray(new Rect2d[0]));
		float[] confArr = new float[scores.size()];
		for (int i = 0; i < scores.size(); i++) {
			confArr[i] = scores.get(i);
		}

		MatOfFloat con = new MatOfFloat(confArr);
		Dnn.NMSBoxes(boxes, con, 0.5F, 0.5F, indexs);
		if (indexs.empty()) {
			Log.i("CHIPI-CHIPI", "indexs is empty");
			return count;
		}

		int[] ints = indexs.toArray();
		for (int i : ints) {
			count[classIds.get(i)]++;
		}

		return count;
	}
}
