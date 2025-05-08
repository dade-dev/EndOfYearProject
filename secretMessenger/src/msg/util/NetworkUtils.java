package msg.util;

import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.regex.Pattern;

/**
 * Utility class for network-related functions, such as retrieving the local IP address
 * and validating IP address formats.
 */
public class NetworkUtils {
    // IP address pattern (IPv4)
    private static final Pattern IPV4_PATTERN = Pattern.compile(
            "^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                    "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                    "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                    "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$");

    /**
     * Retrieves the local IP address of the machine.
     * It attempts to find a non-loopback, up, and non-virtual interface.
     * Excludes interfaces containing "ham" (Hamachi) or "ztt" (ZeroTier) in their names.
     * @return The local IPv4 address as a string, or "127.0.0.1" if an error occurs or no suitable IP is found.
     */
    public static String getLocalIp() {
        try {
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            while (ifaces.hasMoreElements()) {
                NetworkInterface iface = ifaces.nextElement();
                String name = iface.getDisplayName().toLowerCase();
                if (iface.isLoopback() || !iface.isUp() || iface.isVirtual() || name.contains("ham") || name.contains("ztt"))
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

    /**
     * Validates if the given string is a valid IPv4 address.
     * @param ip The string to validate.
     * @return True if the string is a valid IPv4 address, false otherwise.
     */
    public static boolean isValidIpAddress(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }
        return IPV4_PATTERN.matcher(ip).matches();
    }
}