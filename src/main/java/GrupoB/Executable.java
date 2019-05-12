package GrupoB;

import GrupoB.ApplicationServer.Models.NetInfo;
import GrupoB.ApplicationServer.Models.Node;
import GrupoB.Client.NetworkClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Executable {
    private final static String ADDRESS = "localhost";
    private final static int PORT = 9595;

    // Maximum contacts stored in a kBucket
    public final static int K = 3;
    // NodeID bit size
    private final static int keySize = 256;

    private static String nodeID;

    public static Map<String, Node> peers = new HashMap<>();
    @SuppressWarnings("unchecked")
    public static ArrayList<Node>[] kBuckets = new ArrayList[keySize];

    private static NetworkClient initNetClient() {
        return new NetworkClient(ADDRESS, PORT);
    }

    private static void initKBuckets() {
        for (int i = 0; i < keySize; i++)
            kBuckets[i] = new ArrayList<>();
    }

    private static void addNewPeer(Node peer) {
        peers.put(peer.getId(), peer);
    }

    public static int calculateDistance(String node1, String node2) {
        int myDecimalID = Integer.parseInt(node1);
        int peerDecimalID = Integer.parseInt(node2);

        return myDecimalID ^ peerDecimalID;
    }

    /**
     * Calculates the index of the kBucket the peer belongs to
     * @param peerID The nodeID of the peer in question
     * @return The kBucket in which the peer belongs
     */
    public static int calculateKBucket(String peerID) {
        int distance = calculateDistance(nodeID, peerID);

        for (int i = 0; i < keySize; i++)
            if (Math.pow(2, i) < distance && Math.pow(2, i + 1) > distance)
                return i;

        return -1;
    }

    public static void sendToTail(int index, Node peer) {
        int peerIndex = kBuckets[index].indexOf(peer);

        int i;
        for (i = peerIndex; i < kBuckets[index].size(); i++)
            kBuckets[index].add(i, kBuckets[index].get(i + 1));

        kBuckets[index].add(i, peer);
    }

    private static void addToKBucket(Node peer) {
        int index = calculateKBucket(peer.getId());

        // Peer already exists in kBucket
        if (kBuckets[index].contains(peer))
            sendToTail(index, peer);
        else {
            // kBucket has less than K entries
            if (kBuckets[index].size() < K)
                kBuckets[index].add(kBuckets[index].size() - 1, peer);
            else {
                // TODO
                /* Pings the first entry in the kBucket
                    -> If it fails to respond, remove it from
                        the kBucket and insert this new peer
                    -> Else, move it to the end of the
                        kBucket and discard this new peer
                 */
            }
        }
    }

    public static void main(String[] args) {
        NetworkClient client = initNetClient();

        System.out.println("Ping result: " + client.ping());

        initKBuckets();
        NetInfo joinResult = client.join();
        nodeID = joinResult.nodeID;

        System.out.println("My nodeID: " + nodeID);

        if (joinResult.peer != null) {
            addNewPeer(joinResult.peer);
            addToKBucket(joinResult.peer);
        }

        // Client joined the network.
        // It should start computing new blocks.

        // 1 Thread to compute the blocks
        // 1 Thread to handle communications
    }
}
