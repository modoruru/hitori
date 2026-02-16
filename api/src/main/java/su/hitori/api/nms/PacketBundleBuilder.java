package su.hitori.api.nms;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;

import java.util.ArrayList;
import java.util.List;

public final class PacketBundleBuilder {

    private final List<Packet<? super ClientGamePacketListener>> packets = new ArrayList<>();
    private ClientboundBundlePacket packet;

    public void add(Packet<? super ClientGamePacketListener> packet) {
        if(this.packet != null) throw new IllegalStateException();
        packets.add(packet);
    }

    public ClientboundBundlePacket build() {
        if(packet != null) return packet;
        return packet = new ClientboundBundlePacket(packets);
    }
}
