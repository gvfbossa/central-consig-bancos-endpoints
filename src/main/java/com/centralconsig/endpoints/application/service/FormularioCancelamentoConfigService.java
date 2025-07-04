package com.centralconsig.endpoints.application.service;


import com.centralconsig.core.domain.entity.FormularioCancelamentoConfig;
import com.centralconsig.core.domain.repository.FormularioCancelamentoConfigRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

@Service
public class FormularioCancelamentoConfigService {

    private final FormularioCancelamentoConfigRepository repository;

    public FormularioCancelamentoConfigService(FormularioCancelamentoConfigRepository repository) {
        this.repository = repository;
    }

    @PostConstruct
    public void init() {
        if (repository.count() == 0) {
            FormularioCancelamentoConfig config = new FormularioCancelamentoConfig();
            config.setEmail("adm@suapromotora.com.br");
            config.setMotivoCancelamento("DESISTENCIA DA PROPOSTA");
            config.setPromotora("SUA PROMOTORA");
            repository.save(config);
        }
    }

    public FormularioCancelamentoConfig getConfig() {
        return repository.findAll().getFirst();
    }

    public void atualizarConfig(FormularioCancelamentoConfig configAtualizada) {
        configAtualizada.setId(getConfig().getId());
        repository.save(configAtualizada);
    }
}
