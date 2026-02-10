package com.example.algamoney.api.service;

import com.example.algamoney.api.model.Pessoa;
import com.example.algamoney.api.repository.PessoaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

@Service
public class PessoaService {

    private static final Logger log = LoggerFactory.getLogger(PessoaService.class);

    @Autowired
    private PessoaRepository pessoaRepository;

    @Transactional
    public Pessoa salvar(Pessoa pessoa) {
        log.info("Iniciando processo de salvar nova pessoa: {}", pessoa.getNome());
        pessoa.setCodigo(null);

        if (pessoa.getContatos() != null) {
            log.debug("Associando {} contatos à nova pessoa.", pessoa.getContatos().size());
            pessoa.getContatos().forEach(contato -> {
                contato.setCodigo(null);
                contato.setPessoa(pessoa);
            });
        } else {
            log.debug("Nenhum contato para associar. Inicializando lista de contatos vazia.");
            pessoa.setContatos(new ArrayList<>());
        }

        Pessoa pessoaSalva = pessoaRepository.save(pessoa);
        log.info("Pessoa salva com sucesso. Código: {}", pessoaSalva.getCodigo());
        return pessoaSalva;
    }

    @Transactional
    public Pessoa atualizar(Long codigo, Pessoa pessoa) {
        log.info("Iniciando processo de atualização para pessoa com código: {}", codigo);
        Pessoa pessoaSalva = buscarPessoaPeloCodigo(codigo);
        log.debug("Pessoa encontrada no banco: {}", pessoaSalva.getNome());

        pessoaSalva.getContatos().clear();
        log.debug("Contatos antigos da pessoa {} removidos.", codigo);

        if (pessoa.getContatos() != null) {
            log.debug("Adicionando {} novos contatos à pessoa.", pessoa.getContatos().size());
            pessoaSalva.getContatos().addAll(pessoa.getContatos());
        }

        pessoaSalva.getContatos().forEach(c -> c.setPessoa(pessoaSalva));
        log.debug("Associação bidirecional dos contatos com a pessoa {} garantida.", codigo);

        BeanUtils.copyProperties(pessoa, pessoaSalva, "codigo", "contatos");
        log.debug("Propriedades da pessoa (exceto código e contatos) copiadas.");

        Pessoa pessoaAtualizada = pessoaRepository.save(pessoaSalva);
        log.info("Pessoa com código {} atualizada com sucesso.", codigo);
        return pessoaAtualizada;
    }

    public void atualizarPropriedadeAtivo(Long codigo, Boolean ativo) {
        log.info("Atualizando propriedade 'ativo' para {} na pessoa com código: {}", ativo, codigo);
        Pessoa pessoaSalva = buscarPessoaPeloCodigo(codigo);
        pessoaSalva.setAtivo(ativo);
        pessoaRepository.save(pessoaSalva);
        log.info("Propriedade 'ativo' da pessoa com código {} atualizada com sucesso.", codigo);
    }

    public Pessoa buscarPessoaPeloCodigo(Long codigo) {
        log.info("Buscando pessoa pelo código: {}", codigo);
        return pessoaRepository.findById(codigo)
                .orElseThrow(() -> {
                    log.error("Pessoa com código {} não encontrada.", codigo);
                    return new EmptyResultDataAccessException(1);
                });
    }
}
