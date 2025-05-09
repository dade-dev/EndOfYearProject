/**
 * Controller for the SecretMessenger application, handling user interactions and coordinating the model and view.
 */
package msg.controller;

import java.awt.Image;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import msg.config.Config;
import msg.model.Message;
import msg.model.Model;
import msg.net.NetworkService;
import msg.net.PeerDiscoveryService;
import msg.util.LoggerUtil;
import msg.util.NetworkUtils;
import msg.view.Window;

/**
 * The Controller class acts as the intermediary between the Model and the View.
 * It handles user input from the View, updates the Model accordingly,
 * and refreshes the View based on changes in the Model or network events.
 * It also manages network services for messaging and peer discovery.
 */
@SuppressWarnings("rawtypes")
public class Controller implements NetworkService.MessageListener, PeerDiscoveryService.DiscoveryListener {
	private final Model model;
	private final Window view;
	private final NetworkService network;
	private final PeerDiscoveryService discovery;
	private final String myIp;

	/**
	 * Constructs a new Controller.
	 * Initializes the model, view, network services, and sets up the initial state of the application.
	 * @param model The application's data model.
	 */
	public Controller(Model model) {
		this.model = model;
		this.myIp = NetworkUtils.getLocalIp(); // Get IP before view initialization

		// Initialize view *after* getting myIp
		this.view = new Window(this);

		// Discovery & network
		this.discovery = new PeerDiscoveryService(this);
		this.discovery.start();
		this.network = new NetworkService(Config.getListenPort(), this);
		this.network.start();

		// Port
		view.setStatus("In ascolto su porta " + Config.getListenPort());

		// Personal Chat setup((
		if (!model.getPeers().contains(myIp)) {
			model.addMessage(myIp, "--- Questa è la tua chat personale ---");
			model.addMessage(myIp, "--- Questo è il tuo IP: " + myIp + " ---");
		}
		model.setChatName(myIp, "Me"); // Set default name for self-chat

		initializePeerSelection();
	}

	/**
	 * Starts the application by making the main window visible and initiating peer status checking.
	 */
	public void start() {
		view.setVisible(true);
		startPeerStatusChecker();
	}

	private void initializePeerSelection() {
		// Update view after model setup
		SwingUtilities.invokeLater(() -> {
			view.setPeers(getDisplayPeers());
			// Select "Me" chat initially if peers list is not empty
			if (!model.getPeers().isEmpty()) {
				String selfDisplay = getDisplay(myIp, null);
				if (selfDisplay != null) {
					view.selectPeer(selfDisplay); // Load self-chat history
				} else if (!getDisplayPeers().isEmpty()) {
					view.selectPeer(getDisplayPeers().get(0)); // Select first peer otherwise
				}
			}
		});
	}

	private List<String> getDisplayPeers() {
		return model.getPeers().stream().map(ip -> {
			String name = model.getChatName(ip);
			// Ensure "Me" is displayed correctly without IP if name is "Me"
			if (ip.equals(myIp) && name.equals("Me")) {
				return name;
			}
			// Otherwise, show Name (IP) or just IP if no name set
			return (name != null && !name.equals(ip)) ? name + " (" + ip + ")" : ip;
		}).collect(Collectors.toList());
	}

	private String resolveIp(String display) {
		if (display == null)
			return null;
		// Handle "Me" case
		if (display.equals("Me")) {
			return myIp;
		}
		// Handle "Name (IP)" format
		if (display.contains(" (") && display.endsWith(")")) {
			int start = display.lastIndexOf(" (");
			return display.substring(start + 2, display.length() - 1);
		}
		// Assume it's just an IP otherwise
		return display;
	}

	/**
	 * Handles connection events from the NetworkService.
	 * Updates the view's status label and potentially the chat pane with connection information.
	 * @param ip The IP address of the peer involved in the event.
	 * @param connected True if the connection was established, false otherwise.
	 * @param message A descriptive message about the connection event.
	 * @param args Optional arguments, currently used to indicate if a message should be appended to the view.
	 */
	@Override
	public void onConnectionEvent(String ip, boolean connected, String message, Object... args) {
		// Update status in UI thread
		SwingUtilities.invokeLater(() -> {
			// Set status message in the UI
			view.setStatus(message);

			// Optional: you could add a system message to the chat
			if (!model.getPeers().contains(ip) && connected) {
				// If this is a new peer that we just connected to, add them
				view.setPeers(getDisplayPeers());
			} else if (model.getPeers().contains(ip)) {
				// If it's an existing peer, add connection status to their chat
				// model.addMessage(ip, "--- " + message + " ---");

				// If this chat is currently selected, also update the view
				String selectedDisplay = view.getSelectedPeer();
				String selectedIp = resolveIp(selectedDisplay);
				if (ip.equals(selectedIp)) {
					if (args.length != 0)
						view.appendText("--- " + message + " ---");
				}
			}
		});
	}

	/**
	 * Handles the action of sending a message.
	 * Retrieves the selected peer, encrypts the message, sends it via the NetworkService,
	 * and updates the model and view.
	 * @param message The message text or image command to send.
	 */
	public void onSendMessage(String message) {
		new Thread(() -> {
			final String selectedDisplay = view.getSelectedPeer(); // Get selection from view
			final String targetIp = resolveIp(selectedDisplay);

			if (message == null || message.isBlank()) {
				view.setStatus("Scrivi un messaggio!");
				return;
			}
			if (targetIp == null) {
				view.setStatus("Seleziona un peer!");
				return;
			}

			final String messageToSend = message; // Final variable for lambda/inner class

			try {
				// --- Message Handling ---
				final String prefix = "Tu: ";
				final String fullMessageText = prefix + messageToSend;
				final boolean isImage = messageToSend.startsWith("!IMG");
				byte[] imageBytes = null;
				Image imageToSend = null;

				if (isImage) {
					imageBytes = Base64.getDecoder().decode(messageToSend.substring(4));
					imageToSend = new ImageIcon(imageBytes).getImage();
				}

				// 1. Update Model & UI immediately (on EDT)
				final byte[] finalImageBytes = imageBytes; // Final for lambda
				final Image finalImageToSend = imageToSend; // Final for lambda
				SwingUtilities.invokeLater(() -> {
					if (isImage) {
						model.addMessage(targetIp, prefix, finalImageBytes);
						view.appendText(prefix); // Display "Tu: "
						view.appendImage(finalImageToSend); // Display image
					} else {
						model.addMessage(targetIp, fullMessageText); // Store text message
						view.appendText(fullMessageText); // Display text message
					}
					view.clearInput(); // Clear input after adding to UI
				});

				// 2. Encrypt and Send (only if not sending to self, or handle loopback if
				// desired)
				if (!targetIp.equals(myIp)) { // Avoid sending to self over network unless loopback is intended
					// Try to connect to peer first - only try sending if connection is successful
					if (network.connectToPeer(targetIp)) {
						byte[] encrypted = model.encrypt(messageToSend);
						String payload = Base64.getEncoder().encodeToString(encrypted);
						boolean sent = network.sendMessage(targetIp, payload);
						if (sent) {
							view.setStatus("Messaggio inviato a " + model.getChatName(targetIp));
						}
					} else {
						// Connection failed - the specific error message was already handled by
						// onConnectionEvent
						SwingUtilities.invokeLater(() -> {
							view.appendText("--- Messaggio non inviato! Peer non raggiungibile. ---");
							// model.addMessage(targetIp, "--- Messaggio non inviato! Peer non
							// raggiungibile. ---");
						});
					}
				} else {
					// Self-message
					view.setStatus("Messaggio aggiunto alla chat personale");
				}

			} catch (Exception e) {
				final String errorMsg = e.getMessage();
				SwingUtilities.invokeLater(() -> {
					view.setStatus("Errore invio: " + errorMsg);
					view.appendText("--- Errore invio: " + errorMsg + " ---");
					model.addMessage(targetIp, "--- Errore invio: " + errorMsg + " ---");
				});
				LoggerUtil.logError("Controller", "onSendMessage", "Error sending message to: " + targetIp, e);
			}
		}).start();
	}

	/**
	 * Handles the action of adding a new peer.
	 * Validates the IP address, attempts to connect, and if successful,
	 * adds the peer to the model and updates the view.
	 * @param ip The IP address of the peer to add.
	 */
	public void onAddPeer(String ip) {
		new Thread(() -> {
			if (!NetworkUtils.isValidIpAddress(ip)) {
				view.setStatus("Formato IP non valido: " + ip);
				return;
			}
			if (ip.equals(myIp)) { // Prevent adding self manually
				view.setStatus("Inserisci un IP valido (non il tuo)!");
				return;
			}

			if (!model.getPeers().contains(ip)) {
				// Try to connect to the peer with 3 attempts, 2 seconds between attempts
				boolean connected = network.connectToPeerWithRetry(ip, 3, 2000);

				if (connected) {
					// Connection successful, add peer to model
					model.addMessage(ip, "--- Conversazione iniziata ---");
					final List<String> updatedPeers = getDisplayPeers();
					final String status = "Peer aggiunto: " + ip;
					final String finalIp = ip; // for use in lambda

					SwingUtilities.invokeLater(() -> {
						view.setPeers(updatedPeers);
						view.setStatus(status);
						// Optionally, select the newly added peer
						String newPeerDisplay = getDisplay(finalIp, null);
						if (newPeerDisplay != null) {
							view.selectPeer(newPeerDisplay);
						}
					});
				} else {
					// Connection failed after retries, don't add the peer
					view.setStatus("Peer non aggiunto: impossibile connettersi a " + ip + " dopo 3 tentativi");
				}
			} else {
				// Try to verify the existing connection
				if (!network.isPeerConnected(ip) && !network.connectToPeerWithRetry(ip, 3, 2000)) {
					// Remove peer after 3 failed connection attempts
					// onRemovePeer itself should handle UI updates on EDT if necessary
					onRemovePeer(ip); // Assuming onRemovePeer correctly handles its UI updates
					view.setStatus("Peer rimosso: impossibile connettersi a " + ip);
				} else {
					view.setStatus("Peer già presente: " + ip);
				}
			}
			view.clearPeerInput();
		}).start();
	}

	/**
	 * Handles the action of removing a peer.
	 * Removes the peer from the model, network service, and updates the view.
	 * @param display The display name (which might include the IP) of the peer to remove.
	 */
	public void onRemovePeer(String display) { // Changed 'ip' to 'display' to match how it's called

		final String ipToRemove = resolveIp(display);
		if (ipToRemove == null) {
			view.setStatus("Impossibile risolvere l'IP per la rimozione.");
			return;
		}

		if (ipToRemove.equals(myIp)) {
			view.setStatus("Non puoi rimuovere la tua chat personale");
			return;
		}
		// Remove peer from model
		model.removePeer(ipToRemove);

		// Remove peer from Sockets
		network.removePeer(ipToRemove);

		// Update UI on EDT
		SwingUtilities.invokeLater(() -> {
			final List<String> updatedPeers = getDisplayPeers();
			view.setPeers(updatedPeers);
			if (updatedPeers.isEmpty()) {
				view.clearChat();
				view.setStatus("Nessun peer disponibile.");
			} else {
				view.selectPeer(updatedPeers.get(0));
				// If setPeers doesn't auto-select, and no listener is reliably selecting the
				// first, uncomment:
			}
			view.setStatus("Peer rimosso: " + display);
		});
	}

	/**
	 * Handles the action of selecting a peer from the list.
	 * Clears the current chat, loads the chat history for the selected peer,
	 * and updates the peer's online status in the view.
	 * @param display The display name (which might include the IP) of the selected peer.
	 */
	public void onPeerSelected(String display) {
		new Thread(() -> {
			final String ip = resolveIp(display);

			if (ip != null) {
				final String currentChatName = model.getChatName(ip); // Get current name
				final List<Message> chatHistory = model.getChat(ip); // Get history (might be null)

				// Verifica lo stato del peer e aggiorna l'interfaccia
				boolean online = network.isPeerOnline(ip);
				SwingUtilities.invokeLater(() -> {
					view.updatePeerStatus(online);
				});

				if (chatHistory != null) {
					for (final Message el : chatHistory) { // Make el final for lambda
						SwingUtilities.invokeLater(() -> {// Process each message on EDT
							String storedMsgText = el.getMessage(); // Original stored message text
							String displayPrefix = "";
							String displayContent = storedMsgText; // Default to full stored text
							Image imageContent = null;

							if (storedMsgText.startsWith("---")) {
								// System messages, display as is
								displayPrefix = "";
								displayContent = storedMsgText;
							} else if (el.haveContent() && el.getContent() instanceof Image) {
								// It's an image message, stored prefix is in storedMsgText
								displayPrefix = storedMsgText; // The stored text *is* the prefix ("Tu: " or
																// "PeerName:")
								displayContent = ""; // No text content to display besides prefix
								imageContent = (Image) el.getContent();
							} else if (storedMsgText.startsWith("Tu: ")) {
								displayPrefix = "Tu: ";
								displayContent = storedMsgText.substring(4);
							} else if (storedMsgText.contains(": ")) { // Check if it's a peer message
								int colonPos = storedMsgText.indexOf(": ");
								// Use the *current* chat name for the prefix when displaying
								String peerPrefixPart = (currentChatName != null && !currentChatName.equals(ip))
										? currentChatName
										: ip;
								displayPrefix = peerPrefixPart + ": ";
								displayContent = storedMsgText.substring(colonPos + 2);
							}
							// else: handle other cases or display raw storedMsgText if needed

							// Append to view
							if (imageContent != null) {
								view.appendText(displayPrefix); // Display prefix
								view.appendImage(imageContent); // Display image
							} else if (!displayContent.trim().isEmpty() || displayContent.startsWith("---")) {
								// Display text messages or system messages
								view.appendText(displayPrefix + displayContent);
							}
						});
					}
				} else {
					// Handle case where chat history is unexpectedly null (e.g., after adding peer
					// but before first message)
					model.addMessage(ip, "--- Chat creata ---"); // Add initial message if missing
					view.appendText("--- Chat creata ---");
				}
				// Update status after processing all messages (or creating chat)
				final String statusUpdate = "Chat caricata: " + ((currentChatName != null) ? currentChatName : ip);
				view.setStatus(statusUpdate);

			} else {
				view.setStatus("Nessun peer selezionato");
			}
		}).start();
	}

	/**
	 * Handles the action of renaming a chat.
	 * Updates the chat name in the model and refreshes the peer list in the view.
	 * @param display The current display name (which might include the IP) of the chat to rename.
	 * @param newName The new name for the chat.
	 */
	public void onRenameChat(String display, String newName) {
		if (display == null || newName == null || newName.isBlank()) {
			view.setStatus("Nome non valido.");
			return;
		}

		final String ip = resolveIp(display);
		if (ip == null) {
			view.setStatus("Seleziona una chat valida da rinominare.");
			return;
		}
		// Prevent renaming self if "Me" is used
		if (ip.equals(myIp) && display.equals("Me")) {
			view.setStatus("Non puoi rinominare la chat 'Me'.");
			return;
		}

		model.setChatName(ip, newName); // Update name in model

		// Update UI on EDT
		SwingUtilities.invokeLater(() -> {
			final List<String> updatedPeers = getDisplayPeers(); // Get updated list
			view.setPeers(updatedPeers); // Update the list in the view
			view.selectPeer(getDisplay(ip, ip));
			view.setStatus("Chat rinominata: " + newName);
		});
	}

	private String getDisplay(String ip, String elses) {
		return getDisplayPeers().stream().filter(p -> resolveIp(p).equals(ip)).findFirst().orElse(elses);
	}

	/**
	 * Handles incoming messages from the NetworkService.
	 * Decrypts the message, adds it to the model, and updates the view if the sender's chat is active.
	 * @param senderIp The IP address of the message sender.
	 * @param base64Message The Base64 encoded and encrypted message content.
	 */
	@Override
	public void onMessageReceived(String senderIp, String base64Message) {
		new Thread(() -> {
			try {
				byte[] encrypted = Base64.getDecoder().decode(base64Message);
				String decryptedMsg = model.decrypt(encrypted); // The actual message content or "!IMG"+base64img

				final String currentName = model.getChatName(senderIp); // Use current name
				final String displayPrefix = (currentName != null) ? currentName + ": " : senderIp + ": "; // Prefix for
																											// display
																											// AND
																											// storage

				if (decryptedMsg.startsWith("!IMG")) {
					byte[] imgBytes = Base64.getDecoder().decode(decryptedMsg.substring(4));
					final Image receivedImage = new ImageIcon(imgBytes).getImage(); // Final for lambda

					// Store image with display prefix
					model.addMessage(senderIp, displayPrefix, imgBytes);

					// Update UI on EDT only if the chat is currently selected
					SwingUtilities.invokeLater(() -> {
						final String selectedPeerDisplay = view.getSelectedPeer();
						final String selectedPeerIp = resolveIp(selectedPeerDisplay);
						if (senderIp.equals(selectedPeerIp)) { // Only append if this chat is active
							view.appendText(displayPrefix);
							view.appendImage(receivedImage);
							view.setStatus("Immagine ricevuta da " + (currentName != null ? currentName : senderIp));
						}
					});
				} else {
					// Store text message with display prefix
					model.addMessage(senderIp, displayPrefix + decryptedMsg);

					// Update UI on EDT only if the chat is currently selected
					SwingUtilities.invokeLater(() -> {
						String selectedPeerDisplay = view.getSelectedPeer();
						String selectedPeerIp = resolveIp(selectedPeerDisplay);
						if (senderIp.equals(selectedPeerIp)) { // Only append if this chat is active
							view.appendText(displayPrefix + decryptedMsg);
						}
						view.setStatus("Messaggio ricevuto da " + (currentName != null ? currentName : senderIp));
					});
				}
			} catch (Exception e) {
				final String errorMsg = e.getMessage();
				view.setStatus("Errore ricezione: " + errorMsg);
				LoggerUtil.logError("Controller", "onMessageReceived",
						"Error processing received message from: " + senderIp, e);
			}
		}).start();
	}

	/**
	 * Handles peer discovery events from the PeerDiscoveryService.
	 * Adds newly discovered peers to the model and updates the view.
	 * @param ip The IP address of the discovered peer.
	 */
	@Override
	public void onPeerDiscovered(String ip) {
		new Thread(() -> {
			if (!ip.equals(myIp) && !model.getPeers().contains(ip)) { // Don't discover self, check if already known
				model.addMessage(ip, "--- Peer trovato in rete ---");
				final List<String> updatedPeers = getDisplayPeers();
				final String status = "Peer trovato: " + ip;
				SwingUtilities.invokeLater(() -> {
					view.setPeers(updatedPeers);
					view.setStatus(status);
				});
			}
		}).start();
	}

	/**
	 * Handles changes in peer online status from the NetworkService.
	 * Updates the view if the affected peer's chat is currently selected.
	 * @param ip The IP address of the peer whose status changed.
	 * @param online True if the peer is now online, false otherwise.
	 */
	@Override
	public void onPeerStatusChange(String ip, boolean online) {
		SwingUtilities.invokeLater(() -> {
			// Aggiorna l'interfaccia utente solo se il peer corrente è quello selezionato
			String currentPeer = view.getSelectedPeer();
			if (currentPeer != null) {
				String currentIp = resolveIp(currentPeer);
				if (currentIp != null && currentIp.equals(ip)) {
					view.updatePeerStatus(online);
				}
			}
		});
	}

	/**
	 * Checks if a peer is online and updates their status in the view.
	 * @param displayName The display name (which might include the IP) of the peer to check.
	 * @return True if the peer is online, false otherwise.
	 */
	public boolean isPeerOnline(String displayName) {
		String ip = resolveIp(displayName);
		if (ip != null) {
			boolean online = network.isPeerOnline(ip);
			SwingUtilities.invokeLater(() -> {
				view.updatePeerStatus(online);
			});
			return online;
		}
		return false;
	}

	private void startPeerStatusChecker() {
		new Thread(() -> {
       			while(true){
            		try{
						Thread.sleep(5000);
						String selectedPeer = view.getSelectedPeer();
			            if (selectedPeer != null) {
				            isPeerOnline(selectedPeer);
			            }
           			}catch(InterruptedException e){}
       			}
		}).start(); 
	}
}