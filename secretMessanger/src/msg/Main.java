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
                String msg = "Impossibile avviare l'applicazione!\n";
                LoggerUtil.logError("Main","main", msg , e);
                System.out.println(msg + e.getMessage());
               
            }
        });
    }
}