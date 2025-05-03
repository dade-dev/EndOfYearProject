package msg;

import javax.swing.SwingUtilities;
import msg.controller.Controller;
import msg.model.Model;
import msg.util.LoggerUtil;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                Model m= new Model();
                Controller c = new Controller(m);
                c.start();
            } catch (Exception e) {
                LoggerUtil.logError("Main","main", "Impossibile avviare l'applicazione!\n", e);
                e.printStackTrace();
            }
        });
    }
}