package com.sakan.common.security;

import java.util.Optional;

/** Gives controllers the caller of the current request. */
public interface CurrentUserProvider {

    /** The caller, or empty when the request is anonymous. */
    Optional<AuthenticatedUser> find();

    /** The caller. Throws an authentication exception when the request is anonymous. */
    AuthenticatedUser require();
}
