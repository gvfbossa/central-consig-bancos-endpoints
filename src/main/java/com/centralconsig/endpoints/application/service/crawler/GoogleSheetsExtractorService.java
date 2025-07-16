package com.centralconsig.endpoints.application.service.crawler;

import com.centralconsig.core.application.service.crawler.WebDriverService;
import com.centralconsig.core.domain.entity.GoogleSheet;
import com.centralconsig.core.domain.repository.GoogleSheetRepository;
import com.centralconsig.endpoints.application.service.util.CsvProcessorService;
import jakarta.annotation.PostConstruct;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class GoogleSheetsExtractorService {

    private final WebDriverService webDriverService;
    private final GoogleSheetRepository sheetRepository;
    private final CsvProcessorService csvProcessorService;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    @Value("${sheet.download.dir}")
    private String DOWNLOAD_DIR;

    private static final Logger log = LoggerFactory.getLogger(GoogleSheetsExtractorService.class);

    public GoogleSheetsExtractorService(WebDriverService webDriverService, @Lazy CsvProcessorService csvProcessorService, GoogleSheetRepository sheetRepository) {
        this.webDriverService = webDriverService;
        this.csvProcessorService = csvProcessorService;
        this.sheetRepository = sheetRepository;
    }

    @PostConstruct
    public void init() {
        File directory = getFileDir();
        if (!directory.exists())
            directory.mkdirs();
    }

    @Scheduled(cron = "0 0 12,15 1,16 * *", zone = "America/Sao_Paulo")
    private void downloadSheetsScheduled() {
        startDownload();
    }

    public void startDownload() {
        if (!isRunning.compareAndSet(false, true)) {
            log.info("Download já em execução. Ignorando nova tentativa.");
            return;
        }
        try {
            if (downloadSheets())
                csvProcessorService.processCpfs();
        } finally {
            isRunning.set(false);
        }
    }

    private File getFileDir() {
        return new File(DOWNLOAD_DIR);
    }

    private boolean downloadSheets() {
        File directory = getFileDir();

        if(Objects.requireNonNull(directory.list()).length > 0) {
            for (File file : Objects.requireNonNull(directory.listFiles())) {
                if (file.isFile()) {
                    file.delete();
                }
            }
        }

        WebDriver driver = webDriverService.criarDriver();

        try {
            List<GoogleSheet> sheets = sheetRepository.findAll();
            for (GoogleSheet sheet : sheets) {
                String linkExport = gerarLinkExportacaoCsv(sheet.getUrl());
                driver.get(linkExport);
                waitForDownload(DOWNLOAD_DIR, sheet.getFileName());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            driver.quit();
        }
        return true;
    }

    public String gerarLinkExportacaoCsv(String linkOriginal) {
        Pattern padraoId = Pattern.compile("/d/([a-zA-Z0-9-_]+)");
        Matcher matcherId = padraoId.matcher(linkOriginal);

        Pattern padraoGid = Pattern.compile("gid=([0-9]+)");
        Matcher matcherGid = padraoGid.matcher(linkOriginal);

        if (matcherId.find() && matcherGid.find()) {
            String sheetId = matcherId.group(1);
            String gid = matcherGid.group(1);
            return "https://docs.google.com/spreadsheets/d/" + sheetId + "/export?format=csv&gid=" + gid;
        }

        throw new IllegalArgumentException("Link inválido: " + linkOriginal);
    }

    private void waitForDownload(String downloadPath, String fileName) {
        File dir = new File(downloadPath);
        long startTime = System.currentTimeMillis();
        long timeout = 60_000;

        while (true) {
            File[] files = dir.listFiles();

            assert files != null;
            File file = Arrays.stream(files)
                    .filter(f -> f.getName().contains(fileName))
                    .findFirst()
                    .orElse(null);

            if (System.currentTimeMillis() - startTime > timeout) {
                break;
            }
            if (file != null)
                break;
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {}
        }
        log.info("O arquivo: '" + fileName + "' foi salvo com sucesso em '" + downloadPath + "'");
    }

}
