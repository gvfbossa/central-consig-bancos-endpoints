package com.centralconsig.endpoints.application.service.crawler;

import com.centralconsig.core.application.service.PropostaService;
import com.centralconsig.core.application.service.crawler.WebDriverService;
import com.centralconsig.core.domain.entity.Cliente;
import com.centralconsig.core.domain.entity.FormularioCancelamentoConfig;
import com.centralconsig.core.domain.entity.Proposta;
import com.centralconsig.endpoints.application.service.FormularioCancelamentoConfigService;
import com.centralconsig.endpoints.application.service.GoogleSheetsCancelamentoProcessamentoService;
import com.centralconsig.endpoints.application.service.PreencheFormHelper;
import org.openqa.selenium.*;
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
        Proposta proposta = propostaDb.orElseGet(() -> {
            Proposta p = new Proposta();
            p.setNumeroProposta(numero);
            Cliente cliente = new Cliente();
            cliente.setNome(nome);
            cliente.setCpf(cpf);
            p.setCliente(cliente);
            return p;
        });

        PreencheFormHelper helper = new PreencheFormHelper(driver, wait);

        helper.fillField(config.getEmail(), "Your email", "Seu e-mail");

        helper.click(By.xpath("//div[@role='radio' and @aria-label='Cancelamento de proposta']"));

        helper.fillField(proposta.getNumeroProposta(), "i19 i22");
        helper.fillField(proposta.getCliente().getNome(), "i24 i27");
        helper.fillField(proposta.getCliente().getCpf(), "i29 i32");
        helper.fillField(config.getMotivoCancelamento(), "i34 i37");
        helper.fillField(config.getPromotora(), "i39 i42");
        helper.fillField(config.getEmail(), "i44 i47");

        helper.click(By.xpath("//div[@role='radio' and @aria-label='Estou ciente.']"));

        helper.click(By.xpath("//div[@role='button']//span[contains(text(),'Enviar') or contains(text(),'Submit')]"));

        log.info("Proposta '" + numero + "' cancelada com sucesso.");
    }

}