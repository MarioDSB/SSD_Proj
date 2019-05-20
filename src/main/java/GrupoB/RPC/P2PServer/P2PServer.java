package GrupoB.RPC.P2PServer;

import GrupoB.ApplicationServer.Models.Node;
import GrupoB.Blockchain.Block;
import GrupoB.Executable;
import GrupoB.gRPCService.ClientProto.*;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import java.util.LinkedList;
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

        /**
         * Tries to store a block into the blockchain, and tries to propagate this new block to other peers.
         * Only propagates the block to peers who haven't yet been contacted.
         * @param storeData The store request. It saves the block itself and keeps track
         *                  of nodes that have already been contacted
         */
        private void storeImpl(StoreData storeData) {
            // Try to add the new block to the block chain, verifying it
            if (Block.blockFromBlockData(storeData.getBlock()).verifyBlock())
                Executable.blockChain.add(Block.blockFromBlockData(storeData.getBlock()));
            else
                return;

            LinkedList<NodeInfo> contactedNodes = Node.toNodeInfoList(storeData.getNodes());

            // Contact other nodes (that weren't yet contacted), to update their blockchain
            int index = Executable.calculateKBucket(storeData.getBlock().getMerkleRoot());
            LinkedList<NodeInfo> closestNodes = Executable.getClosestNodes(storeData.getBlock().getMerkleRoot(), index,
                    contactedNodes);

            Executable.performStoreRequest(Block.blockFromBlockData(storeData.getBlock()), closestNodes, contactedNodes);
        }

        @Override
        public void store(StoreData request, StreamObserver<EmptyMessage> responseObserver) {
            logger.log(Level.INFO, "Receiving STORE request. Processing...");

            responseObserver.onNext(EmptyMessage.newBuilder().build());
            responseObserver.onCompleted();

            // Contact other nodes AFTER responding to the sender. That way, the sender can continue working
            // without having to wait for the block to be propagated to the entire network
            storeImpl(request);
        }

        private Nodes findNodeImpl(NodeInfo nodeID) {
            // Try to add the new connection to the kBuckets
            Executable.addToKBucket(Node.fromNodeInfo(nodeID));

            Nodes.Builder builder = Nodes.newBuilder();

            // Finds the kBucket with the closest nodes to nodeID
            int index = Executable.calculateKBucket(nodeID.getNodeID());

            LinkedList<NodeInfo> closestNodes = Executable.getClosestNodes(nodeID.getNodeID(), index);
            for (int i = 0; i < closestNodes.size(); i++)
                builder.setNodes(i, closestNodes.get(i));

            return builder.build();
        }

        @Override
        public void findNode(NodeInfo request, StreamObserver<Nodes> responseObserver) {
            logger.log(Level.INFO, "Receiving FIND_NODE request. Responding...");

            responseObserver.onNext(findNodeImpl(request));
            responseObserver.onCompleted();
        }

        private Blocks getBCImpl() {
            Blocks.Builder builder = Blocks.newBuilder();

            for (Block block : Executable.blockChain)
                builder.addBlock(Block.blockToBlockData(block));

            return builder.build();
        }

        @Override
        public void getBlockchain(EmptyMessage request, StreamObserver<Blocks> responseObserver) {
            logger.log(Level.INFO, "Receiving GET_BLOCKCHAIN request. Responding...");

            responseObserver.onNext(getBCImpl());
            responseObserver.onCompleted();
        }
    }
}
