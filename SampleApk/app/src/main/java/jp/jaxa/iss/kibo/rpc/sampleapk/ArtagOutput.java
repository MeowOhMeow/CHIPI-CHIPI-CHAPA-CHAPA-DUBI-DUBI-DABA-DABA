package jp.jaxa.iss.kibo.rpc.sampleapk;

import org.opencv.core.Mat;

import gov.nasa.arc.astrobee.types.Point;

public class ArtagOutput {
    private Point snapWorld;
    private Mat resultImage;

    public ArtagOutput(Point snapWorld, Mat resultImage) {
        this.snapWorld = snapWorld;
        this.resultImage = resultImage;
    }

    public Point getSnapWorld() {
        return snapWorld;
    }

    public Mat getResultImage() {
        return resultImage;
    }
}
