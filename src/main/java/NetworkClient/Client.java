package NetworkClient;

import GrupoB.gRPCService.ServerProto.EmptyMessage;
import GrupoB.gRPCService.ServerProto.BooleanMessage;
import GrupoB.gRPCService.ServerProto.ServerGrpc;
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
    public void ping() {
        try {
            logger.info("Will try to ping the Central Server...");
            EmptyMessage request = EmptyMessage.newBuilder().build();
            BooleanMessage response = blockingStub.ping(request);
            logger.info("Ping success: " + response.getResult());
        } catch (RuntimeException re) {
            logger.log(Level.WARNING, "RPC failed", re);
        }
    }

    public static void main(String[] args) throws Exception {
        Client client = new Client("localhost", 50051);

        try {
            client.ping();
        } finally {
            client.shutdown();
        }
    }
}
