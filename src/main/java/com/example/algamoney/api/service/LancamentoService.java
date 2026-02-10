package com.example.algamoney.api.service;

import com.example.algamoney.api.dto.LancamentoEstatisticaPessoa;
import com.example.algamoney.api.mail.Mailer;
import com.example.algamoney.api.model.Lancamento;
import com.example.algamoney.api.model.Pessoa;
import com.example.algamoney.api.model.Usuario;
import com.example.algamoney.api.repository.LancamentoRepository;
import com.example.algamoney.api.repository.PessoaRepository;
import com.example.algamoney.api.repository.UsuarioRepository;
import com.example.algamoney.api.service.exception.PessoaInexistenteOuInativaException;
import com.example.algamoney.api.storage.S3;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.sql.Date;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class LancamentoService {

    private static final String DESTINATARIOS = "ROLE_PESQUISAR_LANCAMENTO";
    private static final Logger log = LoggerFactory.getLogger(LancamentoService.class);

    @Autowired
    private PessoaRepository pessoaRepository;

    @Autowired
    private LancamentoRepository lancamentoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private Mailer mailer;

    @Autowired
    private S3 s3;

    @Scheduled(cron = "0 0 6 * * *")
    public void avisarSobreLancamentosVencidos() {
        log.info("Iniciando tarefa agendada: Avisar sobre lançamentos vencidos.");
        if (log.isDebugEnabled()) {
            log.debug("Preparando envio de e-mails de aviso de lançamentos vencidos.");
        }

        List<Lancamento> vencidos = lancamentoRepository
                .findByDataVencimentoLessThanEqualAndDataPagamentoIsNull(LocalDate.now());

        if (vencidos.isEmpty()) {
            log.info("Sem lançamentos vencidos para aviso.");
            return;
        }

        log.info("Existem {} lançamentos vencidos.", vencidos.size());

        List<Usuario> destinatarios = usuarioRepository
                .findByPermissoesDescricao(DESTINATARIOS);

        if (destinatarios.isEmpty()) {
            log.warn("Existem lançamentos vencidos, mas o sistema não encontrou destinatários.");
            return;
        }

        mailer.avisarSobreLancamentosVencidos(vencidos, destinatarios);

        log.info("Envio de e-mail de aviso concluído.");
    }

    public byte[] relatorioPorPessoa(LocalDate inicio, LocalDate fim) throws Exception {
        log.info("Gerando relatório de lançamentos por pessoa para o período de {} a {}", inicio, fim);
        List<LancamentoEstatisticaPessoa> dados = lancamentoRepository.porPessoa(inicio, fim);
        log.debug("Dados para o relatório coletados: {} registros.", dados.size());

        Map<String, Object> parametros = new HashMap<>();
        parametros.put("DT_INICIO", Date.valueOf(inicio));
        parametros.put("DT_FIM", Date.valueOf(fim));
        parametros.put("REPORT_LOCALE", new Locale("pt", "BR"));

        InputStream inputStream = this.getClass().getResourceAsStream(
                "/relatorios/lancamentos-por-pessoa.jasper");

        JasperPrint jasperPrint = JasperFillManager.fillReport(inputStream, parametros,
                new JRBeanCollectionDataSource(dados));

        byte[] relatorio = JasperExportManager.exportReportToPdf(jasperPrint);
        log.info("Relatório gerado com sucesso. Tamanho: {} bytes.", relatorio.length);
        return relatorio;
    }

    public Lancamento salvar(Lancamento lancamento) {
        log.info("Iniciando processo de salvar novo lançamento: {}", lancamento.getDescricao());
        validarPessoa(lancamento);

        if (StringUtils.hasText(lancamento.getAnexo())) {
            log.debug("Salvando anexo {} no S3.", lancamento.getAnexo());
            s3.salvar(lancamento.getAnexo());
        }

        Lancamento lancamentoSalvo = lancamentoRepository.save(lancamento);
        log.info("Lançamento salvo com sucesso. Código: {}", lancamentoSalvo.getCodigo());
        return lancamentoSalvo;
    }

    public Lancamento atualizar(Long codigo, Lancamento lancamento) {
        log.info("Iniciando processo de atualização para lançamento com código: {}", codigo);
        Lancamento lancamentoSalvo = buscarLancamentoExistente(codigo);
        if (!lancamento.getPessoa().equals(lancamentoSalvo.getPessoa())) {
            log.debug("Pessoa do lançamento foi alterada. Validando nova pessoa.");
            validarPessoa(lancamento);
        }

        if (StringUtils.isEmpty(lancamento.getAnexo())
                && StringUtils.hasText(lancamentoSalvo.getAnexo())) {
            log.debug("Removendo anexo antigo: {}", lancamentoSalvo.getAnexo());
            s3.remover(lancamentoSalvo.getAnexo());
        } else if (StringUtils.hasLength(lancamento.getAnexo())
                && !lancamento.getAnexo().equals(lancamentoSalvo.getAnexo())) {
            log.debug("Substituindo anexo antigo {} pelo novo {}.", lancamentoSalvo.getAnexo(), lancamento.getAnexo());
            s3.substituir(lancamentoSalvo.getAnexo(), lancamento.getAnexo());
        }

        BeanUtils.copyProperties(lancamento, lancamentoSalvo, "codigo");
        log.debug("Propriedades do lançamento (exceto código) copiadas.");

        Lancamento lancamentoAtualizado = lancamentoRepository.save(lancamentoSalvo);
        log.info("Lançamento com código {} atualizado com sucesso.", codigo);
        return lancamentoAtualizado;
    }

    private void validarPessoa(Lancamento lancamento) {
        log.debug("Validando pessoa do lançamento. Código pessoa: {}", lancamento.getPessoa().getCodigo());
        Optional<Pessoa> pessoa = Optional.empty();
        if (lancamento.getPessoa().getCodigo() != null) {
            pessoa = pessoaRepository.findById(lancamento.getPessoa().getCodigo());
        }

        if (pessoa.isEmpty() || pessoa.get().isInativo()) {
            log.error("Pessoa com código {} é inexistente ou inativa.", lancamento.getPessoa().getCodigo());
            throw new PessoaInexistenteOuInativaException();
        }
        log.debug("Pessoa validada com sucesso.");
    }

    private Lancamento buscarLancamentoExistente(Long codigo) {
        log.debug("Buscando lançamento existente com código: {}", codigo);
        return lancamentoRepository.findById(codigo).orElseThrow(() -> {
            log.error("Lançamento com código {} não encontrado.", codigo);
            return new IllegalArgumentException("Lançamento não encontrado");
        });
    }

}
