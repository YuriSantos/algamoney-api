package com.example.algamoney.api.resource;

import com.example.algamoney.api.model.Cidade;
import com.example.algamoney.api.repository.CidadeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/cidades")
public class CidadeResource {

	private static final Logger log = LoggerFactory.getLogger(CidadeResource.class);
	
	@Autowired
	private CidadeRepository cidadeRepository;
	
	@GetMapping
	@PreAuthorize("isAuthenticated()")
	public List<Cidade> pesquisar(@RequestParam Long estado) {
		log.info("Pesquisando cidades para o estado com código: {}", estado);
		List<Cidade> cidades = cidadeRepository.findByEstadoCodigo(estado);
		log.info("Total de cidades encontradas para o estado {}: {}", estado, cidades.size());
		return cidades;
	}

}
