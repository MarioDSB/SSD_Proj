package GrupoB.ApplicationServer.Services;

import GrupoB.ApplicationServer.CentralClient;
import GrupoB.ApplicationServer.Models.JoinRequest;
import GrupoB.ApplicationServer.Models.NetInfo;
import GrupoB.ApplicationServer.Models.Node;
import GrupoB.ApplicationServer.Models.Test;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("/central")
public class CentralServerService {

    @GET
    @Path("/test")
    @Produces(MediaType.APPLICATION_JSON)
    public Test test() {
        Test testModel = new Test();
        testModel.setMessage("Hello World!!!");
        return testModel;
    }

    @GET
    @Path("/ping")
    @Produces(MediaType.APPLICATION_JSON)
    public boolean ping() {
        return CentralClient.client.ping();
    }

    @POST
    @Path("/join")
    @Produces(MediaType.APPLICATION_JSON)
    public NetInfo join(JoinRequest jr) {
        System.out.println(jr.address + ":" + jr.port + " is joining...");
        return NetInfo.fromNetworkInfo(CentralClient.client.join(jr.address, jr.port, jr.work));
    }

    @GET
    @Path("/generate")
    @Produces(MediaType.APPLICATION_JSON)
    public Node generateBlock() {
        return Node.fromNodeInfo(CentralClient.client.generateBlock());
    }

    @GET
    @Path("/generation")
    @Produces(MediaType.APPLICATION_JSON)
    public boolean generation() {
        return CentralClient.client.generation();
    }
}
