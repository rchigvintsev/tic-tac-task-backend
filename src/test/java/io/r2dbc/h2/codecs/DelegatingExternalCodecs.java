package io.r2dbc.h2.codecs;

import lombok.RequiredArgsConstructor;
import org.h2.value.Value;
import org.springframework.util.Assert;
import reactor.util.annotation.Nullable;

import java.util.List;

/**
 * Implementation of {@link Codecs} that fallbacks to external codecs when delegate cannot encode or decode some value.
 *
 * @author Roman Chigvintsev
 */
@RequiredArgsConstructor
public class DelegatingExternalCodecs implements Codecs {
    private final Codecs delegate;
    private final List<Codec<?>> externalCodecs;

    @SuppressWarnings("unchecked")
    @Override
    public <T> T decode(Value value, int dataType, Class<? extends T> type) {
        Assert.notNull(type, "Type must not be null");
        try {
            return delegate.decode(value, dataType, type);
        } catch (IllegalArgumentException e) {
            for (Codec<?> codec : externalCodecs) {
                if (codec.canDecode(dataType, type)) {
                    return ((Codec<T>) codec).decode(value, type);
                }
            }
            throw e;
        }
    }

    @Override
    public Value encode(Object value) {
        Assert.notNull(value, "Value must not be null");
        try {
            return delegate.encode(value);
        } catch (IllegalArgumentException e) {
            for (Codec<?> codec : externalCodecs) {
                if (codec.canEncode(value)) {
                    return codec.encode(value);
                }
            }
            throw e;
        }
    }

    @Override
    public Value encodeNull(Class<?> type) {
        Assert.notNull(type, "Type must not be null");
        try {
            return delegate.encodeNull(type);
        } catch (Exception e) {
            for (Codec<?> codec : externalCodecs) {
                if (codec.canEncodeNull(type)) {
                    return codec.encodeNull();
                }
            }
            throw e;
        }
    }

    @Override
    @Nullable
    public Class<?> preferredType(int dataType) {
        Class<?> result = delegate.preferredType(dataType);
        if (result == null) {
            for (Codec<?> codec : externalCodecs) {
                if (codec.canDecode(dataType, Object.class)) {
                    result = codec.type();
                }
            }
        }
        return result;
    }
}
