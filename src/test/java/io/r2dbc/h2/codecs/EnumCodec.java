package io.r2dbc.h2.codecs;

import org.h2.value.Value;
import org.h2.value.ValueString;
import org.springframework.util.Assert;

/**
 * Codec that encodes/decodes {@link Enum}s using {@link Enum#name()} method to represent enumeration value as text
 * in database.
 *
 * @author Roman Chigvintsev
 */
@SuppressWarnings("rawtypes")
public class EnumCodec extends AbstractCodec<Enum> {
    /**
     * Creates new instance of this class.
     */
    public EnumCodec() {
        super(Enum.class);
    }

    @Override
    boolean doCanDecode(int dataType) {
        return dataType == Value.STRING;
    }

    @SuppressWarnings("unchecked")
    @Override
    Enum doDecode(Value value, Class<? extends Enum> type) {
        if (value == null) {
            return null;
        }
        Assert.notNull(type, "Type must not be null");
        return Enum.valueOf((Class<? extends Enum>) type, value.getString());
    }

    @Override
    Value doEncode(Enum value) {
        Assert.notNull(value, "Value must not be null");
        return ValueString.get(value.name());
    }
}
