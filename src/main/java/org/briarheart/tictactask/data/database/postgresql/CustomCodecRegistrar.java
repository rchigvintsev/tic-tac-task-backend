package org.briarheart.tictactask.data.database.postgresql;

import io.netty.buffer.ByteBufAllocator;
import io.r2dbc.postgresql.api.PostgresqlConnection;
import io.r2dbc.postgresql.codec.CodecRegistry;
import io.r2dbc.postgresql.extension.CodecRegistrar;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

/**
 * Implementation of {@link CodecRegistrar} for registering of custom PostgreSQL codecs.
 *
 * @author Roman Chigvintsev
 */
public class CustomCodecRegistrar implements CodecRegistrar {
    @Override
    public Publisher<Void> register(PostgresqlConnection connection,
                                    ByteBufAllocator allocator,
                                    CodecRegistry registry) {
        return Mono.fromRunnable(() -> registry.addLast(new EnumCodec(allocator)));
    }
}
