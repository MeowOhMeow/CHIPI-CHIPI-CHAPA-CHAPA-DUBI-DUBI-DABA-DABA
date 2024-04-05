package debugging;

import org.opencv.core.Core;
import org.opencv.dnn.*;

public class Debugging {

	public static void main(String[] args) {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		Net dnnNet = Dnn.readNet("./src/best.onnx");
		System.out.println("Successly loaded!");

		/* TODO:
		 * 1. Load the image
		 * 2. Preprocess the image
		 * 3. Set the input
		 * 4. Forward the image
		 * 5. Get the output
		 * 6. Postprocess the output
		 * 
		 * Note: please follow the java naming convention, 
		 * and you can make private methods or class to make the code more readable.
		*/
	}

}
