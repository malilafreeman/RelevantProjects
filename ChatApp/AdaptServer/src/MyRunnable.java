import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class MyRunnable implements Runnable {

    private Socket mySocket;

    public MyRunnable(Socket myInputSocket) {
        mySocket = myInputSocket;
    }

    @Override
    public void run() {

        try {

            InputStream input = mySocket.getInputStream();

            Scanner myScanner = new Scanner(input);

            Request request = new Request(myScanner);

            // If the request is not valid, throws up error page
            try {
                request.checkRequestIsValid();
            }
            catch (IOException e) {
                System.out.println("Invalid request: " + e.getMessage());
            }

            OutputStream myOutput = mySocket.getOutputStream();

            PrintWriter myWriter = new PrintWriter(myOutput);

            Response response = new Response(myOutput, request, mySocket, myWriter);

            mySocket.close();
            myScanner.close();

            input.close();
            myWriter.close();
        }
        catch (IOException | InterruptedException e) {
            System.out.println("IO Exception in Runnable: " + e.getMessage());
            System.out.println("Could not establish connection.");
        }
        catch (Exception e){
            System.out.println("Exception in runnable: " + e.getMessage());
        }
    }
}