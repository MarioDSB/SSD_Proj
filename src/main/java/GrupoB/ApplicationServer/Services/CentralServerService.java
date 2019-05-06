package GrupoB.ApplicationServer.Services;

import GrupoB.ApplicationServer.Models.Test;
import GrupoB.RPC.CentralServer.CentralClient;
import GrupoB.ApplicationServer.Models.NodeInfo;

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
    @Path("/joinToChain")
    @Produces(MediaType.APPLICATION_JSON)
    public NodeInfo joinToChain(@QueryParam("address") String address, @QueryParam("port") String port) {
        // Comunicate with CentralServer
        CentralClient serverConnection = new CentralClient("localhost", 50051);
        try {
            /* Access a service running on the local machine on port 50051 */
            String response = serverConnection.join("localhost", 50050);
            System.out.println("!!!RESPOSTA: " + response);

            /* Devera retornar a resposta do CentralServerClient */

            /* Just for test*/
            return new NodeInfo(1L,"localhost",50050);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }
}
