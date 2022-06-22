import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

// The request class parses client request headers. It also determines whether a request is a WebSocket request or
// an HTTP request. It parses request header information by first parsing the first line, then placing the rest of
// the header information into a map.


public class Request {

    // Member variables - The information we need from the client's request header
    private String command_;
    private String fileName_;
    private String protocol_;
    File myFile_;

    // WebSocket member variables

    // To parse the connection line from the request header
    private String connectionLine;
    // To parse the key line from the request header
    private String keyLine;
    // To keep track of whether the request is an HTTP request or a WebSocket request
    private boolean isWSRequest;



    public Request(Scanner myScanner) throws IOException, NoSuchAlgorithmException {

        // Assume that the request is an HTTP request and not a WebSocket request
        isWSRequest = false;

        // Parse first line of request header
        command_ = myScanner.next();
        fileName_ = myScanner.next();
        System.out.println(fileName_);
        protocol_ = myScanner.next();

        // Parse the rest of the request header
        Map<String, String> map = new HashMap<String, String>();

        myScanner.nextLine();
        while (myScanner.hasNextLine()){
            // Split the request header strings into 2 parts based on the ":"
            String [] stringPairings = myScanner.nextLine().split(": ", 2);

            // The request header is finished if there is an empty line - break out of the while loop
            if (stringPairings[0].equals("")){
                break;
            }
            map.put((stringPairings[0]), (stringPairings[1]));
        }

        System.out.println("The map is: " + map);

        connectionLine = map.get("Connection");
        System.out.println("Connection line:" + connectionLine);

        String requestKey = map.get("Sec-WebSocket-Key");
        System.out.println("Original key: " + requestKey);

        // If there is a request key, we know that it is a WebSocket request
        if (requestKey != null) {
            isWSRequest = true;

            // generateResponseKey function concatenates the key with the magic string and hashes the bytes
            keyLine = generateResponseKey(requestKey);
            System.out.println("New key: " + keyLine);
        }
        // Finished parsing header and getting necessary lines

            if (fileName_.equals("/")) {
                myFile_ = new File("src/index.html"); // index.html is the default page located in src
            }
            else {
                myFile_ = new File("src" + fileName_); // filename will otherwise have a "/" in front of it already and will be located in src
            }
    }

        // Generates encoded string to be used in response header
        public String generateResponseKey(String requestKey) throws NoSuchAlgorithmException {
            String magicString = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
            requestKey += magicString;
            MessageDigest md = MessageDigest.getInstance("SHA-1");

            byte[] hashed = md.digest( requestKey.getBytes() );
            String result = Base64.getEncoder().encodeToString( hashed );

            return result;
        }

        // If the client asks for an invalid file or if "GET" is not in the header, an error page will be returned

        public void checkRequestIsValid () throws IOException {

            if (fileName_.equals("/word")){
                myFile_ = new File("/Users/malilafreeman/Desktop/myGithubRepo/CS6011/Week5/Day1Assignment/WebChat/app/src/main/res/layout/activity_chat_display.xml");
                System.out.println("In check request is valid");
            }

            if (!myFile_.isFile()) {
                myFile_ = new File("src/error.html"); // Error page
                throw new FileNotFoundException("File not found.");
            }

            if (!command_.equals("GET")) {
                myFile_ = new File("src/error.html"); // Error page
                throw new IOException("Command not recognized.");
            }
        }

        // Get functions

        public String getCommand_ () {
            return command_;
        }

        public String getFileName_ () {
            return fileName_;
        }

        public String getProtocol_ () {
            return protocol_;
        }

        public File getMyFile_ () {
            return myFile_;
        }

        public String getConnectionLine () {
            return connectionLine;
        }

        public String getKeyLine () {
            return keyLine;
        }

        public boolean getIsWsRequest(){
            return isWSRequest;
        }
}