package GrupoB.Client;

import GrupoB.ApplicationServer.Models.JoinRequest;
import GrupoB.ApplicationServer.Models.NetInfo;
import GrupoB.ApplicationServer.Models.Node;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.internal.jackson.jaxrs.json.JacksonJsonProvider;

import javax.ws.rs.NotFoundException;
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
        try {
            return client.target(baseURI).path("central/ping")
                    .request(MediaType.APPLICATION_JSON)
                    .get(Boolean.class);
        } catch (NotFoundException ignored) {
            return false;
        }
    }

    public NetInfo join(String work) {
        try {
            JoinRequest jr = new JoinRequest();
            jr.address = this.address;
            jr.port = this.port;
            jr.work = work;

            return client.target(baseURI).path("central/join")
                    .request(MediaType.APPLICATION_JSON)
                    .post(Entity.entity(jr, MediaType.APPLICATION_JSON), NetInfo.class);
        } catch (NotFoundException ignored) {
            return null;
        }
    }

    /**
     * Gets the node that is currently generating the new block
     * @return the node that is generating the new block
     */
    public Node generateBlock() {
        try {
            return client.target(baseURI).path("central/generate")
                    .request(MediaType.APPLICATION_JSON)
                    .get(Node.class);
        } catch (NotFoundException ignored) {
            return null;
        }
    }

    /**
     * Signals the tracker that a new block was generated
     * This function is only used when the network is in PoS mode
     */
    public void blockGenerated() {
        client.target(baseURI).path("central/generation")
                .request(MediaType.APPLICATION_JSON)
                .get();
    }
}
