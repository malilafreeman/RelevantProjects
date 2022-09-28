import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Date;

// The response class uses the information from the parsed request headers to send responses back to the client.
// It sends different responses based on whether the response is an HTTP response or a response over a WebSocket.

public class Response {

    // Member variables for response headers
    private String HTTPVersion_;
    private int statusCode_;
    private String stringStatus_;
    private String contentType_ = "text/html";
    private long contentLength_;
    private Date date_;

    // Need an output stream (passed into constructor) for writing out
    private OutputStream os;
    // Need a roomName member variable to keep track of room name throughout
    private String roomName = "no name";

    public Response(OutputStream myOutput, Request request, Socket mySocket, PrintWriter myWriter) throws IOException, InterruptedException {

        os = myOutput;

        // Handles HTTP responses

        if (!request.getIsWsRequest()) {

            // contentType_ is automatically set to text/html, so check for other file types

            if (request.getFileName_().endsWith(".css")) { // check to see if the file is a .css file
                contentType_ = "text/css";
            }
            if (request.getFileName_().endsWith(".js")) { // check to see if the file is a .js file
                contentType_ = "text/javascript";
            }
            if (request.getFileName_().endsWith ("word")){
                contentType_ = "text/xml";
            }

            HTTPVersion_ = request.getProtocol_();
            contentLength_ = request.getMyFile_().length();

            date_ = new Date(System.currentTimeMillis());

            // Successful server connection if GET is in the header and the file was found
            if (request.getCommand_().equals("GET") && request.getMyFile_().isFile()) {
                statusCode_ = 200;
                stringStatus_ = "OK"; // Successful connection
            }
            // Unsuccessful connection if GET is not in the header and/or if the file was not found
            else {
                statusCode_ = 404;
                stringStatus_ = "ERROR: Not found."; // Connection not successful
            }

            // if the requested file was found, return a header (with a success message), as well as the rest of the
            // header components and the contents of the file

            if (request.getMyFile_().isFile()) {

                myWriter.write(HTTPVersion_ + " " + statusCode_ + " " + stringStatus_ + "\n");
                myWriter.write("Date: " + date_ + "\n");
                myWriter.write("Server: Malila's Server.\n");
                myWriter.write("Content-Type: " + contentType_ + "\n");
                myWriter.write("Content-Length: " + contentLength_ + "\n");
                myWriter.println(); // end header with blank line

                myWriter.flush();

                // Transfer contents of the client's requested file (using an input stream) to an output stream
                File fileName = request.getMyFile_();
                if (fileName.isFile()) {
                    FileInputStream inputStream = new FileInputStream(fileName);
                    inputStream.transferTo(os);
                }
            }
            // If the file does not exist, we need to return an error code. If the client's requested file is not a file,
            // statusCode and stringStatus will be "404" and "Error", respectively.
            else {
                myWriter.write(HTTPVersion_ + " " + statusCode_ + " " + stringStatus_);
            }
            myWriter.flush();
        }

        // Handles WebSocket response

        else {

            HTTPVersion_ = "HTTP/1.1";
            myWriter.print(HTTPVersion_ + " 101 Switching Protocols" + "\r\n");
            myWriter.print("Upgrade: websocket\r\n");
            myWriter.print("Connection: " + request.getConnectionLine() + "\r\n");
            myWriter.print("Sec-WebSocket-Accept: " + request.getKeyLine() + "\r\n");
            // Need a blank line to signal end of header
            myWriter.print("\r\n");

            myWriter.flush();

            // Get an input stream connected to the socket
            InputStream ins = mySocket.getInputStream();

            // Continuously listen for new WebSocket connections
            // Extract information from client data frames
            while (true) {
                System.out.println("In while loop");
                try {
                    DataInputStream dataIn = new DataInputStream(ins);
                    System.out.println("1");

                    // byte 0 contains finBit and opCode
                    byte byte0 = dataIn.readByte();

                    // Opcode must be 8
                    if ((byte0 >> 4 & 0xF) != 8) {
                        mySocket.close();
                    }

                    // MASK bit must be 1
                    if ((byte0 >> 7 & 0x1) != 1) {
                        mySocket.close();
                    }

                    System.out.println("2");

                    // Get finBit - Ox80 is 10000000 in bits
                    byte finBit = (byte) (byte0 & 0x80);
                    finBit = (byte) (finBit >>> 7);

                    // Get opCode - 0x0F is 00001111 in bits
                    byte opCode = (byte) (byte0 & 0x0F);

                    // byte 1 contains mask Code and payload length
                    byte byte1 = dataIn.readByte();

                    // get MaskCode
                    byte maskCode = (byte) (byte1 & 0x80);
                    maskCode = (byte) (maskCode >>> 7);

                    // get payLoad Length
                    // initialize at -1 so that if it stays at -1, we know something is wrong
                    long payloadLength = -1;

                    //Get payload length, which is in last 7 bits of 1st byte.
                    // 0x7F is 01111111 in bits
                    byte plLength = (byte) (byte1 & 0x7f);

                    if (plLength < 126) {
                        payloadLength = plLength;
                    }
                    // extended payload length
                    else if (plLength == (short) 126) {
                        payloadLength = dataIn.readShort();
                    }
                    // extended payload length
                    else if (plLength == (short) 127) {
                        payloadLength = dataIn.readLong();
                    }

                    // get masking key, which is 4 bytes
                    byte[] mask = dataIn.readNBytes(4);

                    // need a new byte[] of size payload length to represent both the decoded and the encoded messages
                    byte[] decodedMessage = new byte[(int) payloadLength];
                    byte[] encodedMessage = new byte[(int) payloadLength];

                    for (int i = 0; i < encodedMessage.length; i++) {
                        // read in the payload into encoded message
                        encodedMessage[i] = dataIn.readByte();
                        // mask each byte in the payload to get the decoded message
                        decodedMessage[i] = (byte) (encodedMessage[i] ^ mask[i % 4]);
                    }

                    // Make a string from the decoded message
                    String message = new String(decodedMessage, StandardCharsets.UTF_8);
                    System.out.println("***Decoded message*** " + message);

                    // Need to parse the message so that we know what is sent in from the WebSocket
                    String[] parseMessage = message.split(" ", 2);
                    String firstPosition = parseMessage[0];

                    // "join" is sent when the client join button is pressed
                    if (firstPosition.equals("join")) {
                        Room myRoom = Room.getRoom(parseMessage[1]);
                        // Add client to the room
                        myRoom.joinRoom(mySocket);
                        roomName = myRoom.getRoomName();
                    }
                    // "leave" is sent when the client join button is pressed
                    else if (firstPosition.equals("leave")) {
                        Room myRoom = Room.getRoom(roomName);
                        // Removes client from the room
                        myRoom.removeClient(mySocket);
                        // Removes the room if there are no clients in the room
                        myRoom.checkRoomEmpty();
                        System.out.println("removed client" + mySocket);
                    }
                    else {
                        String wholeMessage = "{\"user\" : \"" + parseMessage[0] + "\", \"message\" : \"" + parseMessage[1] + "\"}";
                        Room myRoom = Room.getRoom(roomName);
                        // Sends message to client if they are in the room
                        if (myRoom.clientInRoom(mySocket)) {
                            myRoom.sendMessageToRoom(wholeMessage);
                        }
                    }
                }
                catch (Exception e) {
                    System.out.println("Caught exception in while loop. Message: " + e);
                    Room myRoom = Room.getRoom(roomName);
                    myRoom.removeClient(mySocket);
                    myRoom.checkRoomEmpty();
                }
            }
        }
    }
}

