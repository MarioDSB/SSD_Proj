package GrupoB;

import GrupoB.ApplicationServer.Models.NetInfo;
import GrupoB.ApplicationServer.Models.Node;
import GrupoB.Blockchain.Block;
import GrupoB.Blockchain.MerkleRoot;
import GrupoB.Client.NetworkClient;
import GrupoB.RPC.P2PClient.P2PClientRPC;
import GrupoB.Utils.HashCash;
import GrupoB.gRPCService.ClientProto.NodeInfo;

import java.security.NoSuchAlgorithmException;
import java.util.*;

public class Executable {
    private final static String ADDRESS = "localhost";
    private final static int PORT = 9595;

    // Maximum contacts stored in a kBucket
    public final static int K = 3;
    // NodeID bit size
    private final static int keySize = 256;

    private static String nodeID;

    @SuppressWarnings("unchecked")
    public static ArrayList<Node>[] kBuckets = new ArrayList[keySize];

    public static List<Block> blockChain = new LinkedList<>();

    private static NetworkClient initNetClient() {
        return new NetworkClient(ADDRESS, PORT);
    }

    private static void initKBuckets() {
        for (int i = 0; i < keySize; i++)
            kBuckets[i] = new ArrayList<>();
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

    /**
     * Gets the closest node from a list of nodes in relation to a nodeID
     * @param nodeID nodeID of the node in question
     * @param nodeQueue The list of nodes
     * @return The closest node of the list
     */
    private static NodeInfo getClosestNode(String nodeID, List<NodeInfo> nodeQueue) {
        NodeInfo closestNode = null;
        int closestDistance = Integer.MAX_VALUE;

        for (NodeInfo node : nodeQueue) {
            int distance = calculateDistance(nodeID, node.getNodeID());
            if (distance < closestDistance) {
                closestDistance = distance;
                closestNode = node;
            }
        }

        return closestNode;
    }

    /**
     * Force the new node to perform a initial computation.
     * This computation has no objective other than making it harder to perform an Eclipse Attack.
     * @throws NoSuchAlgorithmException SHA-1 not supported
     */
    private static void forceComputation() throws NoSuchAlgorithmException {
        HashCash.mintCash(UUID.randomUUID().toString(), 30);
    }

    private static void processJoin(NetInfo joinResult) {
        // Force the new node to perform a initial computation.
        // This computation has no objective other than making it harder to perform an Eclipse Attack.
        try {
            forceComputation();
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Couldn't perform the initial computation. Exiting...");
            System.exit(0);
        }

        nodeID = joinResult.nodeID;

        System.out.println("Successfully joined the network. My nodeID is " + nodeID);

        // Updates the routing table (kBuckets) of the other nodes of the network
        if (joinResult.peer != null) {
            // addNewPeer(joinResult.peer);
            addToKBucket(joinResult.peer);

            // On joining the network, the kBuckets will be empty,
            // which means that the peer will always be added to them.
            P2PClientRPC p2pClient = new P2PClientRPC(joinResult.peer.getAddress(), joinResult.peer.getPort());

            List<NodeInfo> prevResponse = null;
            List<String> contactedNodes = new LinkedList<>();
            List<NodeInfo> nodeQueue = new LinkedList<>();

            List<NodeInfo> response = p2pClient.findNode(nodeID);
            contactedNodes.add(joinResult.peer.getId());
            while (!response.equals(prevResponse)) {
                if (prevResponse != null) {
                    prevResponse.clear();
                    prevResponse.addAll(response);
                } else prevResponse = response;

                nodeQueue.addAll(response);

                // Selects the closest recorded node, and checks if it was already contacted
                NodeInfo closestNode = getClosestNode(nodeID, nodeQueue);
                while (contactedNodes.contains(closestNode.getNodeID())) {
                    nodeQueue.remove(closestNode);
                    closestNode = getClosestNode(nodeID, nodeQueue);
                }

                // Close the previous P2PClient and open a new connection,
                // to the closest recorded node, creating a new findNode request
                try {
                    p2pClient.shutdown();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                p2pClient = new P2PClientRPC(closestNode.getAddress(), closestNode.getPort());
                response = p2pClient.findNode(nodeID);

                // Adds the contactedNode to the kBuckets
                // and to the list of contacted nodes
                addToKBucket(Node.fromNodeInfo(closestNode));
                contactedNodes.add(closestNode.getNodeID());
            }

            // Get the blockchain from the peer
            p2pClient = new P2PClientRPC(joinResult.peer.getAddress(), joinResult.peer.getPort());
            blockChain = p2pClient.getBlockchain();
            try {
                p2pClient.shutdown();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (blockChain == null) {
                System.out.println("Couldn't get the blockchain, although it exists. Exiting...");
                System.exit(0);
            }
        }
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


        // Client joined the network. It should start computing new blocks.
        while(true) {
            try {
                HashCash.mintCash(UUID.randomUUID().toString(), 32);

                // TODO: The block was created. Try to add it to the blockchain.

                // TODO: Create a real transactions list
                LinkedList<String> transactions = new LinkedList<>();

                Block newBlock = new Block(MerkleRoot.computeMerkleRoot(transactions), transactions);
            } catch (NoSuchAlgorithmException e) {
                System.out.println("Couldn't compute new blocks. Exiting...");
                return;
            }
        }
    }
}
