// Malila Freeman
// October 29, 2021
// Chat Server
// This program creates a chat server as well as a chat client (a browser). Clients can type in a username and a room name,
// and they will be added to the room where they can send and exchange messages. For each client, a thread is added so that
// multiple parts of the program can be executed at the same time. The program uses Web Sockets so that clients can send
// messages to a server and receive responses without having to poll the server for a reply.

// My upgrades:
// I added a timestamp to each message displayed in the client
// For the user sending messages, their messages will be displayed in blue on the right-hand side of the chat box. For the
// user receiving messages, the messages they receive will be displayed in grey on the left-hand side of the chat box.


import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class AdaptServer {

    public static void main(String[] args) throws IOException, InterruptedException {

        ServerSocket originalSocket = null;
        try {
            originalSocket = new ServerSocket(8080); // Establish a new server in the constructor
        }
        catch (SocketException e){
            System.out.println("Unable to open socket.");
            System.out.println(e.getMessage());
            System.exit(-1);
        }


        while (true) { // Server runs continuously

            try {

                System.out.println("Waiting for connection...");

                // Create a client socket to use for the rest of the program
                Socket mySocket = originalSocket.accept();

                // Create threads
                MyRunnable runnable = new MyRunnable(mySocket);
                Thread thread = new Thread(runnable);
                thread.start();

                System.out.println("Connection established.");
            }
            catch (IOException e ){
                System.out.println("Main Server Exception: " + e.getMessage());
            }
        }
    }
}