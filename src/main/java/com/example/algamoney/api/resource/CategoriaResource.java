package com.example.algamoney.api.resource;

import com.example.algamoney.api.event.RecursoCriadoEvent;
import com.example.algamoney.api.model.Categoria;
import com.example.algamoney.api.repository.CategoriaRepository;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/categorias")
public class CategoriaResource {

    private static final Logger log = LoggerFactory.getLogger(CategoriaResource.class);

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private ApplicationEventPublisher publisher;

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_PESQUISAR_CATEGORIA') and hasAuthority('SCOPE_read')")
    public List<Categoria> listar() {
        log.info("Listando todas as categorias");
        List<Categoria> categorias = categoriaRepository.findAll();
        log.info("Total de categorias encontradas: {}", categorias.size());
        return categorias;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_CADASTRAR_CATEGORIA') and hasAuthority('SCOPE_write')")
    public ResponseEntity<Categoria> criar(@Valid @RequestBody Categoria categoria, HttpServletResponse response) {
        log.info("Criando nova categoria: {}", categoria.getNome());
        Categoria categoriaSalva = categoriaRepository.save(categoria);
        log.info("Categoria criada com sucesso. Código: {}", categoriaSalva.getCodigo());

        publisher.publishEvent(new RecursoCriadoEvent(this, response, categoriaSalva.getCodigo()));

        return ResponseEntity.status(HttpStatus.CREATED).body(categoriaSalva);
    }

    @GetMapping("/{codigo}")
    @PreAuthorize("hasAuthority('ROLE_PESQUISAR_CATEGORIA') and hasAuthority('SCOPE_read')")
    public ResponseEntity<Categoria> buscarPeloCodigo(@PathVariable Long codigo) {
        log.info("Buscando categoria pelo código: {}", codigo);
        Optional<Categoria> categoria = categoriaRepository.findById(codigo);

        if (categoria.isPresent()) {
            log.info("Categoria encontrada: {}", categoria.get().getNome());
            return ResponseEntity.ok(categoria.get());
        } else {
            log.warn("Categoria com código {} não encontrada", codigo);
            return ResponseEntity.notFound().build();
        }
    }

}
