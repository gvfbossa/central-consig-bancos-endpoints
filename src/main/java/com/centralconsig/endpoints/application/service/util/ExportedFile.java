package com.centralconsig.endpoints.application.service.util;

import lombok.Getter;

@Getter
public class ExportedFile {
    private final byte[] dados;
    private final String nomeArquivo;

    public ExportedFile(byte[] dados, String nomeArquivo) {
        this.dados = dados;
        this.nomeArquivo = nomeArquivo;
    }

}