package jp.jaxa.iss.kibo.rpc.sampleapk;

import android.content.res.AssetManager;
import android.content.res.Resources;
import android.util.Log;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;

import org.opencv.core.*;
import org.opencv.core.Mat;
import org.opencv.dnn.Dnn;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Class to handle the inference of YOLO model
 */
public class YOLOInference {
    private static final String TAG = "YOLOInference";

    private static OrtEnvironment env;
    private static OrtSession session;
    private static final float CONF_THRESHOLD = 0.55f;
    private static final float NMS_THRESHOLD = 0.55f;

    /**
     * Initialize the YOLO model
     * 
     * @param resources: resources of the application
     */
    public static void init(Resources resources) {
        // Initialize an ONNX model execution environment and session
        env = OrtEnvironment.getEnvironment();
        session = null;
        // Obtain an asset manager object to read the application's resource file
        AssetManager assetManager = resources.getAssets();
        try {
            // Creates a session options object to configure session-related options
            OrtSession.SessionOptions options = new OrtSession.SessionOptions();
            // Opens the ONNX model from the assets folder and reads it into a byte array.
            InputStream stream = assetManager.open("best.onnx");
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = stream.read(buffer)) != -1) {
                byteStream.write(buffer, 0, bytesRead);
            }
            byteStream.flush();
            byte[] bytes = byteStream.toByteArray();
            // Create an ONNX session
            session = env.createSession(bytes, options);
        } catch (IOException | OrtException e) {
            Log.i(TAG, "Error: fail to create Ortsession with exception: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * Transfer the image to a tensor
     * 
     * @param src: image to be transferred
     * @return tensor of the image
     */
    private static OnnxTensor transfer2Tensor(Mat src) {
        Imgproc.cvtColor(src, src, Imgproc.COLOR_BGR2RGB);
        src.convertTo(src, CvType.CV_32FC1, 1. / 255);
        int channels = src.channels();

        long count = 1;
        long netWidth = 640;
        long netHeight = 640;

        float[] whc = new float[Long.valueOf(channels).intValue() * Long.valueOf(netWidth).intValue()
                * Long.valueOf(netHeight).intValue()];
        src.get(0, 0, whc);
        float[] chw = whc2cwh(whc);
        OnnxTensor tensor = null;

        env = OrtEnvironment.getEnvironment();
        try {
            tensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(chw),
                    new long[] {count, channels, netWidth, netHeight});
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
        return tensor;
    }

    /**
     * Converts an image from WHC (Width, Height, Channels) format to CWH (Channels,
     * Width, Height) format.
     *
     * @param src the source image in WHC format to be converted
     * @return the converted image in CWH format
     */
    private static float[] whc2cwh(float[] src) {
        if (src == null || src.length % 3 != 0) {
            throw new IllegalArgumentException("Source image must not be null and its length must be a multiple of 3.");
        }

        float[] cwh = new float[src.length];
        int index = 0;

        // Iterate over each channel (assuming 3 channels: R, G, B)
        for (int channel = 0; channel < 3; ++channel) {
            // Iterate over each element in the source array for the current channel
            for (int i = channel; i < src.length; i += 3) {
                cwh[index] = src[i];
                index++;
            }
        }

        return cwh;
    }

    /**
     * Get the predictions of the image
     * 
     * @param img: image to be predicted
     * @return the predictions of the image
     */
    public static AreaItem getPredictions(Mat img) {
        if (session == null) {
            Log.i(TAG, "session is null");
            return null;
        }

        Mat image = img.clone();
        int[] originalShape = { img.rows(), img.cols() };
        int maxSide = Math.max(img.rows(), img.cols());
        int newHeight, newWidth;
        double ratio;
        if (maxSide == img.rows()) {
            newHeight = 640;
            ratio = (double) newHeight / img.rows();
            newWidth = (int) (img.cols() * ratio);
        } else {
            newWidth = 640;
            ratio = (double) newWidth / img.cols();
            newHeight = (int) (img.rows() * ratio);
        }

        Mat resizedImage = new Mat();
        Imgproc.resize(image, resizedImage, new Size(newWidth, newHeight));

        Mat backgroundImage = Mat.zeros(640, 640, resizedImage.type());
        int minX = (640 - resizedImage.cols()) / 2;
        int minY = (640 - resizedImage.rows()) / 2;
        int maxX = minX + resizedImage.cols();
        int maxY = minY + resizedImage.rows();
        resizedImage.copyTo(backgroundImage.submat(minY, maxY, minX, maxX));

        OnnxTensor tensor = transfer2Tensor(backgroundImage);
        OrtSession.Result modelOutput;
        try {
            modelOutput = session.run(Collections.singletonMap("images", tensor));
        } catch (OrtException e) {
            Log.i(TAG, "Error: fail to run session with exception: " + e.getMessage());
            return null;
        }
        OnnxTensor detectionResult;
        if (modelOutput != null) {
            detectionResult = (OnnxTensor) modelOutput.get(0);
        } else {
            Log.i(TAG, "Error: result is null");
            return null;
        }
        float[][][] resultInFloat;
        try {
            resultInFloat = (float[][][]) detectionResult.getValue();
        } catch (OrtException e) {
            Log.i(TAG, "Error: fail to get value with exception: " + e.getMessage());
            return null;
        }
        // select batch 0
        float[][] output0 = resultInFloat[0];

        List<Integer> classIds = new ArrayList<>();
        List<Float> scores = new ArrayList<>();
        List<Rect2d> boxList = new ArrayList<>();

        // (14, 86400) -> (86400, 14)
        float[][] outputTransposed = new float[output0[0].length][output0.length];
        for (int i = 0; i < output0.length; i++) {
            for (int j = 0; j < output0[0].length; j++) {
                outputTransposed[j][i] = output0[i][j];
            }
        }

        for (float[] boxResult : outputTransposed) {
            float[] classScores = new float[boxResult.length - 4];
            if (boxResult.length - 4 >= 0)
                System.arraycopy(boxResult, 4, classScores, 0, boxResult.length - 4);
            float maxScore = -Float.MAX_VALUE;
            for (float score : classScores) {
                if (score > maxScore) {
                    maxScore = score;
                }
            }

            if (maxScore >= CONF_THRESHOLD) {
                int classId = 0;
                float maxClassScore = 0;
                for (int index = 4; index < outputTransposed[0].length; index++) {
                    if (boxResult[index] > maxClassScore) {
                        maxClassScore = boxResult[index];
                        classId = index - 4;
                    }
                }
                double x = boxResult[0];
                double y = boxResult[1];
                double w = boxResult[2];
                double h = boxResult[3];

                int left = (int) ((x - w / 2) * originalShape[1] / 640);
                int top = (int) ((y - h / 2) * originalShape[0] / 640);
                int width = (int) (w * originalShape[1] / 640);
                int height = (int) (h * originalShape[0] / 640);

                classIds.add(classId);
                scores.add(maxScore);
                boxList.add(new Rect2d(left, top, width, height));
            }
        }

        // Apply Non-maximum suppression(remove boxes with high overlap)
        // Select the highest confidence box first, then calculate IOU and delete the
        // high overlapped boxes. Repeat the above 2 steps until all boxes are processed
        MatOfInt candidates = new MatOfInt();
        MatOfRect2d boxes = new MatOfRect2d(boxList.toArray(new Rect2d[0]));
        float[] confArray = new float[scores.size()];
        for (int i = 0; i < scores.size(); i++) {
            confArray[i] = scores.get(i);
        }

        MatOfFloat confs = new MatOfFloat(confArray);
        Dnn.NMSBoxes(boxes, confs, CONF_THRESHOLD, NMS_THRESHOLD, candidates);
        if (candidates.empty()) {
            Log.i(TAG, "indices is empty");
            return null;
        }

        int[] indices = candidates.toArray();
        float[] confidences = confs.toArray();

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

        return new AreaItem(returnIdx, count[returnIdx]);
    }
}