package debugging;

import org.opencv.core.Core;
import org.opencv.dnn.*;

public class Debugging {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		Net dnnNet = Dnn.readNet("./src/best.onnx");
		System.out.println("Successly loaded!");
	}

}
