package edu.stevens.cs549.ftpserver;

import java.io.InputStream;
import java.net.InetAddress;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Properties;

import org.apache.log4j.PropertyConfigurator;

/**
 *
 * @author dduggan
 */
public class ServerMain {
	
	private static String serverPropsFile = "/server.properties";
	private static String loggerPropsFile = "/log4j.properties";

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
    	
/*        PropertyConfigurator.configure(ServerMain.class.getClassLoader()
                .getResource(loggerPropsFile));
*/
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }
        new ServerMain();
    }
    
	public ServerMain () {
    	try {
            PropertyConfigurator.configure(getClass().getResource(loggerPropsFile));
    		/*
    		 * Load server properties.
    		 */
    		Properties props = new Properties();
    		InputStream in = getClass().getResourceAsStream(serverPropsFile);
    		props.load(in);
    		in.close();
        	String rootDir = (String)props.get("server.path");
        	String serverName = (String)props.get("server.name");
        	String serverIp = (String)props.get("server.ip");
        	int serverPort = Integer.parseInt((String)props.get("server.port"));
        	/*
        	 * Register factory object in registry.
        	 */
            ServerFactory stub = new ServerFactory (InetAddress.getByName(serverIp), serverPort, rootDir);
            Registry registry = LocateRegistry.createRegistry(serverPort);
            registry.rebind(serverName, stub);
            Server.log.info("Server bound [port="+serverPort+"]");
    	
    	} catch (java.io.FileNotFoundException e) {
    		System.err.println ("Server error: "+serverPropsFile+" file not found.");
    	} catch (java.io.IOException e) {
    		System.err.println ("Server error: IO exception.");
    	} catch (Exception e) {
    		Server.log.severe ("Server exception:");
    		e.printStackTrace();
    	}
    	
    }

}