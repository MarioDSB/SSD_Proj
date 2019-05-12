package GrupoB.ApplicationServer;

import GrupoB.ApplicationServer.Services.CentralServerService;
import GrupoB.RPC.NetworkClient.NetClientRPC;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

public class CentralClient {

    public static final int DEFAULT_PORT = 50051;

    public static NetClientRPC client = null;

    private static void startJetty(int port) throws Exception {
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");

        Server jettyServer = new Server(port);
        jettyServer.setHandler(context);

        ServletHolder jerseyServlet = context.addServlet(
                org.glassfish.jersey.servlet.ServletContainer.class, "/*");
        jerseyServlet.setInitOrder(0);
        jerseyServlet.setInitParameter(
                "jersey.config.server.provider.classnames",
                CentralServerService.class.getCanonicalName()
        );
        try {
            jettyServer.start();
            jettyServer.join();
        }  catch(Exception e) {
            throw e;
        } finally{
            jettyServer.destroy();
        }
    }

    public static void main(String[] args) throws Exception {
        client = new NetClientRPC("localhost", DEFAULT_PORT);
        startJetty(9595);
    }

}
