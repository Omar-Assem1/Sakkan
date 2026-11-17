package com.sakan.common.security;

import java.util.Optional;

public interface CurrentUserProvider {

    Optional<AuthenticatedUser> find();

    AuthenticatedUser require();
}
