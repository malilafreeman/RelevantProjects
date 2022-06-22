import java.util.HashMap;

/**
 * DNSCache represents a local cache that stores the first answer for any question.
 */

public class DNSCache {

    // Use a hashmap to represent the cache (with the question as the key and the message as the value)
    HashMap<DNSQuestion, DNSMessage> cache;

    public DNSCache(){
        cache = new HashMap<>();
    }

    /**
     * insertRecord() stores a message in the hashmap with its question as the key
     *
     * @param question - the key to be stored
     * @param message - the message to be stored
     */
    void insertRecord(DNSQuestion question, DNSMessage message){

        cache.put(question, message);
    }


    /**
     * queryCache() checks to see if a question already exists in the cache. If it does but the time stamp has expired,
     * it removes the message from the cache and returns null. Otherwise, it returns the message that corresponds with the question.
     * If the question doesn't exist at all, it returns null.
     *
     * @param key - question to use to check to see if the message exists in the cache
     * @return null if the message doesn't exist at all or if the timestamp has expired.
     *         DNSMessage if the question/message pair exists in the cache and it has not expired.
     */
    DNSMessage queryCache(DNSQuestion key){

        if ((cache.get(key) != null) && (!cache.get(key).answers[0].timestampValid())){
            cache.remove(key);
            System.out.println("Record not found");
            return null;
        }

        return cache.get(key);
    }
}
