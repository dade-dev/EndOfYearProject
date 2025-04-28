package msg.controller;

import java.awt.Image;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import msg.model.Message;
import msg.model.Model;
import msg.net.NetworkService;
import msg.net.NetworkUtils;
import msg.net.PeerDiscoveryService;
import msg.view.Window;

@SuppressWarnings("rawtypes")
public class Controller implements NetworkService.MessageListener, PeerDiscoveryService.DiscoveryListener {
    private final Model model;
    private final Window view;
    private final NetworkService network;
    private final PeerDiscoveryService discovery;
    private final String myIp;
    private final int listenPort = 9000;

    public Controller(Model model) {
        this.model = model;
        this.view = new Window(this);
        this.myIp = NetworkUtils.getLocalIp();

        // Discovery & network
        this.discovery = new PeerDiscoveryService(this);
        this.discovery.start();
        this.network = new NetworkService(listenPort, this);
        this.network.start();

        // Port
        view.setStatus("In ascolto su porta " + listenPort);

        // Personal Chat
        if (!model.getPeers().contains(myIp)) {
            model.addMessage(myIp, "--- Questa è la tua chat personale ---");
        }
        model.setChatName(myIp, "Me");
        view.setPeers(getDisplayPeers());
    }

    private List<String> getDisplayPeers() {
        return model.getPeers().stream()
            .map(ip -> {
                String name = model.getChatName(ip);
                return name.equals(ip) ? ip : name + " (" + ip + ")";
            })
            .collect(Collectors.toList());
    }

    private String resolveIp(String display) {
        if (display == null) return null;
        //with this implementation I can change the name of the chat and still take the correct ip
        if (display.contains("(") && display.contains(")")) {
            int start = display.lastIndexOf('(');
            int end = display.lastIndexOf(')');
            if (start != -1 && end != -1 && start < end) {
                return display.substring(start + 1, end);
            }
        }
        return display;
    }


    public void onSendMessage(String message) {
        new Thread(() -> {
            String disp = view.getSelectedPeer();
            String targetIp = resolveIp(disp);
            if (message == null || message.isBlank() || targetIp == null) {
                view.setStatus("Seleziona un peer e scrivi un messaggio!");
                return;
            }

            try {
                byte[] encrypted = model.encrypt(message);
                String payload = Base64.getEncoder().encodeToString(encrypted);
                network.connectToPeer(targetIp);
                network.sendMessage(targetIp, payload);
                if (message.startsWith("!IMG")) {
                    byte[] imgBytes = Base64.getDecoder().decode(message.substring(4));
                    model.addImage(myIp, imgBytes);
                    model.addMessage(targetIp, "Tu: ");
                    //we use this to force the printing at the same time
                    SwingUtilities.invokeLater(() -> {
                        view.appendText("Tu: ");
                        ImageIcon icon = new ImageIcon(imgBytes);
                        Image img = icon.getImage();
                        view.appendImage(img);
                    });
                } else {
                    model.addMessage(targetIp, "Tu: " + message);
                    view.appendText("Tu: " + message);
                }

                view.clearInput();
                view.setStatus("Messaggio inviato a " + model.getChatName(targetIp));

            } catch (Exception ignored) {}
        }).start();
    }


    public void onAddPeer(String ip) {
        new Thread(() -> {
        	if (ip == null || ip.isBlank()) {
                view.setStatus("Inserisci un IP valido!");
                return;
            }
            if (!model.getPeers().contains(ip)) {
                model.addMessage(ip, "--- Conversazione iniziata ---");
                view.setPeers(getDisplayPeers());
                view.setStatus("Peer aggiunto: " + ip);
            }
            view.clearPeerInput();
        }).start();
    }

    public void onPeerSelected(String display) {
       new Thread(() -> {
    	   String ip = resolveIp(display);
           view.clearChat();
           
           if(ip != null) {
           	if(model.getChat(ip) == null)
           		model.addMessage(ip, "--- Nuova chat creata ---");
           	else
           		for(Message el : (ArrayList<Message>) model.getChat(ip)) {
           			view.appendText(el.getMessage());
           			if(el.haveContent() && el.getContent() instanceof Image)
           				view.appendImage((Image)el.getContent());
           				
           		}
           	view.setStatus("Chat caricata: " + model.getChatName(ip));
           }else {
           	view.setStatus("Nessun peer selezionato");
           }  
       }).start();
    }

    public void onRenameChat(String display, String newName) {
        String ip = resolveIp(display);
        if (ip == null) {
            view.setStatus("Seleziona una chat valida da rinominare.");
            return;
        }
        
        model.setChatName(ip, newName);
        
        view.setPeers(getDisplayPeers());
        
        String newDisplay = model.getChatName(ip).equals(ip) ? ip : model.getChatName(ip) + " (" + ip + ")";
        
        view.selectPeer(newDisplay);
        view.setStatus("Chat rinominata: " + newName);
    }

    @Override
    public void onMessageReceived(String senderIp, String base64Message) {
        new Thread(() -> {
            try {
                byte[] encrypted = Base64.getDecoder().decode(base64Message);
                String msg = model.decrypt(encrypted);
                String name = model.getChatName(senderIp);
                String disp = name.equals(senderIp) ? senderIp : name + " (" + senderIp + ")";

                if (msg.startsWith("!IMG")) {
                    byte[] imgBytes = Base64.getDecoder().decode(msg.substring(4));
                    model.addImage(senderIp, imgBytes);
                    model.addMessage(senderIp, disp + ": ");
                    //we use this to force the printing at the same time
                    SwingUtilities.invokeLater(() -> {
                        view.appendText(disp + ": ");
                        ImageIcon icon = new ImageIcon(imgBytes);
                        Image img = icon.getImage();
                        view.appendImage(img);

                        view.setStatus("Immagine ricevuta da " + disp);
                    });
                } else {
                    model.addMessage(senderIp, disp + ": " + msg);
                    view.appendText(disp + ": " + msg);
                    view.setStatus("Messaggio ricevuto da " + disp);
                }

            } catch (Exception ignored) {}
        }).start();
    }


    @Override
    public void onPeerDiscovered(String ip) {
        new Thread(() -> {
        	if (!model.getPeers().contains(ip)) {
                model.addMessage(ip, "--- Peer trovato in rete ---");
                view.setPeers(getDisplayPeers());
                view.setStatus("Peer trovato: " + ip);
            }
        }).start();
    }
}