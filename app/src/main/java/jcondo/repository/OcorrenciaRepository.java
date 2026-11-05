package jcondo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jcondo.entity.Ocorrencia;
import jcondo.entity.StatusOcorrencia;

@Repository
public interface OcorrenciaRepository extends JpaRepository<Ocorrencia, Long> {

    List<Ocorrencia> findAllByOrderByAbertaEmDesc();

    List<Ocorrencia> findByStatusOrderByAbertaEmDesc(StatusOcorrencia status);

    List<Ocorrencia> findByMoradorIdOrderByAbertaEmDesc(Long moradorId);

    long countByStatus(StatusOcorrencia status);

    long countByMoradorIdAndStatusNot(Long moradorId, StatusOcorrencia status);

    @Modifying
    @Query("delete from Ocorrencia o where o.morador.id = :moradorId")
    void excluirDoMorador(@Param("moradorId") Long moradorId);
}
