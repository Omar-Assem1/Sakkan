package com.sakan.common.domain;

/**
 * Currencies a price can be stored in (FR-11). Shared by every module that handles money
 * (property, offer, contract, payment), so it lives in {@code common}.
 */
public enum Currency {
    EGP,
    USD
}
