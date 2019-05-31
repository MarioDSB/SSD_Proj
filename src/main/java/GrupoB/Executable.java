package GrupoB;

import GrupoB.ApplicationServer.Models.NetInfo;
import GrupoB.ApplicationServer.Models.Node;
import GrupoB.Blockchain.Block;
import GrupoB.Blockchain.MerkleRoot;
import GrupoB.Client.NetworkClient;
import GrupoB.RPC.NetworkClient.NetClientRPC;
import GrupoB.RPC.P2PClient.P2PClientRPC;
import GrupoB.RPC.P2PServer.P2PServer;
import GrupoB.Utils.HashCash;
import GrupoB.Utils.NetUtils;
import GrupoB.gRPCService.ClientProto.NodeInfo;

import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Executable {
    private static String ADDRESS = "";
    private final static int PORT = 50052;

    // private static String SERVER_ADDRESS = "localhost";
    private static String SERVER_ADDRESS = "10.164.0.2";
    private static int SERVER_PORT = 50051;

    // Maximum contacts stored in a kBucket
    private final static int K = 3;
    // NodeID bit size
    private final static int keySize = 256;

    public static LinkedList<String> transactions = new LinkedList<>();

    private static String nodeID;

    @SuppressWarnings("unchecked")
    private static ArrayList<Node>[] kBuckets = new ArrayList[keySize];

    public static List<Block> blockChain = new LinkedList<>();

    // private static NetworkClient netClient;

    private static NetClientRPC netClient;

    private static boolean pow = true;

    private static NetworkClient initNetworkClient() {
        return new NetworkClient(ADDRESS, PORT);
    }

    private static NetClientRPC initNetClient() {
        return new NetClientRPC(SERVER_ADDRESS, SERVER_PORT);
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
    private static long calculateDistance(String node1, String node2) {
        long myDecimalID = Long.parseLong(node1, 16);
        long peerDecimalID = Long.parseLong(node2, 16);

        return myDecimalID ^ peerDecimalID;
    }

    /**
     * Calculates the index of the kBucket the peer belongs to
     * @param peerID The nodeID of the peer in question
     * @return The kBucket in which the peer belongs
     */
    public static int calculateKBucket(String peerID) {
        long distance = calculateDistance(nodeID, peerID);

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
            kBuckets[index].set(i, kBuckets[index].get(i + 1));

        kBuckets[index].set(i, peer);
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
                kBuckets[index].add(kBuckets[index].size(), peer);
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
        long closestDistance = Long.MAX_VALUE;

        for (NodeInfo node : nodeQueue) {
            long distance = calculateDistance(nodeID, node.getNodeID());
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
    private static String forceComputation() throws NoSuchAlgorithmException {
        return HashCash.mintCash(UUID.randomUUID().toString(), 24).toString();
    }

    private static void processJoin(NetInfo joinResult) {
        nodeID = joinResult.nodeID;
        pow = joinResult.pow;

        System.out.println("Successfully joined the network. My nodeID is " + nodeID);

        // Updates the routing table (kBuckets) of the other nodes of the network
        if (joinResult.peer != null) {
            addToKBucket(joinResult.peer);

            // On joining the network, the kBuckets will be empty,
            // which means that the peer will always be added to them.
            P2PClientRPC p2pClient = new P2PClientRPC(joinResult.peer.getAddress(), joinResult.peer.getPort());

            List<NodeInfo> prevResponse = new LinkedList<>();
            List<String> contactedNodes = new LinkedList<>();
            List<NodeInfo> nodeQueue = new LinkedList<>();

            List<NodeInfo> response = p2pClient.findNode(ADDRESS, PORT, nodeID);
            if (response == null) {
                System.out.println("Wasn't able to join the network. Exiting...");
                System.exit(0);
            }

            System.out.println("#Response nodes: " + response.size());
            if (response.size() != 0) {
                contactedNodes.add(joinResult.peer.getId());
                while (!response.containsAll(prevResponse) || prevResponse.size() == 0) {
                    if (prevResponse.size() != 0) {
                        prevResponse.clear();
                        prevResponse.addAll(response);
                    } else prevResponse.addAll(response);

                    nodeQueue.addAll(response);

                    // Selects the closest recorded node, and checks if it was already contacted
                    NodeInfo closestNode = getClosestNode(nodeID, nodeQueue);
                    System.out.println(closestNode.getNodeID());
                    while (contactedNodes.contains(closestNode.getNodeID())) {
                        nodeQueue.remove(closestNode);
                        if (nodeQueue.size() == 0)
                            break;
                        closestNode = getClosestNode(nodeID, nodeQueue);
                    }

                    if (nodeQueue.size() != 0) {
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
                        // Remove the contactedNode from the queue of nodes to contact
                        addToKBucket(Node.fromNodeInfo(closestNode));
                        contactedNodes.add(closestNode.getNodeID());
                        nodeQueue.remove(closestNode);
                    } else break;
                }
            }

            // Get the blockchain from the peer
            p2pClient = new P2PClientRPC(joinResult.peer.getAddress(), joinResult.peer.getPort());
            blockChain = p2pClient.getBlockchain();
            System.out.println("Got " + blockChain.size() + " blocks from the peers");
            transactions.add("Got " + blockChain.size() + " blocks from the peers");
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
        long maxDistance = calculateDistance(nodeID, furthestNode.getNodeID());

        for (int i = 1; i < closestNodes.size(); i++) {
            long aux = calculateDistance(nodeID, closestNodes.get(i).getNodeID());
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
            if (indexToSearch + indexOffset < 0 || indexToSearch + indexOffset >= kBuckets.length) {
                return closestNodes;
            }

            return getClosestNodes(id, indexToSearch, indexOffset, closestNodes, contactedNodes);
        }

        for (int i = 0; i < kBuckets[indexToSearch].size(); i++) {
            closestNodes = checkAndInsert(id, Node.toNodeInfo(kBuckets[indexToSearch].get(i)), closestNodes);
        }

        /*  Remove the nodes we DON'T WANT to contact.
            This is used only when we already contacted some nodes and don't want to contact them again,
            to avoid having communication loops in the network. */
        for (NodeInfo node : contactedNodes) {
            closestNodes.remove(node);
        }

        indexOffset = adjustOffset(indexOffset);

        // If the closestNodes is filled
        if (closestNodes.size() == Executable.K)
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
                                           LinkedList<NodeInfo> contactedNodes, String cash) {
        contactedNodes.addAll(closestNodes);

        for (NodeInfo node : closestNodes) {
            P2PClientRPC p2pClientRPC = new P2PClientRPC(node.getAddress(), node.getPort());

            p2pClientRPC.store(Block.blockToBlockData(newBlock), Node.toNodes(contactedNodes), cash);
            try {
                p2pClientRPC.shutdown();
            } catch (InterruptedException e) {
                System.out.println("ERROR: Couldn't close P2PClient. (gRPC connection)");
                e.printStackTrace();
            }
        }
    }

    private static void performStoreRequest(Block newBlock, LinkedList<NodeInfo> closestNodes, String cash) {
        NodeInfo myself = NodeInfo.newBuilder().setAddress(ADDRESS).setPort(PORT).setNodeID(nodeID).build();
        LinkedList<NodeInfo> contactedNodes = new LinkedList<>();
        contactedNodes.add(myself);

        performStoreRequest(newBlock, closestNodes, contactedNodes, cash);
    }

    /**
     * Starts computing new blocks, using the Proof of Work method.
     */
    private static void proofOfWork() {
        while(true) {
            try {
                // Just to avoid having forked chains, as there is no prevention against that...
                TimeUnit.SECONDS.sleep(5);

                System.out.println("Generating a new block");
                transactions.add("Generating a new block");

                String cash = HashCash.mintCash(UUID.randomUUID().toString(), 28).toString();

                System.out.println("I made a new block!");
                transactions.add("I made a new block!");

                Block newBlock = new Block(MerkleRoot.computeMerkleRoot(transactions), transactions);

                // Now that the new block has been created, the transactions have been saved in the blockchain.
                // Reset the transactions list, so that we can save other transactions again.
                transactions.clear();

                // Get the K closest nodes to the block's ID.
                int index = calculateKBucket(newBlock.getBlockID());
                LinkedList<NodeInfo> closestNodes = getClosestNodes(newBlock.getBlockID(), index);

                blockChain.add(newBlock);
                System.out.println("Blocks in the chain: " + blockChain.size());
                performStoreRequest(newBlock, closestNodes, cash);

                transactions.add("I gossiped the block!");
            } catch (NoSuchAlgorithmException e) {
                System.out.println("Couldn't compute new blocks. Exiting...");
                return;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void performStorePoS(Block newBlock, LinkedList<NodeInfo> closestNodes,
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

    private static void performStorePoS(Block newBlock, LinkedList<NodeInfo> closestNodes) {
        NodeInfo myself = NodeInfo.newBuilder().setAddress(ADDRESS).setPort(PORT).setNodeID(nodeID).build();
        LinkedList<NodeInfo> contactedNodes = new LinkedList<>();
        contactedNodes.add(myself);

        performStorePoS(newBlock, closestNodes, contactedNodes);
    }

    private static Node currentGenerator;

    private static void getGenerator() {
        currentGenerator = Node.fromNodeInfo(netClient.generateBlock());
        System.out.println("Node " + currentGenerator.getId() + " is generating a block");
        transactions.add("Node " + currentGenerator.getId() + " is generating a block");
    }

    private static void proofOfStake() {
        getGenerator();

        while(true) {
            try {
                // This node is the one generating the block
                if (currentGenerator.getId().equals(nodeID)) {
                    System.out.println("I'm making a block");
                    transactions.add("I'm making a block");

                    Block newBlock = new Block(MerkleRoot.computeMerkleRoot(transactions), transactions);

                    System.out.println("I made a block!");
                    transactions.add("I made a block!");

                    // Now that the new block has been created, the transactions have been saved in the blockchain.
                    // Reset the transactions list, so that we can save other transactions again.
                    transactions.clear();

                    // netClient.blockGenerated();
                    netClient.generation();

                    // Get the K closest nodes to the block's ID
                    int index = calculateKBucket(newBlock.getBlockID());
                    LinkedList<NodeInfo> closestNodes = getClosestNodes(newBlock.getBlockID(), index);

                    blockChain.add(newBlock);
                    System.out.println("Blocks in the chain: " + blockChain.size());
                    performStorePoS(newBlock, closestNodes);

                    System.out.println("I gossiped the block and passed on the generation of a new one!");
                    transactions.add("I gossiped the block and passed on the generation of a new one!");
                }

                // Workaround to prevent spamming the server with requests
                forceComputation();
                getGenerator();
            } catch (Exception ignored) {
                return;
            }
        }
    }

    /**
     * Starts the application that lets the client generate blocks.
     * @param args No arguments need to be provided
     */
    public static void main(String[] args) {
        ADDRESS = NetUtils.getLocalIP();
        if (ADDRESS == null || ADDRESS.equals("")) {
            System.out.println("Couldn't get this machine's IP address. Exiting...");
            return;
        }

        // netClient = initNetworkClient();
        netClient = initNetClient();

        if(!netClient.ping()) {
            System.out.println("ERROR: Couldn't contact central server. Exiting...");
            return;
        }

        // Force the new node to perform a initial computation.
        // This computation has no objective other than making it harder to perform an Eclipse attack.
        String work;
        try {
            work = forceComputation();
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Couldn't perform the initial computation. Exiting...");
            return;
        }

        // NetInfo joinResult = netClient.join(work);
        NetInfo joinResult = NetInfo.fromNetworkInfo(netClient.join(ADDRESS, PORT, work));

        if (joinResult.nodeID.equals("")) {
            System.out.println("ERROR: Couldn't join the network. Exiting...");
            return;
        }

        initKBuckets();
        // Launch the P2PServer
        Thread thread = new Thread(() -> {
            try {
                P2PServer.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        thread.start();

        processJoin(joinResult);

        // Client joined the network. It should start computing new blocks.
        if (pow) {
            proofOfWork();
        } else {
            proofOfStake();
        }
    }
}
