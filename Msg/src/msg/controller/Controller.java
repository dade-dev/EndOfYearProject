package msg.controller;

import msg.model.Model;
import msg.view.Window;
import msg.view.Window.PeerEntry;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.Color;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Base64;

public class Controller {
    private final Model m;
    private final Window v;
    private final int port = 9000;
    // Map IP -> Socket e Writer
    private final Map<String, Socket> sockets = new ConcurrentHashMap<>();
    private final Map<String, BufferedWriter> writers = new ConcurrentHashMap<>();
    // Mappa IP -> Nome (inizialmente uguale all'IP)
    private final Map<String, String> names = new ConcurrentHashMap<>();
    private String manualTargetIp = "";

    public Controller(Model m) {
        try {
            this.m = m;
            String myIp = getLocalIp();
            v = new Window(myIp);
            // Inizializza il nome di self
            names.put(myIp, myIp);
            // Visualizza la chat del peer selezionato
            v.ipList.addListSelectionListener(new ListSelectionListener() {
                @Override
                public void valueChanged(ListSelectionEvent e) {
                    String ip = v.getSelectedIp();
                    if(ip != null) {
                        v.setChat(m.getChat(ip));
                    }
                }
            });
            v.sendBtn.addActionListener(sendMsg());
            v.toggleModeBtn.addActionListener(e -> toggleMode());
            // Listener per il set manual IP
            v.setManualIpBtn.addActionListener(e -> {
                String tip = v.manualIpField.getText().trim();
                if(!tip.isEmpty()) {
                    manualTargetIp = tip;
                    if(!sockets.containsKey(tip)) {
                        connect(tip);
                    }
                }
            });
            v.renameBtn.addActionListener(e -> {
                String sel = v.getSelectedIp();
                String newName = v.renameField.getText().trim();
                if(sel != null && !newName.isEmpty()) {
                    names.put(sel, newName);
                    updatePeerList();
                }
            });
            askForPeer();
            startListener();
            startDiscovery();
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    private ActionListener sendMsg() {
        return e -> {
            try {
                String targetIp = !manualTargetIp.isEmpty() ? manualTargetIp : v.getSelectedIp();
                if (targetIp == null || targetIp.isEmpty() || !sockets.containsKey(targetIp))
                    return;
                String txt = v.getInput();
                if(txt.isEmpty())
                    return;
                BufferedWriter w = writers.get(targetIp);
                byte[] enc = m.enc(txt);
                String b64 = Base64.getEncoder().encodeToString(enc);
                w.write(b64);
                w.newLine();
                w.flush();
                m.addMsg(targetIp, "You: " + txt);
                if(targetIp.equals(v.getSelectedIp()))
                    v.setChat(m.getChat(targetIp));
                v.clearInput();
            } catch(Exception ex) {
                ex.printStackTrace();
            }
        };
    }

    // Avvia il thread che ascolta le connessioni in entrata
    private void startListener() {
        new Thread(() -> {
            try (ServerSocket ss = new ServerSocket(port)) {
                while (true) {
                    Socket s = ss.accept();
                    String ip = s.getInetAddress().getHostAddress();
                    addConnection(s, ip);
                    listenSocket(s, ip);
                }
            } catch(IOException ex) {
                ex.printStackTrace();
            }
        }).start();
    }

    private void addConnection(Socket s, String ip) {
        sockets.put(ip, s);
        try {
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
            writers.put(ip, bw);
        } catch(IOException ex) {
            ex.printStackTrace();
        }
        names.putIfAbsent(ip, ip);
        SwingUtilities.invokeLater(() -> updatePeerList());
    }

    private void connect(String ip) {
        try {
            Socket s = new Socket();
            s.connect(new InetSocketAddress(ip, port), 500);
            addConnection(s, ip);
            listenSocket(s, ip);
        } catch(IOException ex) {
            // Se non è possibile connettersi, si ignora
        }
    }

    private void listenSocket(Socket s, String ip) {
        new Thread(() -> {
            try (BufferedReader r = new BufferedReader(new InputStreamReader(s.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) {
                    byte[] b = Base64.getDecoder().decode(line);
                    String msg = m.dec(b);
                    m.addMsg(ip, "Them: " + msg);
                    // Se il peer attualmente visualizzato corrisponde, aggiorna la chat
                    if(ip.equals(v.getSelectedIp()))
                        v.addChat("Them: " + msg);
                }
            } catch(Exception ex) {
                ex.printStackTrace();
            }
        }).start();
    }

    private void startDiscovery() {
        new Thread(() -> {
            while (true) {
                discoverPeers();
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ignored) {}
            }
        }).start();
    }

    // Esegue "arp -a" e tenta la connessione a ciascun IP trovato (che non sia il nostro)
    private void discoverPeers() {
        try {
            Process p = Runtime.getRuntime().exec("arp -a");
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            Pattern pattern = Pattern.compile("(\\d{1,3}(?:\\.\\d{1,3}){3})");
            String myIp = getLocalIp();
            while((line = reader.readLine()) != null) {
                Matcher m = pattern.matcher(line);
                while(m.find()) {
                    String ip = m.group(1);
                    if(ip.equals(myIp))
                        continue;
                    if(!sockets.containsKey(ip)) {
                        connect(ip);
                    }
                }
            }
        } catch(IOException ex) {
            ex.printStackTrace();
        }
    }

    private void updatePeerList() {
        v.clearPeers();
        for (String ip : names.keySet()) {
            v.addPeer(new PeerEntry(ip, names.get(ip)));
        }
    }

    private void askForPeer() {
        String remote = JOptionPane.showInputDialog("Enter peer IP (leave blank to skip):");
        if(remote != null && !remote.isEmpty()){
            connect(remote);
        }
    }

    private String getLocalIp() {
        try {
            Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();
            while(nis.hasMoreElements()){
                NetworkInterface ni = nis.nextElement();
                if(ni.isLoopback() || !ni.isUp())
                    continue;
                Enumeration<InetAddress> addrs = ni.getInetAddresses();
                while(addrs.hasMoreElements()){
                    InetAddress addr = addrs.nextElement();
                    if(!addr.isLoopbackAddress() && addr instanceof Inet4Address)
                        return addr.getHostAddress();
                }
            }
        } catch(SocketException e) {
            e.printStackTrace();
        }
        return "IP Not Found";
    }

    private void toggleMode() {
        Color bg, fg;
        if(v.getContentPane().getBackground().equals(Color.DARK_GRAY)){
            bg = Color.LIGHT_GRAY;
            fg = Color.BLACK;
            v.toggleModeBtn.setText("Dark Mode");
        } else {
            bg = Color.DARK_GRAY;
            fg = Color.WHITE;
            v.toggleModeBtn.setText("Light Mode");
        }
        v.getContentPane().setBackground(bg);
        v.chatArea.setBackground(bg);
        v.chatArea.setForeground(fg);
        v.input.setBackground(bg);
        v.input.setForeground(fg);
        v.ipList.setBackground(bg);
        v.ipList.setForeground(fg);
        v.myIpLabel.setForeground(fg);
        v.settingsPanel.setBackground(bg);
    }
}
