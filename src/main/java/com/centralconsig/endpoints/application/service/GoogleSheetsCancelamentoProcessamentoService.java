package com.centralconsig.endpoints.application.service;

import com.centralconsig.core.domain.repository.GoogleSheetRepository;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.BatchUpdateValuesRequest;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.ServiceAccountCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class GoogleSheetsCancelamentoProcessamentoService {

    private static final String APPLICATION_NAME = "CrawlerConsig";
    private static final String SPREADSHEET_ID = "1s9y_R5RJS3hOhoMpIQYwNXKcsdnKYCg5BeCUtyvvdfk";
    private final String SPREADSHEET_NAME = "CANCELAMENTO";

    private final Sheets sheetsService;
    private final GoogleSheetRepository googleSheetRepository;

    private static final Logger log = LoggerFactory.getLogger(GoogleSheetsCancelamentoProcessamentoService.class);

    public GoogleSheetsCancelamentoProcessamentoService(GoogleSheetRepository googleSheetRepository) throws Exception {
        this.googleSheetRepository = googleSheetRepository;

        InputStream credentialsStream = getClass().getClassLoader()
                .getResourceAsStream("google/centralconsig-crawler-sheets-54eb9933de47.json");

        assert credentialsStream != null;
        var credentials = ServiceAccountCredentials.fromStream(credentialsStream)
                .createScoped(Collections.singletonList("https://www.googleapis.com/auth/spreadsheets"));

        this.sheetsService = new Sheets.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials)
        ).setApplicationName(APPLICATION_NAME).build();
    }

    public List<List<String>> obterNumerosParaProcessar() throws IOException {
        List<String> numeros = new ArrayList<>();
        List<String> nomes = new ArrayList<>();
        List<String> cpfs = new ArrayList<>();

        String[] columns = {"A", "B", "C"};
        for (int i = 0; i < 3; i++) {
            String range = SPREADSHEET_NAME + "!" + columns[i] + "2:" + columns[i];
            ValueRange response = sheetsService.spreadsheets().values()
                    .get(SPREADSHEET_ID, range)
                    .execute();

            List<List<Object>> values = response.getValues();
            if (values == null || values.isEmpty()) {
                return Collections.emptyList();
            }

            for (List<Object> row : values) {
                if (!row.isEmpty()) {
                    if (i == 0)
                        numeros.add(row.getFirst().toString());
                    else if (i == 1)
                        nomes.add(row.getFirst().toString().isEmpty() ? "VAZIO" : row.getFirst().toString());
                    else
                        cpfs.add(row.getFirst().toString().isEmpty() ? "VAZIO" : row.getFirst().toString());
                }
            }
        }

        return List.of(numeros, nomes, cpfs);
    }

    public void marcarComoProcessado(List<Integer> linhas) throws IOException {
        List<ValueRange> updates = new ArrayList<>();

        for (Integer linha : linhas) {
            String cellCancelada = SPREADSHEET_NAME + "!D" + (linha + 2);
            String cellData = SPREADSHEET_NAME + "!E" + (linha + 3);

            ValueRange valueRangeCancelada = new ValueRange()
                    .setRange(cellCancelada)
                    .setValues(List.of(List.of("OK")));
            updates.add(valueRangeCancelada);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

            ValueRange valueRangeData = new ValueRange()
                    .setRange(cellData)
                    .setValues(List.of(List.of(LocalDateTime.now().format(formatter))));
            updates.add(valueRangeData);
        }

        BatchUpdateValuesRequest batchRequest = new BatchUpdateValuesRequest()
                .setValueInputOption("RAW")
                .setData(updates);

        sheetsService.spreadsheets().values()
                .batchUpdate(SPREADSHEET_ID, batchRequest)
                .execute();
    }

}
