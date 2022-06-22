import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;

/**
 * DNSQuestion represents a client request.
 */

public class DNSQuestion {

    // Components of a DNS question
    public String[] domainName;
    byte[] QTYPE;
    byte[] QCLASS;

    private DNSQuestion(){

    }

    /**
     * decodeQuestion() reads the header from a message
     *
     * @param inputStream - maintains the same input stream
     * @param message - message contains the readDomainName method, so we want to pass it in
     * @return DNSQuestion - represents the question component of the message
     * @throws IOException
     */

    static DNSQuestion decodeQuestion(InputStream inputStream, DNSMessage message) throws IOException {

        DNSQuestion question = new DNSQuestion();

        // Reads the domain name (first time seeing it).
        question.domainName = message.readDomainName(inputStream);

        // Next two bytes of the DNS question contain the QTYPE
        question.QTYPE = inputStream.readNBytes(2);

        // Next two bites of the DNS question contain the QCLASS
        question.QCLASS = inputStream.readNBytes(2);

        return question;
    }

    /**
     * writeBytes() writes the question bytes which will be sent to the client.
     *
     * @param byteArrayOutputStream - for maintaining the same output stream
     * @param domainNameLocations - contains the locations of different name components (useful for compression)
     * @throws IOException
     */

    void writeBytes(ByteArrayOutputStream byteArrayOutputStream, HashMap<String, Integer> domainNameLocations) throws IOException {

        // Will write the correct domain name taking compression into consideration
        DNSMessage.writeDomainName(byteArrayOutputStream, domainNameLocations, domainName);

        byteArrayOutputStream.write(QTYPE);

        byteArrayOutputStream.write(QCLASS);
    }


    @Override
    public String toString() {
        return "DNSQuestion{" +
                "QTYPE=" + Arrays.toString(QTYPE) +
                ", QCLASS=" + Arrays.toString(QCLASS) +
                ", domainName=" + Arrays.toString(domainName) +
                '}';
    }


    /**
     * equals and hashCode are needed to use a question as a HashMap key (for the cache)
     */

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DNSQuestion that = (DNSQuestion) o;
        return Arrays.equals(QTYPE, that.QTYPE) && Arrays.equals(QCLASS, that.QCLASS) && Arrays.equals(domainName, that.domainName);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(QTYPE);
        result = 31 * result + Arrays.hashCode(QCLASS);
        result = 31 * result + Arrays.hashCode(domainName);
        return result;
    }
}