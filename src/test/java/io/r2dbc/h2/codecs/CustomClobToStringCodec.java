package io.r2dbc.h2.codecs;

import io.r2dbc.h2.client.Client;
import org.h2.value.Value;
import org.h2.value.ValueNull;
import org.springframework.util.Assert;

import java.io.StringReader;

/**
 * Fixed version of {@link ClobToStringCodec} from <a href="https://github.com/r2dbc/r2dbc-h2">r2dbc-h2</a> repository.
 *
 * @author Roman Chigvintsev
 */
// TODO: consider to remove this class after migration to new version of "r2dbc-h2" library
class CustomClobToStringCodec extends AbstractCodec<String> {
    private final Client client;

    CustomClobToStringCodec(Client client) {
        super(String.class);
        this.client = client;
    }

    @Override
    boolean doCanDecode(int dataType) {
        return dataType == Value.CLOB;
    }

    @Override
    String doDecode(Value value, Class<? extends String> type) {
        if (value == null || value instanceof ValueNull) {
            return null;
        }
        return value.getString();
    }

    @Override
    Value doEncode(String value) {
        Assert.notNull(value, "Value must not be null");
        Value clob = client.getSession().getDataHandler().getLobStorage()
                .createClob(new StringReader(value), value.length());
        client.getSession().addTemporaryLob(clob);
        return clob;
    }
}
