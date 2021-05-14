package org.briarheart.orchestra.data.database.postgresql;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.r2dbc.postgresql.client.Parameter;
import io.r2dbc.postgresql.codec.Codec;
import io.r2dbc.postgresql.message.Format;
import io.r2dbc.postgresql.type.PostgresqlObjectId;
import io.r2dbc.postgresql.util.ByteBufUtils;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;

import static io.r2dbc.postgresql.message.Format.FORMAT_TEXT;
import static io.r2dbc.postgresql.type.PostgresqlObjectId.*;

/**
 * Codec that encodes/decodes {@link Enum}s using {@link Enum#name()} method to represent enumeration value as text
 * in database.
 *
 * @author Roman Chigvintsev
 */
public class EnumCodec implements Codec<Enum<?>> {
    private final ByteBufAllocator byteBufAllocator;

    /**
     * Creates new instance of this class with the given byte buffer allocator.
     *
     * @param byteBufAllocator byte buffer allocator (must not be {@code null})
     */
    public EnumCodec(ByteBufAllocator byteBufAllocator) {
        Assert.notNull(byteBufAllocator, "Byte buffer allocator must not be null");
        this.byteBufAllocator = byteBufAllocator;
    }

    @Override
    public boolean canDecode(int dataType, Format format, Class<?> type) {
        Assert.notNull(type, "Type must not be null");

        if (PostgresqlObjectId.isValid(dataType) && (type == Object.class || type.isEnum())) {
            PostgresqlObjectId objectId = valueOf(dataType);
            return BPCHAR == objectId
                    || CHAR == objectId
                    || TEXT == objectId
                    || UNKNOWN == objectId
                    || VARCHAR == objectId;
        }

        return false;
    }

    @Override
    public boolean canEncode(Object value) {
        Assert.notNull(value, "Value must not be null");
        return value.getClass().isEnum();
    }

    @Override
    public boolean canEncodeNull(Class<?> type) {
        Assert.notNull(type, "Type must not be null");
        return type.isEnum();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public Enum<?> decode(ByteBuf buffer, int dataType, Format format, Class<? extends Enum<?>> type) {
        if (buffer == null) {
            return null;
        }
        Assert.notNull(type, "Type must not be null");
        return Enum.valueOf((Class<? extends Enum>) type, ByteBufUtils.decode(buffer));
    }

    @Override
    public Parameter encode(Object value) {
        Assert.notNull(value, "Value must not be null");
        return new Parameter(FORMAT_TEXT, VARCHAR.getObjectId(), Mono.fromSupplier(()
                -> ByteBufUtils.encode(byteBufAllocator, ((Enum<?>) value).name())));
    }

    @Override
    public Parameter encodeNull() {
        return new Parameter(FORMAT_TEXT, VARCHAR.getObjectId(), Parameter.NULL_VALUE);
    }

    @Override
    public Class<?> type() {
        return Enum.class;
    }
}
