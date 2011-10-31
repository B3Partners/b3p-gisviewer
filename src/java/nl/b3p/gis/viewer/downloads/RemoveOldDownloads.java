package nl.b3p.gis.viewer.downloads;

import java.io.File;
import java.util.Date;
import java.util.TimerTask;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Timer Task to clean up the downloads folder.
 * 
 * @author Boy
 */
public class RemoveOldDownloads extends TimerTask {
    private static final Log log = LogFactory.getLog(RemoveOldDownloads.class);
    
    private String downloadPath = null;
    private int folderAliveInSeconds = 86400;

    public RemoveOldDownloads(String path, int alive) {
        this.downloadPath = path;
        this.folderAliveInSeconds = alive;
    }
    
    @Override
    public void run() {  
        log.info("Started task RemoveOldDownloads.");
        
        Date now = new Date();        
        File workingDir = new File(downloadPath);
        
        if (workingDir.exists()) {
            if (workingDir.isDirectory()) {
                File[] files = workingDir.listFiles();
                for (int i = 0; i < files.length; i++) {                    
                    if (files[i].isDirectory()) {
                        long seconds = (now.getTime() - files[i].lastModified()) / 1000;
                        
                        log.info("Checking folder: " + files[i].toString() + " is alive: " + seconds);
                        
                        if (seconds > folderAliveInSeconds) {
                            removeFilesAndFolder(files[i]);
                        }
                    }
                }
            }
        }
    }
    
    private void removeFilesAndFolder(File folder) {
        if (folder.exists()) {
            log.info("Cleaning folder: " + folder.toString());
            
            if (folder.isDirectory()) {
                File[] files = folder.listFiles();
                for (int i = 0; i < files.length; i++) {
                    files[i].delete();
                }
            }
            
            folder.delete();
        }
    }
}