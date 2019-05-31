package GrupoB.RPC.P2PClient;

import GrupoB.Blockchain.Block;
import GrupoB.Executable;
import GrupoB.gRPCService.ClientProto.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class P2PClientRPC {
    private static final Logger logger = Logger.getLogger(P2PClientRPC.class.getName());

    private final ManagedChannel channel;
    private final ClientGrpc.ClientBlockingStub blockingStub;

    public P2PClientRPC(String host, int port) {
        channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext(true)
                .build();
        blockingStub = ClientGrpc.newBlockingStub(channel);
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    /** Ping the peer */
    public boolean ping() {
        try {
            logger.info("Will try to ping a peer...");
            Executable.transactions.add("Will try to ping a peer...");
            EmptyMessage request = EmptyMessage.newBuilder().build();
            BooleanMessage response = blockingStub.ping(request);

            return response.getResult();
        } catch (RuntimeException re) {
            Executable.transactions.add("RPC failed");
            logger.log(Level.WARNING, "RPC failed", re);
        }

        return false;
    }

    /**
     * Tries to find a node by its nodeID
     * @param nodeID The nodeID of the node to find
     * @return A list of nodes closest to the target
     */
    public List<NodeInfo> findNode(String address, int port, String nodeID) {
        try {
            logger.info("Will try to find node " + nodeID + "...");
            Executable.transactions.add("Will try to find node " + nodeID + "...");
            NodeInfo request = NodeInfo.newBuilder()
                    .setAddress(address)
                    .setPort(port)
                    .setNodeID(nodeID)
                    .build();

            return blockingStub.findNode(request).getNodesList();
        } catch (RuntimeException re) {
            Executable.transactions.add("RPC failed. Trying again.");
            logger.log(Level.WARNING, "RPC failed. Trying again.", re);

            try {
                logger.info("Will try to find node " + nodeID + "...");
                Executable.transactions.add("Will try to find node " + nodeID + "...");
                NodeInfo request = NodeInfo.newBuilder()
                        .setAddress(address)
                        .setPort(port)
                        .setNodeID(nodeID)
                        .build();

                return blockingStub.findNode(request).getNodesList();
            } catch (RuntimeException re2) {
                Executable.transactions.add("RPC failed. Trying again.");
                logger.log(Level.WARNING, "RPC failed. Trying again.", re2);
            }
        }

        return null;
    }

    public void store(BlockData newBlock, Nodes contactedNodes) {
        try {
            logger.info("Will try to send a request to store (PoS) a block...");
            Executable.transactions.add("Will try to send a request to store (PoS) a block...");

            StoreData request = StoreData.newBuilder()
                    .setBlock(newBlock)
                    .setNodes(contactedNodes)
                    .build();

            blockingStub.storePoS(request);
        } catch (RuntimeException re) {
            Executable.transactions.add("RPC failed");
            logger.log(Level.WARNING, "RPC failed", re);
        }
    }

    public void store(BlockData newBlock, Nodes contactedNodes, String cash) {
        try {
            logger.info("Will try to send a request to store a block...");
            Executable.transactions.add("Will try to send a request to store a block...");

            StoreData request = StoreData.newBuilder()
                    .setBlock(newBlock)
                    .setNodes(contactedNodes)
                    .setCash(cash)
                    .build();

            blockingStub.store(request);
        } catch (RuntimeException re) {
            Executable.transactions.add("RPC failed");
            logger.log(Level.WARNING, "RPC failed", re);
        }
    }

    public LinkedList<Block> getBlockchain() {
        try {
            logger.info("Will try to get the blockchain...");
            Executable.transactions.add("Will try to get the blockchain...");

            EmptyMessage request = EmptyMessage.newBuilder().build();
            Blocks blocks = blockingStub.getBlockchain(request);

            return Block.blockListFromBlocks(blocks);
        } catch (RuntimeException re) {
            Executable.transactions.add("RPC failed");
            logger.log(Level.WARNING, "RPC failed", re);
        }

        return null;
    }
}
