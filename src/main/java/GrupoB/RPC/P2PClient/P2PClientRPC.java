package GrupoB.RPC.P2PClient;

import GrupoB.gRPCService.ClientProto.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

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
            logger.info("Will try to ping the peer...");
            EmptyMessage request = EmptyMessage.newBuilder().build();
            BooleanMessage response = blockingStub.ping(request);

            return response.getResult();
        } catch (RuntimeException re) {
            logger.log(Level.WARNING, "RPC failed", re);
        }

        return false;
    }

    /**
     * Tries to find a node by its nodeID
     * @param nodeID The nodeID of the node to find
     * @return A list of nodes closest to the target
     */
    public List<NodeInfo> findNode(String nodeID) {
        try {
            logger.info("Will try to find node " + nodeID + "...");
            NodeID request = NodeID.newBuilder().setNodeID(nodeID).build();

            return blockingStub.findNode(request).getNodesList();
        } catch (RuntimeException re) {
            logger.log(Level.WARNING, "RPC failed", re);
        }

        return null;
    }
}
