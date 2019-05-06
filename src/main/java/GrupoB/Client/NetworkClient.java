package GrupoB.Client;

import GrupoB.ApplicationServer.Models.JoinRequest;
import GrupoB.ApplicationServer.Models.NetInfo;
import GrupoB.ApplicationServer.Models.Node;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.internal.jackson.jaxrs.json.JacksonJsonProvider;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;

public class NetworkClient {
    private String address;
    private int port;
    private Client client;
    private String baseURI;

    public NetworkClient(String address, int port) {
        ClientConfig config = new ClientConfig(Node.class);
        config.register(JacksonJsonProvider.class);
        client = ClientBuilder.newClient(config);

        this.baseURI = "http://" + address + ":" + port +  "/";
        this.address = address;
        this.port = port;
    }

    public boolean ping() {
        return client.target(baseURI).path("central/ping")
                .request(MediaType.APPLICATION_JSON)
                .get(Boolean.class);
    }

    public NetInfo join() {
        JoinRequest jr = new JoinRequest();
        jr.address = this.address;
        jr.port = port;

        return client.target(baseURI).path("central/join")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.entity(jr, MediaType.APPLICATION_JSON), NetInfo.class);
    }
}
