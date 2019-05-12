package GrupoB.RPC.P2PServer;

import GrupoB.ApplicationServer.Models.Node;
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

        private LinkedList<NodeInfo> closestNodes;

        @Override
        public void ping(EmptyMessage request, StreamObserver<BooleanMessage> responseObserver) {
            logger.log(Level.INFO, "Receiving ping. Responding...");

            responseObserver.onNext(BooleanMessage.newBuilder().setResult(true).build());
            responseObserver.onCompleted();
        }

        @Override
        public void store(BlockData request, StreamObserver<EmptyMessage> responseObserver) {

        }

        /**
         * Check if there is some node further away than the nodeToInsert,
         * and replace it, if it is the case
         * @param nodeID nodeID of the node we are searching
         * @param nodeToInsert node we want to check
         */
        private void checkAndInsert(String nodeID, NodeInfo nodeToInsert) {

            // Get the furthest node from the nodeID
            NodeInfo furthestNode = closestNodes.get(0);
            int maxDistance = Executable.calculateDistance(nodeID, furthestNode.getNodeID());

            for (int i = 1; i < closestNodes.size(); i++) {
                int aux = Executable.calculateDistance(nodeID, closestNodes.get(i).getNodeID());
                if (aux > maxDistance) {
                    maxDistance = aux;
                    furthestNode = closestNodes.get(i);
                }
            }

            // If the nodeToInsert is closer than the furthestNode,
            // replace the furthestNode with the nodeToInsert
            if (Executable.calculateDistance(nodeID, nodeToInsert.getNodeID()) < maxDistance) {
                closestNodes.remove(furthestNode);
                closestNodes.add(nodeToInsert);
            }
        }

        /**
         * Searches up and down, starting on the kBucket[indexToSearch],
         * trying to get the K closest peers to the nodeID
         * @param nodeID nodeID in question
         * @param indexToSearch starting index
         * @param indexOffset index offset
         */
        private void getClosestNodes(String nodeID, int indexToSearch, int indexOffset) {
            indexToSearch = indexToSearch + indexOffset;

            // if (closestNodes.size() == Executable.K)
            //     return;

            for (int i = 0; i < Executable.K; i++) {
                checkAndInsert(nodeID, Node.toNodeInfo(
                        Executable.kBuckets[indexToSearch].get(i)));
            }

            int prevAbs = Math.abs(indexToSearch - indexOffset);

            if (indexOffset <= 0) {
                indexOffset -= 1;
                indexOffset = -indexOffset;
            } else {
                indexOffset += 1;
                indexOffset = -indexOffset;
            }

            // If the closestNodes is filled...
            if (closestNodes.size() == Executable.K)
                // ... and there are no more
                // nodes that may be closer...
                if (prevAbs != Math.abs(indexToSearch - indexOffset))
                    return;

            getClosestNodes(nodeID, indexToSearch, indexOffset);
        }

        private Nodes findNodeImpl(NodeID nodeID) {
            Nodes.Builder builder = Nodes.newBuilder();

            // Finds the kBucket with the closest nodes to nodeID
            int index = Executable.calculateKBucket(nodeID.getNodeID());

            closestNodes = new LinkedList<>();
            getClosestNodes(nodeID.getNodeID(), index, 0);
            for (int i = 0; i < closestNodes.size(); i++)
                builder.setNodes(i, closestNodes.get(i));

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
