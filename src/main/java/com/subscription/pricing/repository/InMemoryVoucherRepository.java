package com.subscription.pricing.repository;

import com.subscription.pricing.model.Voucher;
import com.subscription.pricing.model.VoucherType;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory thread-safe implementation of VoucherRepository.
 * Preloaded with default promotional vouchers (SAVE20, HALFPRICE).
 */
@Repository
public class InMemoryVoucherRepository implements VoucherRepository {

    private final Map<String, Voucher> vouchers = new ConcurrentHashMap<>();

    public InMemoryVoucherRepository() {
        initDefaultVouchers();
    }

    public final void initDefaultVouchers() {
        vouchers.clear();
        save(new Voucher("SAVE20", VoucherType.FLAT, new BigDecimal("20.00")));
        save(new Voucher("HALFPRICE", VoucherType.PERCENTAGE, new BigDecimal("0.50")));
    }

    @Override
    public Optional<Voucher> findByCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(vouchers.get(code.trim().toUpperCase()));
    }

    @Override
    public void save(Voucher voucher) {
        if (voucher == null) {
            throw new IllegalArgumentException("Voucher cannot be null");
        }
        vouchers.put(voucher.getCode(), voucher);
    }

    @Override
    public boolean existsByCode(String code) {
        if (code == null) {
            return false;
        }
        return vouchers.containsKey(code.trim().toUpperCase());
    }

    @Override
    public void deleteByCode(String code) {
        if (code != null) {
            vouchers.remove(code.trim().toUpperCase());
        }
    }
}
