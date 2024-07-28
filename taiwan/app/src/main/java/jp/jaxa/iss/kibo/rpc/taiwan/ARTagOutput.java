package jp.jaxa.iss.kibo.rpc.taiwan;

import org.opencv.core.Mat;

import gov.nasa.arc.astrobee.types.Point;

/**
 * Class to hold the information of an AR tag output
 */
public class ARTagOutput {
    private Point snapWorld;
    private Point ARTagWorld;
    private Mat resultImage;
    private Boolean valid;
    private int areaIdx;

    /**
     * Constructor for the ArtagOutput class
     * 
     * @param snapWorld:   snap world point
     * @param ARTagWorld:  artag world point
     * @param resultImage: result image
     * @param valid:       if the AR tag is valid or not
     * @param areaIdx:     index of the area
     */
    public ARTagOutput(Point snapWorld, Point ARTagWorld, Mat resultImage, Boolean valid, int areaIdx) {
        this.snapWorld = snapWorld;
        this.ARTagWorld = ARTagWorld;
        this.resultImage = resultImage;
        this.valid = valid;
        this.areaIdx = areaIdx;
    }

    /**
     * Get the snap world point
     * 
     * @return snap world point
     */
    public Point getSnapWorld() {
        return snapWorld;
    }

    /**
     * Get the ARTag world point
     * 
     * @return ARTag world point
     */
    public Point getARTagWorld() {
        return ARTagWorld;
    }

    /**
     * Get the result image
     * 
     * @return result image
     */
    public Mat getResultImage() {
        return resultImage;
    }

    /**
     * Get if the AR tag is valid or not
     * 
     * @return if the AR tag is valid or not
     */
    public Boolean getValid() {
        return valid;
    }

    /**
     * Get areaIdx
     * 
     * @return areaIdx
     */
    public int getAreaIdx() {
        return areaIdx;
    }
}
