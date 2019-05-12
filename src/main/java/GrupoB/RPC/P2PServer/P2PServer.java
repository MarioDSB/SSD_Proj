package GrupoB.RPC.P2PServer;

import GrupoB.gRPCService.ClientProto.*;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import java.util.logging.Level;
import java.util.logging.Logger;

public class P2PServer {
    private static final Logger logger = Logger.getLogger(P2PServer.class.getName());

    /* The port on which the server should run */
    private final int PORT = 50052;
    private Server server;

    private void start() throws Exception {
        server = ServerBuilder.forPort(PORT)
                .addService(ClientGrpc.bindService(new P2PServerProtoImpl()))
                .build()
                .start();
        logger.info("Server started, listening on " + PORT);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // Use stderr here since the logger may have been reset by its JVM shutdown hook.
            System.err.println("*** shutting down gRPC server since JVM is shutting down");
            P2PServer.this.stop();
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
        final P2PServer server = new P2PServer();
        server.start();
        server.blockUntilShutdown();
    }

    private class P2PServerProtoImpl implements ClientGrpc.Client {

        @Override
        public void ping(EmptyMessage request, StreamObserver<BooleanMessage> responseObserver) {
            logger.log(Level.INFO, "Receiving ping. Responding...");

            responseObserver.onNext(BooleanMessage.newBuilder().setResult(true).build());
            responseObserver.onCompleted();
        }

        @Override
        public void store(BlockData request, StreamObserver<EmptyMessage> responseObserver) {

        }

        private Nodes findNodeImpl(NodeID nodeID) {
            Nodes.Builder builder = Nodes.newBuilder();

            // TODO: Get the K closest nodes to the nodeID in question

            return builder.build();
        }

        @Override
        public void findNode(NodeID request, StreamObserver<Nodes> responseObserver) {
            logger.log(Level.INFO, "Receiving FIND_NODE request. Responding...");

            responseObserver.onNext(findNodeImpl(request));
            responseObserver.onCompleted();
        }
    }
}
