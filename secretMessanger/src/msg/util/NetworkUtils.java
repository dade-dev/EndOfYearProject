package msg.util;

import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.regex.Pattern;

public class NetworkUtils {
    // IP address pattern (IPv4)
    private static final Pattern IPV4_PATTERN = Pattern.compile(
            "^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                    "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                    "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                    "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$");

    public static String getLocalIp() {
        try {
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            while (ifaces.hasMoreElements()) {
                NetworkInterface iface = ifaces.nextElement();
                String name = iface.getDisplayName().toLowerCase();
                if (iface.isLoopback() || !iface.isUp() || name.contains("ham") || name.contains("ztt"))
                    continue;
                for (InterfaceAddress addr : iface.getInterfaceAddresses()) {
                    String ip = addr.getAddress().getHostAddress();
                    if (!ip.startsWith("127.") && !ip.contains(":")) {
                        return ip;
                    }
                }
            }
            LoggerUtil.logWarning("NetworkUtils", "getLocalIp",
                    "Could not find a non-loopback interface, defaulting to 127.0.0.1");
        } catch (Exception e) {
            LoggerUtil.logError("NetworkUtils", "getLocalIp", "Error getting local IP address", e);
        }
        return "127.0.0.1";
    }

    public static boolean isValidIpAddress(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }
        return IPV4_PATTERN.matcher(ip).matches();
    }
}