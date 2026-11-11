package ypfunding.common.protocol;

import ypfunding.common.constant.NetworkConstants;
import ypfunding.common.protocol.packets.Packet;
import ypfunding.common.protocol.packets.PacketType;
import ypfunding.common.protocol.packets.Packets;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class PacketManager {


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

    public static ByteBuffer mux(Packet packet) throws IOException {
        ByteBuffer out = ByteBuffer.allocate(NetworkConstants.MAX_BUFFER_SIZE);
        out.put((byte) packet.getType().ordinal()); //request type
        out.put((byte) PKT_TO_REQUESTS_MAP.get(packet.getClass()).ordinal()); //request code
        out.putShort((short) 0); //length of packet
        int size0 = out.position();
        packet.encode(out);
        int size1 = out.position();

        if (size1 > Short.MAX_VALUE) {
            throw new IOException("Packet too large: " + size1);
        }

        out.putShort(2, (short) (size1 - size0));
        out.flip();
        return out;
    }

    public static RawPacketTemp demux(ByteBuffer buffer) {
        int headerSize = NetworkConstants.PACKET_LEN_HEADER_SIZE + NetworkConstants.MESSAGE_TYPE_HEADER_SIZE;

        if (buffer.remaining() < headerSize) {
            return null;
        }
        int position = buffer.position();

        int packetType = Byte.toUnsignedInt(buffer.get());
        int packetCode = Byte.toUnsignedInt(buffer.get());
        int payloadLen = Short.toUnsignedInt(buffer.getShort());

        if (buffer.remaining() < payloadLen) {
            buffer.position(position);
            return null;
        }

        if (packetCode >= ALL_PACKETS.length) {
            throw new IllegalStateException("Invalid request number: " + packetCode);
        }


        byte[] payloadBuf = new byte[payloadLen];
        buffer.get(payloadBuf);
        return new RawPacketTemp(PacketType.values()[packetType], packetCode, ALL_PACKETS[packetCode].getPacketClass(), ByteBuffer.wrap(payloadBuf));
    }

    @SuppressWarnings("unchecked")
    public static <P extends Packet> P createPacket(Class<P> clazz) {
        return (P) PKT_GENERATORS.get(clazz).apply(PKT_CONSTRUCTORS.get(clazz));
    }

    public static <P extends Packet> P decodePacket(ByteBuffer buffer, Class<P> clazz) {
        P pkt = createPacket(clazz);
        try {
            pkt.decode(buffer);
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

}
