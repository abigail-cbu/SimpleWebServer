/**
 * SimpleWebServer.java
 * <p>
 * This toy web server is used to illustrate security vulnerabilities.
 * This web server only supports extremely simple HTTP GET requests.
 * <p>
 * Author: Abigail Lu
 * Date: 10/08/2018
 * Updated: 10/22/2018
 * Description: CSC 513 SimpleWebServer Project
 */

import java.io.*; //  BufferedReader, InputStreamReader, OutputStreamWriter, FileReader
import java.net.*; // ServerSocket and Socket
import java.util.*; // StringTokenizer

public class SimpleWebServer {

    // VARIABLES
    /* Run the HTTP server on this TCP port. */
    private static final int PORT = 8080;

    /* The socket used to process incoming connections from web clients */
    private static ServerSocket dServerSocket;

    private static final int MAX_DOWNLOAD_LIMIT = 10000;

    private static final String ROOT_DIRECTORY = "/Users/abigaillu/IntelliJProjects/Java/SimpleWebServer/";

    // CONSTRUCTOR
    public SimpleWebServer() throws Exception {
        dServerSocket = new ServerSocket(PORT); // from java.net.*
    }

    // METHODS

    /**
     * Starts Web Server and waits until client connects to web server
     * before starting to process their request
     *
     * @throws Exception
     */
    public void run() throws Exception {
        // wait until client connects to localhost:8080 in a browser
        // and connect to web server
        System.out.println("Looking for socket to connect to.");
        while (true) {
            Socket s = dServerSocket.accept();  // from java.net.*

            System.out.println("Connected to socket: " + s.getLocalPort());
         /* then process the client's request */
            processRequest(s); // go straight here after browser is open and client is linked
        }
    }

    /**
     * Processes HTTP request from client and responds with the file the user requested
     * or a HTTP error code
     *
     * @param s
     * @throws Exception
     */
    public void processRequest(Socket s) throws Exception {
        System.out.println("In processRequest...");
     /* used to read data from the client */
        BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream())); // from java.io.*

 	    /* used to write data to the client */
        OutputStreamWriter osw = new OutputStreamWriter(s.getOutputStream()); // from java.io.*

        // grab the 2 tokens, or GET request parameters, from HTTP request to find the file to server
        // however, only handles 2 tokens
        String request = br.readLine();
        String command = null;
        String pathname = null;

        // bad request if contains blank lines or less than 2 tokens
        try {
            StringTokenizer st = new StringTokenizer(request, " "); // from java.util.*
            command = st.nextToken(); // 1st param: get the HTTP request (GET, PUT, etc.)
            System.out.println("command: " + command);

            pathname = st.nextToken(); // 2nd param: get the file client wants served
            System.out.println("pathname: " + pathname);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            // if an exception occurred, close stream writer and return error to client right away
            osw.write("HTTP/1.0 400 Bad Request\n\n");
            osw.close();
            return;
        }

        // only allow GET requests to pass through
        if (command.equals("GET")) {
            serveFile(osw, pathname);
        } else {
            osw.write("HTTP/1.0 501 Not Implemented\n\n");
        }

 	/* close the connection to the client */
        osw.close();
    }


    /**
     * GET file from disk and serve to client
     *
     * @param osw
     * @param pathname
     * @throws Exception
     */
    public void serveFile(OutputStreamWriter osw, String pathname) throws Exception {
        System.out.println("In serveFile() with pathname: " + pathname);
        FileReader fr = null;
        int c = -1;
        int sentBytes = 0;

        /* remove the initial slash at the beginning
        of the pathname in the request */
        if (pathname.charAt(0) == '/')
            pathname = pathname.substring(1);

 	    /* if there was no filename specified by the
        client, serve the "index.html" file */
        if (pathname.equals(""))
            pathname = "index.html";

        System.out.println("newPathname: " + pathname);
        /* Try to open file specified by pathname */
        try {
            pathname = checkPath(pathname); // check path so hackers cannot access root folder
            fr = new FileReader(pathname); // from java.io.*
            c = fr.read();
        } catch (Exception e) {
        /* If the file is not found, return the
        appropriate HTTP response code. */
            System.out.println(e.getMessage());
            osw.write("HTTP/1.0 404 Not Found");
            return;
        }

        /* If the requested file can be successfully opened
        and read, then return an OK response code and
        send the contents of the file. */
        osw.write("HTTP/1.0 200 OK");
        while ((c != -1) && (sentBytes < MAX_DOWNLOAD_LIMIT)) { // prevent big files from being served to client
            osw.write(c);
            sentBytes++;
            c = fr.read();
        }
    }

    /**
     * Used to handle different paths and prevent clients from
     * accessing files in root folder.
     * Prevents 'directory attack'
     * example: GET ../../../../etc/shadow HTTP/1.0
     *
     * @param pathname
     * @return
     * @throws Exception
     */
    public String checkPath(String pathname) throws Exception {
        File target = new File(pathname);
        File cwd = new File(System.getProperty("user.dir")); // get current directory of pathname

        // 10/21/2018: Ch. 4 HW: updated to use my own getCanonicalPath method
        String targetStr = target.getCanonicalPath(); // normalize pathname
        String cwdStr = cwd.getCanonicalPath();

        targetStr = getCanonicalPath(target.getPath());

        // 10/22/2018: no longer needs do this check because pathname is now always starting at ROOT_DIRECTORY
        if (targetStr.isEmpty()) // check if file exist in current directory
            throw new Exception("File Not Found");
        else
            return targetStr;
    }

    /**
     * Used to normalize a path and ensures that the pathname is an absolute path.
     * This method should be written like java.io.File.getCanonicalPath().
     * Only get access to files in SimpleWebServer folder files.
     *
     * @param pathname
     * @return absolute path or an IOException if they path cannot be normalized
     * @throws Exception
     */
    public String getCanonicalPath(String pathname) throws IOException {
        String newPathName = "";

        List<String> directories = Arrays.asList(pathname.split("/"));

        for (int i = 0; i < directories.size(); i++) {
            if (directories.get(i).equals("..") || directories.get(i).equals(".")) {
                directories.remove(i); // normalize pathname
            }
        }

        if (!directories.isEmpty()) {
            newPathName = ROOT_DIRECTORY + String.join("/", directories);
        }

        return newPathName;
    }

    /* This method is called when the program is run from
       the command line. */
    public static void main(String argv[]) throws Exception {

        System.out.println("Starting SimpleWebServer.java");
     /* Create a SimpleWebServer object, and run it */
        SimpleWebServer sws = new SimpleWebServer();
        sws.run();
    }
}
