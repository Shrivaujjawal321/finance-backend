package com.finance.service;

import com.finance.dto.request.FinancialRecordRequest;
import com.finance.dto.response.FinancialRecordResponse;
import com.finance.entity.FinancialRecord;
import com.finance.entity.User;
import com.finance.enums.RecordType;
import com.finance.exception.ResourceNotFoundException;
import com.finance.repository.FinancialRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class FinancialRecordService {

    private final FinancialRecordRepository recordRepository;

    public Page<FinancialRecordResponse> getRecords(RecordType type, String category,
                                                     LocalDate startDate, LocalDate endDate,
                                                     Pageable pageable) {
        return recordRepository.findWithFilters(type, category, startDate, endDate, pageable)
                .map(FinancialRecordResponse::from);
    }

    public FinancialRecordResponse getRecordById(Long id) {
        return FinancialRecordResponse.from(findActiveRecord(id));
    }

    @Transactional
    public FinancialRecordResponse createRecord(FinancialRecordRequest request, User currentUser) {
        FinancialRecord record = FinancialRecord.builder()
                .amount(request.getAmount())
                .type(request.getType())
                .category(request.getCategory())
                .date(request.getDate())
                .description(request.getDescription())
                .createdBy(currentUser)
                .deleted(false)
                .build();

        return FinancialRecordResponse.from(recordRepository.save(record));
    }

    @Transactional
    public FinancialRecordResponse updateRecord(Long id, FinancialRecordRequest request) {
        FinancialRecord record = findActiveRecord(id);

        record.setAmount(request.getAmount());
        record.setType(request.getType());
        record.setCategory(request.getCategory());
        record.setDate(request.getDate());
        record.setDescription(request.getDescription());

        return FinancialRecordResponse.from(recordRepository.save(record));
    }

    @Transactional
    public void deleteRecord(Long id) {
        FinancialRecord record = findActiveRecord(id);
        record.setDeleted(true);
        recordRepository.save(record);
    }

    private FinancialRecord findActiveRecord(Long id) {
        return recordRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Financial record not found with id: " + id));
    }
}
