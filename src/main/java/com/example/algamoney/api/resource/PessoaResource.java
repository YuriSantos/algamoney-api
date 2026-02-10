package com.example.algamoney.api.resource;

import com.example.algamoney.api.event.RecursoCriadoEvent;
import com.example.algamoney.api.model.Pessoa;
import com.example.algamoney.api.repository.PessoaRepository;
import com.example.algamoney.api.service.PessoaService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/pessoas")
public class PessoaResource {

    private static final Logger log = LoggerFactory.getLogger(PessoaResource.class);

    @Autowired
    private PessoaRepository pessoaRepository;

    @Autowired
    private PessoaService pessoaService;

    @Autowired
    private ApplicationEventPublisher publisher;

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_CADASTRAR_PESSOA') and hasAuthority('SCOPE_write')")
    public ResponseEntity<Pessoa> criar(@Valid @RequestBody Pessoa pessoa, HttpServletResponse response) {
        log.info("Criando nova pessoa: {}", pessoa.getNome());
        Pessoa pessoaSalva = pessoaService.salvar(pessoa);
        log.info("Pessoa criada com sucesso. Código: {}", pessoaSalva.getCodigo());
        publisher.publishEvent(new RecursoCriadoEvent(this, response, pessoaSalva.getCodigo()));
        return ResponseEntity.status(HttpStatus.CREATED).body(pessoaSalva);
    }

    @GetMapping("/{codigo}")
    @PreAuthorize("hasAuthority('ROLE_PESQUISAR_PESSOA') and hasAuthority('SCOPE_read')")
    public ResponseEntity<Pessoa> buscarPeloCodigo(@PathVariable Long codigo) {
        log.info("Buscando pessoa pelo código: {}", codigo);
        Optional<Pessoa> pessoa = pessoaRepository.findById(codigo);
        if (pessoa.isPresent()) {
            log.info("Pessoa encontrada: {}", pessoa.get().getNome());
            return ResponseEntity.ok(pessoa.get());
        } else {
            log.warn("Pessoa com código {} não encontrada", codigo);
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{codigo}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('ROLE_REMOVER_PESSOA') and hasAuthority('SCOPE_write')")
    public void remover(@PathVariable Long codigo) {
        log.info("Removendo pessoa com código: {}", codigo);
        this.pessoaRepository.deleteById(codigo);
        log.info("Pessoa com código {} removida com sucesso", codigo);
    }

    @PutMapping("/{codigo}")
    @PreAuthorize("hasAuthority('ROLE_CADASTRAR_PESSOA') and hasAuthority('SCOPE_write')")
    public ResponseEntity<Pessoa> atualizar(@PathVariable Long codigo, @Valid @RequestBody Pessoa pessoa) {
        log.info("Atualizando pessoa com código: {}", codigo);
        Pessoa pessoaSalva = pessoaService.atualizar(codigo, pessoa);
        log.info("Pessoa com código {} atualizada com sucesso", codigo);
        return ResponseEntity.ok(pessoaSalva);
    }

    @PutMapping("/{codigo}/ativo")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('ROLE_CADASTRAR_PESSOA') and hasAuthority('SCOPE_write')")
    public void atualizarPropriedadeAtivo(@PathVariable Long codigo, @RequestBody Boolean ativo) {
        log.info("Atualizando propriedade 'ativo' para {} na pessoa com código: {}", ativo, codigo);
        pessoaService.atualizarPropriedadeAtivo(codigo, ativo);
        log.info("Propriedade 'ativo' da pessoa com código {} atualizada com sucesso", codigo);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_PESQUISAR_PESSOA')")
    public Page<Pessoa> pesquisar(@RequestParam(required = false, defaultValue = "") String nome, Pageable pageable) {
        log.info("Pesquisando pessoas com nome contendo: '{}' e paginação: {}", nome, pageable);
        Page<Pessoa> pessoas = pessoaRepository.findByNomeContaining(nome, pageable);
        log.info("Pessoas encontradas: {}", pessoas.getTotalElements());
        return pessoas;
    }

}
