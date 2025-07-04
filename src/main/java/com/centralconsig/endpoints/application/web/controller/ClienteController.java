package com.centralconsig.endpoints.application.web.controller;


import com.centralconsig.core.application.dto.response.ClienteResponseDTO;
import com.centralconsig.core.application.service.ClienteService;
import com.centralconsig.core.domain.entity.Cliente;
import com.centralconsig.endpoints.application.service.ExportacaoClienteService;
import com.centralconsig.endpoints.application.service.util.ExportedFile;
import org.json.JSONObject;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/cliente")
public class ClienteController {

    private final ClienteService clienteService;
    private final ExportacaoClienteService exportacaoClienteService;

    public ClienteController(ClienteService clienteService, ExportacaoClienteService exportacaoClienteService) {
        this.clienteService = clienteService;
        this.exportacaoClienteService = exportacaoClienteService;
    }

    @GetMapping("/todos")
    public ResponseEntity<?> getAllClientes() {
        List<Cliente> clientes = clienteService.getAllClientes();

        if (!clientes.isEmpty())
            return ResponseEntity.ok(clienteService.getAllClientes());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    @GetMapping("/busca")
    public ResponseEntity<?> getClienteByMatriculaOrCpf(
            @RequestParam(required = false) String cpf,
            @RequestParam(required = false) String matricula) {

        try {
            ClienteResponseDTO cliente = clienteService.buscaClientePorCpfOuMatricula(cpf, matricula);

            if (cliente == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            return ResponseEntity.ok(cliente);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Erro ao realizar a busca");
        }
    }

    @PostMapping("/black-list")
    public ResponseEntity<?> atualizaBlackListCliente(@RequestParam String cpf) {
        try {
            Cliente cliente = clienteService.findByCpf(cpf);

            if (cliente == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            cliente.setBlackList(!cliente.isBlackList());
            clienteService.salvarOuAtualizarCliente(cliente);

            return ResponseEntity.ok(HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Erro ao atualizar BlackList Cliente");
        }
    }

    @PostMapping("/excel")
    public ResponseEntity<byte[]> exportarClientesParaExcel(@RequestBody String dataInicio) {
        String data = new JSONObject(dataInicio).getString("dataInicio");
        LocalDate parsedData = LocalDate.parse(data.contains(("T")) ? data.substring(0, data.indexOf("T")) : data);

        ExportedFile arquivo = exportacaoClienteService.gerarExcel(parsedData);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + arquivo.getNomeArquivo() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(arquivo.getDados());
    }

}
