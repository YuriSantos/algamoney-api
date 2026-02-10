package com.example.algamoney.api.resource;

import com.example.algamoney.api.dto.Anexo;
import com.example.algamoney.api.dto.LancamentoEstatisticaCategoria;
import com.example.algamoney.api.dto.LancamentoEstatisticaDia;
import com.example.algamoney.api.event.RecursoCriadoEvent;
import com.example.algamoney.api.exceptionHandler.AlgamoneyExceptionHandler.Erro;
import com.example.algamoney.api.model.Lancamento;
import com.example.algamoney.api.repository.LancamentoRepository;
import com.example.algamoney.api.repository.filter.LancamentoFilter;
import com.example.algamoney.api.repository.projection.ResumoLancamento;
import com.example.algamoney.api.service.LancamentoService;
import com.example.algamoney.api.service.exception.PessoaInexistenteOuInativaException;
import com.example.algamoney.api.storage.S3;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/lancamentos")
public class LancamentoResource {

    private static final Logger log = LoggerFactory.getLogger(LancamentoResource.class);

    @Autowired
    private LancamentoRepository lancamentoRepository;

    @Autowired
    private LancamentoService lancamentoService;

    @Autowired
    private ApplicationEventPublisher publisher;

    @Autowired
    private MessageSource messageSource;

    @Autowired
    private S3 s3;

    @PostMapping("/anexo")
    @PreAuthorize("hasAuthority('ROLE_CADASTRAR_LANCAMENTO') and hasAuthority('SCOPE_write')")
    public Anexo uploadAnexo(@RequestParam MultipartFile anexo) {
        log.info("Iniciando upload de anexo: {}", anexo.getOriginalFilename());
        String nome = s3.salvarTemporariamente(anexo);
        Anexo anexoDto = new Anexo(nome, s3.configurarUrl(nome));
        log.info("Upload de anexo concluído. Nome: {}, URL: {}", anexoDto.getNome(), anexoDto.getUrl());
        return anexoDto;
    }

    @GetMapping("/relatorios/por-pessoa")
    @PreAuthorize("hasAuthority('ROLE_PESQUISAR_LANCAMENTO') and hasAuthority('SCOPE_read')")
    public ResponseEntity<byte[]> relatorioPorPessoa(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate inicio,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate fim) throws Exception {
        log.info("Gerando relatório por pessoa. Período de {} a {}", inicio, fim);
        byte[] relatorio = lancamentoService.relatorioPorPessoa(inicio, fim);
        log.info("Relatório gerado com sucesso. Tamanho: {} bytes", relatorio.length);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE)
                .body(relatorio);
    }

    @GetMapping("/estatisticas/por-dia")
    @PreAuthorize("hasAuthority('ROLE_PESQUISAR_LANCAMENTO') and hasAuthority('SCOPE_read')")
    public List<LancamentoEstatisticaDia> porDia() {
        log.info("Buscando estatísticas de lançamentos por dia.");
        List<LancamentoEstatisticaDia> estatisticas = this.lancamentoRepository.porDia(LocalDate.now());
        log.info("Estatísticas por dia encontradas: {} registros", estatisticas.size());
        return estatisticas;
    }

    @GetMapping("/estatisticas/por-categoria")
    @PreAuthorize("hasAuthority('ROLE_PESQUISAR_LANCAMENTO') and hasAuthority('SCOPE_read')")
    public List<LancamentoEstatisticaCategoria> porCategoria() {
        log.info("Buscando estatísticas de lançamentos por categoria.");
        List<LancamentoEstatisticaCategoria> estatisticas = this.lancamentoRepository.porCategoria(LocalDate.now());
        log.info("Estatísticas por categoria encontradas: {} registros", estatisticas.size());
        return estatisticas;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_PESQUISAR_LANCAMENTO') and hasAuthority('SCOPE_read')")
    public Page<Lancamento> pesquisar(LancamentoFilter lancamentoFilter, Pageable pageable) {
        log.info("Pesquisando lançamentos com filtro: {} e paginação: {}", lancamentoFilter, pageable);
        Page<Lancamento> lancamentos = lancamentoRepository.filtrar(lancamentoFilter, pageable);
        log.info("Lançamentos encontrados: {}", lancamentos.getTotalElements());
        return lancamentos;
    }

    @GetMapping(params = "resumo")
    @PreAuthorize("hasAuthority('ROLE_PESQUISAR_LANCAMENTO') and hasAuthority('SCOPE_read')")
    public Page<ResumoLancamento> resumir(LancamentoFilter lancamentoFilter, Pageable pageable) {
        log.info("Resumindo lançamentos com filtro: {} e paginação: {}", lancamentoFilter, pageable);
        Page<ResumoLancamento> resumos = lancamentoRepository.resumir(lancamentoFilter, pageable);
        log.info("Resumos de lançamentos encontrados: {}", resumos.getTotalElements());
        return resumos;
    }

    @GetMapping("/{codigo}")
    @PreAuthorize("hasAuthority('ROLE_PESQUISAR_LANCAMENTO') and hasAuthority('SCOPE_read')")
    public ResponseEntity<Lancamento> buscarPeloCodigo(@PathVariable Long codigo) {
        log.info("Buscando lançamento pelo código: {}", codigo);
        Optional<Lancamento> lancamento = lancamentoRepository.findById(codigo);
        if (lancamento.isPresent()) {
            log.info("Lançamento encontrado: {}", lancamento.get().getDescricao());
            return ResponseEntity.ok(lancamento.get());
        } else {
            log.warn("Lançamento com código {} não encontrado", codigo);
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_CADASTRAR_LANCAMENTO') and hasAuthority('SCOPE_write')")
    public ResponseEntity<Lancamento> criar(@Valid @RequestBody Lancamento lancamento, HttpServletResponse response) {
        log.info("Criando novo lançamento: {}", lancamento.getDescricao());
        Lancamento lancamentoSalvo = lancamentoService.salvar(lancamento);
        log.info("Lançamento criado com sucesso. Código: {}", lancamentoSalvo.getCodigo());
        publisher.publishEvent(new RecursoCriadoEvent(this, response, lancamentoSalvo.getCodigo()));
        return ResponseEntity.status(HttpStatus.CREATED).body(lancamentoSalvo);
    }

    @ExceptionHandler({PessoaInexistenteOuInativaException.class})
    public ResponseEntity<Object> handlePessoaInexistenteOuInativaException(PessoaInexistenteOuInativaException ex) {
        String mensagemUsuario = messageSource.getMessage("pessoa.inexistente-ou-inativa", null, LocaleContextHolder.getLocale());
        String mensagemDesenvolvedor = ex.toString();
        List<Erro> erros = List.of(new Erro(mensagemUsuario, mensagemDesenvolvedor));
        log.error("Erro de negócio: Pessoa inexistente ou inativa. Detalhes: {}", mensagemDesenvolvedor);
        return ResponseEntity.badRequest().body(erros);
    }

    @DeleteMapping("/{codigo}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('ROLE_REMOVER_LANCAMENTO') and hasAuthority('SCOPE_write')")
    public void remover(@PathVariable Long codigo) {
        log.info("Removendo lançamento com código: {}", codigo);
        lancamentoRepository.deleteById(codigo);
        log.info("Lançamento com código {} removido com sucesso", codigo);
    }

    @PutMapping("/{codigo}")
    @PreAuthorize("hasAuthority('ROLE_CADASTRAR_LANCAMENTO')")
    public ResponseEntity<Lancamento> atualizar(@PathVariable Long codigo, @Valid @RequestBody Lancamento lancamento) {
        log.info("Atualizando lançamento com código: {}", codigo);
        try {
            Lancamento lancamentoSalvo = lancamentoService.atualizar(codigo, lancamento);
            log.info("Lançamento com código {} atualizado com sucesso", codigo);
            return ResponseEntity.ok(lancamentoSalvo);
        } catch (IllegalArgumentException e) {
            log.error("Erro ao atualizar lançamento {}: {}", codigo, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
}
