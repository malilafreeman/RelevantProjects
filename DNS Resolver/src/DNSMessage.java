import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * DNSMessage represents an entire DNS message (either a client request, a google request, a google response, or a client response)
 */
public class DNSMessage {

    ByteArrayInputStream inputStream;

    // A DNSMessage represented in bytes
    byte[] byteMessage;

    DNSHeader header;

    DNSQuestion question;

    DNSRecord[] answers;
    DNSRecord[] authorityRecords;
    DNSRecord[] additionalRecords;

    private DNSMessage(){

    }

    /**
     * decodeMessage() represents an entire message
     *
     * @param bytes - the bytes represented by the DatagramPacket containing the information necessary for creating a message.
     * @return DNSMessage - a new message containing the appropriate header, question, and records according to the packet.
     * @throws IOException
     */

    static DNSMessage decodeMessage(byte[] bytes) throws IOException {

        DNSMessage message = new DNSMessage();

        // The new message should have its own inputStream
        message.inputStream = new ByteArrayInputStream(bytes);

        message.byteMessage = bytes;


        // Establish the message's header, question, and record components
        message.header = DNSHeader.decodeHeader(message.inputStream);

        message.question = DNSQuestion.decodeQuestion(message.inputStream, message);

        // QR indicates that the message is a response. If it is a response, we need to decode the record. If not, no
        // record is needed
        if (message.header.QR == 1){

            // ANCOUNT indicates how many answers the message contains - build an array of this size.
            message.answers = new DNSRecord[message.header.ANCOUNTShort];

            // Each answer in the answer array should be its own record
            for (int i = 0; i < message.answers.length; i++) {

                DNSRecord record = DNSRecord.decodeRecord(message.inputStream, message);
                message.answers[i] = record;
            }

            // NSCOUNT indicates how many authority records the message contains - build an array of this size.
            message.authorityRecords = new DNSRecord[message.header.NSCOUNTShort];

            // Each authority record in the authority records array should be its own record
            for (int i = 0; i < message.authorityRecords.length; i++) {

                DNSRecord record = DNSRecord.decodeRecord(message.inputStream, message);
                message.authorityRecords[i] = record;
            }

            // ARCOUNT indicates how many additional records the message contains - build an array of this size.
            message.additionalRecords = new DNSRecord[message.header.ARCOUNTShort];

            // Each additional record in the additional records array should be its own record
            for (int i = 0; i < message.additionalRecords.length; i++) {

                DNSRecord record = DNSRecord.decodeRecord(message.inputStream, message);
                message.additionalRecords[i] = record;
            }

        }

        return message;
    }

    /**
     * This version of readDomainName() reads the pieces of a domain name starting from the current position of the input stream
     * (used if the domain name is not compressed).
     *
     * @param inputStream - maintains use of the same inputStream.
     * @return - String[] representing the components of the domain name (i.e. google.com contains "google" and "com")
     * @throws IOException
     */

    String[] readDomainName(InputStream inputStream) throws IOException {

        // Make an array of byte arrays which represent the labels, or the components of the domain name.
        ArrayList<byte[]> labels = new ArrayList<>();

        // If the domain name is not compressed, the length byte will indicate how many bytes to read for the domain name.
        byte[] length = inputStream.readNBytes(1);

        // A length of zero indicates that there are no more labels to read.
        while (length[0] != 0){
            // Read as many bytes as the length byte indicates
            byte[] label = inputStream.readNBytes(length[0]);
            // Add the label to the array of labels
            labels.add(label);

            length = inputStream.readNBytes(1);
        }

        // Need to turn the array of labels into a string array
        String[] labelsAsStrings = new String[labels.size()];

        for (int i = 0; i < labels.size(); i++){
            String toAdd = new String(labels.get(i));
            labelsAsStrings[i] = toAdd;
        }

        return labelsAsStrings;
    }

    /**
     * This version of readDomainName() is used when there's compression and we need to find the domain from earlier in the message.
     * This method makes a ByteArrayInputStream that starts at the specified byte and calls the other version of this method.
     *
     * @param firstByte - represents the offset, or where in the inputStream the domain name is located.
     * @return - String[] representing the components of the domain name (i.e. google.com contains "google" and "com")
     * @throws IOException
     */

    String[] readDomainName(int firstByte) throws IOException {

        ByteArrayInputStream forReadDomainNameCompression = new ByteArrayInputStream(byteMessage);

        // Read up until the indicated byte to get to the correct location (but we don't actually use these bytes for anything)
        byte [] toDiscard = forReadDomainNameCompression.readNBytes(firstByte);

        // Call on the other version of readDomainName
        String[] domainName = readDomainName(forReadDomainNameCompression);

        return domainName;
    }

    /**
     * buildResponse() builds an entire message response based on the request and the answers intended to send back to the client.
     *
     * @param request - represents the request message initially sent to us in the client request.
     * @param responseFromGoogle - represents the response message sent to us from our Google request.
     * @return DNSMessage - represents the response message that we will send back to the client with the appropriate
     * header, question, and answer components.
     */

    static DNSMessage buildResponse(DNSMessage request, DNSMessage responseFromGoogle){

        DNSMessage responseMessage = new DNSMessage();

        responseMessage.header = DNSHeader.buildResponseHeader(request, responseFromGoogle);

        // Question and record sections should be based off of the response we got back from our Google query
        responseMessage.question = responseFromGoogle.question;

        responseMessage.answers = responseFromGoogle.answers;

        responseMessage.authorityRecords = responseFromGoogle.authorityRecords;

        responseMessage.additionalRecords = responseFromGoogle.additionalRecords;

        return responseMessage;
    }

    /**
     * toBytes() gets the message bytes to put in a packet and send back to the client.
     *
     * @return byte[] - represents the response message in bytes.
     * @throws IOException
     */

    byte[] toBytes() throws IOException {

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        header.writeBytes(outputStream);

        // Create a hashmap to keep track of where domain names are located within the message
        HashMap<String,Integer> domainNameLocations = new HashMap<>();

        question.writeBytes(outputStream, domainNameLocations);

        // For our purposes, we are only writing the first answer back to the client.
        if (answers.length > 0){
            answers[0].writeBytes(outputStream, domainNameLocations);
        }

        // If there are any authority records, we want to write them back to the client.
        for (DNSRecord r : authorityRecords){
            r.writeBytes(outputStream, domainNameLocations);
        }

        // If there are any additional records, we want to write them back to the client.
        for (DNSRecord r: additionalRecords){
            r.writeBytes(outputStream, domainNameLocations);
        }


        // Creates a byte[] from the output stream we created.
        return outputStream.toByteArray();
    }

    /**
     * If this is the first time we've seen this domain name in the packet, write it using DNS encoding
     * (each segment of the domain prefixed with its length, 0 at the end), and add it to the hash map.
     * Otherwise, write a back pointer to where the domain has been seen previously.
     *
     * @param byteArrayOutputStream - for maintaining the same output stream
     * @param domainLocations - hashmap that contains current domain name pieces and their positions in the message
     * @param domainNamePieces - string components of the domain name
     * @throws IOException
     */

    static void writeDomainName(ByteArrayOutputStream byteArrayOutputStream, HashMap<String,Integer> domainLocations, String[] domainNamePieces) throws IOException {

        // Get the full domain name, including dots
        String domainNameString = octetsToString(domainNamePieces);

        // If this name is not in the hashmap, we need to write it using encoding and add it to the hashmap.
        if (domainLocations.get(domainNameString) == null){

            // out.size tells us how many we've read in so far, which keeps track of the domain name's location in the message
            domainLocations.put(domainNameString, byteArrayOutputStream.size());

            for (String s : domainNamePieces) {
                int length = s.length();

                // DNS encoding: write the length of the domain name segment followed by the segment
                byteArrayOutputStream.write(((byte) length));
                byteArrayOutputStream.write(s.getBytes());
            }
            // Terminate domain name with a length of zero
            byteArrayOutputStream.write((byte)0);
        }

        // If this name is in the hashmap, it has been seen already and we need to write a pointer to its location.
        else {

            // Get the domain name's location in the stream (called the offset) from the hashmap
            int offset = domainLocations.get(domainNameString);

            // Indicate compression with two "1" bits
            int indicateCompression = 0xC000;

            // Represents 2 bytes with 2 "1" bits and the offset
            int compressedDomainName = (offset | indicateCompression);

            // Need to write one byte at a time (writing an int in a byteArrayStream will only write the last 8 bits)
            int writeFirstByte = compressedDomainName >> 8;

            byteArrayOutputStream.write(writeFirstByte);

            // Writes the last 8 bits from the original int
            byteArrayOutputStream.write(compressedDomainName);

        }

    }

    /**
     * octetsToString() joins the pieces of a domain name with dots (i.e. "google" and "com" becomes "google.com").
     *
     * @param octets - components of the domain name
     * @return - singular string that represents the domain name components connected with dots
     */

    static String octetsToString(String[] octets){

        String domainNameWithDots;

        // If the domain name has no components, create an empty string
        if (octets.length == 0){
            domainNameWithDots = "";
        }
        // Get the first domain name component
        else {
            domainNameWithDots = octets[0];
        }

        for (int i = 1; i < octets.length; i++){
            domainNameWithDots += "." + octets[i];
        }

        return domainNameWithDots;
    }


    @Override
    public String toString() {
        return "DNSMessage{" +
                "inputStream=" + inputStream +
                ", byteMessage=" + Arrays.toString(byteMessage) +
                ", header=" + header +
                ", question=" + question +
                ", answers=" + Arrays.toString(answers) +
                ", authorityRecords=" + Arrays.toString(authorityRecords) +
                ", additionalRecords=" + Arrays.toString(additionalRecords) +
                '}';
    }

}
