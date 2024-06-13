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
import java.io.FileWriter;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
//import java.util.HashMap;



public class Fine_optimize_parameter { 
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
			session = env.createSession("./src/main/java/Debug_new/best_0612.onnx"); 
		}catch (OrtException e) {
        	e.printStackTrace();
        }
		String filePath = "Pair_result_0612_245.txt";
		FileWriter writer = null;
		try {
			writer = new FileWriter(filePath, true);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		List<String> imgList = new ArrayList<>();
		imgList.add("source folder/images/0.jpg"); 
		imgList.add("source folder/images/1.jpg"); 
		imgList.add("source folder/images/2.jpg"); 
		imgList.add("source folder/images/3.jpg"); 
		imgList.add("source folder/images/4.jpg"); 
		imgList.add("source folder/images/5.jpg"); 
		imgList.add("source folder/images/6.jpg"); //重疊
		imgList.add("source folder/images/7.jpg"); 
		imgList.add("source folder/images/8.jpg"); 
		imgList.add("source folder/images/9.jpg"); 
		imgList.add("source folder/images/10.jpg"); 
		imgList.add("source folder/images/11.jpg"); 
		imgList.add("source folder/images/12.jpg"); 
		imgList.add("source folder/images/13.jpg"); 
		imgList.add("source folder/images/14.jpg"); 
		imgList.add("source folder/images/15.jpg"); 
		imgList.add("source folder/images/16.jpg"); 
		imgList.add("source folder/images/17.jpg"); 
		imgList.add("source folder/images/18.jpg"); 
		imgList.add("source folder/images/19.jpg"); 
		imgList.add("source folder/images/20.jpg"); 
		imgList.add("source folder/images/21.jpg"); 
		imgList.add("source folder/images/22.jpg"); 
		imgList.add("source folder/images/23.jpg"); 
		imgList.add("source folder/images/24.jpg"); 
		imgList.add("source folder/images/25.jpg"); 
		imgList.add("source folder/images/26.jpg"); 
		imgList.add("source folder/images/27.jpg"); 
		imgList.add("source folder/images/28.jpg"); 
		imgList.add("source folder/images/29.jpg"); 
		imgList.add("source folder/images/30.jpg"); 
		imgList.add("source folder/images/31.jpg"); 
		imgList.add("source folder/images/32.jpg"); 
		imgList.add("source folder/images/33.jpg"); 
		imgList.add("source folder/images/34.jpg"); 
		imgList.add("source folder/images/35.jpg"); 
		imgList.add("source folder/images/36.jpg"); 
		imgList.add("source folder/images/37.jpg"); 
		imgList.add("source folder/images/38.jpg"); 
		imgList.add("source folder/images/39.jpg"); 
		imgList.add("source folder/images/40.jpg"); 
		imgList.add("source folder/images/41.jpg"); 
		imgList.add("source folder/images/42.jpg"); 
		imgList.add("source folder/images/43.jpg"); 
		imgList.add("source folder/images/44.jpg"); 
		imgList.add("source folder/images/45.jpg"); 
		imgList.add("source folder/images/46.jpg"); 
		imgList.add("source folder/images/47.jpg"); 
		imgList.add("source folder/images/48.jpg"); 
		imgList.add("source folder/images/49.jpg"); 
		imgList.add("source folder/images/50.jpg"); 
		imgList.add("source folder/images/51.jpg"); 
		imgList.add("source folder/images/52.jpg"); //重疊
		imgList.add("source folder/images/53.jpg"); 
		imgList.add("source folder/images/54.jpg"); 
		imgList.add("source folder/images/55.jpg"); 
		imgList.add("source folder/images/56.jpg"); 
		imgList.add("source folder/images/57.jpg"); 
		imgList.add("source folder/images/58.jpg"); 
		imgList.add("source folder/images/59.jpg"); 
		imgList.add("source folder/images/60.jpg"); 
		imgList.add("source folder/images/61.jpg"); 
		imgList.add("source folder/images/62.jpg"); 
		imgList.add("source folder/images/63.jpg"); 
		imgList.add("source folder/images/64.jpg"); //切到背景
		imgList.add("source folder/images/65.jpg"); 
		imgList.add("source folder/images/66.jpg"); //重疊
		imgList.add("source folder/images/67.jpg"); 
		imgList.add("source folder/images/68.jpg"); 
		imgList.add("source folder/images/69.jpg"); //重疊
		imgList.add("source folder/images/70.jpg"); 
		imgList.add("source folder/images/71.jpg"); 
		imgList.add("source folder/images/72.jpg"); //重疊
		imgList.add("source folder/images/73.jpg"); 
		imgList.add("source folder/images/74.jpg"); 
		imgList.add("source folder/images/75.jpg"); 
		imgList.add("source folder/images/76.jpg"); 
		imgList.add("source folder/images/77.jpg"); 
		imgList.add("source folder/images/78.jpg"); 
		imgList.add("source folder/images/79.jpg"); 
		imgList.add("source folder/images/80.jpg"); //重疊
		imgList.add("source folder/images/81.jpg"); 
		imgList.add("source folder/images/82.jpg"); 
		imgList.add("source folder/images/83.jpg"); //重疊
		imgList.add("source folder/images/84.jpg"); 
		imgList.add("source folder/images/85.jpg"); //重疊
		imgList.add("source folder/images/86.jpg"); 
		imgList.add("source folder/images/87.jpg"); 
		imgList.add("source folder/images/88.jpg"); 
		imgList.add("source folder/images/89.jpg"); 
		imgList.add("source folder/images/90.jpg"); 
		imgList.add("source folder/images/91.jpg"); 
		imgList.add("source folder/images/92.jpg"); 
		imgList.add("source folder/images/93.jpg"); 
		imgList.add("source folder/images/94.jpg"); 
		imgList.add("source folder/images/95.jpg"); 
		imgList.add("source folder/images/96.jpg"); 
		imgList.add("source folder/images/97.jpg"); 
		imgList.add("source folder/images/98.jpg"); 
		imgList.add("source folder/images/99.jpg"); 
		imgList.add("source folder/images/100.jpg"); 
		imgList.add("source folder/images/101.jpg"); 
		imgList.add("source folder/images/102.jpg"); 
		imgList.add("source folder/images/103.jpg"); 
		imgList.add("source folder/images/104.jpg"); 
		imgList.add("source folder/images/105.jpg"); 
		
		imgList.add("source folder/images/106.jpg"); 
		imgList.add("source folder/images/107.jpg"); 
		imgList.add("source folder/images/108.jpg"); 
		imgList.add("source folder/images/109.jpg"); 
		imgList.add("source folder/images/110.jpg"); 
		imgList.add("source folder/images/111.jpg"); 
		imgList.add("source folder/images/112.jpg"); 
		imgList.add("source folder/images/113.jpg"); 
		imgList.add("source folder/images/114.jpg"); 
		imgList.add("source folder/images/115.jpg"); 
		imgList.add("source folder/images/116.jpg"); 
		imgList.add("source folder/images/117.jpg"); 
		imgList.add("source folder/images/118.jpg"); 
		imgList.add("source folder/images/119.jpg"); 
		imgList.add("source folder/images/120.jpg"); 
		imgList.add("source folder/images/121.jpg"); 
		imgList.add("source folder/images/122.jpg"); 
		imgList.add("source folder/images/123.jpg"); 
		imgList.add("source folder/images/124.jpg"); 
		imgList.add("source folder/images/125.jpg"); 
		imgList.add("source folder/images/126.jpg"); 
		imgList.add("source folder/images/127.jpg"); 
		imgList.add("source folder/images/128.jpg"); 
		imgList.add("source folder/images/129.jpg"); 
		imgList.add("source folder/images/130.jpg"); 
		imgList.add("source folder/images/131.jpg"); 
		imgList.add("source folder/images/132.jpg"); 
		imgList.add("source folder/images/133.jpg"); 
		imgList.add("source folder/images/134.jpg"); 
		imgList.add("source folder/images/135.jpg"); 
		imgList.add("source folder/images/136.jpg"); 
		imgList.add("source folder/images/137.jpg"); 
		imgList.add("source folder/images/138.jpg"); 
		imgList.add("source folder/images/139.jpg"); 
		imgList.add("source folder/images/140.jpg"); 
		imgList.add("source folder/images/141.jpg"); 
		imgList.add("source folder/images/142.jpg"); 
		imgList.add("source folder/images/143.jpg"); 
		imgList.add("source folder/images/144.jpg"); 
		imgList.add("source folder/images/145.jpg"); 
		imgList.add("source folder/images/146.jpg"); 
		imgList.add("source folder/images/147.jpg"); 
		imgList.add("source folder/images/148.jpg"); 
		imgList.add("source folder/images/149.jpg"); 
		imgList.add("source folder/images/150.jpg"); 
		imgList.add("source folder/images/151.jpg"); 
		imgList.add("source folder/images/152.jpg");
		imgList.add("source folder/images/153.jpg"); 
		imgList.add("source folder/images/154.jpg"); 
		imgList.add("source folder/images/155.jpg"); 
		imgList.add("source folder/images/156.jpg"); 
		imgList.add("source folder/images/157.jpg"); 
		imgList.add("source folder/images/158.jpg"); 
		imgList.add("source folder/images/159.jpg"); 
		imgList.add("source folder/images/160.jpg"); 
		imgList.add("source folder/images/161.jpg"); 
		imgList.add("source folder/images/162.jpg"); 
		imgList.add("source folder/images/163.jpg"); 
		imgList.add("source folder/images/164.jpg");
		imgList.add("source folder/images/165.jpg"); 
		imgList.add("source folder/images/166.jpg");
		imgList.add("source folder/images/167.jpg"); 
		imgList.add("source folder/images/168.jpg"); 
		imgList.add("source folder/images/169.jpg");
		imgList.add("source folder/images/170.jpg"); 
		imgList.add("source folder/images/171.jpg"); 
		imgList.add("source folder/images/172.jpg");
		imgList.add("source folder/images/173.jpg"); 
		imgList.add("source folder/images/174.jpg"); 
		imgList.add("source folder/images/175.jpg"); 
		imgList.add("source folder/images/176.jpg"); 
		imgList.add("source folder/images/177.jpg"); 
		imgList.add("source folder/images/178.jpg"); 
		imgList.add("source folder/images/179.jpg"); 
		imgList.add("source folder/images/180.jpg");
		imgList.add("source folder/images/181.jpg"); 
		imgList.add("source folder/images/182.jpg"); 
		imgList.add("source folder/images/183.jpg");
		imgList.add("source folder/images/184.jpg"); 
		imgList.add("source folder/images/185.jpg");
		imgList.add("source folder/images/186.jpg"); 
		imgList.add("source folder/images/187.jpg"); 
		imgList.add("source folder/images/188.jpg"); 
		imgList.add("source folder/images/189.jpg"); 
		imgList.add("source folder/images/190.jpg"); 
		imgList.add("source folder/images/191.jpg"); 
		imgList.add("source folder/images/192.jpg"); 
		imgList.add("source folder/images/193.jpg"); 
		imgList.add("source folder/images/194.jpg"); 
		imgList.add("source folder/images/195.jpg"); 
		imgList.add("source folder/images/196.jpg"); 
		imgList.add("source folder/images/197.jpg"); 
		imgList.add("source folder/images/198.jpg"); 
		imgList.add("source folder/images/199.jpg"); 
		imgList.add("source folder/images/200.jpg"); 
		imgList.add("source folder/images/201.jpg"); 
		imgList.add("source folder/images/202.jpg"); 
		imgList.add("source folder/images/203.jpg"); 
		imgList.add("source folder/images/204.jpg"); 
		imgList.add("source folder/images/205.jpg"); 
		imgList.add("source folder/images/206.jpg");
		imgList.add("source folder/images/207.jpg"); 
		imgList.add("source folder/images/208.jpg"); 
		imgList.add("source folder/images/209.jpg"); 
		imgList.add("source folder/images/210.jpg"); 
		imgList.add("source folder/images/211.jpg"); 
		imgList.add("source folder/images/212.jpg"); 
		imgList.add("source folder/images/213.jpg"); 
		imgList.add("source folder/images/214.jpg"); 
		imgList.add("source folder/images/215.jpg"); 
		imgList.add("source folder/images/216.jpg"); 
		imgList.add("source folder/images/217.jpg"); 
		imgList.add("source folder/images/218.jpg"); 
		imgList.add("source folder/images/219.jpg"); 
		imgList.add("source folder/images/220.jpg"); 
		imgList.add("source folder/images/221.jpg"); 
		imgList.add("source folder/images/222.jpg"); 
		imgList.add("source folder/images/223.jpg"); 
		imgList.add("source folder/images/224.jpg"); 
		imgList.add("source folder/images/225.jpg"); 
		imgList.add("source folder/images/226.jpg"); 
		imgList.add("source folder/images/227.jpg"); 
		imgList.add("source folder/images/228.jpg"); 
		imgList.add("source folder/images/229.jpg"); 
		imgList.add("source folder/images/230.jpg"); 
		imgList.add("source folder/images/231.jpg"); 
		imgList.add("source folder/images/232.jpg"); 
		imgList.add("source folder/images/233.jpg"); 
		imgList.add("source folder/images/234.jpg"); 
		imgList.add("source folder/images/235.jpg"); 
		imgList.add("source folder/images/236.jpg"); 
		imgList.add("source folder/images/237.jpg"); 
		imgList.add("source folder/images/238.jpg"); 
		imgList.add("source folder/images/239.jpg"); 
		imgList.add("source folder/images/240.jpg"); 
		imgList.add("source folder/images/241.jpg"); 
		imgList.add("source folder/images/242.jpg"); 
		imgList.add("source folder/images/243.jpg"); 
		imgList.add("source folder/images/244.jpg"); 
		imgList.add("source folder/images/245.jpg"); 
		
		List<Pair<Integer, Integer>> info = new ArrayList<>();
		info.add(new Pair<>(1, 1)); //0
		info.add(new Pair<>(7, 1)); //1
		info.add(new Pair<>(9, 1)); //2
		info.add(new Pair<>(5, 4)); //3
		info.add(new Pair<>(0, 1)); //4
		info.add(new Pair<>(8, 1)); //5
		info.add(new Pair<>(2, 3)); //6
		info.add(new Pair<>(9, 1)); //7
		info.add(new Pair<>(6, 1)); //8
		info.add(new Pair<>(3, 3)); //9
		info.add(new Pair<>(8, 1)); //10
		info.add(new Pair<>(7, 3)); //11
		info.add(new Pair<>(3, 1)); //12
		info.add(new Pair<>(4, 3)); //13
		info.add(new Pair<>(5, 4)); //14
		info.add(new Pair<>(7, 1)); //15
		info.add(new Pair<>(1, 2)); //16
		info.add(new Pair<>(6, 1)); //17
		info.add(new Pair<>(8, 4)); //18
		info.add(new Pair<>(2, 2)); //19
		info.add(new Pair<>(5, 3)); //20
		info.add(new Pair<>(7, 1)); //21
		info.add(new Pair<>(9, 1)); //22
		info.add(new Pair<>(9, 3)); //23
		info.add(new Pair<>(2, 5)); //24
		info.add(new Pair<>(1, 1)); //25
		info.add(new Pair<>(1, 3)); //26
		info.add(new Pair<>(5, 1)); //27
		info.add(new Pair<>(4, 1)); //28
		info.add(new Pair<>(3, 5)); //29
		info.add(new Pair<>(7, 1)); //30
		info.add(new Pair<>(8, 2)); //31
		info.add(new Pair<>(0, 1)); //32
		info.add(new Pair<>(3, 2)); //33
		info.add(new Pair<>(6, 1)); //34
		info.add(new Pair<>(9, 1)); //35
		info.add(new Pair<>(8, 3)); //36
		info.add(new Pair<>(6, 2)); //37
		info.add(new Pair<>(3, 1)); //38
		info.add(new Pair<>(7, 2)); //39
		info.add(new Pair<>(4, 2)); //40
		info.add(new Pair<>(2, 2)); //41
		info.add(new Pair<>(1, 1)); //42
		info.add(new Pair<>(1, 4)); //43
		info.add(new Pair<>(2, 1)); //44
		info.add(new Pair<>(7, 1)); //45
		info.add(new Pair<>(8, 1)); //46
		info.add(new Pair<>(7, 3)); //47
		info.add(new Pair<>(5, 1)); //48
		info.add(new Pair<>(6, 1)); //49
		info.add(new Pair<>(4, 3)); //50
		info.add(new Pair<>(0, 2)); //51
		info.add(new Pair<>(8, 3)); //52
		info.add(new Pair<>(1, 1)); //53
		info.add(new Pair<>(0, 1)); //54
		info.add(new Pair<>(3, 2)); //55
		info.add(new Pair<>(1, 2)); //56
		info.add(new Pair<>(2, 1)); //57
		info.add(new Pair<>(6, 3)); //58
		info.add(new Pair<>(4, 1)); //59
		info.add(new Pair<>(3, 4)); //60
		info.add(new Pair<>(6, 3)); //61
		info.add(new Pair<>(3, 3)); //62
		info.add(new Pair<>(1, 4)); //63
		info.add(new Pair<>(2, 2)); //64
		info.add(new Pair<>(3, 2)); //65
		info.add(new Pair<>(4, 4)); //66
		info.add(new Pair<>(4, 1)); //67
		info.add(new Pair<>(8, 3)); //68
		info.add(new Pair<>(7, 3)); //69
		info.add(new Pair<>(8, 3)); //70
		info.add(new Pair<>(7, 4)); //71
		info.add(new Pair<>(9, 3)); //72
		info.add(new Pair<>(4, 2)); //73
		info.add(new Pair<>(0, 2)); //74
		info.add(new Pair<>(2, 1)); //75
		info.add(new Pair<>(9, 1)); //76
		info.add(new Pair<>(2, 3)); //77
		info.add(new Pair<>(0, 1)); //78
		info.add(new Pair<>(0, 4)); //79
		info.add(new Pair<>(9, 3)); //80
		info.add(new Pair<>(7, 1)); //81
		info.add(new Pair<>(1, 4)); //82
		info.add(new Pair<>(0, 2)); //83
		info.add(new Pair<>(9, 1)); //84
		info.add(new Pair<>(9, 3)); //85
		info.add(new Pair<>(6, 1)); //86
		info.add(new Pair<>(8, 3)); //87
		info.add(new Pair<>(0, 1)); //88
		info.add(new Pair<>(0, 3)); //89
		info.add(new Pair<>(3, 1)); //90
		info.add(new Pair<>(3, 1)); //91
		info.add(new Pair<>(3, 1)); //92
		info.add(new Pair<>(1, 1)); //93
		info.add(new Pair<>(4, 1)); //94
		info.add(new Pair<>(5, 3)); //95
		info.add(new Pair<>(8, 2)); //96
		info.add(new Pair<>(4, 1)); //97
		info.add(new Pair<>(4, 1)); //98
		info.add(new Pair<>(5, 1)); //99
		info.add(new Pair<>(5, 1)); //100
		info.add(new Pair<>(5, 1)); //101
		info.add(new Pair<>(9, 3)); //102
		info.add(new Pair<>(6, 2)); //103
		info.add(new Pair<>(0, 4)); //104
		info.add(new Pair<>(8, 3)); //105
		
		info.add(new Pair<>(0, 3)); //106
		info.add(new Pair<>(5, 1)); //107
		info.add(new Pair<>(4, 3)); //108
		info.add(new Pair<>(1, 4)); //109
		info.add(new Pair<>(1, 1)); //110
		info.add(new Pair<>(8, 5)); //111
		info.add(new Pair<>(3, 4)); //112
		info.add(new Pair<>(7, 2)); //113
		info.add(new Pair<>(9, 1)); //114
		info.add(new Pair<>(9, 1)); //115
		info.add(new Pair<>(5, 3)); //116
		info.add(new Pair<>(3, 2)); //117
		info.add(new Pair<>(9, 3)); //118
		info.add(new Pair<>(4, 2)); //119
		info.add(new Pair<>(4, 1)); //120
		info.add(new Pair<>(0, 4)); //121
		info.add(new Pair<>(1, 4)); //122
		info.add(new Pair<>(6, 3)); //123
		info.add(new Pair<>(4, 2)); //124
		info.add(new Pair<>(1, 1)); //125
		info.add(new Pair<>(2, 1)); //126
		info.add(new Pair<>(0, 2)); //127
		info.add(new Pair<>(7, 4)); //128
		info.add(new Pair<>(8, 3)); //129
		info.add(new Pair<>(8, 1)); //130
		info.add(new Pair<>(1, 4)); //131
		info.add(new Pair<>(2, 2)); //132
		info.add(new Pair<>(3, 4)); //133
		info.add(new Pair<>(7, 2)); //134
		info.add(new Pair<>(2, 1)); //135
		info.add(new Pair<>(3, 6)); //136
		info.add(new Pair<>(1, 4)); //137
		info.add(new Pair<>(5, 2)); //138
		info.add(new Pair<>(9, 4)); //139
		info.add(new Pair<>(1, 1)); //140
		info.add(new Pair<>(0, 4)); //141
		info.add(new Pair<>(3, 2)); //142
		info.add(new Pair<>(4, 3)); //143
		info.add(new Pair<>(8, 2)); //144
		info.add(new Pair<>(3, 1)); //145
		info.add(new Pair<>(9, 1)); //146
		info.add(new Pair<>(2, 1)); //147
		info.add(new Pair<>(6, 2)); //148
		info.add(new Pair<>(0, 2)); //149
		info.add(new Pair<>(2, 1)); //150
		info.add(new Pair<>(4, 2)); //151
		info.add(new Pair<>(3, 3)); //152
		info.add(new Pair<>(5, 4)); //153
		info.add(new Pair<>(7, 3)); //154
		info.add(new Pair<>(7, 1)); //155
		info.add(new Pair<>(6, 2)); //156
		info.add(new Pair<>(7, 2)); //157
		info.add(new Pair<>(0, 2)); //158
		info.add(new Pair<>(2, 1)); //159
		info.add(new Pair<>(2, 1)); //160
		info.add(new Pair<>(6, 2)); //161
		info.add(new Pair<>(8, 3)); //162
		info.add(new Pair<>(4, 3)); //163
		info.add(new Pair<>(1, 1)); //164
		info.add(new Pair<>(8, 1)); //165
		info.add(new Pair<>(9, 2)); //166
		info.add(new Pair<>(7, 2)); //167
		info.add(new Pair<>(4, 2)); //168
		info.add(new Pair<>(2, 5)); //169
		info.add(new Pair<>(4, 1)); //170
		info.add(new Pair<>(4, 5)); //171
		info.add(new Pair<>(2, 3)); //172
		info.add(new Pair<>(5, 4)); //173
		info.add(new Pair<>(7, 4)); //174
		info.add(new Pair<>(5, 1)); //175
		info.add(new Pair<>(1, 1)); //176
		info.add(new Pair<>(8, 2)); //177
		info.add(new Pair<>(5, 2)); //178
		info.add(new Pair<>(6, 2)); //179
		info.add(new Pair<>(1, 1)); //180
		info.add(new Pair<>(8, 4)); //181
		info.add(new Pair<>(2, 4)); //182
		info.add(new Pair<>(4, 2)); //183
		info.add(new Pair<>(5, 4)); //184
		info.add(new Pair<>(4, 1)); //185
		info.add(new Pair<>(7, 3)); //186
		info.add(new Pair<>(9, 2)); //187
		info.add(new Pair<>(1, 5)); //188
		info.add(new Pair<>(4, 2)); //189
		info.add(new Pair<>(7, 1)); //190
		info.add(new Pair<>(7, 3)); //191
		info.add(new Pair<>(3, 3)); //192
		info.add(new Pair<>(5, 1)); //193
		info.add(new Pair<>(0, 2)); //194
		info.add(new Pair<>(3, 1)); //195
		info.add(new Pair<>(1, 4)); //196
		info.add(new Pair<>(2, 4)); //197
		info.add(new Pair<>(6, 4)); //198
		info.add(new Pair<>(9, 2)); //199
		info.add(new Pair<>(9, 1)); //200
		info.add(new Pair<>(4, 1)); //201
		info.add(new Pair<>(6, 3)); //202
		info.add(new Pair<>(1, 5)); //203
		info.add(new Pair<>(0, 3)); //204
		info.add(new Pair<>(6, 1)); //205
		info.add(new Pair<>(5, 2)); //206
		info.add(new Pair<>(0, 2)); //207
		info.add(new Pair<>(3, 2)); //208
		info.add(new Pair<>(2, 2)); //209
		info.add(new Pair<>(3, 1)); //210
		info.add(new Pair<>(5, 2)); //211
		info.add(new Pair<>(2, 1)); //212
		info.add(new Pair<>(3, 3)); //213
		info.add(new Pair<>(1, 5)); //214
		info.add(new Pair<>(2, 1)); //215
		info.add(new Pair<>(0, 5)); //216
		info.add(new Pair<>(3, 3)); //217
		info.add(new Pair<>(2, 1)); //218
		info.add(new Pair<>(6, 2)); //219
		info.add(new Pair<>(2, 1)); //220
		info.add(new Pair<>(3, 4)); //221
		info.add(new Pair<>(6, 3)); //222
		info.add(new Pair<>(2, 2)); //223
		info.add(new Pair<>(8, 4)); //224
		info.add(new Pair<>(3, 1)); //225
		info.add(new Pair<>(7, 2)); //226
		info.add(new Pair<>(5, 3)); //227
		info.add(new Pair<>(0, 2)); //228
		info.add(new Pair<>(4, 4)); //229
		info.add(new Pair<>(5, 1)); //230
		info.add(new Pair<>(9, 5)); //231
		info.add(new Pair<>(3, 4)); //232
		info.add(new Pair<>(5, 3)); //233
		info.add(new Pair<>(8, 2)); //234
		info.add(new Pair<>(5, 1)); //235
		info.add(new Pair<>(2, 4)); //236
		info.add(new Pair<>(5, 1)); //237
		info.add(new Pair<>(3, 4)); //238
		info.add(new Pair<>(4, 2)); //239
		info.add(new Pair<>(3, 1)); //240
		info.add(new Pair<>(1, 4)); //241
		info.add(new Pair<>(5, 2)); //242
		info.add(new Pair<>(3, 2)); //243
		info.add(new Pair<>(0, 3)); //244
		info.add(new Pair<>(5, 1)); //245
		
		for (int num = 0; num < imgList.size(); num++) {
            String img_name = imgList.get(num);
            
            Mat img = Imgcodecs.imread(img_name); 
    		if (img.empty()) {
                System.out.println("Error: Image not loaded correctly. Check the image path.");
    		}
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
//    		System.out.println("output_shape: [" + output_trans.length + ", " + output_trans[0].length + "]");
    		
    		//這邊開始跑迴圈測試--------------------------------------------------------------------------------------
    		int target_item_idx = info.get(num).getKey();
    		int target_count = info.get(num).getValue();
    		List<Pair<Float, Float>> passed_parameters = new ArrayList<>();
    		
            try {
                writer.write("self.set" + num + " = set([");
                writer.flush();
                System.err.println("寫入成功1");
            } catch (IOException e) {
                System.err.println("寫入失敗：" + e.getMessage());
                e.printStackTrace();
            } 
    		
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
    					
    					if (max_score >= confThreshold) { //原max_score=0.5
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
    				//original program---------------------------------------------------------------------------
    				
    				//如果是對的答案，就把這組(confThreshold, nmsThreshold)變成pair，存入list後印出來
    				if((returnIdx == target_item_idx) && (count[returnIdx] == target_count)) { 
    					Pair<Float, Float> pair = new Pair<>(confThreshold, nmsThreshold);
    					passed_parameters.add(pair);
    				}
    			}
    		}
    		
    		//印出來
//    		System.out.print("Passed Parameters Results:");
//    		System.out.println();
    		for (int i = 0; i < passed_parameters.size(); i++) {
    			System.out.print("(" + passed_parameters.get(i).getKey() + ", " + passed_parameters.get(i).getValue() + "), ");
    		}
    		
    		try{ 
                for (int i = 0; i < passed_parameters.size(); i++) {
                    writer.write("(" + passed_parameters.get(i).getKey() + ", " + passed_parameters.get(i).getValue() + "), ");
                    System.out.print("(" + passed_parameters.get(i).getKey() + ", " + passed_parameters.get(i).getValue() + "), ");
                    System.out.print("\n");
                }
                writer.write("])");
                writer.write("\n");
                writer.write("\n");
                writer.flush();
                System.err.println("寫入成功2");
            } catch (IOException e) {
            	System.err.println("寫入失敗：" + e.getMessage());
                e.printStackTrace();
            }	
        }
	}
}
