package nl.b3p.gis.viewer.downloads;

import java.io.File;
import java.util.HashMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Boy
*/
public class RemoveFilesWithDelayThread extends Thread {

    private static final Log log = LogFactory.getLog(RemoveFilesWithDelayThread.class);
    private File file = null;
    private long delay;
    private boolean stop = false;
    private String name = null;
    private HashMap container = null;

    public RemoveFilesWithDelayThread(String name, File file, long delay, HashMap container) {
        this.name = name;
        this.file = file;
        this.delay = delay;
        this.container = container;
    }

    @Override
    public void run() {
        if (!stop) {
            try {
                this.sleep(delay);
            } catch (InterruptedException ex) {
                log.error("Error while sleeping: " + ex);
            }
            if (!stop) {
                DownloadUtil.removeFiles(file);
                if (container.get(this.name) != null) {
                    container.remove(this.name);
                }
            }
        }
    }

    public void stopThread() {
        stop = true;
    }
}