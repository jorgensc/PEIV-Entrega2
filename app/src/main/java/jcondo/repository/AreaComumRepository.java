package jcondo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import jcondo.entity.AreaComum;

@Repository
public interface AreaComumRepository extends JpaRepository<AreaComum, Long> {

    // trava a area durante a reserva pra duas pessoas nao pegarem o mesmo horario ao mesmo tempo
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from AreaComum a where a.id = :id")
    Optional<AreaComum> buscarComBloqueio(@Param("id") Long id);

    List<AreaComum> findAllByOrderByNomeAsc();

    List<AreaComum> findByAtivaTrueOrderByNomeAsc();
}
