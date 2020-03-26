package org.briarheart.orchestra.data.database.postgresql;

import io.netty.buffer.ByteBufAllocator;
import io.r2dbc.postgresql.api.PostgresqlConnection;
import io.r2dbc.postgresql.codec.Codec;
import io.r2dbc.postgresql.codec.CodecRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * @author Roman Chigvintsev
 */
class CustomCodecRegistrarTest {
    private CustomCodecRegistrar codecRegistrar;

    @BeforeEach
    void setUp() {
        codecRegistrar = new CustomCodecRegistrar();
    }

    @Test
    void shouldRegisterEnumCodec() {
        PostgresqlConnection connectionMock = mock(PostgresqlConnection.class);
        ByteBufAllocator byteBufAllocatorMock = mock(ByteBufAllocator.class);
        TestCodecRegistry codecRegistry = new TestCodecRegistry();
        ((Mono<Void>) codecRegistrar.register(connectionMock, byteBufAllocatorMock, codecRegistry)).block();
        assertTrue(codecRegistry.codecs.getFirst() instanceof EnumCodec);
    }

    private static class TestCodecRegistry implements CodecRegistry {
        final Deque<Codec<?>> codecs = new LinkedList<>();

        @Override
        public void addFirst(Codec<?> codec) {
            codecs.addFirst(codec);
        }

        @Override
        public void addLast(Codec<?> codec) {
            codecs.addLast(codec);
        }

        @Override
        public Iterator<Codec<?>> iterator() {
            return codecs.iterator();
        }
    }
}
