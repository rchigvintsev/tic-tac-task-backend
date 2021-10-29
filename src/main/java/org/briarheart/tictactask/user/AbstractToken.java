package org.briarheart.tictactask.user;

import io.jsonwebtoken.lang.Assert;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * @author Roman Chigvintsev
 */
@Data
@NoArgsConstructor
public abstract class AbstractToken {
    @Id
    private Long id;
    private Long userId;
    private String email;
    private String tokenValue;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    public AbstractToken(AbstractToken other) {
        Assert.notNull(other, "Token must not be null");
        this.id = other.id;
        this.userId = other.userId;
        this.email = other.email;
        this.tokenValue = other.tokenValue;
        this.createdAt = other.createdAt;
        this.expiresAt = other.expiresAt;
    }

    protected AbstractToken(AbstractTokenBuilder<? extends AbstractToken> builder) {
        Assert.notNull(builder, "Token builder must not be null");
        this.id = builder.id;
        this.userId = builder.userId;
        this.email = builder.email;
        this.tokenValue = builder.tokenValue;
        this.createdAt = builder.createdAt;
        this.expiresAt = builder.expiresAt;
    }

    public boolean isExpired() {
        return !expiresAt.isAfter(LocalDateTime.now(ZoneOffset.UTC));
    }

    public static abstract class AbstractTokenBuilder<T extends AbstractToken> {
        private Long id;
        private Long userId;
        private String email;
        private String tokenValue;
        private LocalDateTime createdAt;
        private LocalDateTime expiresAt;

        public AbstractTokenBuilder<T> id(Long id) {
            this.id = id;
            return this;
        }

        public AbstractTokenBuilder<T> userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public AbstractTokenBuilder<T> email(String email) {
            this.email = email;
            return this;
        }

        public AbstractTokenBuilder<T> tokenValue(String tokenValue) {
            this.tokenValue = tokenValue;
            return this;
        }

        public AbstractTokenBuilder<T> createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public AbstractTokenBuilder<T> expiresAt(LocalDateTime expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }

        public abstract T build();
    }
}
