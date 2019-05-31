package GrupoB.ApplicationServer.Models;

// import GrupoB.gRPCService.ServerProto.NodeInfo;


import GrupoB.gRPCService.ClientProto.NodeInfo;
import GrupoB.gRPCService.ClientProto.Nodes;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.LinkedList;

@XmlRootElement
public class Node implements Serializable {
    private String nodeID;
    private String address;
    private Integer port;

    public Node(String nodeID, String address, Integer port) {
        this.nodeID = nodeID;
        this.address = address;
        this.port = port;
    }

    public String getId() {
        return this.nodeID;
    }

    public String getAddress() {
        return address;
    }

    public Integer getPort() {
        return port;
    }

    public static Node fromNodeInfo(GrupoB.gRPCService.ServerProto.NodeInfo nodeInfo) {
        return new Node(nodeInfo.getNodeID(), nodeInfo.getAddress(), nodeInfo.getPort());
    }

    public static Node fromNodeInfo(NodeInfo nodeInfo) {
        return new Node(nodeInfo.getNodeID(), nodeInfo.getAddress(), nodeInfo.getPort());
    }

    public static NodeInfo toNodeInfo(Node node) {
        return NodeInfo.newBuilder()
                .setNodeID(node.nodeID)
                .setPort(node.port)
                .setAddress(node.address)
                .build();
    }

    public static LinkedList<NodeInfo> toNodeInfoList(Nodes nodes) {
        LinkedList<NodeInfo> nodeInfos = new LinkedList<>();

        nodeInfos.addAll(nodes.getNodesList());

        return nodeInfos;
    }

    public static Nodes toNodes(LinkedList<NodeInfo> nodes) {
        return Nodes.newBuilder().addAllNodes(nodes).build();
    }

    @Override
    public String toString() {
        return "Node{" +
                "address='" + address + '\'' +
                ", port=" + port +
                '}';
    }
}
