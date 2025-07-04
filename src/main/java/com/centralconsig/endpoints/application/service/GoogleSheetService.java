package com.centralconsig.endpoints.application.service;

import com.centralconsig.core.domain.entity.GoogleSheet;
import com.centralconsig.core.domain.repository.GoogleSheetRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GoogleSheetService {

    private final GoogleSheetRepository googleSheetRepository;

    public GoogleSheetService(GoogleSheetRepository googleSheetRepository) {
        this.googleSheetRepository = googleSheetRepository;
    }

    public List<GoogleSheet> findAll() {
        return googleSheetRepository.findAll();
    }

    public GoogleSheet findByFileName(String decodedNome) {
        return googleSheetRepository.findByFileName(decodedNome);
    }

    public void delete(GoogleSheet sheet) {
        googleSheetRepository.delete(sheet);
    }

    public GoogleSheet save(GoogleSheet sheet) {
        return googleSheetRepository.save(sheet);
    }

}
