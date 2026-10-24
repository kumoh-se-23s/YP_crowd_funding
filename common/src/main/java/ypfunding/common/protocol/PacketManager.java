package ypfunding.common.protocol;

import ypfunding.common.protocol.packets.Packet;
import ypfunding.common.protocol.packets.Packets;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class PacketManager {


    public static final int MAX_BUFFER_SIZE = 1024;
    public static final int PACKET_LEN_HEADER_SIZE = 2;
    public static final int MESSAGE_TYPE_HEADER_SIZE = 2;
    public static final int TIMEOUT_MS = 5000;

    private static final Packets[] ALL_PACKETS = Packets.values();
    private static final Map<Class<? extends Packet>, Constructor<? extends Packet>> PKT_CONSTRUCTORS = new HashMap<>();
    private static final Map<Class<? extends Packet>, Function<Constructor<? extends Packet>, Packet>> PKT_GENERATORS = new HashMap<>();
    private static final Map<Class<? extends Packet>, Packets> PKT_TO_REQUESTS_MAP = new HashMap<>();

    static {
        try {
            for (Packets value : Packets.values()) {
                PKT_TO_REQUESTS_MAP.put(value.getPacketClass(), value);
                PKT_CONSTRUCTORS.put(value.getPacketClass(), value.getPacketClass().getDeclaredConstructor());
                PKT_GENERATORS.put(value.getPacketClass(), PacketManager::constructPacket);
            }
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Packet class does not have a default constructor", e);
        }
    }

    private PacketManager() {

    }

    public static RawPacketTemp demultiplex(byte[] buffer){
        byte[] payloadBuf = new byte[buffer.length - PACKET_LEN_HEADER_SIZE - MESSAGE_TYPE_HEADER_SIZE];
        ByteBuffer payload = ByteBuffer.wrap(payloadBuf);
        int index = (buffer[PACKET_LEN_HEADER_SIZE] & 0xff) | ((buffer[PACKET_LEN_HEADER_SIZE + 1] & 0xff) << 8); // the size of request number is 2 byte
        System.arraycopy(buffer, PACKET_LEN_HEADER_SIZE + MESSAGE_TYPE_HEADER_SIZE, payloadBuf, 0, payloadBuf.length);

        if (index >= ALL_PACKETS.length) {
            throw new IllegalStateException("Invalid request number: " + index);
        }
        return new RawPacketTemp(index, ALL_PACKETS[index].getPacketClass(), payload);
    }

    @SuppressWarnings("unchecked")
    public static <P extends Packet> P decodePacket(byte[] buffer, Class<P> clazz) {

        RawPacketTemp raw = demultiplex(buffer);
        if (raw.packetClass() != clazz) {
            throw new IllegalStateException("Packet type does not match");
        }
        P pkt = (P) PKT_GENERATORS.get(clazz).apply(PKT_CONSTRUCTORS.get(clazz));
        try {
            pkt.decode(raw.payload());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to decode packet", e);
        }
        return pkt;
    }

    private static <P extends Packet> P constructPacket(Constructor<P> ctor) {
        try {
            return ctor.newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("Packet class does not have a default constructor", e);
        }
    }

    public static ByteBuffer makePacketBuffer(Packet packet) throws IOException {
        ByteBuffer out = ByteBuffer.allocate(MAX_BUFFER_SIZE);
        out.putShort((short) 0); //length of packet
        out.putShort((short) PKT_TO_REQUESTS_MAP.get(packet.getClass()).ordinal()); //request number
        packet.encode(out);
        int size = out.position();

        if (size > Short.MAX_VALUE) {
            throw new IOException("Packet too large: " + size);
        }

        out.putShort(0, (short) size);
        out.flip();
        return out;
    }
}
