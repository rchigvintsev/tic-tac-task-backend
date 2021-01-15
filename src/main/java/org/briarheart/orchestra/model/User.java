package org.briarheart.orchestra.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.Collection;
import java.util.Collections;

/**
 * @author Roman Chigvintsev
 */
@Data
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Table("users")
public class User implements UserDetails {
    @Id
    @NotBlank
    @Size(max = 255)
    private String email;

    @Version
    private long version;

    @NotBlank
    @Size(max = 50)
    private String password;

    @Builder.Default
    private boolean enabled = true;

    @Size(max = 255)
    private String fullName;

    @Size(max = 2_000)
    private String imageUrl;

    @Builder.Default
    @Transient
    private Collection<? extends GrantedAuthority> authorities = Collections.emptyList();

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    public void setAuthorities(Collection<? extends GrantedAuthority> authorities) {
        this.authorities = authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    /**
     * Alias for {@link #getEmail()}.
     *
     * @return user email
     */
    @Override
    public String getUsername() {
        return getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
