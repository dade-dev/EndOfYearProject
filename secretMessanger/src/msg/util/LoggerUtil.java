package msg.util;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Utility class to handle logging to a file for the application.
 */
public class LoggerUtil {
    private static final String LOG_FOLDER = "secretMessanger/logs";
    private static final String LOG_FILE_NAME = "secretMessenger.log";
    private static Logger logger;
    
    static {
        try {
            // Create logs directory if it doesn't exist
            File logDir = new File(LOG_FOLDER);
            if (!logDir.exists()) {
                logDir.mkdir();
            }
            
            // Configure the logger
            logger = Logger.getLogger("SecretMessengerLogger");
            
            // Set up the file handler to write to the log file
            FileHandler fileHandler = new FileHandler(LOG_FOLDER + "/" + LOG_FILE_NAME, false);
            fileHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(fileHandler);
            
            // Don't let the logger use parent handlers
            logger.setUseParentHandlers(false);
            
            logger.info("Logger initialized at " + getCurrentTime());
        } catch (IOException e) {
            System.err.println("Failed to initialize logger: " + e.getMessage());
            e.printStackTrace(); // Use standard printStackTrace just for logger initialization errors
        }
    }
    
    /**
     * Logs an error message with exception details.
     * 
     * @param className The name of the class where the error occurred
     * @param methodName The name of the method where the error occurred
     * @param message Additional context about the error
     * @param throwable The exception to log
     */
    public static void logError(String className, String methodName, String message, Throwable throwable) {
        String logMessage = String.format("[%s] Error in %s.%s: %s - %s: %s", 
                getCurrentTime(), className, methodName, message, 
                throwable.getClass().getName(), throwable.getMessage());
        
        logger.log(Level.SEVERE, logMessage, throwable);
    }
    
    /**
     * Logs a warning message.
     * 
     * @param className The name of the class where the warning occurred
     * @param methodName The name of the method where the warning occurred
     * @param message The warning message
     */
    public static void logWarning(String className, String methodName, String message) {
        String logMessage = String.format("[%s] Warning in %s.%s: %s", 
                getCurrentTime(), className, methodName, message);
        
        logger.log(Level.WARNING, logMessage);
    }
    
    /**
     * Logs an informational message.
     * 
     * @param className The name of the class generating the info
     * @param methodName The name of the method generating the info
     * @param message The info message
     */
    public static void logInfo(String className, String methodName, String message) {
        String logMessage = String.format("[%s] Info in %s.%s: %s", 
                getCurrentTime(), className, methodName, message);
        
        logger.log(Level.INFO, logMessage);
    }
    
    /**
     * Gets the current time formatted for logging.
     * 
     * @return Formatted current time
     */
    private static String getCurrentTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return dateFormat.format(new Date());
    }
}