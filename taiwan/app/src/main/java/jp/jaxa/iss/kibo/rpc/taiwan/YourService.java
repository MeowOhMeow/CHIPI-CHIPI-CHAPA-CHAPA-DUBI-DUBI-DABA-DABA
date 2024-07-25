package jp.jaxa.iss.kibo.rpc.taiwan;

import android.util.Log;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import org.opencv.core.Mat;

import gov.nasa.arc.astrobee.Kinematics;
import gov.nasa.arc.astrobee.Result;
import gov.nasa.arc.astrobee.types.Point;
import gov.nasa.arc.astrobee.types.Quaternion;
import jp.jaxa.iss.kibo.rpc.api.KiboRpcService;
import jp.jaxa.iss.kibo.rpc.taiwan.pathfinding.PathFindingAPI;

/**
 * Class meant to handle commands from the Ground Data System and execute them
 * in Astrobee.
 */
public class YourService extends KiboRpcService {
    private static final String TAG = "YourService";
    private static final int LOOP_LIMIT = 10;
    private static final int SNAP_SHOT_WAIT_TIME = 2000;

    private static Quaternion[] areaOrientations = new Quaternion[4];
    private static Point[] snapPoints = new Point[4];
    private static List[] paths = new List[4];
    private static Map<String, Integer> areaInfo = new HashMap<>();
    private static AreaItem[] areaItems = new AreaItem[4];
    private static Map<String, Mat> itemImages = new HashMap<>();
    private static ARTagOutput[] detections;

    private static double expansionVal = 0.08;
    private static Point pointAtAstronaut = new Point(11.1852d, -6.7607d, 4.8828d);
    private static Quaternion quaternionAtAstronaut = new Quaternion(0.707f, 0.707f, 0f, 0f);

    private static final BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
    private static Worker worker = new Worker(queue);
    private static Thread workerThread = new Thread(worker);

    /**
     * Constructor for the YourService class.
     */
    public YourService() {
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
     * Process the area information.
     * Call function "takeAndSaveSnapshot" and add new work in queue
     * 
     * @param areaIdxs: indexes of the areas to process
     */
    private void processingAreaInfo(int[] areaIdxs) {
        Mat image = takeAndSaveSnapshot("Area" + Arrays.toString(areaIdxs) + ".jpg", SNAP_SHOT_WAIT_TIME);

        ARTagException(image, areaIdxs);

        Work work = new Work(areaIdxs, detections, itemImages, snapPoints, areaItems, areaInfo, pointAtAstronaut,
                expansionVal, paths);
        queue.add(work);
    }

    /**
     * Log the path
     * 
     * @param path: the path to log
     */
    public static void logPath(List<Point> path) {
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
    public void moveToTarget(Point targetPoint, Quaternion orientation) {
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
     * Move forward to retake image.
     * 
     * @param move_x, move_y, move_z: the amount of movement
     * @param quaternion: the orientation of Astrobee
     * @param Idxs: Area indexes
     * @return detection result of ARTag Process
     */
    private ARTagOutput retake_forward(double move_x, double move_y, double move_z, Quaternion quaternion, int[] Idxs){
        Kinematics kinematics = api.getRobotKinematics();
        Result isMoveToSuccessResult = null;
        ARTagOutput detection = null;

        int move_count = 0;
        while(move_count < 3)
        {
            isMoveToSuccessResult = api.moveTo(new Point(kinematics.getPosition().getX() + move_x*move_count, kinematics.getPosition().getY() + move_y*move_count, kinematics.getPosition().getZ() + move_z*move_count),
                    quaternion, false);
            if (!isMoveToSuccessResult.hasSucceeded()) {
                Log.i(TAG, "----------Move forward failed, retrying with theta star algorithm----------");
                moveToTarget(new Point(kinematics.getPosition().getX() + move_x*move_count, kinematics.getPosition().getY() + move_y*move_count, kinematics.getPosition().getZ() + move_z*move_count),
                        quaternion);
            }

            Mat image_retake = takeAndSaveSnapshot("Area" + Arrays.toString(Idxs) + ".jpg", SNAP_SHOT_WAIT_TIME);
            detection = ARTagProcess.process(kinematics.getPosition(), kinematics.getOrientation(), image_retake)[0];
            if(detection != null)
                break;
            move_count++;
        }
        Log.i(TAG, "move_count:"+ move_count);
        return detection;
    }

    /**
     * Move to specific point to retake image.
     * 
     * @param point: target point
     * @param quaternion: the orientation of Astrobee
     * @param Idxs: Area indexes
     * @return detection result of ARTag Process
     */
    private ARTagOutput[] retake_moveToPoint(Point point, Quaternion quaternion, int[] Idxs){
        Kinematics kinematics = api.getRobotKinematics();
        Result isMoveToSuccessResult = null;
        isMoveToSuccessResult = api.moveTo(point, quaternion, false);
        if (!isMoveToSuccessResult.hasSucceeded()) {
            Log.i(TAG, "----------Move forward failed, retrying with theta star algorithm----------");
            moveToTarget(point, quaternion);
        }

        Mat image_retake = takeAndSaveSnapshot("Area" + Arrays.toString(Idxs) + ".jpg", SNAP_SHOT_WAIT_TIME);
        ARTagOutput[] detect_arr = ARTagProcess.process(kinematics.getPosition(), kinematics.getOrientation(), image_retake);
        return detect_arr;
    }

    /**
     * Calculate the distance of two points.
     * 
     * @param start_x, start_y, start_z: starting point
     * @param end_x, end_y, end_z: end point
     * @return distance(m)
     */
    private double calculate_distance(double start_x, double start_y, double start_z, double end_x, double end_y, double end_z){
        double distance = Math.sqrt(Math.pow(start_x - end_x, 2) + Math.pow(start_y - end_y, 2) + Math.pow(start_z - end_z, 2));
        return distance;
    }

    /**
     * Calculate the distance of two points.
     * 
     * @param image: image from NavCam
     * @param areaIdxs: Area indexes
     */
    private void ARTagException(Mat image, int[] areaIdxs){
        Kinematics kinematics = api.getRobotKinematics();
        detections = ARTagProcess.process(kinematics.getPosition(), kinematics.getOrientation(), image);

        // Handling the case when no detection is returned
        if ((Arrays.equals(areaIdxs, new int[]{0})) && (detections == null)) {
            Log.i(TAG, "retake image of area 0");
            detections = new ARTagOutput[1];
            detections = retake_moveToPoint(new Point(10.9078d, -9.887558906125106d, 5.1124d),
                    new Quaternion(0.707f, -0.707f, 0f, 0f), new int[]{areaIdxs[0]});
        }
        else if ((Arrays.equals(areaIdxs, new int[]{3})) && (detections == null)) {
            Log.i(TAG, "retake image of area 3");
            detections = new ARTagOutput[1];
            detections[0] = retake_forward(-0.05, 0, 0, new Quaternion(0f, 0.707f, 0.707f, 0f), areaIdxs);
        }
        else if (Arrays.equals(areaIdxs, new int[]{1, 2})){
            // both failed
            if (detections == null){
                // move to area1
                Log.i(TAG, "Both failed, retake image of area 1");
                detections = new ARTagOutput[2];
                ARTagOutput[] detect_arr = retake_moveToPoint(new Point(10.8828d, -8.7924d, 4.557490723909075d),
                        new Quaternion(-0.5f, 0.5f, 0.5f, 0.5f), new int[]{areaIdxs[0]});
                detections[0] = detect_arr[0];
                if(detections[0] == null){
                    detections[0] = retake_forward(0, 0, -0.05, new Quaternion(-0.5f, 0.5f, 0.5f, 0.5f), new int[]{areaIdxs[0]});
                }

                //move to area2
                Log.i(TAG, "Both failed, retake image of area 2");
                detect_arr = retake_moveToPoint(new Point(10.8828d, -7.8424d, 4.569366733183541d),
                        new Quaternion(-0.5f, 0.5f, 0.5f, 0.5f), new int[]{areaIdxs[1]});
                detections[1] = detect_arr[1];
                if(detections[1] == null){
                    detections[1] = retake_forward(0, 0, -0.05, new Quaternion(-0.5f, 0.5f, 0.5f, 0.5f), new int[]{areaIdxs[1]});
                }
            }
            // One in the two failed
            else if (detections.length == 1){
                double distance_to_area1 = calculate_distance(kinematics.getPosition().getX(), kinematics.getPosition().getY(), kinematics.getPosition().getZ()
                        , 10.925, -8.875, 3.76203);
                double distance_to_area2 = calculate_distance(kinematics.getPosition().getX(), kinematics.getPosition().getY(), kinematics.getPosition().getZ()
                        , 10.925, -7.925, 3.76093);

                // area1 failed, area2 success
                if ((distance_to_area1 >= distance_to_area2)){
                    Log.i(TAG, "retake image of area 1");

                    ARTagOutput[] newDetections = new ARTagOutput[2];
                    ARTagOutput[] detect_arr = retake_moveToPoint(new Point(10.8828d, -8.7924d, 4.557490723909075d),
                            new Quaternion(-0.5f, 0.5f, 0.5f, 0.5f), new int[]{areaIdxs[0]});
                    newDetections[0] = detect_arr[0];
                    newDetections[1] = detections[0];
                    detections = newDetections;

                    if(detections[0] == null){
                        detections[0] = retake_forward(0, 0, -0.05, new Quaternion(-0.5f, 0.5f, 0.5f, 0.5f), new int[]{areaIdxs[0]});
                    }
                }
                // area1 success, area2 failed
                else if(distance_to_area1 < distance_to_area2){
                    Log.i(TAG, "retake image of area 2");

                    ARTagOutput[] newDetections = new ARTagOutput[2];
                    ARTagOutput[] detect_arr = retake_moveToPoint(new Point(10.8828d, -7.8424d, 4.569366733183541d),
                            new Quaternion(-0.5f, 0.5f, 0.5f, 0.5f), new int[]{areaIdxs[1]});
                    newDetections[0] = detections[0];
                    newDetections[1] = detect_arr[1];
                    detections = newDetections;

                    if(detections[1] == null){
                        detections[1] = retake_forward(0, 0, -0.05, new Quaternion(-0.5f, 0.5f, 0.5f, 0.5f), new int[]{areaIdxs[1]});
                    }
                }
            }
        }
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
        workerThread.start();

        // The mission starts.
        api.startMission();

        {
            Kinematics kinematics = api.getRobotKinematics();
            Log.i(TAG, "Starting point: " + kinematics.getPosition() + "" + kinematics.getOrientation());
        }
        Result isMoveToSuccessResult = null;

        // move to area 0
        // isMoveToSuccessResult = api.moveTo(new Point(10.9078d, -9.967877763897507d, 5.1124d),
        //         new Quaternion(0.707f, -0.707f, 0f, 0f), false);
//        if (!isMoveToSuccessResult.hasSucceeded()) {
//            Log.i(TAG, "----------Go to area 0 fail, retrying with theta star algorithm----------");
//            moveToTarget(new Point(10.9078d, -9.967877763897507d, 5.1124d), new Quaternion(0.707f, -0.707f, 0f, 0f));
//        }

        //test---------------------------------------------------------------------------------------------
        isMoveToSuccessResult = api.moveTo(new Point(10.0078d, -9.967877763897507d, 5.1124d),
                new Quaternion(0.707f, -0.707f, 0f, 0f), false);
        if (!isMoveToSuccessResult.hasSucceeded()) {
            Log.i(TAG, "----------Go to area 0 fail, retrying with theta star algorithm----------");
            moveToTarget(new Point(10.0078d, -9.967877763897507d, 5.1124d), new Quaternion(0.707f, -0.707f, 0f, 0f));
        }
        //-------------------------------------------------------------------------------------------------

        Log.i(TAG, "--------------------------------------------");
        Log.i(TAG, "go to area 0");
        Log.i(TAG, "--------------------------------------------");
        processingAreaInfo(new int[] { 0 });
        Log.i(TAG, "--------------------------------------------");
        Log.i(TAG, "Area 0 done");
        Log.i(TAG, "--------------------------------------------");

        // move to area 1,2
//        isMoveToSuccessResult = api.moveTo(new Point(11.07, -9.5, 5.17d), new Quaternion(-0.5f, 0.5f, 0.5f, 0.5f),
//                false);
//        if (!isMoveToSuccessResult.hasSucceeded()) {
//            Log.i(TAG, "----------Go to second point fail, retrying with theta star algorithm----------");
//            moveToTarget(new Point(11.07, -9.5, 5.17d), new Quaternion(-0.5f, 0.5f, 0.5f, 0.5f));
//        }

//        isMoveToSuccessResult = api.moveTo(new Point(10.8828, -8.2674, 4.719), new Quaternion(-0.5f, 0.5f, 0.5f, 0.5f),
//                false);
//        if (!isMoveToSuccessResult.hasSucceeded()) {
//            Log.i(TAG, "----------Go to area 1, 2 fail, retrying with theta star algorithm----------");
//            moveToTarget(new Point(10.8828, -8.2674, 4.719), new Quaternion(-0.5f, 0.5f, 0.5f, 0.5f));
//        }
        //test--------------------------------------------------------------------------------------------
        isMoveToSuccessResult = api.moveTo(new Point(11.07d, -6.0d,  4.719), new Quaternion(-0.5f, 0.5f, 0.5f, 0.5f),
                false);
        if (!isMoveToSuccessResult.hasSucceeded()) {
            Log.i(TAG, "----------Go to second point fail, retrying with theta star algorithm----------");
            moveToTarget(new Point(11.07d, -6.0d,  4.719), new Quaternion(-0.5f, 0.5f, 0.5f, 0.5f));
        }
        //-------------------------------------------------------------------------------------------------

        Log.i(TAG, "--------------------------------------------");
        Log.i(TAG, "go to area 1, 2");
        Log.i(TAG, "--------------------------------------------");
        processingAreaInfo(new int[] { 1, 2 });
        Log.i(TAG, "--------------------------------------------");
        Log.i(TAG, "Area 1, 2 done");
        Log.i(TAG, "--------------------------------------------");

        // move to area 3
        // isMoveToSuccessResult = api.moveTo(new Point(10.605058889481256d, -6.7699d, 4.9872000000000005d),
        //         new Quaternion(0f, 0.707f, 0.707f, 0f), false);
//        if (!isMoveToSuccessResult.hasSucceeded()) {
//            Log.i(TAG, "----------Go to area 3 fail, retrying with theta star algorithm----------");
//            moveToTarget(new Point(10.605058889481256d, -6.7699d, 4.9872000000000005d),
//                    new Quaternion(0f, 0.707f, 0.707f, 0f));
//        }

        //test---------------------------------------------------------------------------------------------
        isMoveToSuccessResult = api.moveTo(new Point(11.005058889481256d, -6.7699d, 4.9872000000000005d),
                new Quaternion(0f, 0.707f, 0.707f, 0f), false);
        if (!isMoveToSuccessResult.hasSucceeded()) {
            Log.i(TAG, "----------Go to area 3 fail, retrying with theta star algorithm----------");
            moveToTarget(new Point(11.005058889481256d, -6.7699d, 4.9872000000000005d),
                    new Quaternion(0f, 0.707f, 0.707f, 0f));
        }
        //-------------------------------------------------------------------------------------------------

        Log.i(TAG, "--------------------------------------------");
        Log.i(TAG, "go to area 3");
        Log.i(TAG, "--------------------------------------------");
        processingAreaInfo(new int[] { 3 });
        Log.i(TAG, "--------------------------------------------");
        Log.i(TAG, "Area 3 done");
        Log.i(TAG, "--------------------------------------------");

        worker.stop();

        // move to astronaut
        isMoveToSuccessResult = api.moveTo(pointAtAstronaut, quaternionAtAstronaut, false);
        if (!isMoveToSuccessResult.hasSucceeded()) {
            Log.i(TAG, "----------Go to astronaut fail, retrying with theta star algorithm----------");
            moveToTarget(pointAtAstronaut, quaternionAtAstronaut);
        }

        try {
            workerThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // Save the images
        for (Map.Entry<String, Mat> entry : itemImages.entrySet()) {
            api.saveMatImage(entry.getValue(), entry.getKey());
        }
        // Set the area information
        for (int i = 0; i < 4; i++) {
            api.setAreaInfo(i + 1, areaItems[i].getItem(), areaItems[i].getCount());
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
                List<Point> path = paths[areaIdx];
                logPath(path);
                Result result = null;
                boolean pathSuccess = false;
                int loopCounter = 0;
                while (!pathSuccess && loopCounter < LOOP_LIMIT) {
                    // move to each point in the path
                    for (Point point : path) {
                        result = api.moveTo(point, areaOrientations[areaIdx], false);
                        if (!result.hasSucceeded()) {
                            expansionVal += 0.02;
                            Log.i(TAG, "----------Path corrupt, increasing expansionVal to: " + expansionVal
                                    + "----------");
                            path = PathFindingAPI.findPath(api.getRobotKinematics().getPosition(),
                                    snapPoints[areaIdx],
                                    expansionVal);
                            logPath(path);
                            break;
                        }
                    }
                    pathSuccess = result != null && result.hasSucceeded();
                    loopCounter++;
                }
                Log.i(TAG, "Arrive at the target point");
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