package GrupoB.ApplicationServer;

import GrupoB.ApplicationServer.Services.CentralServerService;
import GrupoB.RPC.NetworkClient.Client;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

public class ApplicationServer {

    public static final int DEFAULT_PORT = 50052;

    public static Client client = null;

    private static void startJetty(int port) throws Exception {
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/services");

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
        client = new Client("localhost", DEFAULT_PORT);
        startJetty(9595);
    }

}
