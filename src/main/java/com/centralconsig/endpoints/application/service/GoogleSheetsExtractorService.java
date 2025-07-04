package com.centralconsig.endpoints.application.service;

import com.centralconsig.core.application.service.crawler.WebDriverService;
import com.centralconsig.core.domain.entity.GoogleSheet;
import com.centralconsig.core.domain.repository.GoogleSheetRepository;
import com.centralconsig.endpoints.application.service.util.CsvProcessorService;
import jakarta.annotation.PostConstruct;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

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
        if (sheetRepository.count() == 0) {
            List<GoogleSheet> sheets = List.of(
                    new GoogleSheet("MARGEM CARTAO CAPITAL - CASA 04-25.csv",
                            "https://docs.google.com/spreadsheets/d/1s9y_R5RJS3hOhoMpIQYwNXKcsdnKYCg5BeCUtyvvdfk/edit?gid=1748821058#gid=1748821058"),
                    new GoogleSheet("MARGEM CARTAO CAPITAL - QUERO MAIS ALINE COMPLETA.csv",
                            "https://docs.google.com/spreadsheets/d/1s9y_R5RJS3hOhoMpIQYwNXKcsdnKYCg5BeCUtyvvdfk/edit?gid=1011816552"),
                    new GoogleSheet("MARGEM CARTAO CAPITAL - MAILING ALINE COMPLETA.csv",
                            "https://docs.google.com/spreadsheets/d/1s9y_R5RJS3hOhoMpIQYwNXKcsdnKYCg5BeCUtyvvdfk/edit?gid=2000302543"));
            sheetRepository.saveAll(sheets);
        }
        File directory = getFileDir();
        if (!directory.exists())
            directory.mkdirs();
        if (Objects.requireNonNull(directory.listFiles()).length == 0)
            startDownload();
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
        WebDriverWait wait = webDriverService.criarWait(driver);

        try {
            List<GoogleSheet> sheets = sheetRepository.findAll();
            for (GoogleSheet sheet : sheets) {
                driver.get(sheet.getUrl());

                wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[text()='Arquivo']")));

                downloadSheet(driver, wait, DOWNLOAD_DIR, sheet.getFileName());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            driver.quit();
        }
        return true;
    }

    private void downloadSheet(WebDriver driver, WebDriverWait wait, String downloadPath, String fileName) {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        WebElement fileMenu = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//div[text()='Arquivo']")));
        fileMenu.click();

        Actions actions = new Actions(driver);
        actions.sendKeys(Keys.DOWN)
                .pause(Duration.ofMillis(500))
                .sendKeys(Keys.RIGHT)
                .pause(Duration.ofMillis(500))
                .sendKeys(Keys.UP)
                .pause(Duration.ofMillis(500))
                .sendKeys(Keys.UP)
                .pause(Duration.ofMillis(500))
                .sendKeys(Keys.ENTER)
                .perform();

        waitForDownload(downloadPath, fileName);
    }

    private void waitForDownload(String downloadPath, String fileName) {
        File dir = new File(downloadPath);
        long startTime = System.currentTimeMillis();
        long timeout = 60_000;

        while (true) {
            File[] files = dir.listFiles();

            assert files != null;
            File file = Arrays.stream(files)
                    .filter(f -> f.getName().equals(fileName))
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
    }

}
