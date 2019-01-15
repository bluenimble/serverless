package com.bluenimble.platform;


import java.io.Serializable;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

public final class OID implements Serializable {

    private static final long serialVersionUID = 3670079982654483072L;

    private static final int OBJECT_ID_LENGTH = 12;
    private static final int LOW_ORDER_THREE_BYTES = 0x00ffffff;

    // Use primitives to represent the 5-byte random value.
    private static final int RANDOM_VALUE1;
    private static final short RANDOM_VALUE2;

    private static final AtomicInteger NEXT_COUNTER = new AtomicInteger(new SecureRandom().nextInt());

    private static final char[] HEX_CHARS = new char[] {
            '0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    private final int timestamp;
    private final int counter;
    private final int randomValue1;
    private final short randomValue2;
    
    public static OID get () {
        return new OID ();
    }
    
    public OID () {
        this (new Date ());
    }

    /**
     * Checks if a string could be an {@code ObjectId}.
     *
     * @param hexString a potential ObjectId as a String.
     * @return whether the string could be an object id
     * @throws IllegalArgumentException if hexString is null
     */
    public static boolean isValid(final String hexString) {
        if (hexString == null) {
            throw new IllegalArgumentException();
        }

        int len = hexString.length();
        if (len != 24) {
            return false;
        }

        for (int i = 0; i < len; i++) {
            char c = hexString.charAt(i);
            if (c >= '0' && c <= '9') {
                continue;
            }
            if (c >= 'a' && c <= 'f') {
                continue;
            }
            if (c >= 'A' && c <= 'F') {
                continue;
            }

            return false;
        }

        return true;
    }

    /**
     * Constructs a new instance using the given date.
     *
     * @param date the date
     */
    public OID(final Date date) {
        this(dateToTimestampSeconds(date), NEXT_COUNTER.getAndIncrement() & LOW_ORDER_THREE_BYTES, false);
    }

    /**
     * Constructs a new instances using the given date and counter.
     *
     * @param date    the date
     * @param counter the counter
     * @throws IllegalArgumentException if the high order byte of counter is not zero
     */
    public OID(final Date date, final int counter) {
        this(dateToTimestampSeconds(date), counter, true);
    }

    /**
     * Creates an ObjectId using the given time, machine identifier, process identifier, and counter.
     *
     * @param timestamp         the time in seconds
     * @param counter           the counter
     * @throws IllegalArgumentException if the high order byte of counter is not zero
     */
    public OID(final int timestamp, final int counter) {
        this(timestamp, counter, true);
    }

    private OID(final int timestamp, final int counter, final boolean checkCounter) {
        this(timestamp, RANDOM_VALUE1, RANDOM_VALUE2, counter, checkCounter);
    }

    private OID(final int timestamp, final int randomValue1, final short randomValue2, final int counter,
                     final boolean checkCounter) {
        if ((randomValue1 & 0xff000000) != 0) {
            throw new IllegalArgumentException("The machine identifier must be between 0 and 16777215 (it must fit in three bytes).");
        }
        if (checkCounter && ((counter & 0xff000000) != 0)) {
            throw new IllegalArgumentException("The counter must be between 0 and 16777215 (it must fit in three bytes).");
        }
        this.timestamp = timestamp;
        this.counter = counter & LOW_ORDER_THREE_BYTES;
        this.randomValue1 = randomValue1;
        this.randomValue2 = randomValue2;
    }

    /**
     * Convert to a byte array.  Note that the numbers are stored in big-endian order.
     *
     * @return the byte array
     */
    public byte[] toByteArray() {
        ByteBuffer buffer = ByteBuffer.allocate(OBJECT_ID_LENGTH);
        putToByteBuffer(buffer);
        return buffer.array();  // using .allocate ensures there is a backing array that can be returned
    }

    /**
     * Convert to bytes and put those bytes to the provided ByteBuffer.
     * Note that the numbers are stored in big-endian order.
     *
     * @param buffer the ByteBuffer
     * @throws IllegalArgumentException if the buffer is null or does not have at least 12 bytes remaining
     * @since 3.4
     */
    public void putToByteBuffer(final ByteBuffer buffer) {
        buffer.put(int3(timestamp));
        buffer.put(int2(timestamp));
        buffer.put(int1(timestamp));
        buffer.put(int0(timestamp));
        buffer.put(int2(randomValue1));
        buffer.put(int1(randomValue1));
        buffer.put(int0(randomValue1));
        buffer.put(short1(randomValue2));
        buffer.put(short0(randomValue2));
        buffer.put(int2(counter));
        buffer.put(int1(counter));
        buffer.put(int0(counter));
    }

    /**
     * Gets the timestamp (number of seconds since the Unix epoch).
     *
     * @return the timestamp
     */
    public int getTimestamp() {
        return timestamp;
    }

    /**
     * Gets the timestamp as a {@code Date} instance.
     *
     * @return the Date
     */
    public Date getDate() {
        return new Date((timestamp & 0xFFFFFFFFL) * 1000L);
    }

    /**
     * Converts this instance into a 24-byte hexadecimal string representation.
     *
     * @return a string representation of the ObjectId in hexadecimal format
     */
    public String toHexString() {
        char[] chars = new char[OBJECT_ID_LENGTH * 2];
        int i = 0;
        for (byte b : toByteArray()) {
            chars[i++] = HEX_CHARS[b >> 4 & 0xF];
            chars[i++] = HEX_CHARS[b & 0xF];
        }
        return new String(chars);
    }

    @Override
    public String toString() {
        return toHexString();
    }

    

    static {
        try {
            SecureRandom secureRandom = new SecureRandom();
            RANDOM_VALUE1 = secureRandom.nextInt(0x01000000);
            RANDOM_VALUE2 = (short) secureRandom.nextInt(0x00008000);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static int dateToTimestampSeconds(final Date time) {
        return (int) (time.getTime() / 1000);
    }

    private static byte int3(final int x) {
        return (byte) (x >> 24);
    }

    private static byte int2(final int x) {
        return (byte) (x >> 16);
    }

    private static byte int1(final int x) {
        return (byte) (x >> 8);
    }

    private static byte int0(final int x) {
        return (byte) (x);
    }

    private static byte short1(final short x) {
        return (byte) (x >> 8);
    }

    private static byte short0(final short x) {
        return (byte) (x);
    }

}