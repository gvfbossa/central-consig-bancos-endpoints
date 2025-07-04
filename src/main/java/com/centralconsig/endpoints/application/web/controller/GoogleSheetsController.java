package com.centralconsig.endpoints.application.web.controller;

import com.centralconsig.core.domain.entity.GoogleSheet;
import com.centralconsig.core.domain.repository.GoogleSheetRepository;
import com.centralconsig.endpoints.application.service.GoogleSheetsExtractorService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/sheets")
public class GoogleSheetsController {

    private final GoogleSheetsExtractorService sheetExtractorService;
    private final GoogleSheetRepository sheetRepository;

    public GoogleSheetsController(GoogleSheetsExtractorService sheetExtractorService, GoogleSheetRepository sheetRepository) {
        this.sheetExtractorService = sheetExtractorService;
        this.sheetRepository = sheetRepository;
    }

    @PostMapping("/download")
    public ResponseEntity<?> downloadSheet() {
        try {
            sheetExtractorService.startDownload();
            return ResponseEntity.ok("Download efetuado com sucesso!");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping
    public ResponseEntity<?> getSheets() {
        return ResponseEntity.ok(sheetRepository.findAll());
    }

    @DeleteMapping("/{nome}")
    public ResponseEntity<Void> removeSheets(@PathVariable String nome) {
        try {
            System.out.println("Nome recebido: " + nome);
            String decodedNome = URLDecoder.decode(nome, StandardCharsets.UTF_8);
            GoogleSheet sheet = sheetRepository.findByFileName(decodedNome);
            if (sheet != null) {
                sheetRepository.delete(sheet);
                return ResponseEntity.noContent().build();
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }



    @PostMapping("/nome")
    public ResponseEntity<?> saveSheets(@RequestBody GoogleSheet sheet) {
        return ResponseEntity.ok(sheetRepository.save(sheet));
    }

}
