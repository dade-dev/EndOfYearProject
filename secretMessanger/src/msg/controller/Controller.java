package msg.controller;

import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import msg.model.Model;
import msg.net.NetworkService;
import msg.net.NetworkUtils;
import msg.net.PeerDiscoveryService;
import msg.view.Window;

public class Controller implements NetworkService.MessageListener, PeerDiscoveryService.DiscoveryListener {
    private Model model;
    private Window view;
    private NetworkService network;
    private String myIp;
    private PeerDiscoveryService discovery;
    private int listenPort = 9000;

    public Controller(Model model) {
        this.model = model;
        this.view = new Window(this);
        this.myIp = NetworkUtils.getLocalIp();
        this.discovery = new PeerDiscoveryService(this);
        this.discovery.start();
        this.network = new NetworkService(listenPort, this);
        this.network.start();
        this.view.setStatus("In ascolto su porta " + listenPort);
        if (!model.getPeers().contains(myIp))
            model.addMessage(myIp, "--- Questa è la tua chat personale ---");
        // Aggiungi il tuo nome (opzionale)
        model.setChatName(myIp, "Me");
        view.setPeers(getDisplayPeers());
    }

    // Visualizza peer con eventuale nome, altrimenti solo IP
    private List<String> getDisplayPeers() {
        return model.getPeers().stream()
                .map(ip -> {
                    String name = model.getChatName(ip);
                    return (name.equals(ip)) ? ip : name + " (" + ip + ")";
                })
                .collect(Collectors.toList());
    }

    // Dato il displayName trova l'IP reale
    private String resolveIp(String display) {
        for (String ip : model.getPeers()) {
            String name = model.getChatName(ip);
            String disp = name.equals(ip) ? ip : name + " (" + ip + ")";
            if (disp.equals(display))
                return ip;
        }
        return null;
    }

    public void onSendMessage(String message) {
        String selectedDisplay = view.getSelectedPeer();
        String selectedIp = resolveIp(selectedDisplay);
        if (message == null || message.isBlank() || selectedIp == null) {
            view.setStatus("Seleziona un peer e scrivi un messaggio!");
            return;
        }
        try {
            byte[] encrypted = model.encrypt(message);
            String base64 = Base64.getEncoder().encodeToString(encrypted);
            network.connectToPeer(selectedIp);
            network.sendMessage(selectedIp, base64);
            model.addMessage(selectedIp, "Tu: " + message);
            refreshChat(selectedIp);
            view.clearInput();
            view.setStatus("Messaggio inviato a " + model.getChatName(selectedIp));
        } catch(Exception ex) {
            view.setStatus("Errore invio: " + ex.getMessage());
        }
    }

    public void onAddPeer(String ip) {
        if (ip == null || ip.isBlank()) {
            view.setStatus("Inserisci un IP valido!");
            return;
        }
        if (!model.getPeers().contains(ip))
            model.addMessage(ip, "--- Conversazione iniziata ---");
        view.setPeers(getDisplayPeers());
        view.clearPeerInput();
        view.setStatus("Peer aggiunto: " + ip);
    }

    public void onPeerSelected(String display) {
        String ip = resolveIp(display);
        refreshChat(ip);
        view.setStatus(ip != null ? "Peer selezionato: " + model.getChatName(ip) : "Nessun peer selezionato");
    }

    // Metodo per rinominare una chat (chiamare dalla View)
    public void onRenameChat(String display, String newName) {
        String ip = resolveIp(display);
        if (ip == null) {
            view.setStatus("Seleziona una chat valida da rinominare.");
            return;
        }
        model.setChatName(ip, newName);
        view.setPeers(getDisplayPeers());
        view.setStatus("Chat rinominata: " + newName);
    }

    private void refreshChat(String senderIp) {
        if (senderIp != null)
            view.setChat(model.getChat(senderIp));
        else
            view.setChat(Collections.emptyList());
    }

    @Override
    public void onMessageReceived(String senderIp, String base64Message) {
        try {
            byte[] encrypted = Base64.getDecoder().decode(base64Message);
            String msg = model.decrypt(encrypted);
            String displayName = model.getChatName(senderIp);
            String display = displayName.equals(senderIp) ? senderIp : displayName + " (" + senderIp + ")";
            model.addMessage(senderIp, display + ": " + msg);
            refreshChat(senderIp);
            view.setPeers(getDisplayPeers());
            view.setStatus("Messaggio ricevuto da " + display);
        } catch(Exception e) {
            // errore decifratura
        }
    }
    @Override
    public void onPeerDiscovered(String ip) {
        if (!model.getPeers().contains(ip))
            model.addMessage(ip, "--- Peer trovato in rete ---");
        view.setPeers(getDisplayPeers());
        view.setStatus("Peer trovato: " + ip);
    }
}