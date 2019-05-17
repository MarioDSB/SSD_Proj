package GrupoB.ApplicationServer.Models;

// import GrupoB.gRPCService.ServerProto.NodeInfo;


import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

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
        return nodeID;
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

    public static Node fromNodeInfo(GrupoB.gRPCService.ClientProto.NodeInfo nodeInfo) {
        return new Node(nodeInfo.getNodeID(), nodeInfo.getAddress(), nodeInfo.getPort());
    }

    public static GrupoB.gRPCService.ClientProto.NodeInfo toNodeInfo(Node node) {
        return GrupoB.gRPCService.ClientProto.NodeInfo.newBuilder()
                .setNodeID(node.nodeID)
                .setPort(node.port)
                .setAddress(node.address)
                .build();
    }

    @Override
    public String toString() {
        return "Node{" +
                "address='" + address + '\'' +
                ", port=" + port +
                '}';
    }
}
