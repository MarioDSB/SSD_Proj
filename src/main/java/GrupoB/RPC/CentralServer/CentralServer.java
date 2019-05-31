package GrupoB.RPC.CentralServer;

import GrupoB.Utils.HashCash;
import GrupoB.gRPCService.ServerProto.*;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CentralServer {
    private static final Logger logger = Logger.getLogger(CentralServer.class.getName());

    /* The port on which the server should run */
    private final int PORT = 50051;
    private Server server;

    private List<NodeInfo> nodes = new LinkedList<>();

    private NodeInfo generator = null;

    // True, if the network is using Proof of Work (PoW).
    // False, if the network is using Proof of Stake (PoS).
    private static boolean pow = true;

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

    private static void showUI() {
        System.out.println("Choose proof module:");
        System.out.println("1: Proof of Work");
        System.out.println("2: Proof of Stake");
    }

    /**
     * Main launches the server from the command line.
     */
    public static void main(String[] args) throws Exception {
        Scanner in = new Scanner(System.in);
        int choice;

        do {
            showUI();
            choice = in.nextInt();
            switch (choice) {
                case 1:
                    pow = true;
                    break;
                case 2:
                    pow = false;
                    break;
                default:
                    System.out.println("Option not supported. Choose 1 or 2.");
                    break;
            }

        } while (choice < 1 || choice > 2);

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
            if (id.equals("")) {
                String uuid = UUID.randomUUID().toString().replace("-", "");

                // We only use 1/4 of the generated uuid
                id = String.valueOf(uuid.toCharArray(), 0, uuid.length() / 4);
            }

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
            if (nodes.size() == 1)
                generator = newNode;

            logger.info("Number of connected nodes: " + nodes.size());

            return builder.setPow(pow).build();
        }

        @Override
        public void join(NodeJoin request, StreamObserver<NetworkInfo> responseObserver) {
            logger.log(Level.INFO, "Receiving join request from " + request.getAddress() + ":" + request.getPort()
                    + ". Processing...");

            responseObserver.onNext(joinImpl(request));
            responseObserver.onCompleted();
        }

        private NodeInfo genImpl() {
            return generator;
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
            logger.log(Level.INFO, "Block created. Setting new generator...");

            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            int generatorIndex = getRandomNumberInRange(nodes.size() - 1);

            generator = nodes.get(generatorIndex);
            System.out.println("Generator: " + generator.getNodeID());

            responseObserver.onNext(BooleanMessage.newBuilder().setResult(true).build());
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
