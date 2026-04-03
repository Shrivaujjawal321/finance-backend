package com.finance.service;

import com.finance.dto.response.DashboardSummary;
import com.finance.dto.response.FinancialRecordResponse;
import com.finance.entity.FinancialRecord;
import com.finance.enums.RecordType;
import com.finance.repository.FinancialRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final FinancialRecordRepository recordRepository;

    @Cacheable(value = "dashboardSummary")
    public DashboardSummary getSummary() {
        BigDecimal totalIncome = recordRepository.sumByType(RecordType.INCOME);
        BigDecimal totalExpenses = recordRepository.sumByType(RecordType.EXPENSE);
        BigDecimal netBalance = totalIncome.subtract(totalExpenses);

        Map<String, BigDecimal> categoryTotals = new LinkedHashMap<>();
        for (Object[] row : recordRepository.sumByCategory()) {
            categoryTotals.put((String) row[0], (BigDecimal) row[1]);
        }

        List<FinancialRecordResponse> recentActivity = recordRepository
                .findRecentActivity(PageRequest.of(0, 10))
                .stream()
                .map(FinancialRecordResponse::from)
                .toList();

        List<DashboardSummary.MonthlyTrend> monthlyTrends = buildMonthlyTrends();

        return DashboardSummary.builder()
                .totalIncome(totalIncome)
                .totalExpenses(totalExpenses)
                .netBalance(netBalance)
                .categoryWiseTotals(categoryTotals)
                .recentActivity(recentActivity)
                .monthlyTrends(monthlyTrends)
                .build();
    }

    private List<DashboardSummary.MonthlyTrend> buildMonthlyTrends() {
        LocalDate sixMonthsAgo = LocalDate.now().minusMonths(6).withDayOfMonth(1);
        List<FinancialRecord> records = recordRepository.findSince(sixMonthsAgo);

        Map<String, Map<RecordType, BigDecimal>> grouped = records.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getDate().format(MONTH_FMT),
                        TreeMap::new,
                        Collectors.groupingBy(
                                FinancialRecord::getType,
                                Collectors.reducing(BigDecimal.ZERO, FinancialRecord::getAmount, BigDecimal::add)
                        )
                ));

        return grouped.entrySet().stream().map(entry -> {
            BigDecimal income = entry.getValue().getOrDefault(RecordType.INCOME, BigDecimal.ZERO);
            BigDecimal expenses = entry.getValue().getOrDefault(RecordType.EXPENSE, BigDecimal.ZERO);
            return DashboardSummary.MonthlyTrend.builder()
                    .month(entry.getKey())
                    .income(income)
                    .expenses(expenses)
                    .net(income.subtract(expenses))
                    .build();
        }).toList();
    }
}
