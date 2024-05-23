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
    private Quaternion[] areaQuaternions = new Quaternion[4];
    private String[] itemMap = {
            "beaker",
            "goggle",
            "hammer",
            "kapton_tape",
            "pipette",
            "screwdriver",
            "thermometer",
            "top",
            "watch",
            "wrench" };

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

    private Mat goToTakeAPic(int area) {
        // Move to a point.
        Point point = areaPoints[area - 1];
        Quaternion quaternion = areaQuaternions[area - 1];
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
        /*int[] count = YOLOInference.getPredictions(image);
        for (int i = 0; i < count.length; ++i) {
            if (count[i] != 0)
                api.setAreaInfo(area, itemMap[i], count[i]);
        }
        */
        Log.i("CHIPI-CHIPI", "end of inference");

        api.saveMatImage(image, "Area" + area + ".jpg");

        api.flashlightControlFront(0f);
        return image;
    }

    @Override
    protected void runPlan1() {
        //YOLOInference.init(this.getResources());

        // The mission starts.
        api.startMission();

        Kinematics result = api.getRobotKinematics();
        Log.i("CHIPI-CHIPI", "Starting point: " + result.getPosition() + "" + result.getOrientation());
        /////////////////
        Mat img1=goToTakeAPic(1);
        double[] ans=ArtagProcess.process(img1,1);
        Point snap=new Point(ans[0],ans[1],ans[2]);

        //api.moveTo(snap, areaQuaternions[0], true);
        Mat distort = api.getMatNavCam();
        api.saveMatImage(distort, "snap1"  + ".jpg");
        /////////////////
        api.moveTo(areaPoints[1], areaQuaternions[1], true);
        Mat distort1 = api.getMatNavCam();
        api.saveMatImage(distort1, "cam2"  + ".jpg");
        Mat img2=goToTakeAPic(2);
        ans=ArtagProcess.process(img2,2);
        snap=new Point(ans[0],ans[1],ans[2]);

        api.moveTo(snap, areaQuaternions[1], true);
        distort = api.getMatNavCam();
        api.saveMatImage(distort, "snap2"  + ".jpg");
        /*
        /////////////////
        api.moveTo(areaPoints[2], areaQuaternions[2], true);
        distort = api.getMatNavCam();
        api.saveMatImage(distort, "cam3"  + ".jpg");
        Mat img3=goToTakeAPic(3);
        ans=ArtagProcess.process(img3,3);
        snap=new Point(ans[0],ans[1],ans[2]);

        api.moveTo(snap, areaQuaternions[2], true);
        distort = api.getMatNavCam();
        api.saveMatImage(distort, "snap3"  + ".jpg");
        ///////////////////
        api.moveTo(areaPoints[3], areaQuaternions[3], true);
        distort = api.getMatNavCam();
        api.saveMatImage(distort, "cam4"  + ".jpg");
        Mat img4=goToTakeAPic(4);
        ans=ArtagProcess.process(img4,4);
        snap=new Point(ans[0],ans[1],ans[2]);

        api.moveTo(snap, areaQuaternions[3], true);
        distort = api.getMatNavCam();
        api.saveMatImage(distort, "snap4"  + ".jpg");
        */

        /* 
        // intersecting point
        api.moveTo(new Point(10.56d, -9.5d, 4.62d), new Quaternion(), true);
        goToTakeAPic(2);
        // intersecting point
        api.moveTo(new Point(11.15d, -8.5d, 4.62d), new Quaternion(), true);
        goToTakeAPic(3);
        // intersecting point
        api.moveTo(new Point(10.56d, -7.4d, 4.62d), new Quaternion(), true);
        goToTakeAPic(4);
        */
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
