package jp.jaxa.iss.kibo.rpc.taiwan.multithreading;

import android.util.Log;

import java.util.List;
import java.util.Map;

import gov.nasa.arc.astrobee.types.Point;
import jp.jaxa.iss.kibo.rpc.taiwan.ARTagOutput;
import jp.jaxa.iss.kibo.rpc.taiwan.AreaItem;
import jp.jaxa.iss.kibo.rpc.taiwan.YOLOInference;
import jp.jaxa.iss.kibo.rpc.taiwan.YourService;
import jp.jaxa.iss.kibo.rpc.taiwan.pathfinding.PathFindingAPI;
import jp.jaxa.iss.kibo.rpc.taiwan.multithreading.Publisher.Path;

/**
 * Class to implement ARTagProcess, YOLOInference, and find the returned path
 */
public class PathCalWork implements Runnable {
    private static final String TAG = "Work";
    private ARTagOutput detection;
    private AreaItem[] areaItems;
    private Map<String, Integer> areaInfo;
    private Point pointAtAstronaut;
    private double expansionVal;
    private static float lower_ave_cof = Float.POSITIVE_INFINITY;
    private static AreaItem lower_cof_areaItem;

    public PathCalWork(ARTagOutput detection, AreaItem[] areaItems, Map<String, Integer> areaInfo, Point pointAtAstronaut,
                       double expansionVal) {
        super(); // This calls the Parent class constructor
        this.detection = detection;
        this.areaItems = areaItems;
        this.areaInfo = areaInfo;
        this.pointAtAstronaut = pointAtAstronaut;
        this.expansionVal = expansionVal;
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
        
        if(YOLOInference.get_ave_cof()<lower_ave_cof){
            lower_ave_cof=YOLOInference.get_ave_cof();
            lower_cof_areaItem=areaItem;
            Log.i(TAG, " lower_ave_cof" +  lower_ave_cof  );
            Log.i(TAG, "ave_cof item: " + lower_cof_areaItem.getItem() + " " + lower_cof_areaItem.getCount());

            YourService.setLowerCofAreaItem(lower_cof_areaItem);
        }

        List<Point> points = PathFindingAPI.findPath(pointAtAstronaut, detection.getSnapWorld(), expansionVal);
        PathFindingAPI.logPoints(points, "Path from astronaut to item " + areaIdx);

        Path path = new Path(points, 0.08, pointAtAstronaut, detection.getSnapWorld());
        YourService.observerImplementation.addElement(String.valueOf(areaIdx), path);
    }
}
