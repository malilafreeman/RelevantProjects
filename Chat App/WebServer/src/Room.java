import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.sql.SQLOutput;
import java.util.ArrayList;

public class Room {

    private String roomName_;

    // Member variable is static because its values are shared by all objects in the class
    private static ArrayList<Room> rooms = new ArrayList<>();
    private ArrayList<Socket> clients = new ArrayList<>();

    public Room (String roomName){
        roomName_ = roomName;
    }

    // Need a static method so that the method doesn't have to be called on a room object
    // Need a synchronized method to prevent thread interference

    // This function makes a room and adds it to the list of rooms if it is not in the list already
    public synchronized static Room getRoom (String roomName){

        // If the room is already in the array, it returns the room
        for (Room r : rooms){
            if (r.roomName_.equals(roomName)){
                return r;
            }
        }

        // If the room is not already in the array, it creates the room
        Room toReturn = new Room (roomName);
        rooms.add(toReturn);

        return toReturn;
    }

    // This function adds a client to a list of clients if the client is not in the list already
    public synchronized void joinRoom (Socket socket) {

        // If the client is already in the list, do not add them
        for (Socket s : clients) {
            if (s == socket) {
                return;
            }
        }

        // Add the client if they are not already in the list
        this.clients.add(socket);
    }

    // This function removes a client from a list of clients so that we can remove them when they leave the room
    public synchronized void removeClient (Socket socket){

        int findClient = -1;
        // Make sure the client exists in the list
        for (int i = 0; i < clients.size(); i++){
            if (clients.get(i) == socket){
                findClient = i;
            }
        }

        // Remove client from the list
        if (findClient != -1) {
            clients.remove(clients.get(findClient));
        }
    }

    // This function determines if the client is in a room
    // so we know whether to send them a message or not
    public boolean clientInRoom (Socket socket){
        for (Socket s : clients){
            if (s == socket){
                return true;
            }
        }
        return false;
    }

    // This function removes the room from the array if
    // there are no clients using the room
    public void checkRoomEmpty () {
        if (clients.isEmpty()){
            rooms.remove(this);
        }
    }

    // This function sends the message to the correct rooms by writing the correct header and the
    // message recieved from the client over the WebSocket

    public synchronized void sendMessageToRoom(String s) throws IOException {

        for (Socket socket : clients) {

            // want a data output stream so that we can write shorts and longs
            DataOutputStream os = new DataOutputStream(socket.getOutputStream());

            // Write the first byte of the header
            // fin r r r    opcode
            // 1   0 0 0    0 0 0 1 = 0x81
            os.write(0x81);

            if (s.length() < 126){
                // Write 2nd byte of header - payload length
                os.write(s.length());
            }
            else if (s.length() < (Short.MAX_VALUE) * 2){
                // Write 3rd and 4th bytes of header with actual length (short), if needed
                os.write(126);
                os.writeShort(s.length());
            }
            else {
                // Write 3rd and 4th bytes of header with actual length (long), if needed
                os.write(127);
                os.writeLong(s.length());
            }

            // Get the bytes from the input message in string form
            byte[] message = s.getBytes();

            os.write(message);
        }
    }

    public String getRoomName() {
        return roomName_;
    }
}
