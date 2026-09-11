package com.subscription.pricing.repository;

import com.subscription.pricing.model.Voucher;

import java.util.Optional;

/**
 * Repository interface for managing and querying promotional vouchers.
 */
public interface VoucherRepository {

    /**
     * Finds a voucher by its promotional code (case-insensitive).
     *
     * @param code the voucher code to look up
     * @return an Optional containing the voucher if found, or empty otherwise
     */
    Optional<Voucher> findByCode(String code);

    /**
     * Saves or updates a voucher.
     *
     * @param voucher the voucher to save
     */
    void save(Voucher voucher);

    /**
     * Checks whether a voucher with the given code exists.
     *
     * @param code the voucher code
     * @return true if exists, false otherwise
     */
    boolean existsByCode(String code);

    /**
     * Deletes a voucher by its code.
     *
     * @param code the voucher code
     */
    void deleteByCode(String code);
}
