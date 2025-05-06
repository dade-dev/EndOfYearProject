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

@SuppressWarnings("rawtypes")
public class Controller implements NetworkService.MessageListener, PeerDiscoveryService.DiscoveryListener {
    private final Model model;
    private final Window view;
    private final NetworkService network;
    private final PeerDiscoveryService discovery;
    private final String myIp;

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
            model.addMessage(myIp,"--- Questo è il tuo IP: "+ myIp +" ---");
        }
        model.setChatName(myIp, "Me"); // Set default name for self-chat

        initializePeerSelection();
    }
    public void start() {
    	view.setVisible(true);
    }
    private void initializePeerSelection(){
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
        return model.getPeers().stream()
                .map(ip -> {
                    String name = model.getChatName(ip);
                    // Ensure "Me" is displayed correctly without IP if name is "Me"
                    if (ip.equals(myIp) && name.equals("Me")) {
                        return name;
                    }
                    // Otherwise, show Name (IP) or just IP if no name set
                    return (name != null && !name.equals(ip)) ? name + " (" + ip + ")" : ip;
                })
                .collect(Collectors.toList());
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

    @Override
    public void onConnectionEvent(String ip, boolean connected, String message) {
        // Update status in UI thread
        SwingUtilities.invokeLater(() ->{
            // Set status message in the UI
            view.setStatus(message);

            // Optional: you could add a system message to the chat
            if (!model.getPeers().contains(ip) && connected) {
                // If this is a new peer that we just connected to, add them
                model.addMessage(ip, "--- Peer connesso: " + ip + " ---");
                view.setPeers(getDisplayPeers());
            } else if (model.getPeers().contains(ip)) {
                // If it's an existing peer, add connection status to their chat
                model.addMessage(ip, "--- " + message + " ---");

                // If this chat is currently selected, also update the view
                String selectedDisplay = view.getSelectedPeer();
                String selectedIp = resolveIp(selectedDisplay);
                if (ip.equals(selectedIp)) {
                    view.appendText("--- " + message + " ---");
                }
            }
        });
    }

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
                            model.addMessage(targetIp, "--- Messaggio non inviato! Peer non raggiungibile. ---");
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
                    model.addMessage(targetIp,"--- Errore invio: " + errorMsg + " ---");
                });
                LoggerUtil.logError("Controller", "onSendMessage", "Error sending message to: " + targetIp, e);
            }
        }).start();
    }

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
                    
                    view.setPeers(updatedPeers);
                    view.setStatus(status);
                } else {
                    // Connection failed after retries, don't add the peer
                	view.setStatus("Peer non aggiunto: impossibile connettersi a " + ip + " dopo 3 tentativi");
                }
            } else {
                // Try to verify the existing connection
                if (!network.isPeerConnected(ip) && !network.connectToPeerWithRetry(ip, 3, 2000)) {
                    // Remove peer after 3 failed connection attempts
                    onRemovePeer(ip);
                    view.setStatus("Peer rimosso: impossibile connettersi a " + ip);
                } else {
                    view.setStatus("Peer già presente: " + ip);
                }
            }
            view.clearPeerInput();
        }).start();
    }

    public void onRemovePeer(String ip) {

        final String currentlySelectedIp = resolveIp(ip);
        if (currentlySelectedIp.equals(myIp)) {
            view.setStatus("Non puoi rimuovere la tua chat personale");
            return;
        }
        // Remove peer from model
        model.removePeer(currentlySelectedIp);

        // Remove peer from Sockets
        network.removePeer(currentlySelectedIp);

        // Update UI
        final List<String> updatedPeers = getDisplayPeers();
        view.setPeers(updatedPeers);
    }

    public void onPeerSelected(String display) {
        new Thread(() -> {
            final String ip = resolveIp(display);
            view.clearChat();

            if (ip != null) {
                final String currentChatName = model.getChatName(ip); // Get current name
                final List<Message> chatHistory = model.getChat(ip); // Get history (might be null)

                if (chatHistory != null) {
                    for (final Message el : chatHistory) { // Make el final for lambda
                        SwingUtilities.invokeLater(() -> {// Process each message on EDT
                            String storedMsgText = el.getMessage(); // Original stored message text
                            String displayPrefix = "";
                            String displayContent = storedMsgText; // Default to full stored text
                            Image imageContent = null ;

                            if (storedMsgText.startsWith("---")) {
                                // System messages, display as is
                                displayPrefix = "";
                                displayContent = storedMsgText;
                            }else if (el.haveContent() && el.getContent() instanceof Image) {
                                // It's an image message, stored prefix is in storedMsgText
                                displayPrefix = storedMsgText; // The stored text *is* the prefix ("Tu: " or "PeerName:")
                                displayContent = ""; // No text content to display besides prefix
                                imageContent =(Image)el.getContent();
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
                final String statusUpdate = "Chat caricata: "
                        + ((currentChatName != null) ? currentChatName : ip);
                view.setStatus(statusUpdate);

            } else {
                view.setStatus("Nessun peer selezionato");
            }
        }).start();
    }

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
        SwingUtilities.invokeLater(() ->{
            final List<String> updatedPeers = getDisplayPeers(); // Get updated list
            view.setPeers(updatedPeers); // Update the list in the view

            view.setStatus("Chat rinominata: " + newName);
        });
    }

    private String getDisplay(String ip,String elses){
        return getDisplayPeers().stream()
                        .filter(p -> resolveIp(p).equals(ip))
                        .findFirst()
                        .orElse(elses);
    }

    @Override
    public void onMessageReceived(String senderIp, String base64Message) {
        new Thread(() -> {
            try {
                byte[] encrypted = Base64.getDecoder().decode(base64Message);
                String decryptedMsg = model.decrypt(encrypted); // The actual message content or "!IMG"+base64img

                final String currentName = model.getChatName(senderIp); // Use current name
                final String displayPrefix = (currentName != null) ? currentName + ": "
                        : senderIp + ": "; // Prefix for display AND storage

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
                    LoggerUtil.logError("Controller", "onMessageReceived","Error processing received message from: " + senderIp, e);
                }
        }).start();
    }

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
}