package dev.huskuraft.universal.api.networking;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import dev.huskuraft.universal.api.core.BlockState;
import dev.huskuraft.universal.api.core.Item;
import dev.huskuraft.universal.api.core.ItemStack;
import dev.huskuraft.universal.api.core.Registry;
import dev.huskuraft.universal.api.core.ResourceLocation;
import dev.huskuraft.universal.api.math.Vector2d;
import dev.huskuraft.universal.api.math.Vector2i;
import dev.huskuraft.universal.api.math.Vector3d;
import dev.huskuraft.universal.api.math.Vector3i;
import dev.huskuraft.universal.api.platform.PlatformReference;
import dev.huskuraft.universal.api.tag.ByteArrayTag;
import dev.huskuraft.universal.api.tag.IntArrayTag;
import dev.huskuraft.universal.api.tag.ListTag;
import dev.huskuraft.universal.api.tag.LongArrayTag;
import dev.huskuraft.universal.api.tag.NumericTag;
import dev.huskuraft.universal.api.tag.RecordTag;
import dev.huskuraft.universal.api.tag.StringTag;
import dev.huskuraft.universal.api.tag.Tag;
import dev.huskuraft.universal.api.text.Style;
import dev.huskuraft.universal.api.text.Text;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public final class NetByteBuf extends WrappedByteBuf {

    public NetByteBuf(ByteBuf source) {
        super(source);
    }

    public static NetByteBuf newBuffer() {
        return new NetByteBuf(Unpooled.buffer());
    }

    public <T> T readNullable(NetByteBufReader<T> reader) {
        if (readBoolean()) return read(reader);
        return null;
    }

    public UUID readUUID() {
        return new UUID(readLong(), readLong());
    }

    public <T extends Enum<T>> T readEnum(Class<T> clazz) {
        return clazz.getEnumConstants()[readVarInt()];
    }

    public String readString() {
        return Utf8String.read(this, 1024);
    }

    public Text readText() {
        return Text.text(readString()).withStyle(
                new Style(
                        readNullable(NetByteBuf::readInt),
                        readNullable(NetByteBuf::readBoolean),
                        readNullable(NetByteBuf::readBoolean),
                        readNullable(NetByteBuf::readBoolean),
                        readNullable(NetByteBuf::readBoolean),
                        readNullable(NetByteBuf::readBoolean)
                )
        ).withSiblings(readList(NetByteBuf::readText));

    }

    public int readVarInt() {
        return VarInt.read(this);
    }

    public long readVarLong() {
        return VarLong.read(this);
    }

    public Item readItem() {
        return readId(Item.REGISTRY);
    }

    public <T extends PlatformReference> T readId(Registry<T> registry) {
        return registry.byId(readVarInt());
    }

    public ItemStack readItemStack() {
        return ItemStack.of(
                readItem(),
                readVarInt()
        );
    }

    public <T> T read(NetByteBufReader<T> reader) {
        return reader.read(this);
    }

    public <T> List<T> readList(NetByteBufReader<T> reader) {
        var i = readVarInt();
        var list = new ArrayList<T>();

        for (int j = 0; j < i; ++j) {
            list.add(read(reader));
        }
        return Collections.unmodifiableList(list);
    }

    public <K, V> Map<K, V> readMap(NetByteBufReader<K> keyReader, NetByteBufReader<V> valueReader) {
        var i = readVarInt();
        var map = new LinkedHashMap<K, V>();

        for (int j = 0; j < i; ++j) {
            map.put(read(keyReader), read(valueReader));
        }
        return Collections.unmodifiableMap(map);
    }

    public <T> void writeNullable(T value, NetByteBufWriter<T> writer) {
        writeBoolean(value != null);
        if (value != null) write(value, writer);
    }

    public void writeUUID(UUID uuid) {
        writeLong(uuid.getMostSignificantBits());
        writeLong(uuid.getLeastSignificantBits());
    }

    public <T extends Enum<T>> void writeEnum(Enum<T> value) {
        writeVarInt(value.ordinal());
    }

    public void writeString(String value) {
        Utf8String.write(this, value, 1024);
    }

    public void writeText(Text value) {
        writeString(value.getString());
        writeNullable(value.getStyle().color(), NetByteBuf::writeInt);
        writeNullable(value.getStyle().bold(), NetByteBuf::writeBoolean);
        writeNullable(value.getStyle().italic(), NetByteBuf::writeBoolean);
        writeNullable(value.getStyle().underlined(), NetByteBuf::writeBoolean);
        writeNullable(value.getStyle().strikethrough(), NetByteBuf::writeBoolean);
        writeNullable(value.getStyle().obfuscated(), NetByteBuf::writeBoolean);
        writeList(value.getSiblings(), NetByteBuf::writeText);
    }

    public void writeVarInt(int value) {
        VarInt.write(this, value);
    }

    public void writeVarLong(long value) {
        VarLong.write(this, value);
    }

    public void writeItem(Item value) {
        writeId(Item.REGISTRY, value);
    }

    public <T extends PlatformReference> void writeId(Registry<T> registry, T value) {
        writeVarInt(registry.getId(value));
    }

    // FIXME: 18/5/24 NBT
    public void writeItemStack(ItemStack value) {
        writeItem(value.getItem());
        writeVarInt(value.getCount());
    }

    public <T> void write(T value, NetByteBufWriter<T> writer) {
        writer.write(this, value);
    }

    public <T> void writeList(Collection<T> collection, NetByteBufWriter<T> writer) {
        writeVarInt(collection.size());
        for (var object : collection) {
            write(object, writer);
        }
    }

    public <K, V> void writeMap(Map<K, V> map, NetByteBufWriter<K> keyWriter, NetByteBufWriter<V> valueWriter) {
        writeVarInt(map.size());
        for (var entry : map.entrySet()) {
            keyWriter.write(this, entry.getKey());
            valueWriter.write(this, entry.getValue());
        }
    }

    public Vector3d readVector3d() {
        return new Vector3d(readDouble(), readDouble(), readDouble());
    }

    public void writeVector3d(Vector3d vector) {
        writeDouble(vector.x());
        writeDouble(vector.y());
        writeDouble(vector.z());
    }

    public Vector2d readVector2d() {
        return new Vector2d(readDouble(), readDouble());
    }

    public void writeVector2d(Vector2d vector) {
        writeDouble(vector.x());
        writeDouble(vector.y());
    }

    public Vector3i readVector3i() {
        return new Vector3i(readVarInt(), readVarInt(), readVarInt());
    }

    public void writeVector3i(Vector3i vector) {
        writeVarInt(vector.x());
        writeVarInt(vector.y());
        writeVarInt(vector.z());
    }

    public Vector2i readVector2i() {
        return new Vector2i(readVarInt(), readVarInt());
    }

    public void writeVector2i(Vector2i vector) {
        writeVarInt(vector.x());
        writeVarInt(vector.y());
    }

    public ResourceLocation readResourceLocation() {
        return ResourceLocation.of(readString(), readString());
    }

    public void writeResourceLocation(ResourceLocation resourceLocation) {
        writeString(resourceLocation.getNamespace());
        writeString(resourceLocation.getPath());
    }

    public BlockState readBlockState() {
        return readId(BlockState.REGISTRY);
    }

    public void writeBlockState(BlockState blockState) {
        writeId(BlockState.REGISTRY, blockState);
    }

    public RecordTag readRecordTag() {
        return (RecordTag) readTag();
    }

    public void writeRecordTag(RecordTag tag) {
        writeTag(tag);
    }

    // ---- NBT 数组标签的读写 ----
    // Netty 的 ByteBuf 只有 readBytes/writeBytes，没有专门的数组方法，
    // 所以这里自己实现：先写长度（varInt，与 MC NBT 的格式一致），再写数据。

    public byte[] readByteArray() {
        var length = readVarInt();
        var value = new byte[length];
        readBytes(value);
        return value;
    }

    public void writeByteArray(byte[] value) {
        writeVarInt(value.length);
        writeBytes(value);
    }

    public int[] readIntArray() {
        var length = readVarInt();
        var value = new int[length];
        for (int i = 0; i < length; i++) {
            value[i] = readInt();
        }
        return value;
    }

    public void writeIntArray(int[] value) {
        writeVarInt(value.length);
        for (var item : value) {
            writeInt(item);
        }
    }

    public long[] readLongArray() {
        var length = readVarInt();
        var value = new long[length];
        for (int i = 0; i < length; i++) {
            value[i] = readLong();
        }
        return value;
    }

    public void writeLongArray(long[] value) {
        writeVarInt(value.length);
        for (var item : value) {
            writeLong(item);
        }
    }

    public Tag readTag() {
        return switch (readByte()) {
            case Tag.TAG_BYTE -> NumericTag.of(readByte());
            case Tag.TAG_SHORT -> NumericTag.of(readShort());
            case Tag.TAG_INT -> NumericTag.of(readInt());
            case Tag.TAG_LONG -> NumericTag.of(readLong());
            case Tag.TAG_FLOAT -> NumericTag.of(readFloat());
            case Tag.TAG_DOUBLE -> NumericTag.of(readDouble());
            case Tag.TAG_STRING -> StringTag.of(readString());
            case Tag.TAG_LIST -> ListTag.of(readList(NetByteBuf::readTag));
            case Tag.TAG_COMPOUND -> RecordTag.of(readMap(NetByteBuf::readString, NetByteBuf::readTag));
            case Tag.TAG_BYTE_ARRAY -> ByteArrayTag.of(readByteArray());
            case Tag.TAG_INT_ARRAY -> IntArrayTag.of(readIntArray());
            case Tag.TAG_LONG_ARRAY -> LongArrayTag.of(readLongArray());
            default -> throw new IllegalArgumentException("Unsupported tag type!");
        };
    }

    public void writeTag(Tag tag) {
        writeByte(tag.getId());
        switch (tag.getId()) {
            case Tag.TAG_BYTE -> writeByte(tag.asPrimitive().getAsByte());
            case Tag.TAG_SHORT -> writeShort(tag.asPrimitive().getAsShort());
            case Tag.TAG_INT -> writeInt(tag.asPrimitive().getAsInt());
            case Tag.TAG_LONG -> writeLong(tag.asPrimitive().getAsLong());
            case Tag.TAG_FLOAT -> writeFloat(tag.asPrimitive().getAsFloat());
            case Tag.TAG_DOUBLE -> writeDouble(tag.asPrimitive().getAsDouble());
            case Tag.TAG_STRING -> writeString(tag.asString().getString());
            case Tag.TAG_LIST -> writeList(tag.asList().stream().toList(), NetByteBuf::writeTag);
            case Tag.TAG_COMPOUND -> writeMap(tag.asRecord().getTags(), NetByteBuf::writeString, NetByteBuf::writeTag);
            case Tag.TAG_BYTE_ARRAY -> writeByteArray(tag.asByteArray().getAsByteArray());
            case Tag.TAG_INT_ARRAY -> writeIntArray(tag.asIntArray().getAsIntArray());
            case Tag.TAG_LONG_ARRAY -> writeLongArray(tag.asLongArray().getAsLongArray());
            default -> throw new IllegalArgumentException("Unsupported tag!");
        }
    }

}
