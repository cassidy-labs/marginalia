package org.cassidylabs.marginalia.application.port.out;

import org.cassidylabs.marginalia.domain.user.User;

import java.util.Optional;

public interface UserPort {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    User save(User user);
}
