package jp.jaxa.iss.kibo.rpc.sampleapk;

/**
 * Class to hold the information of an item in the area
 */
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

    /**
     * Constructor for the AreaItem class
     * 
     * @param itemIdx: index of the item in the itemMap
     * @param count:   count of the item
     */
    public AreaItem(int itemIdx, int count) {
        this.item = itemMap[itemIdx];
        this.count = count;
    }

    /**
     * If the item is not found
     */
    public AreaItem() {
        this.item = null;
        this.count = -1;
    }

    /**
     * Get the item name
     * 
     * @return item name
     */
    public String getItem() {
        return item;
    }

    /**
     * Get the count of the item
     * 
     * @return count of the item
     */
    public int getCount() {
        return count;
    }
}
