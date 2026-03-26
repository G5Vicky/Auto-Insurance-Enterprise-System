package com.insurance.autoinsurance.security;
import com.insurance.autoinsurance.model.AppUser;
import com.insurance.autoinsurance.repository.AppUserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class AppUserDetailsService implements UserDetailsService {
    private final AppUserRepository repo;
    public AppUserDetailsService(AppUserRepository repo) { this.repo = repo; }

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser u = repo.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        return new org.springframework.security.core.userdetails.User(
            u.getUsername(), u.getPasswordHash(), u.isEnabled(),
            true, true, true,
            List.of(new SimpleGrantedAuthority(u.getRole().name()))
        );
    }
}
