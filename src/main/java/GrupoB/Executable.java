package GrupoB;

import GrupoB.ApplicationServer.Models.NetInfo;
import GrupoB.Client.NetworkClient;

public class Executable {
    private final static String ADDRESS = "localhost";
    private final static int PORT = 9595;

    private static NetworkClient initClient() {
        return new NetworkClient(ADDRESS, PORT);
    }

    public static void main(String[] args) {
        NetworkClient client = initClient();

        System.out.println("Ping result: " + client.ping());

        NetInfo joinResult = client.join();

        if (joinResult != null)
            System.out.println("My nodeID: " + joinResult.nodeID);
        else
            System.out.println("Null response");
    }
}
