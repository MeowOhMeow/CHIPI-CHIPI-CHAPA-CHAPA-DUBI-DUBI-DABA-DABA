package main;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Log {
    static BufferedWriter writer;

    static {
        String logsDir = "./local_test/logs/";
        File dir = new File(logsDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");
        String fileName = logsDir + sdf.format(timestamp) + ".txt";
        try {
            writer = new BufferedWriter(new FileWriter(fileName));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void i(String tag, String string) {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        try {
            writer.write(timestamp + " INFO " + tag + ": " + string + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void e(String tag, String string) {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        try {
            writer.write(timestamp + " ERROR " + tag + ": " + string + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void close() {
        try {
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
