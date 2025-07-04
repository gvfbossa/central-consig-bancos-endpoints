package com.centralconsig.endpoints.application.web.controller;

import com.centralconsig.core.application.dto.request.UsuarioLoginQueroMaisCreditoRequestDTO;
import com.centralconsig.core.application.dto.response.UsuarioLoginQueroMaisCreditoResponseDTO;
import com.centralconsig.core.application.mapper.UsuarioMapper;
import com.centralconsig.core.application.service.crawler.UsuarioLoginQueroMaisCreditoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/usuario")
public class UsuarioController {

    private final UsuarioLoginQueroMaisCreditoService usuarioLoginQueroMaisCreditoService;

    public UsuarioController(UsuarioLoginQueroMaisCreditoService usuarioLoginQueroMaisCreditoService) {
        this.usuarioLoginQueroMaisCreditoService = usuarioLoginQueroMaisCreditoService;
    }

    @GetMapping
    public List<UsuarioLoginQueroMaisCreditoResponseDTO> getAllUsuarios() {
        return usuarioLoginQueroMaisCreditoService
                .retornaUsuariosParaCrawler()
                .stream()
                .map(UsuarioMapper::toDto)
                .toList();
    }

    @PostMapping
    public ResponseEntity<?> insereUsuario(@RequestBody UsuarioLoginQueroMaisCreditoRequestDTO usuarioDTO) {
        if (usuarioLoginQueroMaisCreditoService.insereUsuario(usuarioDTO))
            return ResponseEntity.noContent().build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    @PutMapping
    public ResponseEntity<?> alteraUsuario(@RequestBody UsuarioLoginQueroMaisCreditoRequestDTO usuarioDTO) {
        if (usuarioLoginQueroMaisCreditoService.atualizaUsuario(usuarioDTO))
            return ResponseEntity.noContent().build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    @DeleteMapping("/{username}")
    public ResponseEntity<?> removeUsuario(@PathVariable String username) {
        if (usuarioLoginQueroMaisCreditoService.removeUsuario(username))
            return ResponseEntity.noContent().build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

}
