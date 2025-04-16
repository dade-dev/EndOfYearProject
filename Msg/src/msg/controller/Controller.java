package msg.controller;

import msg.model.Model;
import msg.view.Window;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Base64;

public class Controller {
	private final Model m;
	private final Window v;
	private final int port = 9000;
	private final Map<String, Socket> sockets = new ConcurrentHashMap<>();

	public Controller(Model m) {
		try {
			this.m = m;
			String ip = getLocalIp();
			v = new Window(ip);
			v.sendBtn.addActionListener(sendMsg());
			v.ipList.addListSelectionListener(e -> {
				String sel = v.getSelectedIP();
				if (sel != null)
					v.setChat(m.getChat(sel));
			});

			startListener();
			askPeer();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void askPeer() {
		String remote = JOptionPane.showInputDialog("Enter peer IP (leave blank to skip):");
		if (remote != null && !remote.isEmpty())
			connect(remote);
	}

	private void connect(String ip) {
		try {
			Socket s = new Socket(ip, port);
			sockets.put(ip, s);
			v.addIP(ip);
			listenSocket(s, ip);
		} catch (IOException ignored) {
		}
	}

	private void startListener() {
		new Thread(() -> {
			try (ServerSocket ss = new ServerSocket(port)) {
				while (true) {
					Socket s = ss.accept();
					String ip = s.getInetAddress().getHostAddress();
					sockets.put(ip, s);
					v.addIP(ip);
					listenSocket(s, ip);
				}
			} catch (IOException ignored) {
			}
		}).start();
	}

	private void listenSocket(Socket s, String ip) {
		new Thread(() -> {
			try (BufferedReader r = new BufferedReader(new InputStreamReader(s.getInputStream()))) {
				String l;
				while ((l = r.readLine()) != null) {
					byte[] b = Base64.getDecoder().decode(l);
					String msg = m.dec(b);
					m.addMsg(ip, "Them: " + msg);
					if (ip.equals(v.getSelectedIP()))
						v.addChat("Them: " + msg);
				}
			} catch (Exception ignored) {
			}
		}).start();
	}

	private ActionListener sendMsg() {
		return (ActionEvent e) -> {
			try {
				String targetIp = v.getSelectedIP();
				if (targetIp == null || !sockets.containsKey(targetIp))
					return;

				String txt = v.getInput();
				if (txt.isEmpty())
					return;

				Socket s = sockets.get(targetIp);
				BufferedWriter w = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
				byte[] enc = m.enc(txt);
				String b64 = Base64.getEncoder().encodeToString(enc);
				w.write(b64);
				w.newLine();
				w.flush();

				m.addMsg(targetIp, "You: " + txt);
				v.setChat(m.getChat(targetIp));
				v.clearInput();
			} catch (Exception ignored) {
			}
		};
	}

	private String getLocalIp() {
		try {
			Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();
			while (nis.hasMoreElements()) {
				NetworkInterface ni = nis.nextElement();
				if (ni.isLoopback() || !ni.isUp())
					continue;
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
