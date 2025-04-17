package msg.net;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class NetworkService {

    public interface MessageListener {
        void onMessageReceived(String senderIp, String base64Message);
    }

    private final int listenPort;
    private final MessageListener listener;
    private final ConcurrentMap<String, Socket> outgoing = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, BufferedWriter> outWriters = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Socket> incoming = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, BufferedWriter> inWriters = new ConcurrentHashMap<>();
    private boolean running = false;

    public NetworkService(int listenPort, MessageListener listener) {
        this.listenPort = listenPort;
        this.listener = listener;
    }

    public void start() {
        running = true;
        Thread listenerThread = new Thread(this::listen);
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    private void listen() {
        try (ServerSocket serverSocket = new ServerSocket(listenPort)) {
            while (running) {
                Socket socket = serverSocket.accept();
                String ip = socket.getInetAddress().getHostAddress();
                System.out.println("[NetworkService] Connessione in entrata da: " + ip);
                addIncomingConnection(ip, socket);
                startReaderThread(ip, socket);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void connectToPeer(String ip) {
        if (outgoing.containsKey(ip)) return;
        try {
            Socket socket = new Socket(ip, listenPort);
            System.out.println("[NetworkService] Connessione in uscita verso: " + ip);
            addOutgoingConnection(ip, socket);
            startReaderThread(ip, socket);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(String ip, String base64Message) throws IOException {
        BufferedWriter w = outWriters.get(ip);
        if (w == null) w = inWriters.get(ip); // fallback su incoming
        if (w != null) {
        	System.out.println("[NetworkService] Invio messaggio a " + ip + ": " + base64Message);
            w.write(base64Message);
            w.newLine();
            w.flush();
        } else {
            throw new IOException("Nessuna connessione attiva verso " + ip);
        }
    }

    private void addOutgoingConnection(String ip, Socket socket) throws IOException {
        outgoing.put(ip, socket);
        outWriters.put(ip, new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())));
    }

    private void addIncomingConnection(String ip, Socket socket) throws IOException {
        incoming.put(ip, socket);
        inWriters.put(ip, new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())));
    }

    private void startReaderThread(String ip, Socket socket) {
        new Thread(() -> {
            try (BufferedReader r = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) {
                	 System.out.println("[NetworkService] Ricezione messaggio da " + ip + ": " + line);
                	listener.onMessageReceived(ip, line);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "Reader-" + ip).start();
    }
}