package edu.stevens.cs549.ftpserver;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Enumeration;
import java.util.Stack;
import java.util.logging.Logger;

import edu.stevens.cs549.ftpinterface.IServer;

/**
 *
 * @author dduggan
 * @author Tianpei Luo
 */
public class Server extends UnicastRemoteObject
        implements IServer {
	
	static final long serialVersionUID = 0L;
	
	private static final int BUFFER_SIZE = 20*1024;
	
	public static Logger log = Logger.getLogger("edu.stevens.cs.cs549.ftpserver");
    
	/*
	 * For multi-homed hosts, must specify IP address on which to 
	 * bind a server socket for file transfers.  See the constructor
	 * for ServerSocket that allows an explicit IP address as one
	 * of its arguments.
	 */
	private InetAddress host;
	
	final static int backlog = 5;
	
	/*
	 *********************************************************************************************
	 * Current working directory.
	 */
    static final int MAX_PATH_LEN = 1024;
    private Stack<String> cwd = new Stack<String>();
    
    /*
     *********************************************************************************************
     * Data connection.
     */
    
    enum Mode { NONE, PASSIVE, ACTIVE };
    
    private Mode mode = Mode.NONE;
    
    /*
     * If passive mode, remember the server socket.
     */
    
    private ServerSocket dataChan = null;
    
    private InetSocketAddress makePassive () throws IOException {
    	dataChan = new ServerSocket(0, backlog, host);
    	mode = Mode.PASSIVE;
    	return (InetSocketAddress)(dataChan.getLocalSocketAddress());
    }
    
    /*
     * If active mode, remember the client socket address.
     */
    private InetSocketAddress clientSocket = null;
    
    private void makeActive (InetSocketAddress s) {
    	clientSocket = s;
    	mode = Mode.ACTIVE;
    }
    
    /*
     **********************************************************************************************
     */
            
    /*
     * The server can be initialized to only provide subdirectories
     * of a directory specified at start-up.
     */
    private final String pathPrefix;

    public Server(InetAddress host, int port, String prefix) throws RemoteException {
    	super(port);
    	this.host = host;
    	this.pathPrefix = prefix + "/";
        log.info("A client has bound to a server instance.");
    }
    
    public Server(InetAddress host, int port) throws RemoteException {
        this(host, port, "/");
    }
    
    private boolean valid (String s) {
        // File names should not contain "/".
        return (s.indexOf('/')<0);
    }
    
    private static class GetThread implements Runnable {
    	private ServerSocket dataChan = null;
    	private FileInputStream file = null;
    	public GetThread (ServerSocket s, FileInputStream f) { dataChan = s; file = f; }
    	public void run () {
    		/*
    		 * TODO: Process a client request to transfer a file.
    		 */
    		try {
				Socket csocket = dataChan.accept();
				DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(csocket.getOutputStream()));
				byte[]  fbuf =  new byte[BUFFER_SIZE];
				int bytesRead = file.read(fbuf,0,BUFFER_SIZE);
				while(bytesRead!=-1)
				{
					dos.write(fbuf,0,bytesRead);
					bytesRead = file.read(fbuf);
				}
				file.close();
				dos.close();
				csocket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				System.out.println("Exception"+e);
				e.printStackTrace();
			}
    	}
    }
    
    private static class PutThread implements Runnable {
    	private ServerSocket dataChan = null;
    	private FileOutputStream file = null;
    	public PutThread (ServerSocket s, FileOutputStream f) { dataChan = s; file = f; }
    	public void run () {
    		/*
    		 * TODO: Process a client request to transfer a file.
    		 */
    		try {
				Socket csocket = dataChan.accept();
				DataInputStream dis = new DataInputStream(new BufferedInputStream(csocket.getInputStream()));
				byte[] sendBytes = new byte[BUFFER_SIZE];
				String fileName = dis.readUTF();
				long fileLength = dis.readLong();
					
				System.out.println("start to receive "+fileName+" and size is "+fileLength);
				
				while(true){
					int read = 0;
					read=dis.read(sendBytes);
					if(read==-1)
						break;
					file.write(sendBytes,0,read);
					file.flush();
				}
				System.out.println("-----Success Receiving File!!-----");
				
				file.close();
				dis.close();
				csocket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				System.out.println("Exception"+e);
				e.printStackTrace();
			}
    	}
    }
    
    public void get (String file) throws IOException, FileNotFoundException, RemoteException {
        if (!valid(file)) {
            throw new IOException("Bad file name: " + file);
        } else if (mode == Mode.ACTIVE) {
        	Socket xfer = new Socket (clientSocket.getAddress(), clientSocket.getPort());
        	/*
        	 * TODO: connect to client socket to transfer file.
        	 */
        	DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(xfer.getOutputStream()));
        	File f = new File(path()+file);
        	FileInputStream fis = new FileInputStream(f);
        	
        	dos.writeUTF(f.getName());
        	dos.flush();
        	dos.writeLong(f.length());
        	dos.flush();
        	
        	byte[] fbuf = new byte[BUFFER_SIZE];
        	int length = 0;
    	
        	while((length = fis.read(fbuf,0,fbuf.length))>0)
        	{
        		dos.write(fbuf,0,length);
        		dos.flush();
        	}
        	System.out.println("Transfer file !!");
        	fis.close();
        	dos.close();
        	xfer.close();

        	/*
			 * End TODO.
			 */
        } else if (mode == Mode.PASSIVE) {
            FileInputStream f = new FileInputStream(path()+file);
            new Thread (new GetThread(dataChan, f)).start();
            System.out.println("Wait for the client to connect...");
        }
    }
    
    public void put (String file) throws IOException, FileNotFoundException, RemoteException {
    	/*
    	 * TODO: Finish put (both ACTIVE and PASSIVE).
    	 */
    	if (!valid(file)) {
            throw new IOException("Bad file name: " + file);
    	}
        else if (mode == Mode.ACTIVE)
        {	
        	System.out.println("Start connect to the server...");
        	Socket xfer = new Socket (clientSocket.getAddress(), clientSocket.getPort());
        	FileOutputStream fos = new FileOutputStream(path()+file);
        	DataInputStream dis = new DataInputStream(new BufferedInputStream(xfer.getInputStream()));
        	byte[] fbuf = new byte[BUFFER_SIZE];
        	int bytesRead = dis.read(fbuf,0,BUFFER_SIZE);
        	while(bytesRead!=-1)
        	{
        		fos.write(fbuf,0,bytesRead);
        		bytesRead = dis.read(fbuf);
        	}
        	fos.close();
        	dis.close();
        	xfer.close();
    	}
        else if (mode == Mode.PASSIVE)
        {
        	FileOutputStream f = new FileOutputStream(path()+file);
        	new Thread(new PutThread(dataChan,f)).start();
        	System.out.println("----Start thread to wait for connction(passive)-----");
        }
    	
    }
    
    public String[] dir () throws RemoteException {
        // List the contents of the current directory.
        return new File(path()).list();
    }

	public void cd(String dir) throws IOException, RemoteException {
		// Change current working directory (".." is parent directory)
		if (!valid(dir)) {
			throw new IOException("Bad file name: " + dir);
		} else {
			if ("..".equals(dir)) {
				if (cwd.size() > 0)
					cwd.pop();
				else
					throw new IOException("Already in root directory!");
			} else if (".".equals(dir)) {
				;
			} else {
				File f = new File(path());
				if (!f.exists())
					throw new IOException("Directory does not exist: " + dir);
				else if (!f.isDirectory())
					throw new IOException("Not a directory: " + dir);
				else
					cwd.push(dir);
			}
		}
	}

    public String pwd () throws RemoteException {
        // List the current working directory.
        String p = "/";
        for (Enumeration<String> e = cwd.elements(); e.hasMoreElements(); ) {
            p = p + e.nextElement() + "/";
        }
        return p;
    }
    
    private String path () throws RemoteException {
    	return pathPrefix+pwd();
    }
    
    public void port (InetSocketAddress s) {
    	makeActive(s);
    }
    
    public InetSocketAddress pasv () throws IOException {
    	return makePassive();
    }

}