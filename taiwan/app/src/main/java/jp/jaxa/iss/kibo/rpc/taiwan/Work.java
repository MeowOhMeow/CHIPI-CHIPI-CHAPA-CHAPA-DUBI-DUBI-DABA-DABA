package jp.jaxa.iss.kibo.rpc.sampleapk;

import android.util.Log;

import java.util.List;
import java.util.Map;

import org.opencv.core.Mat;

import gov.nasa.arc.astrobee.types.Point;
import jp.jaxa.iss.kibo.rpc.api.KiboRpcService;
import jp.jaxa.iss.kibo.rpc.sampleapk.pathfinding.PathFindingAPI;

//* api is not thread safe.
/**
 * Class to implement ARTagProcess, YOLOInference, and find the returned path
 */
public class Work extends KiboRpcService implements Runnable {
    private static final String TAG = "Work";
    private int[] areaIdxs;
    private static ARTagOutput[] detections;
    private Map<String, Mat> itemImages;
    private Point[] snapPoints;
    private AreaItem[] areaItems;
    private Map<String, Integer> areaInfo;
    private Point pointAtAstronaut;
    private double expansionVal;
    private List[] paths;

    public Work(int[] areaIdxs, ARTagOutput[] detections, Map<String, Mat> itemImages, Point[] snapPoints, AreaItem[] areaItems, Map<String, Integer> areaInfo, Point pointAtAstronaut, double expansionVal, List[] paths) {
        super(); // This calls the Parent class constructor
        this.areaIdxs = areaIdxs;
        this.detections = detections;
        this.itemImages = itemImages;
        this.snapPoints = snapPoints;
        this.areaItems = areaItems;
        this.areaInfo = areaInfo;
        this.pointAtAstronaut = pointAtAstronaut;
        this.expansionVal = expansionVal;
        this.paths = paths;
    }

    @Override
    public void run() {
        for (int ARTagIdx = 0; ARTagIdx < areaIdxs.length; ARTagIdx++) {
            int areaIdx = areaIdxs[ARTagIdx];
            ARTagOutput detection = detections[ARTagIdx];

            // I hope this is thread safe
            Log.i(TAG, "Item " + areaIdx + " location: " + detection.getSnapWorld());
            itemImages.put("Area" + areaIdx + "_result.jpg", detection.getResultImage());

            snapPoints[areaIdx] = detection.getSnapWorld();

            Log.i(TAG, "begin of inference");
            AreaItem areaItem = YOLOInference.getPredictions(detection.getResultImage());
            if (areaItem == null) {
                Log.i(TAG, "No item detected");
                continue;
            }
            Log.i(TAG, "Detected item: " + areaItem.getItem() + " " + areaItem.getCount());
            areaItems[areaIdx] = areaItem;
            areaInfo.put(areaItem.getItem(), areaIdx);

            List<Point> path = PathFindingAPI.findPath(pointAtAstronaut, snapPoints[areaIdx], expansionVal);
            YourService.logPath(path);
            paths[areaIdx] = path;
        }
    }
}
    