package msg;
import msg.controller.Controller;
import msg.model.Model;

public class Main {
    public static void main(String[] args) {
    	Model m;
		try {
			m = new Model();
			new Controller(m);
		} catch (Exception e) {
			System.out.println("Impossible to run the application!");
		}
    }
}
