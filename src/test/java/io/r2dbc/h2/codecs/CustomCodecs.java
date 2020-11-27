package io.r2dbc.h2.codecs;

import io.jsonwebtoken.lang.Assert;
import io.r2dbc.h2.client.Client;
import org.h2.value.Value;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of {@link Codecs} that fallbacks to external codecs when standard codecs cannot encode or decode
 * a given value.
 *
 * @author Roman Chigvintsev
 */
// TODO: consider to delegate to io.r2dbc.h2.codecs.DefaultCodecs after migration to new version of "r2dbc-h2" library
public class CustomCodecs implements Codecs {
    private final List<Codec<?>> codecs;

    /**
     * Creates new instance of this class with the given client and optional list of external codecs.
     *
     * @param client for LOB codecs and whose class loader is used to search for optional codecs
     *               (must not be {@code null})
     * @param externalCodecs optional external codecs
     */
    public CustomCodecs(Client client, List<Codec<?>> externalCodecs) {
        Assert.notNull(client, "Client must not be null");
        this.codecs = createCodecs(client, externalCodecs);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T decode(Value value, int dataType, Class<? extends T> type) {
        Assert.notNull(type, "Type must not be null");
        if (value == null) {
            return null;
        }
        for (Codec<?> codec : codecs) {
            if (codec.canDecode(dataType, type)) {
                return ((Codec<T>) codec).decode(value, type);
            }
        }
        throw new IllegalArgumentException("Failed to decode value of type \"" + type.getName() + "\"");
    }

    @Override
    public Value encode(Object value) {
        Assert.notNull(value, "Value must not be null");
        for (Codec<?> codec : codecs) {
            if (codec.canEncode(value)) {
                return codec.encode(value);
            }
        }
        throw new IllegalArgumentException("Failed to encode value of type \"" + value.getClass().getName() + "\"");
    }

    @Override
    public Value encodeNull(Class<?> type) {
        Assert.notNull(type, "Type must not be null");
        for (Codec<?> codec : codecs) {
            if (codec.canEncodeNull(type)) {
                return codec.encodeNull();
            }
        }
        throw new IllegalArgumentException("Failed to encode null value of type \"" + type.getName() + "\"");
    }

    @Override
    public Class<?> preferredType(int dataType) {
        for (Codec<?> codec : codecs) {
            if (codec.canDecode(dataType, Object.class)) {
                return codec.type();
            }
        }
        return null;
    }

    private List<Codec<?>> createCodecs(Client client, List<Codec<?>> externalCodecs) {
        List<Codec<?>> codecs = new ArrayList<>(List.of(
                new BigDecimalCodec(),
                new BlobToByteBufferCodec(client),
                new BlobCodec(client),
                new BooleanCodec(),
                new ByteCodec(),
                new BytesCodec(),
                new ClobCodec(client),
                new DoubleCodec(),
                new FloatCodec(),
                new IntegerCodec(),
                new LocalDateCodec(),
                new LocalDateTimeCodec(client),
                new LocalTimeCodec(),
                new LongCodec(),
                new OffsetDateTimeCodec(client),
                new ShortCodec(),
                new StringCodec(),
                new UuidCodec(),
                new ZonedDateTimeCodec(client),
                new CustomClobToStringCodec(client)
        ));
        addOptionalCodecs(codecs, client.getClass().getClassLoader());
        if (externalCodecs != null) {
            codecs.addAll(externalCodecs);
        }
        return codecs;
    }

    private void addOptionalCodecs(List<Codec<?>> codecs, ClassLoader classLoader) {
        if (isPresent(classLoader, "org.locationtech.jts.geom.Geometry")) {
            codecs.add(new GeometryCodec());
        }
    }

    private boolean isPresent(ClassLoader classLoader, String fullyQualifiedClassName) {
        try {
            classLoader.loadClass(fullyQualifiedClassName);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
