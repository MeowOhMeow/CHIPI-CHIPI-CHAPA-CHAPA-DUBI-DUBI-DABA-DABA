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

import jp.jaxa.iss.kibo.rpc.taiwan.multithreading.*;
import jp.jaxa.iss.kibo.rpc.taiwan.multithreading.Publisher.*;

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
    private static Map<String, Integer> areaInfo = new HashMap<>();
    private static AreaItem[] areaItems = new AreaItem[4];

    // ! WARN Test expansionVal = 0.02
    public static double expansionVal = 0.08;
    private static Point pointAtAstronaut = new Point(11.1852d, -6.7607d, 4.8828d);
    private static Quaternion quaternionAtAstronaut = new Quaternion(0.707f, 0.707f, 0f, 0f);

    public static final BlockingQueue<Runnable> worksQueue = new LinkedBlockingQueue<>();
    private static Worker worker = new Worker(worksQueue);
    private static Thread workerThread = new Thread(worker);

    private static Map<String, Element<Path>> observerElements = new HashMap<>();
    public static Implementation observerImplementation = new Implementation(observerElements, expansionVal);

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

        // TODO: handle the case when the ar tag is not valid
        initializeIndices(areaIdxs, failedIdxs);
        categorizeDetections(detections, failedIdxs, successfulIdxs);

        processSuccessfulDetections(successfulIdxs, detections);
        // TODO: test failed detections
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
            PathCalWork work = new PathCalWork(detection, areaItems, areaInfo, pointAtAstronaut, expansionVal);
            worksQueue.add(work);
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
                    Utility.processPathToTarget(api, null, stablePoints[areaIdx], areaOrientations[areaIdx]);
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
            PathCalWork work = new PathCalWork(targetDetection, areaItems, areaInfo, pointAtAstronaut, expansionVal);
            worksQueue.add(work);
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

        List<Point> points = (observerElements.get(String.valueOf(areaIdx))).getData().getPoints();
        Utility.processPathToTarget(api, points, snapPoints[areaIdx], areaOrientations[areaIdx]);
    }

    /**
     * Capture and detect the astronaut.
     * 
     * @return the item detected
     */
    private AreaItem captureAndDetectAstronaut() {
        AreaItem areaItem;
        Mat image = Utility.takeAndSaveSnapshot(api, "Astronaut.jpg", SNAP_SHOT_WAIT_TIME);
        ARTagOutput[] detections = ARTagProcess.process(pointAtAstronaut, quaternionAtAstronaut, image);

        int loopCounter = 0;
        while (loopCounter < LOOP_LIMIT && detections.length == 0) {
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
                { new Point(10.9672d, -8.4326d, 4.8557d) },
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
                            areaOrientations[areaGroups[i][0]]);
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
        // The mission starts.
        api.startMission();

        double[][] navCamIntrinsics = api.getNavCamIntrinsics();
        ARTagProcess.setCameraMatrix(navCamIntrinsics[0]);
        ARTagProcess.setDistortCoefficient(navCamIntrinsics[1]);
        YOLOInference.init(this.getResources());
        workerThread.start();

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
            Utility.processPathToTarget(api, null, pointAtAstronaut, quaternionAtAstronaut);
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