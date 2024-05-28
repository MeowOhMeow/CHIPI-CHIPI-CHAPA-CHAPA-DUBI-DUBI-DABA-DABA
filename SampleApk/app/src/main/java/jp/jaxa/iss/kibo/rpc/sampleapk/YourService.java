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

/**
 * Class meant to handle commands from the Ground Data System and execute them
 * in Astrobee.
 */

public class YourService extends KiboRpcService {
    private Point[] areaPoints = new Point[4];
    private Point[] snapPoints = new Point[4];
    private Quaternion[] areaOrientations = new Quaternion[4];
    private List<List<Point>> routes;
    private Map<String, Integer> areaInfo = new java.util.HashMap<>();

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

    private void goToTakeAPic(int areaIdx) {
        // Move to a point.
        Point point = areaPoints[areaIdx];
        Quaternion quaternion = areaOrientations[areaIdx];
        api.moveTo(point, quaternion, false);

        Kinematics kinematics = api.getRobotKinematics();
        Log.i("CHIPI-CHIPI", "Area " + areaIdx + ": " + kinematics.getPosition() + "" + kinematics.getOrientation());

        api.flashlightControlFront(0.01f);
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Get a camera image.
        Mat image = api.getMatNavCam();
        api.saveMatImage(image, "Area" + areaIdx + ".jpg");
        api.flashlightControlFront(0f);

        Log.i("CHIPI-CHIPI", "begin of ArtagProcess.process");
        ArtagOutput detection = ArtagProcess.process(kinematics.getPosition(), kinematics.getOrientation(), image);

        Log.i("CHIPI-CHIPI", "Item location: " + detection.getSnapWorld());
        api.saveMatImage(detection.getResultImage(), "Area" + areaIdx + "_result.jpg");

        snapPoints[areaIdx] = detection.getSnapWorld();

        Log.i("CHIPI-CHIPI", "begin of inference");
        AreaItem areaItem = YOLOInference.getPredictions(detection.getResultImage());
        if (areaItem.getItem() == null) {
            Log.i("CHIPI-CHIPI", "No item detected");
        } else {
            Log.i("CHIPI-CHIPI", "Detected item: " + areaItem.getItem() + " " + areaItem.getCount());
        }
        api.setAreaInfo(areaIdx + 1, areaItem.getItem(), areaItem.getCount());
        areaInfo.put(areaItem.getItem(), areaIdx);

        return;
    }

    @Override
    protected void runPlan1() {
        double[][] navCamIntrinsics = api.getNavCamIntrinsics();
        ArtagProcess.setCameraMatrix(navCamIntrinsics[0]);
        ArtagProcess.setDistortCoefficient(navCamIntrinsics[1]);
        YOLOInference.init(this.getResources());

        // The mission starts.
        api.startMission();

        Kinematics kinematics = api.getRobotKinematics();
        Log.i("CHIPI-CHIPI", "Starting point: " + kinematics.getPosition() + "" + kinematics.getOrientation());


        goToTakeAPic(0);
        // koz 1
        api.moveTo(new Point(10.56d, -9.5d, 4.62d), new Quaternion(), false);
        goToTakeAPic(1);
        // koz 2
        api.moveTo(new Point(11.15d, -8.5d, 4.62d), new Quaternion(), false);
        goToTakeAPic(2);
        // koz 3
        api.moveTo(new Point(10.56d, -7.4d, 4.62d), new Quaternion(), false);
        goToTakeAPic(3);

        // move to astronaut
        api.moveTo(new Point(11.1852d, -7.0784d, 4.8828d), new Quaternion(0.707f, 0.707f, 0f, 0f), false);

        // When you move to the front of the astronaut, report the rounding completion.
        api.reportRoundingCompletion();

        kinematics = api.getRobotKinematics();
        Log.i("CHIPI-CHIPI", "Astronaut: " + kinematics.getPosition() + "" + kinematics.getOrientation());

        api.flashlightControlFront(0.01f);
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Get a camera image.
        Mat image = api.getMatNavCam();
        api.saveMatImage(image, "Astronaut.jpg");
        api.flashlightControlFront(0f);

        Log.i("CHIPI-CHIPI", "begin of ArtagProcess.process");
        ArtagOutput detection = ArtagProcess.process(kinematics.getPosition(), kinematics.getOrientation(), image);

        api.saveMatImage(detection.getResultImage(), "Astronaut_result.jpg");

        Log.i("CHIPI-CHIPI", "begin of inference");
        AreaItem areaItem = YOLOInference.getPredictions(detection.getResultImage());
        if (areaItem.getItem() == null) {
            Log.i("CHIPI-CHIPI", "No item detected");
        }
        else {
            Log.i("CHIPI-CHIPI", "Detected item: " + areaItem.getItem() + " " + areaItem.getCount());
        }

        // Let's notify the astronaut when you recognize it.
        api.notifyRecognitionItem();

        if (areaItem.getItem() != null) {
            int areaIdx = areaInfo.get(areaItem.getItem());
            for (Point point : routes.get(areaIdx)) {
                api.moveTo(point, new Quaternion(), false);
            }
            api.moveTo(snapPoints[areaIdx], areaOrientations[areaIdx], false);
            api.flashlightControlFront(0.01f);
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Get a camera image.
            image = api.getMatNavCam();
            api.saveMatImage(image, "SnapAtArea" + (areaIdx + 1)  + ".jpg");
            api.flashlightControlFront(0f);
        }

        // Take a snapshot of the target item.
        api.takeTargetItemSnapshot();

    }

}
