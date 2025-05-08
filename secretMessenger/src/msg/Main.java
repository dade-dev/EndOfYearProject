/**
 * Main class for the SecretMessenger application.
 */
package msg;

import javax.swing.SwingUtilities;
import msg.controller.Controller;
import msg.model.Model;
import msg.util.LoggerUtil;

/**
 * The entry point of the SecretMessenger application.
 * Initializes the Model and Controller, and starts the application.
 */
public class Main {
    /**
     * The main method that launches the application.
     * @param args Command line arguments (not used).
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                Model m= new Model();
                Controller c = new Controller(m);
                c.start();
            } catch (Exception e) {
                String msg = "Impossibile avviare l'applicazione!\n";
                LoggerUtil.logError("Main","main", msg , e);
                System.out.println(msg + e.getMessage());
               
            }
        });
    }
}