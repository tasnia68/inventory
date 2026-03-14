package com.inventory.system.service;

import com.inventory.system.payload.DataExchangeDataset;
import com.inventory.system.payload.DataImportHistoryDto;
import com.inventory.system.payload.DataImportValidationResultDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface DataExchangeService {
    DataImportValidationResultDto validateImport(DataExchangeDataset dataset, MultipartFile file);

    DataImportHistoryDto startImport(DataExchangeDataset dataset, MultipartFile file);

    List<DataImportHistoryDto> getImportHistory();

    DataImportHistoryDto getImportHistory(UUID id);
}