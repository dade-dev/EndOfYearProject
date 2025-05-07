package msg.model;

public class Message<T> {
	private String message;
	// this might be an Image or a video or a File of whatever we wanna implement
	private final T content;

	public Message(String msg, T cnt) {
		message = msg;
		content = cnt;
	}

	public boolean haveContent() {
		return content != null;
	}

	public String getMessage() {
		return message;
	}

	public T getContent() {
		return content;
	}
	public void setMessage(String msg){
		message = msg;
	}
}
