package jp.jaxa.iss.kibo.rpc.taiwan;

import android.util.Log;

import java.util.*;
import java.util.concurrent.*;

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
    private static final long SNAP_SHOT_WAIT_TIME = 2000;

    private static Quaternion[] areaOrientations = new Quaternion[4];
    private static Point[] snapPoints = new Point[4];
    private static List[] paths = new List[4];
    private static Map<String, Integer> areaInfo = new HashMap<>();
    private static AreaItem[] areaItems = new AreaItem[4];
    private static Map<String, Mat> itemImages = new HashMap<>();

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
        areaOrientations[0] = new Quaternion(0f, 0f, -0.71f, 0.71f);
        areaOrientations[1] = new Quaternion(0.5f, 0.5f, -0.5f, 0.5f);
        areaOrientations[2] = new Quaternion(0.5f, 0.5f, -0.5f, 0.5f);
        areaOrientations[3] = new Quaternion(0f, -0.71f, 0.71f, 0f);
    }

    /**
     * Process the area information.
     * Call function "takeAndSaveSnapshot" and add new work in queue
     *
     * @param areaIdxs: indexes of the areas to process
     */
    private void processingAreaInfo(int[] areaIdxs) {
        Mat image = Utility.takeAndSaveSnapshot(api, "Area" + Arrays.toString(areaIdxs) + ".jpg", SNAP_SHOT_WAIT_TIME);

        ARTagOutput[] detections = handleARTagException(image, areaIdxs);
        // TODO: save the image here, not in the worker thread

        Work work = new Work(areaIdxs, detections, itemImages, snapPoints, areaItems, areaInfo, pointAtAstronaut,
                expansionVal, paths);
        queue.add(work);
    }

    /**
     * Move forward to retake image.
     *
     * @param move_x,     move_y, move_z: the amount of movement
     * @param quaternion: the orientation of Astrobee
     * @param Idxs:       Area indexes
     * @return detection result of ARTag Process
     * 
     *         TODO: refactor this function
     */
    private ARTagOutput retakeForward(double move_x, double move_y, double move_z, Quaternion quaternion, int[] Idxs) {
        Kinematics kinematics = api.getRobotKinematics();
        Result isMoveToSuccessResult = null;
        ARTagOutput detection = null;

        int move_count = 0;
        while (move_count < 3) {
            isMoveToSuccessResult = api.moveTo(
                    new Point(kinematics.getPosition().getX() + move_x * move_count,
                            kinematics.getPosition().getY() + move_y * move_count,
                            kinematics.getPosition().getZ() + move_z * move_count),
                    quaternion, false);
            if (!isMoveToSuccessResult.hasSucceeded()) {
                Log.i(TAG, "----------Move forward failed, retrying with theta star algorithm----------");
                Utility.processPathToTarget(api, null,
                        new Point(kinematics.getPosition().getX() + move_x * move_count,
                                kinematics.getPosition().getY() + move_y * move_count,
                                kinematics.getPosition().getZ() + move_z * move_count),
                        quaternion, expansionVal);
            }

            kinematics = api.getRobotKinematics();
            Mat image_retake = Utility.takeAndSaveSnapshot(api, "Area" + Arrays.toString(Idxs) + ".jpg",
                    SNAP_SHOT_WAIT_TIME);
            detection = ARTagProcess.process(kinematics.getPosition(), kinematics.getOrientation(), image_retake)[0];
            if (detection != null)
                break;
            move_count++;
        }
        Log.i(TAG, "move_count:" + move_count);
        return detection;
    }

    /**
     * Move to specific point to retake image.
     *
     * @param point:      target point
     * @param quaternion: the orientation of Astrobee
     * @param Idxs:       Area indexes
     * @return detection result of ARTag Process
     * 
     *         TODO: refactor this function
     */
    private ARTagOutput[] retakeMoveToPoint(Point point, Quaternion quaternion, int[] Idxs) {
        Result isMoveToSuccessResult = null;
        isMoveToSuccessResult = api.moveTo(point, quaternion, false);
        if (!isMoveToSuccessResult.hasSucceeded()) {
            Log.i(TAG, "----------Move forward failed, retrying with theta star algorithm----------");
            Utility.processPathToTarget(api, null, point, quaternion, expansionVal);
        }

        Kinematics kinematics = api.getRobotKinematics();
        Mat image_retake = Utility.takeAndSaveSnapshot(api, "Area" + Arrays.toString(Idxs) + ".jpg",
                SNAP_SHOT_WAIT_TIME);
        ARTagOutput[] detect_arr = ARTagProcess.process(kinematics.getPosition(), kinematics.getOrientation(),
                image_retake);
        return detect_arr;
    }

    /**
     * Calculate the distance of two points.
     *
     * @param image:    image from NavCam
     * @param areaIdxs: Area indexes
     * @return detection result of ARTag Process
     * 
     *         TODO: refactor this function
     */
    private ARTagOutput[] handleARTagException(Mat image, int[] areaIdxs) {
        Kinematics kinematics = api.getRobotKinematics();
        ARTagOutput[] detections = ARTagProcess.process(kinematics.getPosition(), kinematics.getOrientation(), image);

        // Handling the case when no detection is returned
        if ((Arrays.equals(areaIdxs, new int[] { 0 })) && (detections == null)) {
            Log.i(TAG, "retake image of area 0");
            detections = new ARTagOutput[1];
            detections = retakeMoveToPoint(new Point(10.9078d, -9.887558906125106d, 5.1124d),
                    new Quaternion(0f, 0f, -0.71f, 0.71f), new int[] { areaIdxs[0] });
        } else if ((Arrays.equals(areaIdxs, new int[] { 3 })) && (detections == null)) {
            Log.i(TAG, "retake image of area 3");
            detections = new ARTagOutput[1];

            if (kinematics.getPosition().getX() > 11.1) {
                detections[0] = retakeForward(-0.05, 0, 0, new Quaternion(0f, -0.71f, 0.71f, 0f), areaIdxs);
            }
        } else if (Arrays.equals(areaIdxs, new int[] { 1, 2 })) {
            // both failed
            if (detections == null) {
                // move to area1
                Log.i(TAG, "Both failed, retake image of area 1");
                detections = new ARTagOutput[2];
                ARTagOutput[] detect_arr = retakeMoveToPoint(new Point(10.8828d, -8.7924d, 4.557490723909075d),
                        new Quaternion(0.5f, 0.5f, -0.5f, 0.5f), new int[] { areaIdxs[0] });
                detections[0] = detect_arr[0];
                if (detections[0] == null) {
                    detections[0] = retakeForward(0, 0, -0.05, new Quaternion(0.5f, 0.5f, -0.5f, 0.5f),
                            new int[] { areaIdxs[0] });
                }

                // move to area2
                Log.i(TAG, "Both failed, retake image of area 2");
                detect_arr = retakeMoveToPoint(new Point(10.8828d, -7.7424d, 4.569366733183541d),
                        new Quaternion(0.5f, 0.5f, -0.5f, 0.5f), new int[] { areaIdxs[1] });
                detections[1] = detect_arr[0];
                if (detections[1] == null) {
                    detections[1] = retakeForward(0, 0, -0.05, new Quaternion(0.5f, 0.5f, -0.5f, 0.5f),
                            new int[] { areaIdxs[1] });
                }
            }
            // One in the two failed
            else if (detections.length == 1) {
                Point pos = kinematics.getPosition();
                double distance_to_area1 = Utility.calEuclideanDistance(pos.getX(), pos.getY(), pos.getZ(),
                        10.925, -8.875, 3.76203);
                double distance_to_area2 = Utility.calEuclideanDistance(pos.getX(), pos.getY(), pos.getZ(),
                        10.925, -7.925, 3.76093);

                // area1 failed, area2 success
                if ((distance_to_area1 >= distance_to_area2)) {
                    Log.i(TAG, "retake image of area 1");

                    ARTagOutput[] newDetections = new ARTagOutput[2];
                    ARTagOutput[] detect_arr = retakeMoveToPoint(new Point(10.8828d, -8.7924d, 4.557490723909075d),
                            new Quaternion(0.5f, 0.5f, -0.5f, 0.5f), new int[] { areaIdxs[0] });
                    newDetections[0] = detect_arr[0];
                    newDetections[1] = detections[0];
                    detections = newDetections;

                    if (detections[0] == null) {
                        detections[0] = retakeForward(0, 0, -0.05, new Quaternion(0.5f, 0.5f, -0.5f, 0.5f),
                                new int[] { areaIdxs[0] });
                    }
                }
                // area1 success, area2 failed
                else if (distance_to_area1 < distance_to_area2) {
                    Log.i(TAG, "retake image of area 2");

                    ARTagOutput[] newDetections = new ARTagOutput[2];
                    ARTagOutput[] detect_arr = retakeMoveToPoint(new Point(10.8828d, -7.7424d, 4.569366733183541d),
                            new Quaternion(0.5f, 0.5f, -0.5f, 0.5f), new int[] { areaIdxs[1] });
                    newDetections[0] = detections[0];
                    newDetections[1] = detect_arr[0];
                    detections = newDetections;

                    if (detections[1] == null) {
                        detections[1] = retakeForward(0, 0, -0.05, new Quaternion(0.5f, 0.5f, -0.5f, 0.5f),
                                new int[] { areaIdxs[1] });
                    }
                }
            }
        }

        return detections;
    }

    /**
     * Handle the target item.
     *
     * @param areaItem: the item detected
     */
    private void handleTarget(AreaItem areaItem) {
        if (areaItem == null) {
            Log.i(TAG, "No item detected");
            // TODO: Handle the case when no item is detected
            return;
        }

        Log.i(TAG, "Detected item: " + areaItem.getItem() + " " + areaItem.getCount());
        Integer areaIdx = areaInfo.get(areaItem.getItem());
        Log.i(TAG, "areaIdx: " + areaIdx);

        if (areaIdx == null) {
            Log.i(TAG, "Item not found in the areaInfo map");
            // TODO: Handle the case when the item is not found in the areaInfo map
            return;
        }

        Utility.processPathToTarget(api, paths[areaIdx], snapPoints[areaIdx], areaOrientations[areaIdx], expansionVal);
    }

    /**
     * Capture and detect the astronaut.
     * 
     * @return
     */
    private AreaItem captureAndDetectAstronaut() {
        AreaItem areaItem = null;
        Mat image = Utility.takeAndSaveSnapshot(api, "Astronaut.jpg", SNAP_SHOT_WAIT_TIME);
        ARTagOutput[] detections = ARTagProcess.process(pointAtAstronaut, quaternionAtAstronaut, image);

        int loopCounter = 0;
        while (loopCounter < LOOP_LIMIT && detections == null) {
            loopCounter++;
            Log.i(TAG, "Retaking image of astronaut for the " + loopCounter + " time");
            image = Utility.takeAndSaveSnapshot(api, "Astronaut.jpg", 200);
            detections = ARTagProcess.process(pointAtAstronaut, quaternionAtAstronaut, image);
        }

        if (detections != null) {
            Log.i(TAG, "Astronaut location: " + detections[0].getSnapWorld());
            api.saveMatImage(detections[0].getResultImage(), "Astronaut_result.jpg");
            areaItem = YOLOInference.getPredictions(detections[0].getResultImage());
        } else {
            // TODO: Handle the case when no detection is returned
            Log.i(TAG, "No image returned from ARTagProcess");
        }

        return areaItem;
    }

    /**
     * Process the areas.
     */
    private void processAreas() {
        Point[][] outboundTrips = {
                { new Point(10.9078d, -9.967877763897507d, 5.1124d) },
                { new Point(11.07d, -9.5d, 5.17d), new Point(10.8828d, -8.2674d, 4.719d) },
                { new Point(10.605058889481256d, -6.7699d, 4.9872000000000005d) }
        };

        int[][] areaGroups = {
                { 0 },
                { 1, 2 },
                { 3 }
        };

        for (int i = 0; i < areaGroups.length; i++) {
            Utility.logSeperator();
            Log.i(TAG, "go to area " + Arrays.toString(areaGroups[i]));

            for (int j = 0; j < outboundTrips[i].length; j++) {
                Result isMoveToSuccessResult = api.moveTo(outboundTrips[i][j], areaOrientations[areaGroups[i][0]],
                        false);
                if (!isMoveToSuccessResult.hasSucceeded()) {
                    Log.i(TAG, "Go to area " + Arrays.toString(areaGroups[i])
                            + " fail, retrying with theta star algorithm");
                    Utility.processPathToTarget(api, null, outboundTrips[i][outboundTrips[i].length - 1],
                            areaOrientations[areaGroups[i][0]], expansionVal);
                    break;
                }
            }

            processingAreaInfo(areaGroups[i]);

            Log.i(TAG, "Area " + Arrays.toString(areaGroups[i]) + " done");
            Utility.logSeperator();
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

        // Process the areas
        processAreas();

        worker.stop();

        // move to astronaut
        Result isMoveToSuccessResult = api.moveTo(pointAtAstronaut, quaternionAtAstronaut, false);
        if (!isMoveToSuccessResult.hasSucceeded()) {
            Log.i(TAG, "Go to astronaut fail, retrying with theta star algorithm");
            Utility.processPathToTarget(api, null, pointAtAstronaut, quaternionAtAstronaut, expansionVal);
        }

        try {
            workerThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // TODO: instead of saving the images here, save them in the processingAreaInfo
        for (Map.Entry<String, Mat> entry : itemImages.entrySet()) {
            api.saveMatImage(entry.getValue(), entry.getKey());
        }
        // Set the area information
        for (int i = 0; i < 4; i++) {
            api.setAreaInfo(i + 1, areaItems[i].getItem(), areaItems[i].getCount());
        }

        api.reportRoundingCompletion();

        AreaItem areaItem = captureAndDetectAstronaut();

        // Let's notify the astronaut when you recognize it.
        api.notifyRecognitionItem();

        handleTarget(areaItem);

        // Take a snapshot of the target item.
        api.takeTargetItemSnapshot();

        // The mission ends.
        Utility.logSeperator();
        Log.i(TAG, "Mission complete");
        Utility.logSeperator();
    }

    @Override
    protected void runPlan2() {
        runPlan1();
    }

    @Override
    protected void runPlan3() {
        runPlan1();
    }
}