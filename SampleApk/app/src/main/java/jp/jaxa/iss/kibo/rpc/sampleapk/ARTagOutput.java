package jp.jaxa.iss.kibo.rpc.sampleapk;

import org.opencv.core.Mat;

import gov.nasa.arc.astrobee.types.Point;

/**
 * Class to hold the information of an AR tag output
 */
public class ARTagOutput {
    private Point snapWorld;
    private Mat resultImage;

    /**
     * Constructor for the ArtagOutput class
     * @param snapWorld: snap world point
     * @param resultImage: result image
     */
    public ARTagOutput(Point snapWorld, Mat resultImage) {
        this.snapWorld = snapWorld;
        this.resultImage = resultImage;
    }

    /**
     * Get the snap world point
     * @return snap world point
     */
    public Point getSnapWorld() {
        return snapWorld;
    }

    /**
     * Get the result image
     * @return result image
     */
    public Mat getResultImage() {
        return resultImage;
    }
}
