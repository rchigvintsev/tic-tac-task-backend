package org.briarheart.orchestra.data.database.postgresql;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.r2dbc.postgresql.client.Parameter;
import io.r2dbc.postgresql.type.PostgresqlObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Roman Chigvintsev
 */
@SuppressWarnings("ConstantConditions")
class EnumCodecTest {
    private EnumCodec codec;

    @BeforeEach
    void setUp() {
        ByteBuf byteBufMock = mock(ByteBuf.class);
        ByteBufAllocator byteBufAllocatorMock = mock(ByteBufAllocator.class);
        when(byteBufAllocatorMock.buffer()).thenReturn(byteBufMock);

        this.codec = new EnumCodec(byteBufAllocatorMock);
    }

    @Test
    void shouldThrowExceptionOnConstructWhenByteBufferAllocatorIsNull() {
        assertThrows(NullPointerException.class, () -> new EnumCodec(null));
    }

    @Test
    void shouldSupportEnumEncoding() {
        assertTrue(codec.canEncode(TestEnum.HELLO));
    }

    @Test
    void shouldSupportOnlyEnumEncoding() {
        assertFalse(codec.canEncode("HELLO"));
    }

    @Test
    void shouldThrowExceptionOnCanEncodeWhenValueIsNull() {
        assertThrows(IllegalArgumentException.class, () -> codec.canEncode(null));
    }

    @Test
    void shouldSupportEnumNullEncoding() {
        assertTrue(codec.canEncodeNull(TestEnum.class));
    }

    @Test
    void shouldThrowExceptionOnCanEncodeNullWhenClassIsNull() {
        assertThrows(IllegalArgumentException.class, () -> codec.canEncodeNull(null));
    }

    @Test
    void shouldEncodeEnum() {
        Parameter parameter = codec.encode(TestEnum.HELLO);
        assertNotNull(parameter);
    }

    @Test
    void shouldThrowExceptionOnEncodeWhenValueIsNull() {
        assertThrows(IllegalArgumentException.class, () -> codec.encode(null));
    }

    @Test
    void shouldEncodeNull() {
        Parameter parameter = codec.encodeNull();
        assertNotNull(parameter);
    }

    @Test
    void shouldSupportBpcharDataTypeDecoding() {
        assertTrue(codec.canDecode(PostgresqlObjectId.BPCHAR.getObjectId(), null, TestEnum.class));
    }

    @Test
    void shouldSupportCharDataTypeDecoding() {
        assertTrue(codec.canDecode(PostgresqlObjectId.CHAR.getObjectId(), null, TestEnum.class));
    }

    @Test
    void shouldSupportTextDataTypeDecoding() {
        assertTrue(codec.canDecode(PostgresqlObjectId.TEXT.getObjectId(), null, TestEnum.class));
    }

    @Test
    void shouldSupportVarcharDataTypeDecoding() {
        assertTrue(codec.canDecode(PostgresqlObjectId.VARCHAR.getObjectId(), null, TestEnum.class));
    }

    @Test
    void shouldSupportUnknownDataTypeDecoding() {
        assertTrue(codec.canDecode(PostgresqlObjectId.UNKNOWN.getObjectId(), null, TestEnum.class));
    }

    @Test
    void shouldSupportDecodingToObject() {
        assertTrue(codec.canDecode(PostgresqlObjectId.VARCHAR.getObjectId(), null, Object.class));
    }

    @Test
    void shouldNotSupportInvalidDataTypeDecoding() {
        assertFalse(codec.canDecode(-1, null, TestEnum.class));
    }

    @Test
    void shouldSupportDecodingOnlyToEnumAndObject() {
        assertFalse(codec.canDecode(PostgresqlObjectId.VARCHAR.getObjectId(), null, Boolean.class));
    }

    @Test
    void shouldThrowExceptionOnCanDecodeWhenTypeIsNull() {
        assertThrows(IllegalArgumentException.class, ()
                -> codec.canDecode(PostgresqlObjectId.VARCHAR.getObjectId(), null, null));
    }

    @Test
    void shouldDecode() {
        ByteBuf byteBufMock = mock(ByteBuf.class);
        when(byteBufMock.readCharSequence(anyInt(), eq(StandardCharsets.UTF_8))).thenReturn("HELLO");
        Enum<?> value = codec.decode(byteBufMock, PostgresqlObjectId.VARCHAR.getObjectId(), null, TestEnum.class);
        assertSame(TestEnum.HELLO, value);
    }

    @Test
    void shouldThrowExceptionOnDecodeWhenTypeIsNull() {
        ByteBuf byteBufMock = mock(ByteBuf.class);
        assertThrows(IllegalArgumentException.class, ()
                -> codec.decode(byteBufMock, PostgresqlObjectId.VARCHAR.getObjectId(), null, null));
    }

    @Test
    void shouldReturnNullOnDecodeWhenByteBufferIsNull() {
        assertNull(codec.decode(null, PostgresqlObjectId.VARCHAR.getObjectId(), null, TestEnum.class));
    }

    private enum TestEnum {HELLO}
}
