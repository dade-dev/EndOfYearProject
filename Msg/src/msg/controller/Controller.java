package msg.controller;

import msg.model.Model;
import msg.view.Window;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.*;
import java.util.Base64;
import java.util.Enumeration;
import java.util.concurrent.CopyOnWriteArrayList;

public class Controller {
    private final Model m;
    private final Window v;
    private final CopyOnWriteArrayList<Socket> peers = new CopyOnWriteArrayList<>();
    private final int port = 9000;

    public Controller(Model m) {
        try {
            this.m=m;
            String ip = getLocalIp();
            v = new Window(ip);
            startListener();
            askForPeer();
            v.btn.addActionListener(sendMsg());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void askForPeer() {
        String remoteIp = JOptionPane.showInputDialog("Enter peer IP (leave blank to skip):");
        if (remoteIp != null && !remoteIp.isEmpty()) {
            try {
                Socket s = new Socket(remoteIp, port);
                peers.add(s);
                listenSocket(s);
            } catch (IOException ignored) {}
        }
    }

    private void startListener() {
        new Thread(() -> {
            try (ServerSocket ss = new ServerSocket(port)) {
                while (true) {
                    Socket s = ss.accept();
                    peers.add(s);
                    listenSocket(s);
                }
            } catch (IOException ignored) {}
        }).start();
    }

    private void listenSocket(Socket s) {
        new Thread(() -> {
            try (BufferedReader r = new BufferedReader(new InputStreamReader(s.getInputStream()))) {
                String l;
                while ((l = r.readLine()) != null) {
                    byte[] dec = Base64.getDecoder().decode(l);
                    String msg = m.dec(dec);
                    m.addMsg("Them: " + msg);
                    v.addLine("Them: " + msg);
                }
            } catch (Exception ignored) {}
        }).start();
    }

    private ActionListener sendMsg() {
        return e -> {
            try {
                String txt = v.getText();
                if (txt.isEmpty()) return;
                byte[] enc = m.enc(txt);
                String b64 = Base64.getEncoder().encodeToString(enc);

                for (Socket s : peers) {
                    BufferedWriter w = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
                    w.write(b64);
                    w.newLine();
                    w.flush();
                }

                m.addMsg("You: " + txt);
                v.addLine("You: " + txt);
                v.clearText();
            } catch (Exception ignored) {}
        };
    }

    private String getLocalIp() {
        try {
            Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();
            while (nis.hasMoreElements()) {
                NetworkInterface ni = nis.nextElement();
                if (ni.isLoopback() || !ni.isUp()) continue;
                Enumeration<InetAddress> addrs = ni.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    if (!addr.isLoopbackAddress() && addr instanceof Inet4Address) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return "IP Not Found";
    }
}
