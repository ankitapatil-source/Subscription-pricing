package com.subscription.pricing.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Represents a promotional voucher code with associated discount rules and optional expiration date.
 */
public class Voucher {
    private final String code;
    private final VoucherType type;
    private final BigDecimal value;
    private final LocalDate expiryDate;

    public Voucher(String code, VoucherType type, BigDecimal value) {
        this(code, type, value, null);
    }

    public Voucher(String code, VoucherType type, BigDecimal value, LocalDate expiryDate) {
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("Voucher code cannot be null or empty");
        }
        if (type == null) {
            throw new IllegalArgumentException("Voucher type cannot be null");
        }
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Voucher discount value cannot be null or negative");
        }
        this.code = code.trim().toUpperCase();
        this.type = type;
        this.value = value;
        this.expiryDate = expiryDate;
    }

    public String getCode() {
        return code;
    }

    public VoucherType getType() {
        return type;
    }

    public BigDecimal getValue() {
        return value;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public boolean isExpired(LocalDate currentDate) {
        return expiryDate != null && currentDate != null && currentDate.isAfter(expiryDate);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Voucher voucher = (Voucher) o;
        return Objects.equals(code, voucher.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code);
    }

    @Override
    public String toString() {
        return "Voucher{" +
                "code='" + code + '\'' +
                ", type=" + type +
                ", value=" + value +
                ", expiryDate=" + expiryDate +
                '}';
    }
}
