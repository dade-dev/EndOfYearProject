package msg;

import javax.swing.SwingUtilities;

import msg.controller.Controller;
import msg.model.Model;


public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                Model model = new Model();
                new Controller(model);
            } catch (Exception e) {
                System.err.println("Impossibile avviare l'applicazione!\n" + e.getMessage());
            }
        });
    }
}