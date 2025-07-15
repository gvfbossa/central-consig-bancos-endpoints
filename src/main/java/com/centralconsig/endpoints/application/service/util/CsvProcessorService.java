package com.centralconsig.endpoints.application.service.util;

import com.centralconsig.core.application.service.ClienteService;
import com.centralconsig.core.domain.entity.Cliente;
import com.centralconsig.core.domain.entity.GoogleSheet;
import com.centralconsig.core.domain.entity.Vinculo;
import com.centralconsig.endpoints.application.service.GoogleSheetService;
import com.opencsv.CSVReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileReader;
import java.util.*;

@Service
public class CsvProcessorService {

    private final ClienteService clienteService;
    private final GoogleSheetService googleSheetService;

    @Value("${sheet.download.dir}")
    private String CSV_DIR;

    private static final Logger log = LoggerFactory.getLogger(CsvProcessorService.class);

    public CsvProcessorService(ClienteService clienteService, GoogleSheetService googleSheetService) {
        this.clienteService = clienteService;
        this.googleSheetService = googleSheetService;
    }

    public void processCpfs() {
        File dir = new File(CSV_DIR);
        File[] csvFiles = dir.listFiles((d, name) -> name.endsWith(".csv"));

        if (csvFiles == null || csvFiles.length == 0) {
            log.error("Nenhum arquivo CSV encontrado.");
            return;
        }

        Arrays.sort(csvFiles, (f1, f2) -> {
            boolean f1IsCasa = f1.getName().toUpperCase().contains("CASA");
            boolean f2IsCasa = f2.getName().toUpperCase().contains("CASA");

            if (f1IsCasa && !f2IsCasa) return -1;
            if (!f1IsCasa && f2IsCasa) return 1;
            return 0;
        });

        Map<String, Cliente> clientesMap = new HashMap<>();

        List<GoogleSheet> allSheets = googleSheetService.findAll();

        for (File csvFile : csvFiles) {
            try (CSVReader reader = new CSVReader(new FileReader(csvFile))) {
                String[] header = reader.readNext();
                if (header == null) {
                    log.error("Header não encontrado para o arquivo '" + csvFile.getName() + "'");
                    continue;
                }
                log.info("Processando arquivo: '" + csvFile.getName() + "'");

                GoogleSheet sheet = findSheetByFileName(csvFile, allSheets)
                        .orElseThrow(() -> new IllegalStateException("Nenhuma GoogleSheet encontrada para o arquivo: " + csvFile.getName()));

                int cpfIndex = -1;
                int matriculaIndex = -1;

                for (int i = 0; i < header.length; i++) {
                    if (header[i].equalsIgnoreCase("CPF")) cpfIndex = i;
                    if (header[i].equalsIgnoreCase("Matricula")) matriculaIndex = i;
                }

                if (cpfIndex == -1 || matriculaIndex == -1) {
                    continue;
                }

                String[] row;

                while ((row = reader.readNext()) != null) {
                    if (cpfIndex < row.length && matriculaIndex < row.length) {
                        String cpf = row[cpfIndex];
                        String matricula = row[matriculaIndex];

                        if (cpf != null && matricula != null &&
                                !cpf.trim().isEmpty() && !matricula.trim().isEmpty()) {

                            cpf = cpf.trim();
                            matricula = clienteService.removeZerosAEsquerdaMatricula(matricula);

                            if (cpf.length() == 10)
                                cpf = "0" + cpf;

                            String finalCpf = cpf;
                            Cliente cliente = clientesMap.computeIfAbsent(cpf, k -> {
                                Cliente c = new Cliente();
                                c.setCpf(finalCpf);
                                c.setCasa(csvFile.getName().toUpperCase().contains("CASA"));
                                c.setGoogleSheet(sheet);
                                return c;
                            });

                            Vinculo vinculo = new Vinculo();
                            vinculo.setMatriculaPensionista(matricula);
                            vinculo.setCliente(cliente);
                            cliente.getVinculos().add(vinculo);
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Erro ao processar arquivo " + csvFile.getName() + ": " + e.getMessage());
            }
        }
        clienteService.salvarOuAtualizarEmLote(new ArrayList<>(clientesMap.values()));
        log.info("Clientes inseridos com sucesso através dos arquivos csv");
    }

    private Optional<GoogleSheet> findSheetByFileName(File csvFile, List<GoogleSheet> sheets) {
        return sheets.stream().filter(sheet -> csvFile.getName().toUpperCase().contains(sheet.getFileName().toUpperCase())).findFirst();
    }

}
