package msg.net;

import java.net.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import msg.util.LoggerUtil;
import msg.util.NetworkUtils;

public class PeerDiscoveryService {
    public interface DiscoveryListener {
        void onPeerDiscovered(String ip);
    }

    private static final int BROADCAST_PORT = 45678;
    private static final String BROADCAST_MSG = "SECRET_MESSENGER_DISCOVERY";
    private final Set<String> knownPeers = new CopyOnWriteArraySet<>();
    private final DiscoveryListener listener;
    private boolean running = true;

    public PeerDiscoveryService(DiscoveryListener listener) {
        this.listener = listener;
    }

    public void start() {
        // Listener thread
        Thread listenThread = new Thread(this::listen, "PeerDiscovery-Listen");
        listenThread.setDaemon(true);
        listenThread.start();

        // Broadcast thread
        Thread broadcastThread = new Thread(this::broadcast, "PeerDiscovery-Broadcast");
        broadcastThread.setDaemon(true);
        broadcastThread.start();
    }

    public Set<String> getKnownPeers() {
        return new HashSet<>(knownPeers);
    }

    private void listen() {
        try (DatagramSocket socket = new DatagramSocket(BROADCAST_PORT, InetAddress.getByName("0.0.0.0"))) {
            socket.setBroadcast(true);
            byte[] buf = new byte[128];
            while (running) {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
                String msg = new String(packet.getData(), 0, packet.getLength());
                String senderIp = packet.getAddress().getHostAddress();
                if (BROADCAST_MSG.equals(msg) && !isSelf(senderIp)) {
                    if (knownPeers.add(senderIp) && listener != null) {
                        LoggerUtil.logInfo("PeerDiscoveryService", "listen", "Discovered new peer: " + senderIp);
                        listener.onPeerDiscovered(senderIp);
                    }
                }
            }
        } catch (Exception e) {
            LoggerUtil.logError("PeerDiscoveryService", "listen", "Error in discovery listener", e);
        }
    }
    
    private void broadcast() {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setBroadcast(true);
            byte[] data = BROADCAST_MSG.getBytes();
            DatagramPacket packet = new DatagramPacket(data, data.length,
                    InetAddress.getByName("255.255.255.255"), BROADCAST_PORT);

            LoggerUtil.logInfo("PeerDiscoveryService", "broadcast", "Starting peer discovery broadcasts");
            while (running) {
                try {
                    socket.send(packet);
                    Thread.sleep(2000); // ogni 2 secondi
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    LoggerUtil.logError("PeerDiscoveryService", "broadcast", "Broadcast thread interrupted", e);
                    break;
                } catch (Exception e) {
                    LoggerUtil.logError("PeerDiscoveryService", "broadcast", "Error sending discovery broadcast", e);
                    // Continue broadcasting even if one attempt fails
                }
            }
        } catch (Exception e) {
            LoggerUtil.logError("PeerDiscoveryService", "broadcast", "Fatal error in discovery broadcaster", e);
        }
    }

    private boolean isSelf(String ip) {
        return ip.equals(NetworkUtils.getLocalIp());
    }
}