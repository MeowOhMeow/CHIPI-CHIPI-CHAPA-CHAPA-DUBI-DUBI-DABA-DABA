package jp.jaxa.iss.kibo.rpc.sampleapk;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.opencv.core.Mat;

import gov.nasa.arc.astrobee.Kinematics;
import gov.nasa.arc.astrobee.types.Point;
import gov.nasa.arc.astrobee.types.Quaternion;
import jp.jaxa.iss.kibo.rpc.api.KiboRpcService;
import jp.jaxa.iss.kibo.rpc.sampleapk.pathfinding.PathfindingMain.PathFindingAPI;

/**
 * Class meant to handle commands from the Ground Data System and execute them
 * in Astrobee.
 */
public class YourService extends KiboRpcService {
    private static final String TAG = "YourService";
    private static final int LOOP_LIMIT = 10;

    private Point[] areaPoints = new Point[4];
    private Point[] snapPoints = new Point[4];
    private Quaternion[] areaOrientations = new Quaternion[4];
    private List<List<Point>> routes;
    private Map<String, Integer> areaInfo = new java.util.HashMap<>();

    /**
     * Constructor for the YourService class. This will initialize the area points,
     * orientations, and routes.
     */
    public YourService() {
        areaPoints[0] = new Point(10.9078d, -10.0293d, 5.1124d);
        areaPoints[1] = new Point(10.8828d, -8.7924d, 4.3904d);
        areaPoints[2] = new Point(10.8828d, -7.8424d, 4.4091d);
        areaPoints[3] = new Point(10.5280d, -6.7699d, 4.9872d);
        areaOrientations[0] = new Quaternion(0.707f, -0.707f, 0f, 0f);
        areaOrientations[1] = new Quaternion(-0.5f, 0.5f, 0.5f, 0.5f);
        areaOrientations[2] = new Quaternion(-0.5f, 0.5f, 0.5f, 0.5f);
        areaOrientations[3] = new Quaternion(0f, 0.707f, 0.707f, 0f);
        // Define the route to the area.
        routes = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            routes.add(new ArrayList<Point>());
        }
        routes.get(0).add(new Point(11.2d, -7.4d, 5.27d));
        routes.get(0).add(new Point(10.585d, -8.5d, 5.27d));
        routes.get(0).add(new Point(11.2d, -9.5d, 5.27d));
        routes.get(0).add(areaPoints[0]);
        routes.get(1).add(routes.get(0).get(0));
        routes.get(1).add(new Point(11.125d, -8.5d, 4.645d));
        routes.get(1).add(areaPoints[1]);
        routes.get(2).add(routes.get(0).get(0));
        routes.get(2).add(areaPoints[2]);
        routes.get(3).add(areaPoints[3]);
    }

    /**
     * Take a snapshot and save it.
     * 
     * @param name:     name of the image
     * @param waitTime: time to wait before taking the snapshot. unit: ms
     * @return the snapshot image
     */
    private Mat takeAndSaveSnapshot(String name, int waitTime) {
        api.flashlightControlFront(0.01f);
        try {
            Thread.sleep(waitTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Mat image = api.getMatNavCam();
        api.saveMatImage(image, name);
        api.flashlightControlFront(0f);
        return image;
    }

    /**
     * Go to an area and take a picture.
     * 
     * @param areaIdx: index of the area
     */
    private void goToTakeAPic(int areaIdx) {
        Point point = areaPoints[areaIdx];
        Quaternion quaternion = areaOrientations[areaIdx];
        /*
         * 
         * // Move to a point.
        Point point = areaPoints[areaIdx];
        Quaternion quaternion = areaOrientations[areaIdx];
        //api.moveTo(point, quaternion, false);
        Kinematics kinematics = api.getRobotKinematics();
        Log.i(TAG, "getRobotKinematics Confidence: " + kinematics.getConfidence());
        Point start = new Point(kinematics.getPosition().getX(), kinematics.getPosition().getY(), kinematics.getPosition().getZ());
        Point end = new Point(point.getX(), point.getY(), point.getZ());
        List<Point> path = PathFindingAPI.findPath(start, end);
        // show each point in the path and the number of points in the path
        Log.i(TAG, "------------------- Path -------------------");
        Log.i(TAG, "Number of points in the path: " + path.size());

        for (int i = 0; i < path.size() - 1; i++) {
            Point current = path.get(i);
            Point next = path.get((i + 1));
            Log.i(TAG, current.getX() + "," + current.getY() + "," + current.getZ() + "," + next.getX() + "," + next.getY() + "," + next.getZ());
        }

        Log.i(TAG, "--------------------------------------------");

        for (Point p : path) {
            // Exception handling
            if(null == api.moveTo(new Point(p.getX(), p.getY(), p.getZ()), quaternion, false)){
                Log.i(TAG, "Move to point failed");
            }
            else{
                Log.i(TAG, "point" + p + "x: " + p.getX() + " y: " + p.getY() + " z " + p.getZ());
            }
        }
         */

        Mat image = takeAndSaveSnapshot("Area" + areaIdx + ".jpg", 0);

        Log.i(TAG, "begin of ArtagProcess.process");
        ARTagOutput detection = ARTagProcess.process(point, quaternion, image);

        // Exception handling
        if (detection == null) {
            Log.i(TAG, "No image returned from ArtagProcess");
            snapPoints[areaIdx] = areaPoints[areaIdx]; // TBD whether -1 or areaPoints
            return;
        }

        Log.i(TAG, "Item location: " + detection.getSnapWorld());
        api.saveMatImage(detection.getResultImage(), "Area" + areaIdx + "_result.jpg");

        snapPoints[areaIdx] = detection.getSnapWorld();

        Log.i(TAG, "begin of inference");
        AreaItem areaItem = YOLOInference.getPredictions(detection.getResultImage());
        if (areaItem == null) {
            Log.i(TAG, "No item detected");
            return;
        }
        Log.i(TAG, "Detected item: " + areaItem.getItem() + " " + areaItem.getCount());
        api.setAreaInfo(areaIdx + 1, areaItem.getItem(), areaItem.getCount());
        areaInfo.put(areaItem.getItem(), areaIdx);
    }

    /**
     * Main loop of the service.
     */
    @Override
    protected void runPlan1() {
        double[][] navCamIntrinsics = api.getNavCamIntrinsics();
        ARTagProcess.setCameraMatrix(navCamIntrinsics[0]);
        ARTagProcess.setDistortCoefficient(navCamIntrinsics[1]);
        YOLOInference.init(this.getResources());

        // The mission starts.
        api.startMission();

        Kinematics kinematics = api.getRobotKinematics();
        Log.i(TAG, "Starting point: " + kinematics.getPosition() + "" + kinematics.getOrientation());
        Log.i(TAG, "getRobotKinematics Confidence: " + kinematics.getConfidence());
        
        // area 0
        
        api.moveTo(new Point(10.9078d, -10.0293d, 5.1124d), new Quaternion(0.707f, -0.707f, 0f, 0f), false);
        
        Log.i(TAG, "--------------------------------------------");
        Log.i(TAG, "go to area 0");
        Log.i(TAG, "--------------------------------------------");
        goToTakeAPic(0);
        Log.i(TAG, "--------------------------------------------");
        Log.i(TAG, "Area 0 done");
        Log.i(TAG, "--------------------------------------------");
        // area 1
    
        api.moveTo(new Point(10.700000000000005,-9.699999999999994,4.819999999999999), new Quaternion(-0.5f, 0.5f, 0.5f, 0.5f), false);
        api.moveTo(new Point(10.700000000000005,-9.249999999999988,4.819999999999999), new Quaternion(-0.5f, 0.5f, 0.5f, 0.5f), false);
        api.moveTo(new Point(10.8828d, -8.7924d, 4.3904d), new Quaternion(-0.5f, 0.5f, 0.5f, 0.5f), false);
        
        Log.i(TAG, "--------------------------------------------");
        Log.i(TAG, "go to area 1");
        Log.i(TAG, "--------------------------------------------");
        goToTakeAPic(1);
        Log.i(TAG, "--------------------------------------------");
        Log.i(TAG, "Area 1 done");
        Log.i(TAG, "--------------------------------------------");
        // area 2
        
        api.moveTo(new Point(10.8828d, -7.8424d, 4.4091d), new Quaternion(-0.5f, 0.5f, 0.5f, 0.5f), false);
        
        Log.i(TAG, "--------------------------------------------");
        Log.i(TAG, "go to area 2");
        Log.i(TAG, "--------------------------------------------");
        goToTakeAPic(2);
        Log.i(TAG, "--------------------------------------------");
        Log.i(TAG, "Area 2 done");
        Log.i(TAG, "--------------------------------------------");
        // area 3
        
        api.moveTo(new Point(10.650000000000004,-7.599999999999972,4.62), new Quaternion(0f, 0.707f, 0.707f, 0f), false);
        api.moveTo(new Point(10.600000000000003,-7.149999999999974,4.77), new Quaternion(0f, 0.707f, 0.707f, 0f), false);
        api.moveTo(new Point(10.5280d, -6.7699d, 4.9872d), new Quaternion(0f, 0.707f, 0.707f, 0f), false);
        
        Log.i(TAG, "--------------------------------------------");
        Log.i(TAG, "go to area 3");
        Log.i(TAG, "--------------------------------------------");
        goToTakeAPic(3);
        Log.i(TAG, "--------------------------------------------");
        Log.i(TAG, "Area 3 done");
        Log.i(TAG, "--------------------------------------------");

        // move to astronaut
        Point pointAtAstronaut = new Point(11.1852d, -6.7607d, 4.8828d);
        Quaternion quaternionAtAstronaut = new Quaternion(0.707f, 0.707f, 0f, 0f);
        
        if(null == api.moveTo(pointAtAstronaut, quaternionAtAstronaut, false)){
            Log.i(TAG, "Move to point Astronaut failed");
        }
        else{
            Log.i(TAG, "point" + pointAtAstronaut + "x: " + pointAtAstronaut.getX() + " y: " + pointAtAstronaut.getY() + " z " + pointAtAstronaut.getZ());
        }

        api.reportRoundingCompletion();

        Mat image = takeAndSaveSnapshot("Astronaut.jpg", 1000);

        Log.i(TAG, "begin of ArtagProcess.process");
        ARTagOutput detection = ARTagProcess.process(pointAtAstronaut, quaternionAtAstronaut, image);
        AreaItem areaItem = null;

        int loopCounter = 0;
        while (loopCounter < LOOP_LIMIT && detection == null) {
            loopCounter++;
            Log.i(TAG, "Loop counter: " + loopCounter);
            image = takeAndSaveSnapshot("Astronaut.jpg", 500);
            detection = ARTagProcess.process(pointAtAstronaut, quaternionAtAstronaut, image);
        }
        if (detection != null) {
            Log.i(TAG, "Astronaut location: " + detection.getSnapWorld());
            api.saveMatImage(detection.getResultImage(), "Astronaut_result.jpg");
            areaItem = YOLOInference.getPredictions(detection.getResultImage());
        } else {
            Log.i(TAG, "No image returned from ARTagProcess");
        }

        // Let's notify the astronaut when you recognize it.
        api.notifyRecognitionItem();

        if (areaItem != null) {
            Log.i(TAG, "Detected item: " + areaItem.getItem() + " " + areaItem.getCount());

            Integer areaIdx = areaInfo.get(areaItem.getItem());
            Log.i(TAG, "----------------------------------------");
            Log.i(TAG, "areaIdx: " + areaIdx);
            if (areaIdx != null) {
                Kinematics kinematics1 = api.getRobotKinematics();
                Log.i(TAG, "getRobotKinematics Confidence: " + kinematics1.getConfidence());
                
                List<Point> path = PathFindingAPI.findPath(kinematics1.getPosition(), snapPoints[areaIdx]);
                // show each point in the path and the number of points in the path
                Log.i(TAG, "------------------- Path -------------------");
                Log.i(TAG, "Number of points in the path: " + path.size());

                // show each point in the path and the number of points in the path
                for (int i = 0; i < path.size() - 1; i++) {
                    Point current = path.get(i);
                    Point next = path.get((i + 1));
                    Log.i(TAG, current.getX() + "," + current.getY() + "," + current.getZ() + "," + next.getX() + "," + next.getY() + "," + next.getZ());
                }

                Log.i(TAG, "--------------------------------------------");

                // move to each point in the path
                for (Point p : path) {
                    if(null == api.moveTo(new Point(p.getX(), p.getY(), p.getZ()), areaOrientations[areaIdx], false)){
                        Log.i(TAG, "Move to point failed");
                    }
                    else{
                        Log.i(TAG, "point" + p + "x: " + p.getX() + " y: " + p.getY() + " z " + p.getZ());
                    }
                }

                // Move to the target item.
                Log.i(TAG, "arrived the target item");

                // Get a camera image.
                image = takeAndSaveSnapshot("TargetItem.jpg", 500);
                detection = ARTagProcess.process(snapPoints[areaIdx], areaOrientations[areaIdx], image);
                if (detection != null) {
                    Log.i(TAG, "Item location: " + detection.getSnapWorld());
                    api.saveMatImage(detection.getResultImage(), "TargetItem_result.jpg");
                } else {
                    Log.i(TAG, "No image returned from ARTagProcess");
                }

                //second adjustment
                api.moveTo(detection.getSnapWorld(), areaOrientations[areaIdx], false);
            } else {
                Log.i(TAG, "Item not found in the areaInfo map");
            }
        } else {
            Log.i(TAG, "No item detected");
        }

        // Take a snapshot of the target item.
        api.takeTargetItemSnapshot();

        // The mission ends.
        Log.i(TAG, "--- Mission complete ---");

    }

}
