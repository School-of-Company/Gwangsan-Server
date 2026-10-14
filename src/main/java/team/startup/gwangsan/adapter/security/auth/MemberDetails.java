package team.startup.gwangsan.adapter.security.auth;

import lombok.AllArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import team.startup.gwangsan.application.member.data.MemberAccount;

@AllArgsConstructor
public class MemberDetails implements UserDetails {

    private final MemberAccount member;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(member.role().name()));
    }

    @Override
    public String getPassword() {
        return member.passwordHash();
    }

    @Override
    public String getUsername() {
        return member.phoneNumber();
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
        return true;
    }

    public MemberAccount getMember() {
        return member;
    }
}

