import java.io.*;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Arrays;
import java.time.LocalDateTime;
import java.util.HashMap;

/**
 * DNSRecord represents a DNS record. Everything after the header and question parts of the DNS message are stored as records.
 */

public class DNSRecord {

    // Components of a DNS record
    String[] NAME;
    byte[] nameAsBytes;

    // Used when domain name is compressed
    public short offset;

    byte[] TYPE;
    byte[] CLASS;
    int TTL;
    short RDLENGTH;
    byte[] RDATA;

    // When the record was created
    LocalDateTime creationDate;


    private DNSRecord(){

    }

    /**
     * decodeRecord() reads the record(s) from a message
     *
     * @param inputStream - maintains the same input stream
     * @param message - message contains the readDomainName() method, so we need to pass it in
     * @return DNSRecord - represents the record component of the message
     * @throws IOException
     */

    static DNSRecord decodeRecord(InputStream inputStream, DNSMessage message) throws IOException {

        DNSRecord record = new DNSRecord();

        // Represents the time the record was created
        record.creationDate = LocalDateTime.now();

        // Mark the inputStream so that we can return to this location
        inputStream.mark(2);

        // Need to check the first byte to see if the first 2 bits are set, which indicates compression
        byte[] peekForCompression = inputStream.readNBytes(1);

        // If the first two bits are 1's, it means that the domain name is compressed.
        byte firstTwoBits = (byte) (peekForCompression[0] >> 6);

        if ((firstTwoBits & (0x3)) == 0x3){

            // Need the next byte to get the offset
            byte[] secondCompressionByte = inputStream.readNBytes(1);

            // If the first two bits are 1's, then the next 14 bits represent the location of the domain name in the message (the offset).
            byte firstSixBits = (byte) (peekForCompression[0] << 2);
            firstSixBits = (byte) (firstSixBits >> 2);

            // Mask the first six bits together with the next eight bits
            record.offset = (short) ((short) firstSixBits << 8 | (secondCompressionByte[0] & (0xFF)));

            record.NAME = message.readDomainName(record.offset);
        }

        // If the first two bits are not 1's, it means that the domain name is not compressed
        else {

            // Return back to the initial inputStream location
            inputStream.reset();

            // Call the version of readDomainName that does not deal with compression
            record.NAME = message.readDomainName(inputStream);
        }

        // TYPE, CLASS, TTL, and RDLENGTH are each represented by two bytes
        record.TYPE = inputStream.readNBytes(2);

        record.CLASS = inputStream.readNBytes(2);

        byte[] tempTTL = inputStream.readNBytes(4);
        record.TTL = ((tempTTL[0] << 24) | ((tempTTL[1] & (0xFF0000)) | (tempTTL[2] & (0xFF00) | (tempTTL[3] & (0xFF)))));

        byte[] tempRDLength = inputStream.readNBytes(2);
        record.RDLENGTH = (short) ((tempRDLength[0] << 8) | (tempRDLength[1] & (0xFF)));

        // RDLENGTH indicates how many bytes long RDATA is
        record.RDATA = inputStream.readNBytes(record.RDLENGTH);

        return record;
    }

    /**
     * writeBytes() encodes the record to bytes to be sent back to the client.
     *
     * @param byteArrayOutputStream - maintains the same output stream
     * @param domainNameLocations - - contains the locations of different name components (useful for compression)
     * @throws IOException
     */

    void writeBytes(ByteArrayOutputStream byteArrayOutputStream, HashMap<String, Integer> domainNameLocations) throws IOException {

        DNSMessage.writeDomainName(byteArrayOutputStream, domainNameLocations, NAME);

        byteArrayOutputStream.write(TYPE);

        byteArrayOutputStream.write(CLASS);

        // DataOutputStream can write an int
        DataOutputStream dos = new DataOutputStream(byteArrayOutputStream);

        // Want to write out the updated TTL according to how much time is left before the record expires
        dos.writeInt(getCurrentTTL());

        byteArrayOutputStream.write(shortToBytes(RDLENGTH));

        byteArrayOutputStream.write(RDATA);

    }

    /**
     * Since the TTL will decrease with time as it sits in our cache, getCurrentTTL() returns the updated TTL to write
     * in the response to the client.
     * @return int - represents the updated TTL
     */

    int getCurrentTTL(){

        // Get the current time when this method is called
        LocalDateTime currentTime = LocalDateTime.now();

        // Represents how much time has passed since the creation of the record and the current time
        Duration differenceInTTL = Duration.between(creationDate, currentTime);

        int updatedTTL = (int) (TTL - differenceInTTL.getSeconds());

        return updatedTTL;
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

    /**
     * timestampValid() determines whether the record has expired
     *
     * @return false if the creation date + the time to live is after the current time (the TTL has expired)
     */

    boolean timestampValid(){

        // Get the current time when this method is called
        LocalDateTime currentDate = LocalDateTime.now();

        // The record will expire at the creation date of the record plus the time to live
        LocalDateTime timeOfExpiration = creationDate.plusSeconds(TTL);

        // Returns false if the current time is past when the TTL has expired
        return currentDate.isBefore(timeOfExpiration);
    }

    @Override
    public String toString() {
        return "DNSRecord{" +
                "offset=" + offset +
                ", NAME=" + Arrays.toString(NAME) +
                ", nameAsBytes=" + Arrays.toString(nameAsBytes) +
                ", TYPE=" + Arrays.toString(TYPE) +
                ", CLASS=" + Arrays.toString(CLASS) +
                ", TTL=" + TTL +
                ", RDLENGTH=" + RDLENGTH +
                ", RDATA=" + Arrays.toString(RDATA) +
                ", creationDate=" + creationDate +
                '}';
    }
}
