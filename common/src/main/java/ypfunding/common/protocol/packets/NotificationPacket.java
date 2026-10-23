package ypfunding.common.protocol.packets;

import lombok.Setter;
import ypfunding.common.notifications.CompleteNotification;
import ypfunding.common.notifications.NotificationType;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class NotificationPacket implements Packet {
    @Setter
    private CompleteNotification notification;

    @Override
    public void decode(ByteBuffer in) {
        notification.setType(NotificationType.values()[in.getInt()]);
        byte[] raw = new byte[in.getInt()];
        in.get(raw);
        notification.setMessage(new String(raw, StandardCharsets.UTF_8));
    }

    @Override
    public void encode(ByteBuffer out) {
        out.putInt(notification.getType().ordinal());
        byte[] raw = notification.getMessage().getBytes(StandardCharsets.UTF_8);
        out.putInt(raw.length);
        out.put(raw);
    }


}
