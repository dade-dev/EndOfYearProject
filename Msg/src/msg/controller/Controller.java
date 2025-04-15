package msg.controller;

import msg.view.Window;
import msg.model.Model;

public class Controller {
	private Model m;
	private Window v;
	
	
	public Controller(Model m) {
		this.m = m;
		this.v = new Window(this);
	}

}
