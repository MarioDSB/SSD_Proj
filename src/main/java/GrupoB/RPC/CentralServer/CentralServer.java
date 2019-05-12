package GrupoB.RPC.CentralServer;

import GrupoB.gRPCService.ServerProto.*;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CentralServer {
    private static final Logger logger = Logger.getLogger(CentralServer.class.getName());

    /* The port on which the server should run */
    private final int PORT = 50051;
    private Server server;

    private List<NodeInfo> nodes = new LinkedList<>();

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

    private class ServerProtoImpl implements ServerGrpc.Server {

        private NetworkInfo joinImpl(NodeJoin request) {
            NetworkInfo.Builder builder = NetworkInfo.newBuilder();

            String id = "";
            // Check if the node has already connected. If it has,
            // remove it from the list, to ease up the peer fetch.
            for (NodeInfo node : nodes) {
                if (node.getAddress().equals(request.getAddress())
                        && node.getPort() == request.getPort()) {
                    logger.log(Level.INFO, "Repeated connection. Returning the same nodeID...");

                    id = node.getNodeID();
                    nodes.remove(node);

                    break;
                }
            }

            // No recorded connection from <address>:<port>. Generate a random nodeID.
            if (id.equals(""))
                id = UUID.randomUUID().toString().replace("-", "");

            builder.setNodeID(id);

            // Random peer fetch.
            if (!nodes.isEmpty()) {
                int index = (int) (Math.random() * (nodes.size() - 1));
                builder.setPeer(nodes.get(index));
            }

            // Add the "new" node to the nodes' list
            nodes.add(NodeInfo.newBuilder()
                    .setAddress(request.getAddress())
                    .setPort(request.getPort())
                    .setNodeID(id)
                    .build());

            logger.info("Number of connected nodes: " + nodes.size());

            return builder.build();
        }

        @Override
        public void join(NodeJoin request, StreamObserver<NetworkInfo> responseObserver) {
            logger.log(Level.INFO, "Receiving join request. Responding...");

            responseObserver.onNext(joinImpl(request));
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
