package GrupoB.RPC.CentralServer;

import GrupoB.gRPCService.ServerProto.NetworkInfo;
import GrupoB.gRPCService.ServerProto.NodeJoin;
import GrupoB.gRPCService.ServerProto.ServerGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CentralClient {
    private static final Logger logger = Logger.getLogger(CentralClient.class.getName());

    private final ManagedChannel channel;
    private final ServerGrpc.ServerBlockingStub blockingStub;

    /** Construct client connecting to HelloWorld server at {@code host:port}. */
    public CentralClient(String host, int port) {
        channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext(true)
                .build();
        blockingStub = ServerGrpc.newBlockingStub(channel);
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    public String join(String address, int port) {
        try {
            logger.info("Will try to join " + address+":"+port);
            NodeJoin request = NodeJoin.newBuilder()
                    .setAddress(address)
                    .setPort(port)
                    .build();
            NetworkInfo response = blockingStub.join(request);
            logger.info("NetworkInfo: " + response.getNodeID());
            return "criou " + response.getNodeID();
        } catch (RuntimeException ex) {
            logger.log(Level.WARNING, "RPC failed", ex);
            return "error";
        }
    }

}
