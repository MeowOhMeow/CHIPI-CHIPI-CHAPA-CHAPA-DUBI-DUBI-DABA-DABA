package jp.jaxa.iss.kibo.rpc.taiwan.multithreading;

import jp.jaxa.iss.kibo.rpc.taiwan.YourService;

public class PathUpdateWork implements Runnable {
    @Override
    public void run() {
        YourService.observerImplementation
                .setExpansionVal(YourService.expansionVal);
        YourService.observerImplementation.update();
    }
}
