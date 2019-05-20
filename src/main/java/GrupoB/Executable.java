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
    //TODO: Get the real public IP address (by argv[] or by coding like the IPTest)
    private final static String ADDRESS = "localhost";
    private final static int PORT = 9595;

    // Maximum contacts stored in a kBucket
    private final static int K = 3;
    // NodeID bit size
    private final static int keySize = 256;

    private static String nodeID;

    @SuppressWarnings("unchecked")
    private static ArrayList<Node>[] kBuckets = new ArrayList[keySize];

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
    private static int calculateDistance(String node1, String node2) {
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
    private static void sendToTail(int index, Node peer) {
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
    public static void addToKBucket(Node peer) {
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

                // If it fails to respond, remove it from the kBucket and insert this new peer
                if (!p2pClient.ping()) {
                    kBuckets[index].remove(node);
                    kBuckets[index].add(peer);
                    sendToTail(index, peer);
                }
                // Else, move it to the end of the kBucket and discard this new peer
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
        nodeID = joinResult.nodeID;

        System.out.println("Successfully joined the network. My nodeID is " + nodeID);

        // Updates the routing table (kBuckets) of the other nodes of the network
        if (joinResult.peer != null) {
            addToKBucket(joinResult.peer);

            // On joining the network, the kBuckets will be empty,
            // which means that the peer will always be added to them.
            P2PClientRPC p2pClient = new P2PClientRPC(joinResult.peer.getAddress(), joinResult.peer.getPort());

            List<NodeInfo> prevResponse = null;
            List<String> contactedNodes = new LinkedList<>();
            List<NodeInfo> nodeQueue = new LinkedList<>();

            List<NodeInfo> response = p2pClient.findNode(ADDRESS, PORT, nodeID);
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
                    System.out.println("ERROR: Couldn't close P2PClient. (gRPC connection)");
                    e.printStackTrace();
                }
                p2pClient = new P2PClientRPC(closestNode.getAddress(), closestNode.getPort());
                response = p2pClient.findNode(ADDRESS, PORT, nodeID);

                // Adds the contactedNode to the kBuckets and to the list of contacted nodes
                addToKBucket(Node.fromNodeInfo(closestNode));
                contactedNodes.add(closestNode.getNodeID());
            }

            // Get the blockchain from the peer
            p2pClient = new P2PClientRPC(joinResult.peer.getAddress(), joinResult.peer.getPort());
            blockChain = p2pClient.getBlockchain();
            try {
                p2pClient.shutdown();
            } catch (InterruptedException e) {
                System.out.println("ERROR: Couldn't close P2PClient. (gRPC connection)");
                e.printStackTrace();
            }

            if (blockChain == null) {
                System.out.println("Couldn't get the blockchain, although it exists. Exiting...");
                System.exit(0);
            }
        }
    }

    /**
     * Check if there is some node further away than the nodeToInsert,
     * and replace it, if it is the case
     * @param nodeID nodeID of the node we are searching
     * @param nodeToInsert node we want to check
     */
    private static LinkedList<NodeInfo> checkAndInsert(String nodeID, NodeInfo nodeToInsert,
                                                LinkedList<NodeInfo> closestNodes) {

        // If there is still space for another node, insert it
        if (closestNodes.size() < K) {
            closestNodes.add(nodeToInsert);
            return closestNodes;
        }

        // Get the furthest node from the nodeID
        NodeInfo furthestNode = closestNodes.get(0);
        int maxDistance = calculateDistance(nodeID, furthestNode.getNodeID());

        for (int i = 1; i < closestNodes.size(); i++) {
            int aux = calculateDistance(nodeID, closestNodes.get(i).getNodeID());
            if (aux > maxDistance) {
                maxDistance = aux;
                furthestNode = closestNodes.get(i);
            }
        }

        // If the nodeToInsert is closer than the furthestNode,
        // replace the furthestNode with the nodeToInsert
        if (calculateDistance(nodeID, nodeToInsert.getNodeID()) < maxDistance) {
            closestNodes.remove(furthestNode);
            closestNodes.add(nodeToInsert);
        }

        return closestNodes;
    }

    /**
     * Simple routine to adjust the offset while traversing the kBuckets
     * @param indexOffset the initial offset
     * @return the adjusted offset
     */
    private static int adjustOffset(int indexOffset) {
        if (indexOffset <= 0) {
            indexOffset -= 1;
            indexOffset = -indexOffset;
        } else {
            indexOffset += 1;
            indexOffset = -indexOffset;
        }

        return indexOffset;
    }

    /**
     * Searches up and down, starting on the kBucket[indexToSearch],
     * trying to get the K closest peers to the nodeID
     * @param id nodeID in question
     * @param indexToSearch starting index
     * @param indexOffset index offset
     */
    private static LinkedList<NodeInfo> getClosestNodes(String id, int indexToSearch, int indexOffset,
                                                       LinkedList<NodeInfo> closestNodes,
                                                       LinkedList<NodeInfo> contactedNodes) {
        indexToSearch = indexToSearch + indexOffset;

        // If the indexToSearch is out-of-bounds, try to search the other way
        if (indexToSearch < 0 || indexToSearch >= kBuckets.length) {
            indexOffset = adjustOffset(indexOffset);

            // If all the kBuckets have been searched, return the uncompleted list
            if (indexToSearch + indexOffset < 0 || indexToSearch + indexOffset >= kBuckets.length)
                return closestNodes;

            return getClosestNodes(id, indexToSearch, indexOffset, closestNodes, contactedNodes);
        }

        for (int i = 0; i < K; i++) {
            closestNodes = checkAndInsert(id, Node.toNodeInfo(kBuckets[indexToSearch].get(i)), closestNodes);
        }

        /*  Remove the nodes we don't want to contact.
            This is used only when we already contacted some nodes and don't want to contact them again,
            to avoid having communication loops in the network. */
        for (NodeInfo node : contactedNodes) {
            closestNodes.remove(node);
        }

        int prevAbs = Math.abs(indexToSearch - indexOffset);

        indexOffset = adjustOffset(indexOffset);

        // If the closestNodes is filled...
        if (closestNodes.size() == Executable.K)
            // ... and there are no more nodes that may be closer...
            if (prevAbs != Math.abs(indexToSearch - indexOffset))
                return closestNodes;

        return getClosestNodes(id, indexToSearch, indexOffset, closestNodes, contactedNodes);
    }


    public static LinkedList<NodeInfo> getClosestNodes(String id, int indexToSearch) {
        return getClosestNodes(id, indexToSearch, 0, new LinkedList<>(), new LinkedList<>());
    }

    public static LinkedList<NodeInfo> getClosestNodes(String id, int indexToSearch,
                                                       LinkedList<NodeInfo> contactedNodes) {
        return getClosestNodes(id, indexToSearch, 0, new LinkedList<>(), contactedNodes);
    }

    public static void performStoreRequest(Block newBlock, LinkedList<NodeInfo> closestNodes,
                                           LinkedList<NodeInfo> contactedNodes) {
        contactedNodes.addAll(closestNodes);

        for (NodeInfo node : closestNodes) {
            P2PClientRPC p2pClientRPC = new P2PClientRPC(node.getAddress(), node.getPort());

            p2pClientRPC.store(Block.blockToBlockData(newBlock), Node.toNodes(contactedNodes));
            try {
                p2pClientRPC.shutdown();
            } catch (InterruptedException e) {
                System.out.println("ERROR: Couldn't close P2PClient. (gRPC connection)");
                e.printStackTrace();
            }
        }
    }

    private static void performStoreRequest(Block newBlock, LinkedList<NodeInfo> closestNodes) {
        performStoreRequest(newBlock, closestNodes, new LinkedList<>());
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

        // Force the new node to perform a initial computation.
        // This computation has no objective other than making it harder to perform an Eclipse attack.
        try {
            forceComputation();
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Couldn't perform the initial computation. Exiting...");
            System.exit(0);
        }

        processJoin(joinResult);

        // Client joined the network. It should start computing new blocks.
        while(true) {
            try {
                HashCash.mintCash(UUID.randomUUID().toString(), 26);

                // TODO: Create a real transactions list
                LinkedList<String> transactions = new LinkedList<>();

                Block newBlock = new Block(MerkleRoot.computeMerkleRoot(transactions), transactions);

                // Get the K closest nodes to the block's merkle root hash
                int index = calculateKBucket(newBlock.getMerkleRoot());
                LinkedList<NodeInfo> closestNodes = getClosestNodes(newBlock.getMerkleRoot(), index);
                performStoreRequest(newBlock, closestNodes);
            } catch (NoSuchAlgorithmException e) {
                System.out.println("Couldn't compute new blocks. Exiting...");
                return;
            }
        }
    }
}
