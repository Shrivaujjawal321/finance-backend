package com.finance.repository;

import com.finance.entity.FinancialRecord;
import com.finance.enums.RecordType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FinancialRecordRepository extends JpaRepository<FinancialRecord, Long> {

    Optional<FinancialRecord> findByIdAndDeletedFalse(Long id);

    @Query("SELECT r FROM FinancialRecord r WHERE r.deleted = false " +
            "AND (:type IS NULL OR r.type = :type) " +
            "AND (:category IS NULL OR r.category = :category) " +
            "AND (:startDate IS NULL OR r.date >= :startDate) " +
            "AND (:endDate IS NULL OR r.date <= :endDate) " +
            "ORDER BY r.date DESC")
    Page<FinancialRecord> findWithFilters(
            @Param("type") RecordType type,
            @Param("category") String category,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable);

    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM FinancialRecord r " +
            "WHERE r.deleted = false AND r.type = :type")
    BigDecimal sumByType(@Param("type") RecordType type);

    @Query("SELECT r.category, SUM(r.amount) FROM FinancialRecord r " +
            "WHERE r.deleted = false GROUP BY r.category ORDER BY SUM(r.amount) DESC")
    List<Object[]> sumByCategory();

    @Query("SELECT r FROM FinancialRecord r WHERE r.deleted = false ORDER BY r.createdAt DESC")
    List<FinancialRecord> findRecentActivity(Pageable pageable);

    @Query("SELECT r FROM FinancialRecord r WHERE r.deleted = false AND r.date >= :since ORDER BY r.date ASC")
    List<FinancialRecord> findSince(@Param("since") LocalDate since);
}
