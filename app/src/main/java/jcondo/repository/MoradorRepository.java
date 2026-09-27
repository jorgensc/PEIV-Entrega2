package jcondo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import jcondo.entity.Morador;

@Repository
public interface MoradorRepository extends JpaRepository<Morador, Long> {

    // lista ordenada por nome pra tabela ja vir em ordem alfabetica
    List<Morador> findAllByOrderByNomeAsc();
}
