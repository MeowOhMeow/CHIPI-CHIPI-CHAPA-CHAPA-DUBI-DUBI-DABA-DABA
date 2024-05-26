package jp.jaxa.iss.kibo.rpc.sampleapk;

import android.util.Log;

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
    private Quaternion[] areaQuaternions = new Quaternion[4];

    public YourService() {
        areaPoints[0] = new Point(10.9078d, -10.0293d, 5.1124d);
        areaPoints[1] = new Point(10.8828d, -8.7924d, 4.3904d);
        areaPoints[2] = new Point(10.8828d, -7.8424d, 4.4091d);
        areaPoints[3] = new Point(10.5280d, -6.7699d, 4.9872d);
        areaQuaternions[0] = new Quaternion(0.707f, -0.707f, 0f, 0f);
        areaQuaternions[1] = new Quaternion(-0.5f, 0.5f, 0.5f, 0.5f);
        areaQuaternions[2] = new Quaternion(-0.5f, 0.5f, 0.5f, 0.5f);
        areaQuaternions[3] = new Quaternion(0f, 0.707f, 0.707f, 0f);
    }

    private void goToTakeAPic(int areaIdx) {
        // Move to a point.
        Point point = areaPoints[areaIdx];
        Quaternion quaternion = areaQuaternions[areaIdx];
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
        Log.i("CHIPI-CHIPI", "begin of inference");
        Mat image = api.getMatNavCam();
        api.saveMatImage(image, "Area" + areaIdx + ".jpg");
        api.flashlightControlFront(0f);

        ArtagOutput detection = ArtagProcess.process(kinematics.getPosition(), kinematics.getOrientation(), image);

        Log.i("CHIPI-CHIPI", "Item location: " + detection.getSnapWorld());
        api.saveMatImage(detection.getResultImage(), "Area" + areaIdx + "_result.jpg");

        snapPoints[areaIdx] = detection.getSnapWorld();

        AreaItem areaItem = YOLOInference.getPredictions(detection.getResultImage());
        if (areaItem.getItem() == null) {
            Log.i("CHIPI-CHIPI", "No item detected");
            return;
        }
        api.setAreaInfo(areaIdx + 1, areaItem.getItem(), areaItem.getCount());

        Log.i("CHIPI-CHIPI", "end of inference");

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

        // When you move to the front of the astronaut, report the rounding completion.
        api.reportRoundingCompletion();

        /* ********************************************************** */
        /* Write your code to recognize which item the astronaut has. */
        /* ********************************************************** */

        // Let's notify the astronaut when you recognize it.
        api.notifyRecognitionItem();

        /*
         * Write your code to move Astrobee to the location of the target item (what the
         * astronaut is looking for)
         */

        // Take a snapshot of the target item.
        api.takeTargetItemSnapshot();

    }

}
