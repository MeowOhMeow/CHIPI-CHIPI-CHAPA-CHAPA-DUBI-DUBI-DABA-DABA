package jp.jaxa.iss.kibo.rpc.taiwan;

import android.util.Log;

import org.opencv.core.Mat;

import java.nio.file.Path;
import java.util.List;

import gov.nasa.arc.astrobee.Kinematics;
import gov.nasa.arc.astrobee.types.Point;
import gov.nasa.arc.astrobee.types.Quaternion;
import gov.nasa.arc.astrobee.Result;
import jp.jaxa.iss.kibo.rpc.api.KiboRpcApi;
import jp.jaxa.iss.kibo.rpc.taiwan.multithreading.PathUpdateWork;
import jp.jaxa.iss.kibo.rpc.taiwan.pathfinding.PathFindingAPI;

/**
 * Utility class for common functions
 */
public class Utility {
    private static final String TAG = "Utility";
    private static final int LOOP_LIMIT = 10;
    private static final double INCREMENT = 0.02;
    private static List<Point> tempPath = null;

    /**
     * Get robot kinematics
     * 
     * @param api: KiboRpcApi object
     * @return Robot kinematics
     */
    public static Kinematics getRobotKinematics(KiboRpcApi api) {
        Kinematics kinematics = null;
        int loopCounter = 0;
        while (kinematics == null && loopCounter < LOOP_LIMIT) {
            try {
                kinematics = api.getRobotKinematics();
            } catch (Exception e) {
                Log.i(TAG, "Catch exception using 'api.getRobotKinematics()': " + e.getMessage());
            }
            loopCounter++;
        }
        if (kinematics == null) {
            Log.i(TAG, "Failed to get robot kinematics");
            return null;
        }
        return kinematics;
    }

    /**
     * Sleep for a given number of milliseconds
     * 
     * @param millis: Number of milliseconds to sleep
     */
    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Calculate Euclidean distance between two points
     * 
     * @param p1: First point
     * @param p2: Second point
     * @return Euclidean distance between the two points
     */
    public static double calEuclideanDistance(Point p1, Point p2) {
        return calEuclideanDistance(p1.getX(), p1.getY(), p1.getZ(), p2.getX(), p2.getY(), p2.getZ());
    }

    /**
     * Calculate Euclidean distance between two points
     * 
     * @param x1: X coordinate of the first point
     * @param y1: Y coordinate of the first point
     * @param z1: Z coordinate of the first point
     * @param x2: X coordinate of the second point
     * @param y2: Y coordinate of the second point
     * @param z2: Z coordinate of the second point
     * @return Euclidean distance between the two points
     */
    public static double calEuclideanDistance(double x1, double y1, double z1, double x2, double y2, double z2) {
        return Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2) + Math.pow(z1 - z2, 2));
    }

    /**
     * Process the path to the target point
     * 
     * @param api:               KiboRpcApi object
     * @param path:              Path to the target point
     * @param targetPoint:       Target point
     * @param targetOrientation: Target orientation
     * @return true if the robot successfully moves to the target point, false
     */
    public static boolean processPathToTarget(
            KiboRpcApi api, List<Point> path, Point targetPoint, Quaternion targetOrientation) {
        if (path == null) {
            Log.i(TAG, "Path is null");
            Kinematics kinematics = getRobotKinematics(api);
            if (kinematics == null) {
                return false;
            }
            tempPath = PathFindingAPI.findPath(kinematics.getPosition(),
                    targetPoint,
                    YourService.expansionVal);
        } else {
            tempPath = path;
        }
        double preExpVal = YourService.expansionVal;
        PathFindingAPI.logPoints(tempPath, "Path to the target point");

        boolean pathSuccess = false;
        int loopCounter = 0;

        while (!pathSuccess && loopCounter < LOOP_LIMIT) {
            pathSuccess = moveToPathPoints(api, targetPoint, targetOrientation);
            loopCounter++;
        }
        if (!pathSuccess) {
            Log.i(TAG, "Failed to move to the path points");
            Log.i(TAG, "Resetting expansionVal to: " + preExpVal);
            YourService.expansionVal = preExpVal;
            PathUpdateWork work = new PathUpdateWork();
            YourService.worksQueue.add(work);
            return false;
        }

        Log.i(TAG, "Arrive at the target point");
        return true;
    }

    /**
     * Move to the path points
     * 
     * @param api:               KiboRpcApi object
     * @param targetPoint:       Target point
     * @param targetOrientation: Target orientation
     * @return true if the robot successfully moves to the path points, false
     *         otherwise
     */
    private static boolean moveToPathPoints(KiboRpcApi api, Point targetPoint, Quaternion targetOrientation) {
        Result result = null;

        for (Point point : tempPath) {
            result = api.moveTo(point, targetOrientation, false);

            if (!result.hasSucceeded()) {
                YourService.expansionVal += INCREMENT;

                PathUpdateWork work = new PathUpdateWork();
                YourService.worksQueue.add(work);

                Log.i(TAG, "Path corrupt, increasing expansionVal to: " + YourService.expansionVal);

                tempPath = PathFindingAPI.findPath(api.getRobotKinematics().getPosition(),
                        targetPoint,
                        YourService.expansionVal);

                PathFindingAPI.logPoints(tempPath, "Path after increasing expansionVal");
                return false;
            }
        }

        return result != null && result.hasSucceeded();
    }

    /**
     * Take a snapshot and save it.
     *
     * @param name:     name of the image
     * @param waitTime: time to wait before taking the snapshot. unit: ms
     * @return the snapshot image
     */
    public static Mat takeAndSaveSnapshot(KiboRpcApi api, String name, long waitTime) {
        {
            Result result = api.flashlightControlFront(0.01f);
            int loopCounter = 0;
            while (!result.hasSucceeded() && loopCounter < LOOP_LIMIT) {
                result = api.flashlightControlFront(0.01f);
                loopCounter++;
            }
            if (!result.hasSucceeded()) {
                Log.i(TAG, "Failed to turn on the flashlight");
                return null;
            }
        }
        sleep(waitTime);
        Mat image = null;
        {
            boolean success = false;
            int loopCounter = 0;
            while (!success && loopCounter < LOOP_LIMIT) {
                try {
                    image = api.getMatNavCam();
                    if (image != null)
                        success = true;
                } catch (Exception e) {
                    Log.i(TAG, "Catch exception using 'api.getMatNavCam()': " + e.getMessage());
                }
                loopCounter++;
            }
            if (!success) {
                Log.i(TAG, "Failed to get the image");
                return null;
            }
        }
        api.saveMatImage(image, name);
        {
            Result result = api.flashlightControlFront(0f);
            int loopCounter = 0;
            while (!result.hasSucceeded() && loopCounter < LOOP_LIMIT) {
                result = api.flashlightControlFront(0f);
                loopCounter++;
            }
            if (!result.hasSucceeded()) {
                Log.i(TAG, "Failed to turn off the flashlight");
            }
        }
        return image;
    }

    /**
     * Log a separator
     */
    public static void logSeparator() {
        Log.i(TAG, "----------------------------------------");
    }
}
