package msg.net;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import msg.util.LoggerUtil;

public class NetworkService {

    public interface MessageListener {
        void onMessageReceived(String senderIp, String base64Message);
        void onConnectionEvent(String ip, boolean connected, String message, Object ... args);
        void onPeerStatusChange(String ip, boolean online); // Nuovo metodo per notificare lo stato del peer
    }

    private final int listenPort;
    private final MessageListener listener;
    private final ConcurrentMap<String, Socket> outgoing = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, BufferedWriter> outWriters = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Socket> incoming = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, BufferedWriter> inWriters = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Boolean> peerStatus = new ConcurrentHashMap<>(); // Mappa per tracciare lo stato dei peer
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
                addIncomingConnection(ip, socket);
                startReaderThread(ip, socket);
            }
        } catch (Exception e) {
            LoggerUtil.logError("NetworkService", "listen", "Error in listener thread", e);
        }
    }

    public boolean connectToPeer(String ip) {
        if (outgoing.containsKey(ip)) {
            // Already connected, check if connection is still valid
            if (isConnectionValid(outgoing.get(ip))) {
                // Connection is valid, ensure status is set to online
                if (peerStatus.getOrDefault(ip, false) == false) {
                    peerStatus.put(ip, true);
                    if (listener != null) {
                        listener.onPeerStatusChange(ip, true);
                    }
                }
                return true;
            } else {
                // Connection is dead, remove it so we can try again
                removePeer(ip);
            }
        }

        try {
            // Add a timeout to prevent long hangs on invalid IPs
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(ip, listenPort), 3000); // 3 second timeout
            addOutgoingConnection(ip, socket);
            startReaderThread(ip, socket);

            // Notify listener about successful connection
            if (listener != null) {
                listener.onConnectionEvent(ip, true, "Connesso a " + ip);// if i put another params -> new Object() it will show on view
            }
            return true;
        } catch (UnknownHostException e) {
            // Handle when hostname can't be resolved (e.g., when a name is entered instead of IP)
            if (listener != null) {
                listener.onConnectionEvent(ip, false, "Host non trovato: " + ip);// We check the ip is valid
                // Ensure peer status is set to offline
                peerStatus.put(ip, false);
                listener.onPeerStatusChange(ip, false);
            }
            LoggerUtil.logError("NetworkService", "connectToPeer", "Unknown host: " + ip, e);
            return false;
        } catch (ConnectException e) {
            // Handle connection refused (peer not listening or firewall)
            if (listener != null) {
                listener.onConnectionEvent(ip, false, "Connessione rifiutata a " + ip);
                // Ensure peer status is set to offline
                peerStatus.put(ip, false);
                listener.onPeerStatusChange(ip, false);
            }
            LoggerUtil.logError("NetworkService", "connectToPeer", "Connection refused to: " + ip, e);
            return false;
        } catch (SocketTimeoutException e) {
            // Handle timeout (no response)
            if (listener != null) {
                listener.onConnectionEvent(ip, false, "Timeout connessione a " + ip);
                // Ensure peer status is set to offline
                peerStatus.put(ip, false);
                listener.onPeerStatusChange(ip, false);
            }
            LoggerUtil.logError("NetworkService", "connectToPeer", "Connection timeout to: " + ip, e);
            return false;
        } catch (Exception e) {
            // Handle other errors
            if (listener != null) {
                listener.onConnectionEvent(ip, false, "Errore connessione a " + ip + ": " + e.getMessage());
                // Ensure peer status is set to offline
                peerStatus.put(ip, false);
                listener.onPeerStatusChange(ip, false);
            }
            LoggerUtil.logError("NetworkService", "connectToPeer", "Error connecting to: " + ip, e);
            return false;
        }
    }

    // Connect to peer with retries
    public boolean connectToPeerWithRetry(String ip, int maxAttempts, long delayMs) {
        if (isPeerConnected(ip)) {
            LoggerUtil.logInfo("NetworkService", "connectToPeerWithRetry", "Already connected to " + ip);
            return true;
        }

        int attempts = 0;
        while (attempts < maxAttempts) {
            attempts++;
            LoggerUtil.logInfo("NetworkService", "connectToPeerWithRetry",
                    "Attempt " + attempts + "/" + maxAttempts + " to connect to " + ip);

            if (connectToPeer(ip)) {
                return true;
            }

            if (attempts < maxAttempts) {
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException e) {
                    LoggerUtil.logError("NetworkService", "connectToPeerWithRetry",
                            "Interrupted while waiting to retry connection to " + ip, e);
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }

        LoggerUtil.logWarning("NetworkService", "connectToPeerWithRetry",
                "Failed to connect to " + ip + " after " + maxAttempts + " attempts");
        return false;
    }

    public boolean sendMessage(String ip, String base64Message) throws IOException {
        BufferedWriter w = outWriters.get(ip);
        if (w == null)
            w = inWriters.get(ip); // fallback su incoming

        if (w != null) {
            try {
                w.write(base64Message);
                w.newLine();
                w.flush();
                return true;
            } catch (IOException e) {
                // Handle disconnected peer during send
                removePeer(ip);
                if (listener != null) {
                    listener.onConnectionEvent(ip, false, "Peer disconnesso durante l'invio: " + ip);
                    //NOtifica gia gestitia
                    listener.onPeerStatusChange(ip, false);
                }
                throw new IOException("Nessuna connessione attiva verso " + ip);
            }
        } else {
            throw new IOException("Nessuna connessione attiva verso " + ip);
        }
    }

    private void addOutgoingConnection(String ip, Socket socket) throws IOException {
        outgoing.put(ip, socket);
        outWriters.put(ip, new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())));
        
        // Update peer status to online
        peerStatus.put(ip, true);
        
        // Notify listener about online status
        if (listener != null) {
            listener.onPeerStatusChange(ip, true);
        }
    }

    private void addIncomingConnection(String ip, Socket socket) throws IOException {
        incoming.put(ip, socket);
        inWriters.put(ip, new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())));

        // Update peer status to online
        peerStatus.put(ip, true);
        
        // Notify listener about new incoming connection
        if (listener != null) {
            listener.onConnectionEvent(ip, true, "Online " + ip, new Object());
            listener.onPeerStatusChange(ip, true); // Notify about online status
        }
    }

    private void startReaderThread(String ip, Socket socket) {
        new Thread(() -> {
            try (BufferedReader r = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) {
                    listener.onMessageReceived(ip, line);
                }
            } catch (Exception e) {
                LoggerUtil.logError("NetworkService", "startReaderThread", "Error in reader thread for: " + ip, e);
            } finally {
                // Handle disconnection when reader thread ends
                removePeer(ip);
                if (listener != null) {
                    listener.onConnectionEvent(ip, false, "Peer disconnesso: " + ip);
                    listener.onPeerStatusChange(ip, false); // Notify about offline status
                }
            }
        }, "Reader-" + ip).start();
    }

    // Helper methods for connection management

    private boolean isConnectionValid(Socket socket) {
        if (socket == null || socket.isClosed() || !socket.isConnected()) {
            return false;
        }

        // Additional check: try to read socket state (won't block but will detect
        // closed sockets)
        try {
            socket.getOutputStream().flush(); // Try flushing to test connection
            return true;
        } catch (IOException e) {
            return false; // Connection is broken
        }
    }

    public void removePeer(String ip) {
        // Clean up outgoing connection
        Socket outSocket = outgoing.remove(ip);
        if (outSocket != null) {
            try {
                outSocket.close();
            } catch (IOException e) {
                /* ignore */ }
        }
        outWriters.remove(ip);

        // Clean up incoming connection
        Socket inSocket = incoming.remove(ip);
        if (inSocket != null) {
            try {
                inSocket.close();
            } catch (IOException e) {
                /* ignore */ }
        }
        inWriters.remove(ip);
        
        // Update peer status to offline
        peerStatus.put(ip, false);
        
        // Notify about offline status if listener exists
        if (listener != null) {
            listener.onPeerStatusChange(ip, false);
        }
    }

    public boolean isPeerConnected(String ip) {
        return (outgoing.containsKey(ip) && isConnectionValid(outgoing.get(ip))) ||
                (incoming.containsKey(ip) && isConnectionValid(incoming.get(ip)));
    }

    public boolean isPeerOnline(String ip) {
        Boolean status = peerStatus.get(ip);
        return status != null && status && isPeerConnected(ip);
    }
}