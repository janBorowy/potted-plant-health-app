package pl.student.sensors.service;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.stereotype.Component;
import pl.student.sensors.exception.NotImplementedException;
import pl.student.sensors.model.UserRecord;
import pl.student.sensors.model.UserRepository;

@Component
@RequiredArgsConstructor
public class UserDetailsManagerImpl implements UserDetailsManager {

    private final UserRepository userRepository;

    @Override
    public void createUser(@NonNull UserDetails user) {
        throw new NotImplementedException();
    }

    @Override
    public void updateUser(@NonNull UserDetails user) throws UsernameNotFoundException {
        if (!userRepository.existsByUsername(user.getUsername())) {
            throw new UsernameNotFoundException("user not found");
        }
        userRepository.save(toUserRecord(user));
    }

    @Override
    public void deleteUser(@NonNull String username) {
        throw new NotImplementedException();
    }

    @Override
    public void changePassword(@Nullable String oldPassword, @Nullable String newPassword) {
        throw new NotImplementedException();
    }

    @Override
    public boolean userExists(@NonNull String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    public UserDetails loadUserByUsername(@NonNull String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .map(this::toUserDetails)
                .orElseThrow(() -> new UsernameNotFoundException("user not found"));
    }

    private UserDetails toUserDetails(UserRecord userRecord) {
        return User.withUsername(userRecord.username())
                .password(userRecord.hashedPassword())
                .build();
    }

    private UserRecord toUserRecord(UserDetails userDetails) {
        return UserRecord.builder()
                .username(userDetails.getUsername())
                .hashedPassword(userDetails.getPassword())
                .build();
    }
}
