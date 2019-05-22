package GrupoB.Utils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class NetUtils {
    /**
     * Gets the machine's local IP address
     * @return The IP address. It should be something like (just an example) 192.168.1.200.
     */
    public static String getLocalIP() throws RuntimeException {
        try {
            // Gets the machine's network interfaces
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();

            while (interfaces.hasMoreElements()) {
                NetworkInterface itf = interfaces.nextElement();

                // Filters out 127.0.0.1 and inactive interfaces
                if (itf.isLoopback() || !itf.isUp())
                    continue;

                Enumeration<InetAddress> addresses = itf.getInetAddresses();
                while(addresses.hasMoreElements()) {
                    String ip = addresses.nextElement().getHostAddress();

                    // For some reason, the MAC address is also printed, so we have to filter it out
                    if (ip.contains(":"))
                        continue;

                    return ip;
                }
            }
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }

        return null;
    }
}
