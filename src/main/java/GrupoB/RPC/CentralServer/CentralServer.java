package GrupoB.RPC.CentralServer;

import GrupoB.Utils.HashCash;
import GrupoB.gRPCService.ServerProto.*;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CentralServer {
    private static final Logger logger = Logger.getLogger(CentralServer.class.getName());

    /* The port on which the server should run */
    private final int PORT = 50051;
    private Server server;

    private List<NodeInfo> nodes = new LinkedList<>();

    private NodeInfo generator;

    // True, if the network is using Proof of Work (PoW).
    // False, if the network is using Proof of Stake (PoS).
    private boolean pow = true;

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

            // Check if the client has made the initial work
            try {
                new HashCash(request.getWork());
            } catch (Exception ignored) {
                logger.info("A client hasn't made the necessary work to join the network...");
                return builder.build();
            }

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

            NodeInfo newNode = NodeInfo.newBuilder()
                    .setAddress(request.getAddress())
                    .setPort(request.getPort())
                    .setNodeID(id)
                    .build();

            // Add the "new" node to the nodes' list
            nodes.add(newNode);

            logger.info("Number of connected nodes: " + nodes.size());

            return builder.setPow(pow).build();
        }

        @Override
        public void join(NodeJoin request, StreamObserver<NetworkInfo> responseObserver) {
            logger.log(Level.INFO, "Receiving join request. Responding...");

            responseObserver.onNext(joinImpl(request));
            responseObserver.onCompleted();
        }

        private NodeInfo genImpl() {
            NodeInfo.Builder builder = NodeInfo.newBuilder();

            if (nodes.size() == 1) {
                NodeInfo node = nodes.get(0);

                generator = node;

                return builder.setAddress(node.getAddress())
                        .setPort(node.getPort())
                        .setNodeID(node.getNodeID())
                        .build();
            } else {
                return generator;
            }
        }

        @Override
        public void generateBlock(EmptyMessage request, StreamObserver<NodeInfo> responseObserver) {
            responseObserver.onNext(genImpl());
            responseObserver.onCompleted();
        }

        private int getRandomNumberInRange(int max) {
            Random r = new Random();
            return r.nextInt(max + 1);
        }

        /**
         * Gets a new block generator. This function is only called when the current generator
         * finishes its new block generation.
         */
        @Override
        public void generation(EmptyMessage request, StreamObserver<BooleanMessage> responseObserver) {
            int generatorIndex = getRandomNumberInRange(nodes.size() - 1);

            generator = nodes.get(generatorIndex);
        }

        @Override
        public void ping(EmptyMessage request, StreamObserver<BooleanMessage> responseObserver) {
            logger.log(Level.INFO, "Receiving ping. Responding...");

            responseObserver.onNext(BooleanMessage.newBuilder().setResult(true).build());
            responseObserver.onCompleted();
        }
    }
}
