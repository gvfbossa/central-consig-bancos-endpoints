package com.centralconsig.endpoints.application.web.controller;

import com.centralconsig.core.application.service.system.SystemConfigurationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system-configuration")
public class SystemConfigurationController {

    private final SystemConfigurationService systemConfigurationService;

    public SystemConfigurationController(SystemConfigurationService systemConfigurationService) {
        this.systemConfigurationService = systemConfigurationService;
    }

    @PostMapping("/proposta-automatica/toggle")
    public ResponseEntity<?> atualizaPropostaAutomatica() {
        systemConfigurationService.atualizaValorPropostaAutomatica();
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping("/proposta-automatica")
    public ResponseEntity<?> valorPropostaAutomatica() {
        return ResponseEntity.ok(systemConfigurationService.isPropostaAutomaticaAtiva());
    }

}
