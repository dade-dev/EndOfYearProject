package msg.config;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import msg.util.LoggerUtil;

public class Config {
    private static final String CONFIG_PATH = "secretMessanger/src/msg/config/config.properties";
    private static final String PASSWORD;
    private static final byte[] SALT;

    static {
        File d=new File(CONFIG_PATH);
        if(!d.exists()){
            try {
                d.createNewFile();
            } catch (Exception e) {
            }
        }
        Properties props = new Properties();
        String pwd = null;
        byte[] saltArr = null;
        try (FileInputStream fis = new FileInputStream(CONFIG_PATH)) {
            props.load(fis);
            pwd = props.getProperty("PASSWORD");
            String[] saltStr = props.getProperty("SALT").split(",");
            saltArr = new byte[saltStr.length];
            for (int i = 0; i < saltStr.length; i++) {
                saltArr[i] = Byte.parseByte(saltStr[i].trim());
            }
        } catch (Exception e) {
            pwd="endofyearproject";
            saltArr=new byte[]{ 3, 14, 15, 9, 26, 5, 35, 89, 79, 32, 38, 46, 26, 43, 38, 32 };
            LoggerUtil.logWarning("Config", "<staticInit>", "Using standard password and salt");
        }
        PASSWORD = pwd;
        SALT = saltArr;
    }

    public static String getPassword() {
        return PASSWORD;
    }

    public static byte[] getSalt() {
        return SALT;
    }
}
