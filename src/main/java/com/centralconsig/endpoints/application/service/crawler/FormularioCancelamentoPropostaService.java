package com.centralconsig.endpoints.application.service.crawler;

import com.centralconsig.core.application.service.PropostaService;
import com.centralconsig.core.application.service.crawler.WebDriverService;
import com.centralconsig.core.domain.entity.Cliente;
import com.centralconsig.core.domain.entity.FormularioCancelamentoConfig;
import com.centralconsig.core.domain.entity.Proposta;
import com.centralconsig.endpoints.application.service.FormularioCancelamentoConfigService;
import com.centralconsig.endpoints.application.service.GoogleSheetsCancelamentoProcessamentoService;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class FormularioCancelamentoPropostaService {

    private final WebDriverService webDriverService;
    private WebDriverWait wait;
    private WebDriver driver;

    private final PropostaService propostaService;
    private final FormularioCancelamentoConfigService configService;
    private final GoogleSheetsCancelamentoProcessamentoService googleSheetsCancelamentoProcessamentoService;

    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    @Value("${form.cancelamento.url}")
    private String URL_FORM;

    private static final Logger log = LoggerFactory.getLogger(FormularioCancelamentoPropostaService.class);

    public FormularioCancelamentoPropostaService(WebDriverService webDriverService, PropostaService propostaService,
                                                 FormularioCancelamentoConfigService configService, GoogleSheetsCancelamentoProcessamentoService googleSheetsCancelamentoProcessamentoService) {
        this.webDriverService = webDriverService;
        this.propostaService = propostaService;
        this.configService = configService;
        this.googleSheetsCancelamentoProcessamentoService = googleSheetsCancelamentoProcessamentoService;
    }

    public boolean cancelaPropostas(List<String> numeros) {
        if (!isRunning.compareAndSet(false, true)) {
            log.info("Cancelamento de Propostas já em execução. Ignorando nova tentativa.");
            return false;
        }

        try {
            List<List<String>> dadosCancelamentosPlanilha = googleSheetsCancelamentoProcessamentoService.obterNumerosParaProcessar();
            List<String> numerosCancelamentosPlanilha = dadosCancelamentosPlanilha.getFirst();
            List<String> nomesCancelamentosPlanilha = dadosCancelamentosPlanilha.get(1);
            List<String> cpfsCancelamentosPlanilha = dadosCancelamentosPlanilha.get(2);

            List<String> numerosParaCancelar = numeros.stream()
                    .filter(numero -> !numerosCancelamentosPlanilha.contains(numero))
                    .toList();

            if (numerosCancelamentosPlanilha.isEmpty() && numerosParaCancelar.isEmpty())
                return true;

            this.driver = webDriverService.criarDriver();
            this.wait = webDriverService.criarWait(driver);

            for (int i = 0; i < numerosCancelamentosPlanilha.size(); i++) {
                String numero = numerosCancelamentosPlanilha.get(i);
                acessaForm();
                preencheInformacoesForm(numero, nomesCancelamentosPlanilha.get(i), cpfsCancelamentosPlanilha.get(i));
                propostaService.removerProposta(numero);
            }
            List<Integer> linhasProcessadas = new ArrayList<>();
            for (String numero : numerosCancelamentosPlanilha) {
                int index = numerosCancelamentosPlanilha.indexOf(numero);
                linhasProcessadas.add(index);
            }
            googleSheetsCancelamentoProcessamentoService.marcarComoProcessado(linhasProcessadas);
            for (String numero : numerosParaCancelar) {
                acessaForm();
                preencheInformacoesForm(numero, "", "");
                propostaService.removerProposta(numero);
            }
        } catch (Exception e) {
            log.error("Erro crítico ao cancelar propostas . Erro: " + e.getMessage());
        }  finally {
            isRunning.set(false);
            webDriverService.fecharDriver(driver);
        }
        return true;
    }

    private void acessaForm() {
        driver.get(URL_FORM);
        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//*[contains(text(),'Solicitação de cancelamento de propostas')]")));
    }

    private void preencheInformacoesForm(String numero, String nome, String cpf) {
        FormularioCancelamentoConfig config = configService.getConfig();

        Optional<Proposta> propostaDb = propostaService.retornaPropostaPorNumero(numero);
        Proposta proposta;

        if (propostaDb.isPresent())
            proposta = propostaDb.get();
        else {
            proposta = new Proposta();
            proposta.setNumeroProposta(numero);
            Cliente cliente = new Cliente();
            cliente.setNome(nome);
            cliente.setCpf(cpf);
            proposta.setCliente(cliente);
        }

        wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//input[contains(@aria-label,'Seu e-mail') or contains(@aria-label,'Your email')]")
        )).sendKeys(config.getEmail());

        WebElement radioCancelamento = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//div[@role='radio' and @aria-label='Cancelamento de proposta']")));
        Actions actions = new Actions(driver);
        actions.moveToElement(radioCancelamento).click().perform();


        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//input[@aria-labelledby='i19 i22']"))).sendKeys(proposta.getNumeroProposta());

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//input[@aria-labelledby='i24 i27']")))
                .sendKeys(proposta.getCliente().getNome());

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//input[@aria-labelledby='i29 i32']")))
                .sendKeys(proposta.getCliente().getCpf());

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//input[@aria-labelledby='i34 i37']")))
                .sendKeys(config.getMotivoCancelamento());

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//input[@aria-labelledby='i39 i42']")))
                .sendKeys(config.getPromotora());

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//input[@aria-labelledby='i44 i47']")))
                .sendKeys(config.getEmail());

        WebElement radioEstouCiente = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//div[@role='radio' and @aria-label='Estou ciente.']")));
        actions.moveToElement(radioEstouCiente).click().perform();

        WebElement botaoEnviar = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//div[@role='button']//span[contains(text(),'Enviar') or contains(text(),'Submit')]")));
        actions.moveToElement(botaoEnviar).click().perform();
    }

}