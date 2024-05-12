package jp.jaxa.iss.kibo.rpc.sampleapk;

import android.util.Log;

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
    private Quaternion[] areaQuaternions = new Quaternion[4];

    private static String MapOfItem(int index) {
        String itemName = "";
        switch (index) {
            case 0:
                itemName = "beaker";
                break;
            case 1:
                itemName = "goggle";
                break;
            case 2:
                itemName = "hammer";
                break;
            case 3:
                itemName = "kapton_tape";
                break;
            case 4:
                itemName = "pipette";
                break;
            case 5:
                itemName = "screwdriver";
                break;
            case 6:
                itemName = "thermometer";
                break;
            case 7:
                itemName = "top";
                break;
            case 8:
                itemName = "watch";
                break;
            case 9:
                itemName = "wrench";
                break;
            default:
                itemName = "Unknown";
                break;
        }
        return itemName;
    }

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

    private void goToTakeAPic(int area) {
        // Move to a point.
        Point point = areaPoints[area-1];
        Quaternion quaternion = areaQuaternions[area-1];
        api.moveTo(point, quaternion, true);

        Kinematics result = api.getRobotKinematics();
        Log.i("CHIPI-CHIPI", "Area " + area + ": " + result.getPosition() + "" + result.getOrientation());

        api.flashlightControlFront(0.01f);
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Get a camera image.
        Log.i("CHIPI-CHIPI", "begin of inference");
        Mat image = api.getMatNavCam();
        int[] count = YOLOInference.getPredictions(getResources(), image);
        for (int i = 0; i < count.length; ++i) {
            if (count[i] != 0)
                api.setAreaInfo(area, MapOfItem(i), count[i]);
        }
        Log.i("CHIPI-CHIPI", "end of inference");

        api.saveMatImage(image, "Area" + area + ".jpg");

        api.flashlightControlFront(0f);
    }

    @Override
    protected void runPlan1() {
        // The mission starts.
        api.startMission();

        Kinematics result = api.getRobotKinematics();
        Log.i("CHIPI-CHIPI", "Starting point: " + result.getPosition() + "" + result.getOrientation());

        goToTakeAPic(1);
        api.moveTo(new Point(10.56d, -9.5d, 4.62d), new Quaternion(), true);
        goToTakeAPic(2);
        api.moveTo(new Point(11.15d, -8.5d, 4.62d), new Quaternion(), true);
        goToTakeAPic(3);
        api.moveTo(new Point(10.56d, -7.4d, 4.62d), new Quaternion(), true);
        goToTakeAPic(4);

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
