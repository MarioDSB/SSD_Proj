package GrupoB;

import GrupoB.ApplicationServer.Models.NetInfo;
import GrupoB.ApplicationServer.Models.Node;
import GrupoB.Client.NetworkClient;
import GrupoB.RPC.P2PClient.P2PClientRPC;
import GrupoB.gRPCService.ClientProto.NodeInfo;

import java.util.*;

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

    // THIS FUNCTION MAY BE USELESS
    private static void addNewPeer(Node peer) {
        peers.put(peer.getId(), peer);
    }

    /**
     * Calculates the XOR distance between 2 nodeIDs
     * @param node1 First nodeID
     * @param node2 Second nodeID
     * @return The distance between node1 and node2
     */
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

    /**
     * Moves a node from a kBucket to the tail of it.
     * @param index Index of the kBucket
     * @param peer Node to move
     */
    public static void sendToTail(int index, Node peer) {
        int peerIndex = kBuckets[index].indexOf(peer);

        int i;
        for (i = peerIndex; i < kBuckets[index].size(); i++)
            kBuckets[index].add(i, kBuckets[index].get(i + 1));

        kBuckets[index].add(i, peer);
    }

    /**
     * Tries to add a pier to the kBuckets. Checks if the kBucket is full or not.
     * If the kBucket is full, tries to ping the least-recently seen node. If this
     * node does not respond, it is discarded and the new peer is added. Otherwise,
     * the new peer is discarded.
     * @param peer The peer to add to the kBuckets
     */
    private static void addToKBucket(Node peer) {
        int index = calculateKBucket(peer.getId());

        // Peer already exists in kBucket
        if (kBuckets[index].contains(peer))
            sendToTail(index, peer);
        else {
            // kBucket has less than K entries
            if (kBuckets[index].size() < K) {
                kBuckets[index].add(kBuckets[index].size() - 1, peer);
            } else {
                // Pings the first entry in the kBucket
                Node node = kBuckets[index].get(0);
                P2PClientRPC p2pClient = new P2PClientRPC(node.getAddress(), node.getPort());

                // If it fails to respond, remove it from
                // the kBucket and insert this new peer
                if (!p2pClient.ping()) {
                    kBuckets[index].remove(node);
                    kBuckets[index].add(peer);
                    sendToTail(index, peer);
                }
                // Else, move it to the end of the
                // kBucket and discard this new peer
                else
                    sendToTail(index, node);

                try {
                    p2pClient.shutdown();
                } catch (InterruptedException ie) {
                    System.out.println("ERROR: Couldn't close P2PClient. (gRPC connection)");
                    ie.printStackTrace();
                }
            }
        }
    }

    private static void processJoin(NetInfo joinResult) {
        nodeID = joinResult.nodeID;

        System.out.println("Successfully joined the network. My nodeID is " + nodeID);

        if (joinResult.peer != null) {
            // addNewPeer(joinResult.peer);
            addToKBucket(joinResult.peer);

            // On joining the network, the kBuckets will be empty,
            // which means that the peer will always be added to them.
            P2PClientRPC p2pClient = new P2PClientRPC(joinResult.peer.getAddress(), joinResult.peer.getPort());

            List<NodeInfo> prevResponse = null;
            List<NodeInfo> response = p2pClient.findNode(nodeID);
            while (!response.equals(prevResponse)) {
                for (NodeInfo node : response) {
                    // TODO: Process the received nodes
                }

                prevResponse = response;
                // TODO
                // Keep the findNode going, until both
                // prevResponse and response are the same.

                /*
                    ex:
                    p2pClient.shutdown;
                    p2pClient = new P2PClientRPC(<anotherPeer>.address, <anotherPeer>.port);
                    response = p2pClient.findNode(nodeID);
                 */
            }
        }

        // TODO: Get the blockchain
        /*
            ex:
            netClient.getBlockchain()
                    OR
            p2pClient.getBlockchain()
         */
    }

    public static void main(String[] args) {
        NetworkClient netClient = initNetClient();

        if(!netClient.ping()) {
            System.out.println("ERROR: Couldn't contact central server. Exiting...");
            return;
        }

        initKBuckets();
        NetInfo joinResult = netClient.join();
        if (joinResult == null) {
            System.out.println("ERROR: Couldn't join the network. Exiting...");
            return;
        }

        processJoin(joinResult);

        // Client joined the network.
        // It should start computing new blocks.

        // 1 Thread to compute the blocks
        // 1 Thread to handle communications
    }
}
