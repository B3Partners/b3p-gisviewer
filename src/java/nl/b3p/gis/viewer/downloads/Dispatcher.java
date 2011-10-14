package nl.b3p.gis.viewer.downloads;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Boy
*/
public class Dispatcher extends Thread{
    private final Log log = LogFactory.getLog(this.getClass());

    protected boolean dispatcherActive = false;

    private final List queue = new ArrayList();

    /**
     * used to give threads unique names
     */
    protected static int tcount = 0;

    protected long dispatcherSleepTime = 1000 * 20; // milliseconds

    protected int maxAliveThreads=5;
    /**
     * Add an object to the queue of threads to run
     * @param call the object to add
     */
    public void addCall(DownloadThread call) {
        synchronized (queue) {
            queue.add(call);
            queue.notifyAll(); //notify the running thread that the queue
            // has stuff in it now
        }
    }

    /** construct a thread in the default ThreadGroup with a few options set
     * @param isDaemon <code>true</code> means be a daemon thread,
     *     <code>false</code> means don't.
     * @param priority the value to use when calling setPriority
     * @param autoStart <code>true</code> means call this.start()
     * @see java.lang.Thread#start()
     */
    public Dispatcher(boolean isDaemon, int priority, boolean autoStart) {
        this(null, isDaemon, priority, autoStart);
    }

    /** construct a thread in the default ThreadGroup with a few options set
     * @param tg the ThreadGroup to be in
     * @param isDaemon <code>true</code> means be a daemon thread,
     *     <code>false</code> means don't.
     * @param priority the value to use when calling setPriority
     * @param autoStart <code>true</code> means call this.start()
     * @see java.lang.Thread#start()
     */
    public Dispatcher(ThreadGroup tg, boolean isDaemon, int priority, boolean autoStart) {
        super(tg, "Dispatcher");
        setDaemon(isDaemon);
        setPriority(priority);
        setDispatcherActive(true);
        if (autoStart) {
            start();
        }
    }

    /**
     * start going.  This function dequeues an item and calls run on the object that was dequeued.
     * if no object was on the queue, it waits on the queue.  this function
     * never exits.
     */
    @Override
    public void run() {
        while (isDispatcherActive()) {

            //log.debug("Restarting Dispatcher loop");

            synchronized (queue) {  //lock the queue

                //wait for an enqueue to happen
                while (queue.isEmpty() && isDispatcherActive()) {
                    dispatcherActive = false; // for monitoring
                    try {
                        queue.wait();
                    } catch (java.lang.InterruptedException e) {
                        log.error("InterruptedException waiting for queue: ", e);
                    }
                    dispatcherActive = true; // for monitoring
                }

                ArrayList removedCalls = new ArrayList();
                DownloadThread call = null;
                Iterator it = queue.iterator();
                while (it.hasNext()) {
                    call = (DownloadThread)it.next();
                        switch (call.getThreadStatus().intValue()) {
                            case DownloadThread.STATUS_NEW:
                                int activeTheads=getNumOfActiveCalls();
                                if (activeTheads<getMaxAliveThreads())
                                    call.start();
                                break;
                            case DownloadThread.STATUS_FINISHED:
                                if (!call.isAlive()){
                                    removedCalls.add(call);
                                }
                                break;
                            case DownloadThread.STATUS_ERROR:
                                if (!call.isAlive()){
                                    removedCalls.add(call);
                                }
                                break;
                            default:
                                break;
                        }

                }

                // Remove all calls from the queue that are no longer relevant
                if (!removedCalls.isEmpty()) {
                    Iterator it2 = removedCalls.iterator();
                    while (it2.hasNext()) {
                        call  = (DownloadThread)it2.next();
                        if (call.isAlive()) {
                            log.error("Running CallObject found that should be garbage collected!");
                        } else {
                            queue.remove(call);
                            call = null;
                        }
                    }
                }
            }
            try {
                this.sleep(getDispatcherSleepTime());
            } catch (InterruptedException ex) {
                log.error("Dispatcher thread interrupted: ", ex);
            }
        }
    }

    public int getNumOfActiveCalls() {
        // minus 1 to exclude the Dispatcher thread itself
        ThreadGroup tg = this.getThreadGroup();
        if (tg==null)
            return 0;
        return tg.activeCount()-1;
    }

    public boolean isDispatcherActive() {
        return dispatcherActive;
    }

    public void setDispatcherActive(boolean dispatcherActive) {
        this.dispatcherActive = dispatcherActive;
        synchronized (queue) {
            queue.notifyAll();
        }
    }

    public long getDispatcherSleepTime() {
        return dispatcherSleepTime;
    }

    public void setDispatcherSleepTime(long dispatcherSleepTime) {
        this.dispatcherSleepTime = dispatcherSleepTime;
    }

    public void setMaxAliveThreads(int maxAliveThreads){
        this.maxAliveThreads=maxAliveThreads;
    }
    public int getMaxAliveThreads(){
        return this.maxAliveThreads;
    }

    public List getQueue() {
        return queue;
    }
}