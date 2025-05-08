package msg.net;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import msg.util.LoggerUtil;

/**
 * The NetworkService class is responsible for managing network connections to peers.
 * It handles listening for incoming connections, initiating outgoing connections,
 * sending messages, and receiving messages. It also notifies a listener about
 * received messages and connection events.
 */
public class NetworkService {

    /**
     * Interface for listeners to be notified of network events.
     */
    public interface MessageListener {
        /**
         * Called when a message is received from a peer.
         * @param senderIp The IP address of the sender.
         * @param base64Message The Base64 encoded message content.
         */
        void onMessageReceived(String senderIp, String base64Message);
        /**
         * Called when a connection event occurs (e.g., connection established, failed, or dropped).
         * @param ip The IP address of the peer involved in the event.
         * @param connected True if connected, false otherwise.
         * @param message A descriptive message about the event.
         * @param args Optional arguments for the event.
         */
        void onConnectionEvent(String ip, boolean connected, String message, Object ... args);
        /**
         * Called when a peer's online status changes.
         * @param ip The IP address of the peer.
         * @param online True if the peer is online, false otherwise.
         */
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

    /**
     * Constructs a new NetworkService.
     * @param listenPort The port number to listen on for incoming connections.
     * @param listener The listener to be notified of network events.
     */
    public NetworkService(int listenPort, MessageListener listener) {
        this.listenPort = listenPort;
        this.listener = listener;
    }

    /**
     * Starts the network service, beginning to listen for incoming connections.
     */
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

    /**
     * Attempts to connect to a peer at the specified IP address.
     * Notifies the listener of connection success or failure.
     * @param ip The IP address of the peer to connect to.
     * @return True if the connection was successful or already established and valid, false otherwise.
     */
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

    /**
     * Attempts to connect to a peer with a specified number of retries and delay.
     * @param ip The IP address of the peer to connect to.
     * @param maxAttempts The maximum number of connection attempts.
     * @param delayMs The delay in milliseconds between attempts.
     * @return True if the connection was successful, false otherwise.
     */
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

    /**
     * Sends a Base64 encoded message to the specified peer.
     * @param ip The IP address of the peer to send the message to.
     * @param base64Message The Base64 encoded message.
     * @return True if the message was sent successfully.
     * @throws IOException if there is no active connection to the peer or if a send error occurs.
     */
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

    /**
     * Removes a peer connection, cleaning up associated sockets and writers.
     * Notifies the listener that the peer is now offline.
     * @param ip The IP address of the peer to remove.
     */
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

    /**
     * Checks if there is an active and valid connection (either outgoing or incoming) to the specified peer.
     * @param ip The IP address of the peer.
     * @return True if a valid connection exists, false otherwise.
     */
    public boolean isPeerConnected(String ip) {
        return (outgoing.containsKey(ip) && isConnectionValid(outgoing.get(ip))) ||
                (incoming.containsKey(ip) && isConnectionValid(incoming.get(ip)));
    }

    /**
     * Checks if a peer is considered online.
     * A peer is online if their status is marked as online and a valid connection exists.
     * @param ip The IP address of the peer.
     * @return True if the peer is online, false otherwise.
     */
    public boolean isPeerOnline(String ip) {
        Boolean status = peerStatus.get(ip);
        return status != null && status && isPeerConnected(ip);
    }
}