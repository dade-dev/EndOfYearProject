package msg.model;

/**
 * Represents a message in a chat. A message has a textual part and can optionally
 * contain other content, such as an image.
 * @param <T> The type of the optional content (e.g., java.awt.Image).
 */
public class Message<T> {
	private String message;
	// this might be an Image or a video or a File of whatever we wanna implement
	private final T content;

	/**
	 * Constructs a new Message.
	 * @param msg The textual part of the message.
	 * @param cnt The optional content of the message (can be null).
	 */
	public Message(String msg, T cnt) {
		message = msg;
		content = cnt;
	}

	/**
	 * Checks if this message has additional content beyond text.
	 * @return True if there is content, false otherwise.
	 */
	public boolean haveContent() {
		return content != null;
	}

	/**
	 * Gets the textual part of the message.
	 * @return The message string.
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * Gets the additional content of the message.
	 * @return The content of type T, or null if there is no additional content.
	 */
	public T getContent() {
		return content;
	}

	/**
	 * Sets the textual part of the message.
	 * @param msg The new message string.
	 */
	public void setMessage(String msg){
		message = msg;
	}
}
