package com.subscription.pricing.repository;

import com.subscription.pricing.model.Voucher;
import com.subscription.pricing.model.VoucherType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("InMemoryVoucherRepository Unit Tests")
class InMemoryVoucherRepositoryTest {

    private InMemoryVoucherRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryVoucherRepository();
    }

    @Test
    @DisplayName("Preloaded default vouchers exist")
    void defaultVouchersExist() {
        Optional<Voucher> save20 = repository.findByCode("SAVE20");
        assertThat(save20).isPresent();
        assertThat(save20.get().getType()).isEqualTo(VoucherType.FLAT);
        assertThat(save20.get().getValue()).isEqualByComparingTo("20.00");

        Optional<Voucher> halfPrice = repository.findByCode("HALFPRICE");
        assertThat(halfPrice).isPresent();
        assertThat(halfPrice.get().getType()).isEqualTo(VoucherType.PERCENTAGE);
        assertThat(halfPrice.get().getValue()).isEqualByComparingTo("0.50");
    }

    @Test
    @DisplayName("Lookup is case-insensitive and trims whitespace")
    void lookupIsCaseInsensitive() {
        assertThat(repository.findByCode("save20")).isPresent();
        assertThat(repository.findByCode("  SAVE20  ")).isPresent();
        assertThat(repository.findByCode("halfprice")).isPresent();
    }

    @Test
    @DisplayName("Lookup with null or unknown code returns empty Optional")
    void lookupUnknownReturnsEmpty() {
        assertThat(repository.findByCode(null)).isEmpty();
        assertThat(repository.findByCode("UNKNOWN_CODE")).isEmpty();
    }

    @Test
    @DisplayName("Save and existsByCode work as expected")
    void saveAndExists() {
        Voucher newVoucher = new Voucher("TEST10", VoucherType.FLAT, new BigDecimal("10.00"), LocalDate.now().plusDays(10));
        repository.save(newVoucher);

        assertThat(repository.existsByCode("TEST10")).isTrue();
        assertThat(repository.existsByCode("test10")).isTrue();
        assertThat(repository.existsByCode("NON_EXISTENT")).isFalse();
        assertThat(repository.existsByCode(null)).isFalse();
    }

    @Test
    @DisplayName("Delete removes voucher by code")
    void deleteByCode() {
        assertThat(repository.existsByCode("SAVE20")).isTrue();
        repository.deleteByCode("save20");
        assertThat(repository.existsByCode("SAVE20")).isFalse();
    }

    @Test
    @DisplayName("Save null voucher throws IllegalArgumentException")
    void saveNullThrowsException() {
        assertThatThrownBy(() -> repository.save(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
