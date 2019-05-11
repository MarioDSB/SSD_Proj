package GrupoB.ApplicationServer.Models;

import GrupoB.gRPCService.ServerProto.NodeInfo;

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

    public static Node fromNodeInfo(NodeInfo nodeInfo) {
        return new Node(nodeInfo.getNodeID(), nodeInfo.getAddress(), nodeInfo.getPort());
    }

    @Override
    public String toString() {
        return "Node{" +
                "address='" + address + '\'' +
                ", port=" + port +
                '}';
    }
}
