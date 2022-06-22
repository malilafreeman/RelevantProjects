import java.io.IOException;
import java.io.IOException;
import java.net.*;

/**
 * DNSServer opens up a UDP socket and listens for client requests, sends DNS requests to google and receive
 * responses, and sends DNS responses back to the client.
 */
public class DNSServer {

    DatagramSocket clientSocket;
    DatagramSocket googleSocket;

    byte[] bufferForClientPacket = new byte[256];
    byte[] bufferForGooglePacket = new byte[256];

    DatagramPacket clientPacket;
    DatagramPacket responsePacket;

    public static DNSCache cache;

    DNSMessage googleResponseMessage;

    // Socket will remain open unless there is a problem with establishing or maintaining the socket itself.
    boolean done = false;


    /**
     * DNSServer() opens a UDP socket and listen for requests. When it gets one, it looks at all the questions in the request.
     * If there is a valid answer in the cache, it sends back a response. Otherwise, a new UDP socket is created which
     * forwards the request to Google (8.8.8.8) and stores Google's response. The response is then sent back to the client.
     */

    DNSServer(){

        clientPacket = new DatagramPacket(bufferForClientPacket, bufferForClientPacket.length);

        // Make a new cache each time the server is run
        cache = new DNSCache();

        try {

            // Socket for sending and receiving queries with the user
            clientSocket = new DatagramSocket(8053);

            // Socket for sending and receiving queries with Google
            googleSocket = new DatagramSocket(9000);

            // The server should remain open unless the socket cannot be opened
            while (!done) {

                clientSocket.receive(clientPacket);

                DNSMessage initialQueryMessage = DNSMessage.decodeMessage(bufferForClientPacket);

                // Will return null if the record has not already been added to the cache
                googleResponseMessage = cache.queryCache(initialQueryMessage.question);

                // If the record has not been added to the cache previously, we need to query Google and then add
                // Google's response to the cache
                if (googleResponseMessage == null) {

                    googleResponseMessage = queryGoogle();

                    System.out.println("Queried Google.");

                    // Only insert into the cache if there is no error code for the packet
                    if (googleResponseMessage.header.RCODE == 0){
                        cache.insertRecord(googleResponseMessage.question, googleResponseMessage);
                    }
                }
                else {
                    System.out.println("From cache.");
                }

                DNSMessage responseMessage = DNSMessage.buildResponse(initialQueryMessage, googleResponseMessage);
                byte[] responsePacketInBytes = responseMessage.toBytes();

                // Want the response to be sent over the same port and to the same address as the initial query
                responsePacket = new DatagramPacket(responsePacketInBytes, responsePacketInBytes.length, clientPacket.getAddress(), clientPacket.getPort());
                clientSocket.send(responsePacket);
            }
        }

        catch(SocketException e){
            System.err.println("Unable to open socket.");
            // close the socket
            done = true;
            e.printStackTrace();
        }

        catch(IOException e){
            System.out.println("Unable to receive packet.");
            e.printStackTrace();
        }
    }

    /**
     * queryGoogle() sends a query to Google with the information from the client's initial query and stores Google's response as a DNSMessage.
     *
     * @return - DNSMessage containing Google's DNS response.
     * @throws IOException
     */

    DNSMessage queryGoogle() throws IOException {

        // Send a DNS query to Google
        Inet4Address address = (Inet4Address) InetAddress.getByName("8.8.8.8");

        DatagramPacket queryGooglePacket = new DatagramPacket(bufferForClientPacket, bufferForClientPacket.length, address, 53);

        googleSocket.send(queryGooglePacket);


        // Receive Google's DNS response and store in a message
        DatagramPacket responseGooglePacket = new DatagramPacket(bufferForGooglePacket, bufferForGooglePacket.length);

        googleSocket.receive(responseGooglePacket);

        DNSMessage googleMessage = DNSMessage.decodeMessage(responseGooglePacket.getData());


        return googleMessage;
    }

    public static void main(String[] args) {

        // Establish the server, which keeps the sockets open
        DNSServer server = new DNSServer();

    }
}