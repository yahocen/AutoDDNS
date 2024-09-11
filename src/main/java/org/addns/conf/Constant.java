package org.addns.conf;

import org.addns.App;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author YahocenMiniPC
 */
public class Constant {

    public static final String NAME = "AutoDDNS";

    public static final String APP_BIN_NAME;

    public static final String APP_BIN_PATH;

    public static final String CONFIG_PATH;

    public static final File V4_FILE;

    public static final File V6_FILE;

    public static final String LOG_PATH;

    public static final String LOG_NAME;

    static {
        try {
            //https://stackoverflow.com/questions/77719096/get-directory-of-compiled-native-image-by-graalvm-at-runtime
            Path path = Path.of(App.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            APP_BIN_NAME = path.getFileName().toString();
            APP_BIN_PATH = path.getParent().toString();
            CONFIG_PATH = Paths.get(APP_BIN_PATH, "conf", "config.json").toString();
            V4_FILE = Paths.get(APP_BIN_PATH, "data", "ipv4.txt").toFile();
            V6_FILE = Paths.get(APP_BIN_PATH, "data", "ipv6.txt").toFile();
            LOG_PATH = Paths.get(APP_BIN_PATH, "log").toString();
            LOG_NAME = Paths.get(APP_BIN_PATH, "log", NAME + ".log").toString();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public static final String DOMAIN_KEY = "domain";

    public static final String DNS_KEY = "dns";

    public static final String MODE_KEY = "mode";

    public static final String MODE_V4 = "v4";

    public static final String MODE_V6 = "v6";

    public static final int LOG_LIMIT = 3145728;

    public static final int LOG_COUNT = 10;

}
