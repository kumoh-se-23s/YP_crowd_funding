package ypfunding.common.protocol;

import ypfunding.common.constant.NetworkConstants;
import ypfunding.common.protocol.packets.*;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class PacketManager {


    private static final PacketType[] ALL_PACKET_TYPES = PacketType.values();
    private static final RequestPackets[] ALL_REQUEST_PACKETS = RequestPackets.values();
    private static final ResponsePackets[] ALL_RESPONSE_PACKETS = ResponsePackets.values();
    private static final ResultPackets[] ALL_RESULT_PACKETS = ResultPackets.values();
    private static final Map<Class<? extends Packet>, Constructor<? extends Packet>> PKT_CONSTRUCTORS = new HashMap<>();
    private static final Map<Class<? extends Packet>, Function<Constructor<? extends Packet>, Packet>> PKT_GENERATORS = new HashMap<>();
    private static final Map<Class<? extends Packet>, RequestPackets> PKT_TO_REQUESTS_MAP = new HashMap<>();
    private static final Map<Class<? extends Packet>, ResponsePackets> PKT_TO_RESPONSE_MAP = new HashMap<>();
    private static final Map<Class<? extends Packet>, ResultPackets> PKT_TO_RESULT_MAP = new HashMap<>();

    static {
        try {
            for (RequestPackets value : RequestPackets.values()) {
                PKT_TO_REQUESTS_MAP.put(value.getPacketClass(), value);
                PKT_CONSTRUCTORS.put(value.getPacketClass(), value.getPacketClass().getDeclaredConstructor());
                PKT_GENERATORS.put(value.getPacketClass(), PacketManager::constructPacket);
            }
            for (ResponsePackets value : ResponsePackets.values()) {
                PKT_TO_RESPONSE_MAP.put(value.getPacketClass(), value);
                PKT_CONSTRUCTORS.put(value.getPacketClass(), value.getPacketClass().getDeclaredConstructor());
                PKT_GENERATORS.put(value.getPacketClass(), PacketManager::constructPacket);
            }
            for (ResultPackets value : ResultPackets.values()) {
                PKT_TO_RESULT_MAP.put(value.getPacketClass(), value);
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

        switch (packet) {
            case RequestPacket _ ->
                    out.put((byte) PKT_TO_REQUESTS_MAP.get(packet.getClass()).ordinal());
            case ResponsePacket _ ->
                    out.put((byte) PKT_TO_RESPONSE_MAP.get(packet.getClass()).ordinal());
            case ResultPacket _ ->
                    out.put((byte) PKT_TO_RESULT_MAP.get(packet.getClass()).ordinal());
            default -> throw new IllegalStateException("Invalid packet type: " + packet.getClass());
        }

        out.putShort((short) 0); //length of packet
        int size0 = out.position();
        packet.encode(out);
        int size1 = out.position();

        if (size1 > Short.MAX_VALUE) {
            throw new IOException("Packet too large: " + size1);
        }

        out.putShort(2, (short) (size1 - size0));
        out.flip();

        //로깅
        PacketLogger.logSend(packet, out);

        return out;
    }

    private static RawPacketTemp demuxRequestPacket(int packetCode, int payloadLen, ByteBuffer buffer) {
        if (packetCode >= ALL_REQUEST_PACKETS.length) {
            throw new IllegalStateException("Invalid request number: " + packetCode);
        }

        byte[] payloadBuf = new byte[payloadLen];
        buffer.get(payloadBuf);
        return new RawPacketTemp(PacketType.REQUEST, packetCode, ALL_REQUEST_PACKETS[packetCode].getPacketClass(), ByteBuffer.wrap(payloadBuf));

    }

    private static RawPacketTemp demuxResponsePacket(int packetCode, int payloadLen, ByteBuffer buffer) {
        if (packetCode >= ALL_RESPONSE_PACKETS.length) {
            throw new IllegalStateException("Invalid request number: " + packetCode);
        }

        byte[] payloadBuf = new byte[payloadLen];
        buffer.get(payloadBuf);
        return new RawPacketTemp(PacketType.RESPONSE, packetCode, ALL_RESPONSE_PACKETS[packetCode].getPacketClass(), ByteBuffer.wrap(payloadBuf));

    }

    private static RawPacketTemp demuxResultPacket(int packetCode, int payloadLen, ByteBuffer buffer) {
        if (packetCode >= ALL_RESULT_PACKETS.length) {
            throw new IllegalStateException("Invalid request number: " + packetCode);
        }

        byte[] payloadBuf = new byte[payloadLen];
        buffer.get(payloadBuf);
        return new RawPacketTemp(PacketType.RESULT, packetCode, ALL_RESULT_PACKETS[packetCode].getPacketClass(), ByteBuffer.wrap(payloadBuf));

    }

    public static RawPacketTemp demux(ByteBuffer buffer) {
        int headerSize = NetworkConstants.PACKET_LEN_HEADER_SIZE + NetworkConstants.MESSAGE_TYPE_HEADER_SIZE;

        if (buffer.remaining() < headerSize) {
            return null;
        }
        int position = buffer.position();

        int packetTypeNum = Byte.toUnsignedInt(buffer.get());
        if (packetTypeNum >= ALL_PACKET_TYPES.length) {
            throw new IllegalStateException("Invalid packet type: " + packetTypeNum);
        }

        int packetCode = Byte.toUnsignedInt(buffer.get());
        int payloadLen = Short.toUnsignedInt(buffer.getShort());

        if (buffer.remaining() < payloadLen) {
            buffer.position(position);
            return null;
        }

        PacketType packetType = ALL_PACKET_TYPES[packetTypeNum];
        RawPacketTemp raw = switch (packetType) {
            case REQUEST -> demuxRequestPacket(packetCode, payloadLen, buffer);
            case RESPONSE -> demuxResponsePacket(packetCode, payloadLen, buffer);
            case RESULT -> demuxResultPacket(packetCode, payloadLen, buffer);
        };

        //로깅
        PacketLogger.logReceive(raw);
        return raw;
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
