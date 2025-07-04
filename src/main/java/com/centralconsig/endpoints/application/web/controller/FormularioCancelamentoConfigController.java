package com.centralconsig.endpoints.application.web.controller;

import com.centralconsig.core.domain.entity.FormularioCancelamentoConfig;
import com.centralconsig.endpoints.application.service.FormularioCancelamentoConfigService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/form-cancelamento/config")
public class FormularioCancelamentoConfigController {

    private final FormularioCancelamentoConfigService service;

    public FormularioCancelamentoConfigController(FormularioCancelamentoConfigService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<?> getConfig() {
        FormularioCancelamentoConfig config = service.getConfig();
        return ResponseEntity.ok(config);
    }

    @PutMapping
    public ResponseEntity<?> atualizarConfig(@RequestBody FormularioCancelamentoConfig config) {
        service.atualizarConfig(config);
        return ResponseEntity.ok(HttpStatus.NO_CONTENT);
    }

}
