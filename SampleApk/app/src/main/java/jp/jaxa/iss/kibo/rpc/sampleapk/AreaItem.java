package jp.jaxa.iss.kibo.rpc.sampleapk;

public class AreaItem {
    private static String[] itemMap = {
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

    private String item;
    private int count;

    public AreaItem(int itemIdx, int count) {
        this.item = itemMap[itemIdx];
        this.count = count;
    }

    public AreaItem() {
        this.item = null;
        this.count = -1;
    }

    public String getItem() {
        return item;
    }

    public int getCount() {
        return count;
    }
}
