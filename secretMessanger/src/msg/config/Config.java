package msg.config;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;
import msg.util.LoggerUtil;

public class Config {
    private static final String CONFIG_FOLDER = "config";
    private static final String CONFIG_NAME = "config.properties";
    private static String PASSWORD = "endofyearproject";
    private static byte[] SALT = { 3, 14, 15, 9, 26, 5, 35, 89, 79, 32, 38, 46, 26, 43, 38, 32 };
    private static int LISTEN_PORT = 9000;

    static {
        File configDir = new File(CONFIG_FOLDER);
        if (!configDir.exists()) {
            configDir.mkdir();// mkdir for leading the user where to put the config file
        }
        File configFile = new File("" + CONFIG_FOLDER + "/" + CONFIG_NAME);
        if (!configFile.exists()) {
            LoggerUtil.logInfo("Config", "<staticInit>", "No configuration File");
        }
        Properties props = new Properties();
        String pwd;
        byte[] saltArr;
        Integer listenPort;
        try (FileInputStream fis = new FileInputStream(configFile)) {
            props.load(fis);
            pwd = props.getProperty("PASSWORD");
            String saltStr = props.getProperty("SALT");
            if (pwd == null || saltStr == null) {
                throw new Exception("Configuration file exists, but empty");
            }
            String[] saltS = saltStr.split(",");
            listenPort = Integer.valueOf(props.getProperty("LISTEN_PORT"));
            saltArr = new byte[saltS.length];
            for (int i = 0; i < saltS.length; i++) {
                saltArr[i] = Byte.parseByte(saltS[i].trim());
            }
            PASSWORD = pwd;
            SALT = saltArr;
            LISTEN_PORT = listenPort;
        } catch (Exception e) {
            LoggerUtil.logError("Config", "<staticInit>", "", e);
            LoggerUtil.logInfo("Config", "<staticInit>", "Using standard password, salt and listen port");
        }
    }

    public static String getPassword() {
        return PASSWORD;
    }

    public static byte[] getSalt() {
        return SALT;
    }

    public static int getListenPort() {
        return LISTEN_PORT;
    }
}
