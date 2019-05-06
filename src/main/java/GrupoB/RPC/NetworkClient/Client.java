package GrupoB.RPC.NetworkClient;

import GrupoB.gRPCService.ServerProto.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Client {
    private static final Logger logger = Logger.getLogger(Client.class.getName());

    private final ManagedChannel channel;
    private final ServerGrpc.ServerBlockingStub blockingStub;

    public Client(String host, int port) {
        channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext(true)
                .build();
        blockingStub = ServerGrpc.newBlockingStub(channel);
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    /** Ping the server */
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

    /** Ping the server */
    public boolean ping() {
        try {
            logger.info("Will try to ping the Central Server...");
            EmptyMessage request = EmptyMessage.newBuilder().build();
            BooleanMessage response = blockingStub.ping(request);

            return response.getResult();
        } catch (RuntimeException re) {
            logger.log(Level.WARNING, "RPC failed", re);
        }

        return false;
    }

    /** Join the network*/
    public NetworkInfo join(String address, int port) {
        try {
            NodeJoin request = NodeJoin.newBuilder()
                    .setAddress(address)
                    .setPort(port)
                    .build();

            return blockingStub.join(request);
        } catch (RuntimeException re) {
            logger.log(Level.WARNING, "RPC failed", re);
        }

        return null;
    }

    public static void main(String[] args) throws Exception {
        Client client = new Client("localhost", 50051);

        try {
            client.requests();
        } finally {
            client.shutdown();
        }
    }
}
