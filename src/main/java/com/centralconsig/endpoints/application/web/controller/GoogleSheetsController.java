package com.centralconsig.endpoints.application.web.controller;

import com.centralconsig.core.domain.entity.GoogleSheet;
import com.centralconsig.endpoints.application.service.crawler.GoogleSheetsExtractorService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/sheets")
public class GoogleSheetsController {

    private final GoogleSheetsExtractorService sheetExtractorService;
    private final GoogleSheetService googleSheetService;

    public GoogleSheetsController(GoogleSheetsExtractorService sheetExtractorService, GoogleSheetService googleSheetService) {
        this.sheetExtractorService = sheetExtractorService;
        this.googleSheetService = googleSheetService;
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
        return ResponseEntity.ok(googleSheetService.findAll());
    }

    @DeleteMapping("/{nome}")
    public ResponseEntity<Void> removeSheets(@PathVariable String nome) {
        try {
            String decodedNome = URLDecoder.decode(nome, StandardCharsets.UTF_8);
            GoogleSheet sheet = googleSheetService.findByFileName(decodedNome);
            if (sheet != null) {
                googleSheetService.delete(sheet);
                return ResponseEntity.noContent().build();
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PostMapping("/nome")
    public ResponseEntity<?> saveSheets(@RequestBody GoogleSheet sheet) {
        return ResponseEntity.ok(googleSheetService.save(sheet));
    }

}
