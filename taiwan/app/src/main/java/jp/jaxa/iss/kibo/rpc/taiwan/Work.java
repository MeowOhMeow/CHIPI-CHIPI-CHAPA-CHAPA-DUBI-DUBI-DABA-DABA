package jp.jaxa.iss.kibo.rpc.taiwan;

import android.util.Log;

import java.util.List;
import java.util.Map;

import org.opencv.core.Mat;

import gov.nasa.arc.astrobee.types.Point;
import jp.jaxa.iss.kibo.rpc.api.KiboRpcService;
import jp.jaxa.iss.kibo.rpc.taiwan.pathfinding.PathFindingAPI;
import jp.jaxa.iss.kibo.rpc.taiwan.Publisher.Path;
import jp.jaxa.iss.kibo.rpc.taiwan.Publisher.Implementation;

/**
 * Class to implement ARTagProcess, YOLOInference, and find the returned path
 */
public class Work extends KiboRpcService implements Runnable {
    private static final String TAG = "Work";
    private ARTagOutput detection;
    private AreaItem[] areaItems;
    private Map<String, Integer> areaInfo;
    private Point pointAtAstronaut;
    private double expansionVal;
    private static Implementation observerImplementation;

    public Work(ARTagOutput detection, AreaItem[] areaItems, Map<String, Integer> areaInfo, Point pointAtAstronaut,
            double expansionVal, Implementation observerImplementation) {
        super(); // This calls the Parent class constructor
        this.detection = detection;
        this.areaItems = areaItems;
        this.areaInfo = areaInfo;
        this.pointAtAstronaut = pointAtAstronaut;
        this.expansionVal = expansionVal;
        this.observerImplementation = observerImplementation;
    }

    @Override
    public void run() {
        int areaIdx = detection.getAreaIdx();

        Log.i(TAG, "begin of inference");
        AreaItem areaItem = YOLOInference.getPredictions(detection.getResultImage());
        if (areaItem == null) {
            Log.i(TAG, "No item detected");
            // TODO: handle this case
            return;
        }
        Log.i(TAG, "Detected item: " + areaItem.getItem() + " " + areaItem.getCount());
        areaItems[areaIdx] = areaItem;
        areaInfo.put(areaItem.getItem(), areaIdx);

        List<Point> points = PathFindingAPI.findPath(pointAtAstronaut, detection.getSnapWorld(), expansionVal);
        PathFindingAPI.logPoints(points, "Path from astronaut to item " + areaIdx);

        Path path = new Path(points, 0.08, pointAtAstronaut, detection.getSnapWorld());
        observerImplementation.addPath(String.valueOf(areaIdx), path);
    }
}
