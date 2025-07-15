package com.centralconsig.endpoints.application.service;

import com.centralconsig.core.application.service.ClienteService;
import com.centralconsig.core.domain.entity.Cliente;
import com.centralconsig.core.domain.entity.GoogleSheet;
import com.centralconsig.core.domain.entity.HistoricoConsulta;
import com.centralconsig.core.domain.entity.Vinculo;
import com.centralconsig.endpoints.application.service.util.ExportedFile;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
public class ExportacaoClienteService {

    private final ClienteService clienteService;

    public ExportacaoClienteService(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    public ExportedFile gerarExcel(LocalDate parsedData) {
        List<Cliente> clientes = clienteService.getRelatorioMargensPreenchidasData(parsedData);

        try (Workbook workbook = new XSSFWorkbook()) {
            String dataFormatada = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));

            Sheet sheet = workbook.createSheet(dataFormatada);

            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            Row header = sheet.createRow(0);
            String[] colunas = {
                    "Nome", "CPF", "Matricula", "Data Consulta",
                    "Situacao Beneficio", "Margem Beneficio", "Situacao Credito", "Margem Credito", "Aba Planilha"
            };

            for (int i = 0; i < colunas.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(colunas[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            for (Cliente cliente : clientes) {
                Optional<Vinculo> optVinc = cliente.getVinculos().stream().filter(vinculo -> vinculo.getOrgao() != null).findFirst();

                if (optVinc.isEmpty())
                    continue;

                Vinculo vinc = optVinc.get();

                Optional<HistoricoConsulta> optH = vinc.getHistoricos().stream().filter(historicoConsulta -> historicoConsulta.getDataConsulta().equals(LocalDate.now())).findFirst();
                
                if (optH.isEmpty())
                    continue;
                
                HistoricoConsulta h = optH.get();

                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(cliente.getNome());
                row.createCell(1).setCellValue(cliente.getCpf());
                row.createCell(2).setCellValue(cliente.getVinculos().getFirst().getMatriculaPensionista());
                row.createCell(3).setCellValue(h.getDataConsulta().toString());
                row.createCell(4).setCellValue(h.getSituacaoBeneficio());
                row.createCell(5).setCellValue(h.getMargemBeneficio().isEmpty() ?
                        "0,00" : h.getMargemBeneficio());
                row.createCell(6).setCellValue(h.getSituacaoCredito());
                row.createCell(7).setCellValue(h.getMargemCredito().isEmpty() ?
                        "0,00" : h.getMargemCredito());
                row.createCell(8).setCellValue(cliente.getGoogleSheet() == null ?
                        "" : cliente.getGoogleSheet().getFileName());
            }

            for (int i = 0; i < colunas.length+1; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);

            String nomeArquivo = "Relatorio_Clientes-Margens_" + dataFormatada + ".xlsx";

            return new ExportedFile(out.toByteArray(), nomeArquivo);

        } catch (IOException e) {
            throw new RuntimeException("Erro ao gerar Excel", e);
        }
    }

}
