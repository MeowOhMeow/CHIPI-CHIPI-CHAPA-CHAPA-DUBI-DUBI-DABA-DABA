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

/**
 * Class meant to handle commands from the Ground Data System and execute them
 * in Astrobee.
 */
public class YourService extends KiboRpcService {
    private static final String TAG = "YourService";
    private static final int LOOP_LIMIT = 10;
    private static final long SNAP_SHOT_WAIT_TIME = 2000;
    private static final double STABLE_POINT_THRESHOLD = 0.4;

    private static Point[] stablePoints = { new Point(10.9922d, -9.8876d, 5.2776d),
            new Point(10.9672d, -8.9576d, 4.5575d), new Point(10.9672d, -8.0076d, 4.5693d),
            new Point(10.6925d, -6.9351d, 4.9028d) };
    private static Quaternion[] areaOrientations = { new Quaternion(0f, 0f, -0.71f, 0.71f),
            new Quaternion(0.5f, 0.5f, -0.5f, 0.5f), new Quaternion(0.5f, 0.5f, -0.5f, 0.5f),
            new Quaternion(0f, -0.71f, 0.71f, 0f) };
    private static Point[] snapPoints = new Point[4];
    private static List[] paths = new List[4];
    private static Map<String, Integer> areaInfo = new HashMap<>();
    private static AreaItem[] areaItems = new AreaItem[4];

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
    }

    /**
     * Process the area information.
     * Call function "takeAndSaveSnapshot" and add new work in queue
     *
     * @param areaIdxs: indexes of the areas to process
     */
    private void processingAreaInfo(int[] areaIdxs) {
        Mat image = Utility.takeAndSaveSnapshot(api, "Area" + Arrays.toString(areaIdxs) + ".jpg", SNAP_SHOT_WAIT_TIME);
        Kinematics kinematics = api.getRobotKinematics();
        ARTagOutput[] detections = ARTagProcess.process(kinematics.getPosition(), kinematics.getOrientation(), image);

        Set<Integer> failedIdxs = new HashSet<>();
        List<Integer> successfulIdxs = new ArrayList<>();

        initializeIndices(areaIdxs, failedIdxs);
        categorizeDetections(detections, failedIdxs, successfulIdxs);

        processSuccessfulDetections(successfulIdxs, detections);
        processFailedDetections(failedIdxs);
    }

    /**
     * Initialize the indices.
     * 
     * @param areaIdxs:   the area indices
     * @param failedIdxs: the failed indices
     */
    private void initializeIndices(int[] areaIdxs, Set<Integer> failedIdxs) {
        for (int idx : areaIdxs) {
            failedIdxs.add(idx);
        }
    }

    /**
     * Categorize the detections.
     * 
     * @param detections:     the detections
     * @param failedIdxs:     the failed indices
     * @param successfulIdxs: the successful indices
     */
    private void categorizeDetections(ARTagOutput[] detections, Set<Integer> failedIdxs, List<Integer> successfulIdxs) {
        for (ARTagOutput detection : detections) {
            Integer idx = detection.getAreaIdx();
            if (failedIdxs.remove(idx)) {
                successfulIdxs.add(idx);
            }
        }
    }

    /**
     * Process the successful detections.
     * 
     * @param successfulIdxs: the successful indices
     * @param detections:     the detections
     */
    private void processSuccessfulDetections(List<Integer> successfulIdxs, ARTagOutput[] detections) {
        for (int i = 0; i < successfulIdxs.size(); i++) {
            int areaIdx = successfulIdxs.get(i);
            ARTagOutput detection = detections[i];
            Log.i(TAG, "Item " + areaIdx + " location: " + detection.getSnapWorld());
            snapPoints[areaIdx] = detection.getSnapWorld();
            // TODO: handle the case when the ar tag it too far from the astrobee
            api.saveMatImage(detection.getResultImage(), "Area" + areaIdx + "_result.jpg");
            Work work = new Work(detection, areaItems, areaInfo, pointAtAstronaut, expansionVal, paths);
            queue.add(work);
        }
    }

    /**
     * Process the failed detections.
     * 
     * @param failedIdxs: the failed indices
     */
    private void processFailedDetections(Set<Integer> failedIdxs) {
        for (int areaIdx : failedIdxs) {
            Log.i(TAG, "Item " + areaIdx + " not detected");

            Kinematics kinematics = api.getRobotKinematics();
            if (Utility.calEuclideanDistance(kinematics.getPosition(),
                    stablePoints[areaIdx]) > STABLE_POINT_THRESHOLD) {
                Result isMoveToSuccessResult = api.moveTo(stablePoints[areaIdx], areaOrientations[areaIdx], false);
                if (!isMoveToSuccessResult.hasSucceeded()) {
                    Log.i(TAG, "Move to stable point " + areaIdx + " fail, retrying with theta star algorithm");
                    Utility.processPathToTarget(api, null, stablePoints[areaIdx], areaOrientations[areaIdx],
                            expansionVal);
                }

                handleRetakeForFailedDetection(areaIdx, kinematics);
            }
        }
    }

    /**
     * Handle the retake for failed detection.
     *
     * @param areaIdx:    the area index
     * @param kinematics: the kinematics
     */
    private void handleRetakeForFailedDetection(int areaIdx, Kinematics kinematics) {
        Mat imageRetake = Utility.takeAndSaveSnapshot(api, "Area" + areaIdx + "_stable.jpg", SNAP_SHOT_WAIT_TIME);
        ARTagOutput[] detectionsRetake = ARTagProcess.process(kinematics.getPosition(), kinematics.getOrientation(),
                imageRetake);

        ARTagOutput targetDetection = findTargetDetection(detectionsRetake, areaIdx);

        if (targetDetection != null) {
            Log.i(TAG, "Item " + areaIdx + " location: " + targetDetection.getSnapWorld());
            snapPoints[areaIdx] = targetDetection.getSnapWorld();
            // TODO: handle the case when the ar tag it too far from the astrobee
            api.saveMatImage(targetDetection.getResultImage(), "Area" + areaIdx + "_result.jpg");
            Work work = new Work(targetDetection, areaItems, areaInfo, pointAtAstronaut, expansionVal, paths);
            queue.add(work);
        } else {
            Log.i(TAG, "Item " + areaIdx + " still not detected");
        }
    }

    /**
     * Find the target detection.
     * 
     * @param detections: the detections
     * @param areaIdx:    the area index
     * @return the target detection
     */
    private ARTagOutput findTargetDetection(ARTagOutput[] detections, int areaIdx) {
        for (ARTagOutput detection : detections) {
            if (detection.getAreaIdx() == areaIdx) {
                return detection;
            }
        }
        return null;
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
     * 
     * @deprecated
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
     * 
     * @deprecated
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
     * 
     * @deprecated
     */
    private ARTagOutput[] handleARTagException(Mat image, int[] areaIdxs) {
        Kinematics kinematics = api.getRobotKinematics();
        ARTagOutput[] detections = ARTagProcess.process(kinematics.getPosition(), kinematics.getOrientation(), image);

        // Handling the case when no detection is returned
        if ((Arrays.equals(areaIdxs, new int[] { 0 })) && (detections == null)) {
            Log.i(TAG, "retake image of area 0");
            detections = new ARTagOutput[1];
            detections = retakeMoveToPoint(new Point(10.9078d, -9.887558906125106d,
                    5.1124d),
                    new Quaternion(0f, 0f, -0.71f, 0.71f), new int[] { areaIdxs[0] });
        } else if ((Arrays.equals(areaIdxs, new int[] { 3 })) && (detections == null)) {
            Log.i(TAG, "retake image of area 3");
            detections = new ARTagOutput[1];

            if (kinematics.getPosition().getX() > 11.1) {
                detections[0] = retakeForward(-0.05, 0, 0, new Quaternion(0f, -0.71f, 0.71f,
                        0f), areaIdxs);
            }
        } else if (Arrays.equals(areaIdxs, new int[] { 1, 2 })) {
            // both failed
            if (detections == null) {
                // move to area1
                Log.i(TAG, "Both failed, retake image of area 1");
                detections = new ARTagOutput[2];
                ARTagOutput[] detect_arr = retakeMoveToPoint(new Point(10.8828d, -8.7924d,
                        4.557490723909075d),
                        new Quaternion(0.5f, 0.5f, -0.5f, 0.5f), new int[] { areaIdxs[0] });
                detections[0] = detect_arr[0];
                if (detections[0] == null) {
                    detections[0] = retakeForward(0, 0, -0.05, new Quaternion(0.5f, 0.5f, -0.5f,
                            0.5f),
                            new int[] { areaIdxs[0] });
                }

                // move to area2
                Log.i(TAG, "Both failed, retake image of area 2");
                detect_arr = retakeMoveToPoint(new Point(10.8828d, -7.7424d,
                        4.569366733183541d),
                        new Quaternion(0.5f, 0.5f, -0.5f, 0.5f), new int[] { areaIdxs[1] });
                detections[1] = detect_arr[0];
                if (detections[1] == null) {
                    detections[1] = retakeForward(0, 0, -0.05, new Quaternion(0.5f, 0.5f, -0.5f,
                            0.5f),
                            new int[] { areaIdxs[1] });
                }
            }
            // One in the two failed
            else if (detections.length == 1) {
                Point pos = kinematics.getPosition();
                double distance_to_area1 = Utility.calEuclideanDistance(pos.getX(),
                        pos.getY(), pos.getZ(),
                        10.925, -8.875, 3.76203);
                double distance_to_area2 = Utility.calEuclideanDistance(pos.getX(),
                        pos.getY(), pos.getZ(),
                        10.925, -7.925, 3.76093);

                // area1 failed, area2 success
                if ((distance_to_area1 >= distance_to_area2)) {
                    Log.i(TAG, "retake image of area 1");

                    ARTagOutput[] newDetections = new ARTagOutput[2];
                    ARTagOutput[] detect_arr = retakeMoveToPoint(new Point(10.8828d, -8.7924d,
                            4.557490723909075d),
                            new Quaternion(0.5f, 0.5f, -0.5f, 0.5f), new int[] { areaIdxs[0] });
                    newDetections[0] = detect_arr[0];
                    newDetections[1] = detections[0];
                    detections = newDetections;

                    if (detections[0] == null) {
                        detections[0] = retakeForward(0, 0, -0.05, new Quaternion(0.5f, 0.5f, -0.5f,
                                0.5f),
                                new int[] { areaIdxs[0] });
                    }
                }
                // area1 success, area2 failed
                else if (distance_to_area1 < distance_to_area2) {
                    Log.i(TAG, "retake image of area 2");

                    ARTagOutput[] newDetections = new ARTagOutput[2];
                    ARTagOutput[] detect_arr = retakeMoveToPoint(new Point(10.8828d, -7.7424d,
                            4.569366733183541d),
                            new Quaternion(0.5f, 0.5f, -0.5f, 0.5f), new int[] { areaIdxs[1] });
                    newDetections[0] = detections[0];
                    newDetections[1] = detect_arr[0];
                    detections = newDetections;

                    if (detections[1] == null) {
                        detections[1] = retakeForward(0, 0, -0.05, new Quaternion(0.5f, 0.5f, -0.5f,
                                0.5f),
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
     * @return the item detected
     * 
     *         TODO: need to adjust because of the new ARTagProcess.process
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

        Log.i(TAG, "Astronaut location: " + detections[0].getSnapWorld());
        api.saveMatImage(detections[0].getResultImage(), "Astronaut_result.jpg");
        areaItem = YOLOInference.getPredictions(detections[0].getResultImage());

        return areaItem;
    }

    /**
     * Process the areas.
     */
    private void processAreas() {
        Point[][] outboundTrips = {
                { new Point(10.9922d, -9.8876d, 5.2776d) },
                { new Point(10.98d, -8.467d, 4.85d), new Point(10.9672d, -8.4326d, 4.8557d) },
                { new Point(10.6925d, -6.9351d, 4.9028d) }
        };

        int[][] areaGroups = {
                { 0 },
                { 1, 2 },
                { 3 }
        };

        for (int i = 0; i < areaGroups.length; i++) {
            Utility.logSeparator();
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
            Utility.logSeparator();
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
        Utility.logSeparator();
        Log.i(TAG, "Mission complete");
        Utility.logSeparator();
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