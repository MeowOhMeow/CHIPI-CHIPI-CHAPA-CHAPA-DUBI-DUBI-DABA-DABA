package jp.jaxa.iss.kibo.rpc.sampleapk;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Class to implement multithreading
 */
public class Worker implements Runnable {
    private BlockingQueue<Runnable> queue = null;
    private AtomicBoolean running = new AtomicBoolean(true);
    //signal that used to stop the Worker threading process
    private static final Runnable POISON_PILL = new Runnable() {
        @Override
        public void run() {
        }
    };

    public Worker(BlockingQueue<Runnable> queue) {
        this.queue = queue;
    }

    /**
     * Gracefully stop the worker thread.
     */
    public void stop() {
        running.set(false);
        queue.offer(POISON_PILL);
    }

    @Override
    public void run() {
        //parallel porcessing with Main thread and Worker thread 
        while (running.get()) {
            try {
                Runnable work = queue.take();
                if (work == POISON_PILL) {
                    break;
                }
                work.run();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        //finish the remianed task in Worker thread
        Runnable work = null;
        while ((work = queue.poll()) != null) {
            if (work != POISON_PILL) {
                work.run();
            }
        }
    }
}

    