package nl.b3p.gis.viewer.downloads;

import java.io.File;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Boy
 */
public class DownloadUtil {

    private static final Log log = LogFactory.getLog(DownloadUtil.class);

    public static void removeFiles(File file) {
        if (file.exists()) {
            if (file.isDirectory()) {
                File[] files = file.listFiles();
                for (int i = 0; i < files.length; i++) {
                    removeFiles(files[i]);
                }
                if (!file.delete()) {
                    log.error("can not delete file: " + file.getAbsolutePath());
                }
            } else {
                if (!file.delete()) {
                    log.error("can not delete file: " + file.getAbsolutePath());
                }
            }
        }
    }
}
