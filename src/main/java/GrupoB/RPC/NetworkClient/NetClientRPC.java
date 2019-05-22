package GrupoB.RPC.NetworkClient;

import GrupoB.Executable;
import GrupoB.gRPCService.ServerProto.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NetClientRPC {
    private static final Logger logger = Logger.getLogger(NetClientRPC.class.getName());

    private final ManagedChannel channel;
    private final ServerGrpc.ServerBlockingStub blockingStub;

    public NetClientRPC(String host, int port) {
        channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext(true)
                .build();
        blockingStub = ServerGrpc.newBlockingStub(channel);
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    /*
    // THIS FUNCTION IS JUST FOR TESTING PURPOSES
    public void requests() {
        try {
            logger.info("Will try to ping the Central Server...");

            EmptyMessage request = EmptyMessage.newBuilder().build();
            BooleanMessage response = blockingStub.ping(request);
            logger.info("Ping success: " + response.getResult());

            NodeJoin request2 = NodeJoin.newBuilder()
                    .setAddress("localhost")
                    .setPort(50051)
                    .build();
            NetworkInfo response2 = blockingStub.join(request2);
            logger.info("Received NodeID: " + response2.getNodeID());
        } catch (RuntimeException re) {
            logger.log(Level.WARNING, "RPC failed", re);
        }
    }
    */

    /** Ping the server */
    public boolean ping() {
        try {
            logger.info("Will try to ping the central server...");
            Executable.transactions.add("Will try to ping the central server...");

            EmptyMessage request = EmptyMessage.newBuilder().build();
            BooleanMessage response = blockingStub.ping(request);

            return response.getResult();
        } catch (RuntimeException re) {
            logger.log(Level.WARNING, "RPC failed", re);
            Executable.transactions.add("RPC failed");
        }

        return false;
    }

    /** Join the network*/
    public NetworkInfo join(String address, int port) {
        try {
            logger.info("Will try to join the network...");
            Executable.transactions.add("Will try to join the network...");

            NodeJoin request = NodeJoin.newBuilder()
                    .setAddress(address)
                    .setPort(port)
                    .build();

            return blockingStub.join(request);
        } catch (RuntimeException re) {
            logger.log(Level.WARNING, "RPC failed", re);
            Executable.transactions.add("RPC failed");
        }

        return null;
    }

    // Just for testing
    /*
    public static void main(String[] args) throws Exception {
        NetClientRPC client = new NetClientRPC("localhost", 50051);

        try {
            client.requests();
        } finally {
            client.shutdown();
        }
    }
    */
}
