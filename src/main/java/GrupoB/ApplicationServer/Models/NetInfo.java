package GrupoB.ApplicationServer.Models;

import GrupoB.gRPCService.ServerProto.NetworkInfo;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class NetInfo {
    public String nodeID;
    public Node peer;

    public static NetInfo fromNetworkInfo(NetworkInfo networkInfo) {
        NetInfo netInfo = new NetInfo();

        netInfo.nodeID = networkInfo.getNodeID();
        netInfo.peer = Node.fromNodeInfo(networkInfo.getPeer());

        return netInfo;
    }
}
