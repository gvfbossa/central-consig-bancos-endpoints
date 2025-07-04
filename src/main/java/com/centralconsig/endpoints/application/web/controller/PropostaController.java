package com.centralconsig.endpoints.application.web.controller;

import com.centralconsig.core.application.dto.response.PropostaResponseDTO;
import com.centralconsig.core.application.mapper.PropostaMapper;
import com.centralconsig.core.application.service.PropostaService;
import com.centralconsig.core.domain.entity.Proposta;
import com.centralconsig.endpoints.application.service.ExportacaoPropostaService;
import com.centralconsig.endpoints.application.service.crawler.FormularioCancelamentoPropostaService;
import com.centralconsig.endpoints.application.service.util.ExportedFile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/proposta")
public class PropostaController {

    private final PropostaService propostaService;
    private final FormularioCancelamentoPropostaService cancelamentoPropostaService;
    private final ExportacaoPropostaService exportacaoPropostaService;

    public PropostaController(PropostaService propostaService, FormularioCancelamentoPropostaService cancelamentoPropostaService,
                              ExportacaoPropostaService exportacaoPropostaService) {
        this.propostaService = propostaService;
        this.cancelamentoPropostaService = cancelamentoPropostaService;
        this.exportacaoPropostaService = exportacaoPropostaService;
    }

    @GetMapping
    public ResponseEntity<?> getAllPropostas() {
        List<Proposta> propostasPage = propostaService.getTodasAsPropostasNaoProcessadas();

        List<PropostaResponseDTO> propostas = propostasPage.stream()
            .map(PropostaMapper::toDto)
            .toList();

        return ResponseEntity.ok(propostas);
    }

    @GetMapping("/numero")
    public ResponseEntity<Proposta> getPropostaByNumero(@RequestParam String numero) {
        return propostaService.retornaPropostaPorNumero(numero)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @DeleteMapping("/cancelamento")
    public ResponseEntity<?> cancelaPropostasByNumero(@RequestBody List<String> numeros) {
        if (cancelamentoPropostaService.cancelaPropostas(numeros))
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    @PostMapping("/{numeroProposta}/processa")
    public ResponseEntity<?> marcarComoProcessada(@PathVariable String numeroProposta) {
        propostaService.processaProposta(numeroProposta);
        return ResponseEntity.ok().build();
    }


    @PostMapping("/excel")
    public ResponseEntity<byte[]> exportarPropostasParaExcel(@RequestBody List<String> numerosPropostas) {
        List<Proposta> propostas = propostaService.todasAsPropostas().stream()
                    .filter(proposta -> numerosPropostas.contains(proposta.getNumeroProposta()))
                    .toList();

        ExportedFile arquivo = exportacaoPropostaService.gerarExcel(propostas);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + arquivo.getNomeArquivo() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(arquivo.getDados());
    }



}
