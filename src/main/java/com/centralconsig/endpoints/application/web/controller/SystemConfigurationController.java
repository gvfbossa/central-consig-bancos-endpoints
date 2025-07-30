package com.centralconsig.endpoints.application.web.controller;

import com.centralconsig.core.application.dto.request.SystemConfigurationRequestDTO;
import com.centralconsig.core.application.service.system.SystemConfigurationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/system-configuration")
public class SystemConfigurationController {

    private final SystemConfigurationService systemConfigurationService;

    public SystemConfigurationController(SystemConfigurationService systemConfigurationService) {
        this.systemConfigurationService = systemConfigurationService;
    }

    @GetMapping("/proposta-automatica")
    public ResponseEntity<?> valorPropostaAutomatica() {
        return ResponseEntity.ok(systemConfigurationService.getSystemConfigurations());
    }

    @PostMapping("/proposta-automatica/toggle")
    public ResponseEntity<?> atualizaPropostaAutomatica(@RequestBody SystemConfigurationRequestDTO systemConfigurationRequestDTO) {
        systemConfigurationService.atualizaValorPropostaAutomatica(systemConfigurationRequestDTO);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

}
