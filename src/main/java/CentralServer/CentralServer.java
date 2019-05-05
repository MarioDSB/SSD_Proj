package CentralServer;

import GrupoB.gRPCService.ServerProto.*;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CentralServer {
    private static final Logger logger = Logger.getLogger(CentralServer.class.getName());

    /* The port on which the server should run */
    private final int PORT = 50051;
    private Server server;

    private void start() throws Exception {
        server = ServerBuilder.forPort(PORT)
                .addService(ServerGrpc.bindService(new ServerProtoImpl()))
                .build()
                .start();
        logger.info("Server started, listening on " + PORT);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // Use stderr here since the logger may have been reset by its JVM shutdown hook.
            System.err.println("*** shutting down gRPC server since JVM is shutting down");
            CentralServer.this.stop();
            System.err.println("*** server shut down");
        }));
    }

    private void stop() {
        if (server != null)
            server.shutdown();
    }

    /**
     * Await termination on the main thread since the grpc library uses daemon threads.
     */
    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    /**
     * Main launches the server from the command line.
     */
    public static void main(String[] args) throws Exception {
        final CentralServer server = new CentralServer();
        server.start();
        server.blockUntilShutdown();
    }

    private NetworkInfo joinImpl(NodeJoin request) {
        NetworkInfo.Builder builder = NetworkInfo.newBuilder();
        String id = UUID.randomUUID().toString().replace("-", "");
        NodeInfo info = null;

        // Write new node on file with all nodes
        File nodesFile = new File("nodeList.txt");
        try {
            if (!nodesFile.createNewFile()) {
                BufferedReader reader = new BufferedReader(new FileReader(nodesFile.getPath()));
                String line = reader.readLine();
                reader.close();

                String[] split = line.split(":");

                info = NodeInfo.newBuilder()
                        .setAddress(split[0])
                        .setPort(Integer.parseInt(split[1]))
                        .setNodeID(split[2])
                        .build();
            }

            Files.write(nodesFile.toPath(), (request.getAddress()
                    + ":" + request.getPort()
                    + ":" + id).getBytes());
        } catch (Exception ignored) {
            logger.log(Level.WARNING, "Unable to create or read nodes' file");
        }

        builder.setNodeID(id).setPeers(0, info);

        return builder.build();
    }

    private class ServerProtoImpl implements ServerGrpc.Server {

        @Override
        public void join(NodeJoin request, StreamObserver<NetworkInfo> responseObserver) {
            logger.log(Level.INFO, "Receiving join request. Responding...");

            NetworkInfo netInfo = joinImpl(request);
            NetworkInfo.Builder builder = NetworkInfo.newBuilder()
                    .setNodeID(netInfo.getNodeID());

            // Only 1 peer is being returned. This for-loop is not needed!
            for (int i = 0; i < netInfo.getPeersList().size(); i++)
                builder.setPeers(i, netInfo.getPeers(i));

            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }

        @Override
        public void ping(EmptyMessage request, StreamObserver<BooleanMessage> responseObserver) {
            logger.log(Level.INFO, "Receiving ping. Responding...");

            responseObserver.onNext(BooleanMessage.newBuilder().setResult(true).build());
            responseObserver.onCompleted();
        }
    }
}
