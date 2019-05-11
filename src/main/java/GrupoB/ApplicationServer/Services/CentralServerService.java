package GrupoB.ApplicationServer.Services;

import GrupoB.ApplicationServer.ApplicationServer;
import GrupoB.ApplicationServer.Models.JoinRequest;
import GrupoB.ApplicationServer.Models.NetInfo;
import GrupoB.ApplicationServer.Models.Node;
import GrupoB.ApplicationServer.Models.Test;
import GrupoB.RPC.CentralServer.CentralClient;

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

    @POST
    @Path("/joinToChain")
    @Produces(MediaType.APPLICATION_JSON)
    public Node joinToChain(@QueryParam("address") String address, @QueryParam("port") String port) {
        // Comunicate with CentralServer
        CentralClient serverConnection = new CentralClient("localhost", 50051);
        try {
            /* Access a service running on the local machine on port 50051 */
            String response = serverConnection.join("localhost", 50050);
            System.out.println("!!!RESPOSTA: " + response);

            /* Devera retornar a resposta do CentralServerClient */

            /* Just for test*/
            return new Node("","localhost",50050);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    @GET
    @Path("/ping")
    @Produces(MediaType.APPLICATION_JSON)
    public boolean ping() {
        return ApplicationServer.client.ping();
    }

    @POST
    @Path("/join")
    @Produces(MediaType.APPLICATION_JSON)
    public NetInfo join(JoinRequest jr) {
        System.out.println(jr.address + ":" + jr.port + " is joining...");
        return NetInfo.fromNetworkInfo(ApplicationServer.client.join(jr.address, jr.port));
    }
}
