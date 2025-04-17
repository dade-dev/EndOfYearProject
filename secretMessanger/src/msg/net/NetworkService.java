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
    private final ConcurrentMap<String, BufferedWriter> writers = new ConcurrentHashMap<>();
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
                addConnection(ip, socket);
                startReaderThread(ip, socket);
            }
        } catch (Exception e) {
            // log error, but do not crash
        }
    }

    public void connectToPeer(String ip) {
        if (outgoing.containsKey(ip)) return;
        try {
            Socket socket = new Socket(ip, listenPort);
            addConnection(ip, socket);
            startReaderThread(ip, socket);
        } catch (Exception ignored) {}
    }

    public void sendMessage(String ip, String base64Message) throws IOException {
        BufferedWriter w = writers.get(ip);
        if (w != null) {
            w.write(base64Message);
            w.newLine();
            w.flush();
        }
    }

    private void addConnection(String ip, Socket socket) throws IOException {
        outgoing.put(ip, socket);
        writers.put(ip, new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())));
    }

    private void startReaderThread(String ip, Socket socket) {
        new Thread(() -> {
            try (BufferedReader r = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) {
                    listener.onMessageReceived(ip, line);
                }
            } catch (Exception ignored) {}
        }, "Reader-" + ip).start();
    }
}