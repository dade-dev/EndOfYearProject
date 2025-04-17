package msg.net;

import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

public class NetworkUtils {
    public static String getLocalIp() {
        try {
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            while (ifaces.hasMoreElements()) {
                NetworkInterface iface = ifaces.nextElement();
                if (iface.isLoopback() || !iface.isUp()) continue;
                for (InterfaceAddress addr : iface.getInterfaceAddresses()) {
                    String ip = addr.getAddress().getHostAddress();
                    if (!ip.startsWith("127.") && !ip.contains(":")) return ip;
                }
            }
        } catch (Exception e) { }
        return "127.0.0.1";
    }
}