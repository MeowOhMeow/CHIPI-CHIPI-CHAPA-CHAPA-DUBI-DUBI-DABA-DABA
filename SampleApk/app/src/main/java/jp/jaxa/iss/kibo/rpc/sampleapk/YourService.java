package jp.jaxa.iss.kibo.rpc.sampleapk;

import android.util.Log;

import java.util.*;

import org.opencv.core.Mat;

import gov.nasa.arc.astrobee.Kinematics;
import gov.nasa.arc.astrobee.Result;
import gov.nasa.arc.astrobee.types.Point;
import gov.nasa.arc.astrobee.types.Quaternion;
import jp.jaxa.iss.kibo.rpc.api.KiboRpcService;
import jp.jaxa.iss.kibo.rpc.sampleapk.pathfinding.PathFindingAPI;

/**
 * Class meant to handle commands from the Ground Data System and execute them
 * in Astrobee.
 */
public class YourService extends KiboRpcService {
    private static final String TAG = "YourService";
    private static final int LOOP_LIMIT = 10;
    private static final int SNAP_SHOT_WAIT_TIME = 2000;

    private Point[] areaPoints = new Point[4];
    private Quaternion[] areaOrientations = new Quaternion[4];
    private Point[] snapPoints = new Point[4];
    private Map<String, Integer> areaInfo = new HashMap<>();

    private double expansionVal = 0.08;
    private Point pointAtAstronaut = new Point(11.1852d, -6.7607d, 4.8828d);
    private Quaternion quaternionAtAstronaut = new Quaternion(0.707f, 0.707f, 0f, 0f);

    /**
     * Constructor for the YourService class. This will initialize the area points,
     * orientations, and routes.
     */
    public YourService() {
        areaPoints[0] = new Point(10.9078d, -9.967877763897507d, 5.1124d);
        areaPoints[1] = new Point(10.8828d, -8.2674d, 4.719d);
        areaPoints[2] = new Point(10.8828d, -8.2674d, 4.719d);
        areaPoints[3] = new Point(10.605058889481256d, -6.7699d, 4.9872000000000005d);

        areaOrientations[0] = new Quaternion(0.707f, -0.707f, 0f, 0f);
        areaOrientations[1] = new Quaternion(-0.5f, 0.5f, 0.5f, 0.5f);
        areaOrientations[2] = new Quaternion(-0.5f, 0.5f, 0.5f, 0.5f);
        areaOrientations[3] = new Quaternion(0f, 0.707f, 0.707f, 0f);
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
     * process the area information
     * 
     * @param areaIdxs: indexes of the areas to process
     * 
     * TODO: use multiple threads to process the areas
     */
    private void processingAreaInfo(int[] areaIdxs) {
        Mat image = takeAndSaveSnapshot("Area" + Arrays.toString(areaIdxs) + ".jpg", SNAP_SHOT_WAIT_TIME);

        Kinematics kinematics = api.getRobotKinematics();
        ARTagOutput[] detections = ARTagProcess.process(kinematics.getPosition(), kinematics.getOrientation(), image);
        if (detections == null) {
            // TODO: handle the case when no detection is returned
            return;
        }

        for (int ARTagIdx = 0; ARTagIdx < areaIdxs.length; ARTagIdx++) {
            int areaIdx = areaIdxs[ARTagIdx];
            ARTagOutput detection = detections[ARTagIdx];

            Log.i(TAG, "Item " + areaIdx + " location: " + detection.getSnapWorld());
            api.saveMatImage(detection.getResultImage(), "Area" + areaIdx + "_result.jpg");

            snapPoints[areaIdx] = detection.getSnapWorld();

            Log.i(TAG, "begin of inference");
            AreaItem areaItem = YOLOInference.getPredictions(detection.getResultImage());
            if (areaItem == null) {
                Log.i(TAG, "No item detected");
                continue;
            }
            Log.i(TAG, "Detected item: " + areaItem.getItem() + " " + areaItem.getCount());
            api.setAreaInfo(areaIdx + 1, areaItem.getItem(), areaItem.getCount());
            areaInfo.put(areaItem.getItem(), areaIdx);

            // TODO: compute to path from astronaut to the item here
        }
    }

    /**
     * Log the path
     * 
     * @param path: the path to log
     */
    private void logPath(List<Point> path) {
        Log.i(TAG, "------------------- Path -------------------");
        Log.i(TAG, "Number of points in the path: " + path.size());

        // show each point in the path and the number of points in the path
        for (int i = 0; i < path.size() - 1; i++) {
            Point current = path.get(i);
            Point next = path.get((i + 1));
            Log.i(TAG, current.getX() + "," + current.getY() + "," + current.getZ() + "," + next.getX() + ","
                    + next.getY() + "," + next.getZ());
        }
        Log.i(TAG, "--------------------------------------------");
    }

    /**
     * Move to the target point by applying theta star algorithm
     * 
     * @param targetPoint: the target point
     * @param orientation: the orientation
     */
    private void moveToTarget(Point targetPoint, Quaternion orientation) {
        List<Point> path = PathFindingAPI.findPath(api.getRobotKinematics().getPosition(), targetPoint,
                expansionVal);
        logPath(path);
        Result result = null;
        boolean pathSuccess = false;
        int loopCounter = 0;
        while (!pathSuccess && loopCounter < LOOP_LIMIT) {
            // move to each point in the path
            for (Point point : path) {
                result = api.moveTo(point, orientation, false);
                if (!result.hasSucceeded()) {
                    expansionVal += 0.02;
                    Log.i(TAG, "----------Path corrupt, increasing expansionVal to: " + expansionVal
                            + "----------");
                    path = PathFindingAPI.findPath(api.getRobotKinematics().getPosition(),
                            targetPoint,
                            expansionVal);
                    logPath(path);
                    break;
                }
            }
            pathSuccess = result != null && result.hasSucceeded();
            loopCounter++;
        }

        Log.i(TAG, "Arrive at the target point");
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

        {
            Kinematics kinematics = api.getRobotKinematics();
            Log.i(TAG, "Starting point: " + kinematics.getPosition() + "" + kinematics.getOrientation());
        }

        Result isMoveToSuccessResult = null;
        isMoveToSuccessResult = api.moveTo(new Point(10.9078d, -9.967877763897507d, 5.1124d), new Quaternion(0.707f, -0.707f, 0f, 0f), false);
        if (!isMoveToSuccessResult.hasSucceeded()) {
            Log.i(TAG, "----------Go to area 0 fail, retrying with theta star algorithm----------");
            moveToTarget(new Point(10.9078d, -9.967877763897507d, 5.1124d), new Quaternion(0.707f, -0.707f, 0f, 0f));
        }

        Log.i(TAG, "--------------------------------------------");
        Log.i(TAG, "go to area 0");
        Log.i(TAG, "--------------------------------------------");
        processingAreaInfo(new int[] { 0 });
        Log.i(TAG, "--------------------------------------------");
        Log.i(TAG, "Area 0 done");
        Log.i(TAG, "--------------------------------------------");

        isMoveToSuccessResult = api.moveTo(new Point(11.07, -9.5, 5.17d), new Quaternion(-0.5f, 0.5f, 0.5f, 0.5f), false);
        if (!isMoveToSuccessResult.hasSucceeded()) {
            Log.i(TAG, "----------Go to second point fail, retrying with theta star algorithm----------");
            moveToTarget(new Point(11.07, -9.5, 5.17d), new Quaternion(-0.5f, 0.5f, 0.5f, 0.5f));
        }

        isMoveToSuccessResult = api.moveTo(new Point(10.8828, -8.2674, 4.719), new Quaternion(-0.5f, 0.5f, 0.5f, 0.5f), false);
        if (!isMoveToSuccessResult.hasSucceeded()) {
            Log.i(TAG, "----------Go to area 1, 2 fail, retrying with theta star algorithm----------");
            moveToTarget(new Point(10.8828, -8.2674, 4.719), new Quaternion(-0.5f, 0.5f, 0.5f, 0.5f));
        }

        Log.i(TAG, "--------------------------------------------");
        Log.i(TAG, "go to area 1, 2");
        Log.i(TAG, "--------------------------------------------");
        processingAreaInfo(new int[] { 1, 2 });
        Log.i(TAG, "--------------------------------------------");
        Log.i(TAG, "Area 1, 2 done");
        Log.i(TAG, "--------------------------------------------");

        isMoveToSuccessResult = api.moveTo(new Point(10.605058889481256d, -6.7699d, 4.9872000000000005d),
                new Quaternion(0f, 0.707f, 0.707f, 0f), false); 
        if (!isMoveToSuccessResult.hasSucceeded()) {
            Log.i(TAG, "----------Go to area 3 fail, retrying with theta star algorithm----------");
            moveToTarget(new Point(10.605058889481256d, -6.7699d, 4.9872000000000005d),
                    new Quaternion(0f, 0.707f, 0.707f, 0f));
        }

        Log.i(TAG, "--------------------------------------------");
        Log.i(TAG, "go to area 3");
        Log.i(TAG, "--------------------------------------------");
        processingAreaInfo(new int[] { 3 });
        Log.i(TAG, "--------------------------------------------");
        Log.i(TAG, "Area 3 done");
        Log.i(TAG, "--------------------------------------------");

        // move to astronaut
        isMoveToSuccessResult = api.moveTo(pointAtAstronaut, quaternionAtAstronaut, false);
        if (!isMoveToSuccessResult.hasSucceeded()) {
            Log.i(TAG, "----------Go to astronaut fail, retrying with theta star algorithm----------");
            moveToTarget(pointAtAstronaut, quaternionAtAstronaut);
        }

        api.reportRoundingCompletion();

        AreaItem areaItem = null;
        {
            Mat image = takeAndSaveSnapshot("Astronaut.jpg", SNAP_SHOT_WAIT_TIME);
            ARTagOutput[] detections = ARTagProcess.process(pointAtAstronaut,
                    quaternionAtAstronaut, image);

            int loopCounter = 0;
            while (loopCounter < LOOP_LIMIT && detections == null) {
                loopCounter++;
                image = takeAndSaveSnapshot("Astronaut.jpg", 200);
                detections = ARTagProcess.process(pointAtAstronaut,
                        quaternionAtAstronaut, image);
            }
            if (detections != null) {
                Log.i(TAG, "Astronaut location: " + detections[0].getSnapWorld());
                api.saveMatImage(detections[0].getResultImage(), "Astronaut_result.jpg");
                areaItem = YOLOInference.getPredictions(detections[0].getResultImage());
            } else {
                Log.i(TAG, "No image returned from ARTagProcess");
            }
        }

        // Let's notify the astronaut when you recognize it.
        api.notifyRecognitionItem();

        if (areaItem != null) {
            Log.i(TAG, "Detected item: " + areaItem.getItem() + " " + areaItem.getCount());

            Integer areaIdx = areaInfo.get(areaItem.getItem());
            Log.i(TAG, "----------------------------------------");
            Log.i(TAG, "areaIdx: " + areaIdx);
            if (areaIdx != null) {
                moveToTarget(snapPoints[areaIdx], areaOrientations[areaIdx]);

                // Get a camera image.
                Mat image = takeAndSaveSnapshot("TargetItem.jpg", SNAP_SHOT_WAIT_TIME);
                ARTagOutput[] detections = ARTagProcess.process(snapPoints[areaIdx],
                        areaOrientations[areaIdx], image);
                if (detections != null) {
                    Log.i(TAG, "Item location: " + detections[0].getSnapWorld());
                    api.saveMatImage(detections[0].getResultImage(), "TargetItem_result.jpg");
                } else {
                    Log.i(TAG, "No image returned from ARTagProcess");
                }
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
