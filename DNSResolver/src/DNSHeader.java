import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * DNSHeader stores all the data provided by the 12 byte DNS header.
 */
public class DNSHeader {

    // Components of a DNS header
    short ID;
    byte[] thirdAndFourthHeaderBytes;
    byte QR;
    byte OpCode;
    byte GroupAATCRD;
    byte GroupRAZADCD;
    byte RCODE;

    byte[] QDCOUNT;
    byte[] ANCOUNT;
    byte[] NSCOUNT;
    byte[] ARCOUNT;

    // Short versions are used to make comparisons more easily
    short ANCOUNTShort;
    short NSCOUNTShort;
    short ARCOUNTShort;


    private DNSHeader(){

    }

    /**
     * decodeHeader() reads the header from a message
     *
     * @param inputStream - for maintaining the same input stream
     * @return DNSHeader - a new header with the correct components represented
     * @throws IOException
     */

    public static DNSHeader decodeHeader(ByteArrayInputStream inputStream) throws IOException {

        DNSHeader header = new DNSHeader();

        // First two bytes of the header contain the ID
        byte[] tempID = inputStream.readNBytes(2);
        header.ID = (short) ((tempID[0] << 8) | (tempID[1] & (0xFF)));

        // Next two bytes of the header contain the QR, OpCode, AA, TC, RD, RA, Z, AD, CD, and RCODE
        header.thirdAndFourthHeaderBytes = inputStream.readNBytes(2);

        byte thirdHeaderByte = header.thirdAndFourthHeaderBytes[0];

        header.QR = (byte)(thirdHeaderByte >> 7 & 0x1);

        header.OpCode = (byte) (thirdHeaderByte << 1);
        header.OpCode = (byte) (header.OpCode >> 4);

        header.GroupAATCRD = (byte) (thirdHeaderByte << 5);
        header.GroupAATCRD = (byte) (header.GroupAATCRD >> 5);


        byte fourthHeaderByte = header.thirdAndFourthHeaderBytes[1];

        header.GroupRAZADCD = (byte) (fourthHeaderByte >> 4);

        header.RCODE = (byte) (fourthHeaderByte << 4);
        header.RCODE = (byte) (header.RCODE >> 4);

        // QDCOUNT, ANCOUNT, NSCOUNT, and RCOUNT are each represented by two bytes

        header.QDCOUNT = inputStream.readNBytes(2);

        header.ANCOUNT = inputStream.readNBytes(2);
        header.ANCOUNTShort = (short) ((header.ANCOUNT[0] << 8) | (header.ANCOUNT[1] & (0xFF)));

        header.NSCOUNT = inputStream.readNBytes(2);
        header.NSCOUNTShort = (short) ((header.NSCOUNT[0] << 8) | (header.NSCOUNT[1] & (0xFF)));

        header.ARCOUNT = inputStream.readNBytes(2);
        header.ARCOUNTShort = (short) ((header.ARCOUNT[0] << 8) | (header.ARCOUNT[1] & (0xFF)));


        return header;
    }

    /**
     * buildResponseHeader() creates the header for the response. Copies some fields from the initial client query and Google's response.
     *
     * @param request - initial request received from client
     * @param response - response from Google
     * @return DNSHeader - represents new header for our response message to the client.
     */

    static DNSHeader buildResponseHeader(DNSMessage request, DNSMessage response){

        DNSHeader responseHeader = new DNSHeader();

        // ID should match the initial query
        responseHeader.ID = request.header.ID;

        // Remaining header components should match Google's response header
        responseHeader.thirdAndFourthHeaderBytes = response.header.thirdAndFourthHeaderBytes;

        responseHeader.QDCOUNT = response.header.QDCOUNT;
        responseHeader.ANCOUNTShort = response.header.ANCOUNTShort;
        responseHeader.NSCOUNTShort = response.header.NSCOUNTShort;
        responseHeader.ARCOUNTShort = response.header.ARCOUNTShort;

        return responseHeader;
    }

    /**
     * writeBytes() encodes the header to bytes to be sent back to the client.
     *
     * @param outputStream - maintains the same output stream
     * @throws IOException
     */

    void writeBytes(OutputStream outputStream) throws IOException {

        outputStream.write(shortToBytes(ID));

        outputStream.write(thirdAndFourthHeaderBytes);

        outputStream.write(QDCOUNT);

        // ANCOUNT will never be greater than 1 for our purposes, since we only want to send back 1 answer
        if (ANCOUNTShort > (short)1){
            ANCOUNTShort = 1;
        }

        outputStream.write(shortToBytes(ANCOUNTShort));

        outputStream.write(shortToBytes(NSCOUNTShort));

        outputStream.write(shortToBytes(ARCOUNTShort));
    }

    /**
     * shortToBytes() takes in a short and returns it as a byte array
     *
     * @param s - short to be converted into a byte array
     * @return byte[] - byte version of the short
     */

    byte[] shortToBytes(short s){

        ByteBuffer bb = ByteBuffer.allocate(2);
        bb.putShort(s);
        return bb.array();
    }

    @Override
    public String toString() {
        return "DNSHeader{" +
                "ID=" + ID +
                ", thirdAndFourthHeaderBytes=" + Arrays.toString(thirdAndFourthHeaderBytes) +
                ", QR=" + QR +
                ", OpCode=" + OpCode +
                ", GroupAATCRD=" + GroupAATCRD +
                ", GroupRAZADCD=" + GroupRAZADCD +
                ", RCODE=" + RCODE +
                ", QDCOUNT=" + Arrays.toString(QDCOUNT) +
                ", ANCOUNT=" + Arrays.toString(ANCOUNT) +
                ", NSCOUNT=" + Arrays.toString(NSCOUNT) +
                ", ARCOUNT=" + Arrays.toString(ARCOUNT) +
                ", ANCOUNTShort=" + ANCOUNTShort +
                ", NSCOUNTShort=" + NSCOUNTShort +
                ", ARCOUNTShort=" + ARCOUNTShort +
                '}';
    }
}