package GrupoB.ApplicationServer.Models;

import GrupoB.gRPCService.ServerProto.NetworkInfo;
import GrupoB.gRPCService.ServerProto.NodeInfo;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class NetInfo {
    public String nodeID;
    public Node peer;
    public boolean pow;

    public static NetInfo fromNetworkInfo(NetworkInfo networkInfo) {
        NetInfo netInfo = new NetInfo();

        netInfo.nodeID = networkInfo.getNodeID();
        if (netInfo.nodeID.equals("")) {
            // The node wasn't able to join the network
            return netInfo;
        }

        if (!networkInfo.getPeer().equals(NodeInfo.getDefaultInstance()))
            netInfo.peer = Node.fromNodeInfo(networkInfo.getPeer());
        else
            netInfo.peer = null;

        netInfo.pow = networkInfo.getPow();

        return netInfo;
    }
}
