package jcondo.repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jcondo.entity.Reserva;
import jcondo.entity.StatusReserva;

@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    // dois horarios se sobrepoem quando um comeca antes do outro terminar
    @Query("""
            select count(r) > 0 from Reserva r
            where r.area.id = :areaId
              and r.dataReserva = :data
              and r.status = jcondo.entity.StatusReserva.CONFIRMADA
              and r.horaInicio < :fim
              and r.horaFim > :inicio
            """)
    boolean existeConflito(@Param("areaId") Long areaId, @Param("data") LocalDate data,
                           @Param("inicio") LocalTime inicio, @Param("fim") LocalTime fim);

    List<Reserva> findByAreaIdAndDataReservaAndStatusOrderByHoraInicioAsc(
            Long areaId, LocalDate data, StatusReserva status);

    List<Reserva> findByMoradorIdOrderByDataReservaDescHoraInicioDesc(Long moradorId);

    List<Reserva> findAllByOrderByDataReservaDescHoraInicioDesc();

    List<Reserva> findByDataReservaOrderByHoraInicioAsc(LocalDate data);

    List<Reserva> findByDataReservaGreaterThanEqualAndStatusOrderByDataReservaAscHoraInicioAsc(
            LocalDate data, StatusReserva status);

    List<Reserva> findByMoradorIdAndDataReservaGreaterThanEqualAndStatusOrderByDataReservaAscHoraInicioAsc(
            Long moradorId, LocalDate data, StatusReserva status);

    long countByDataReservaGreaterThanEqualAndStatus(LocalDate data, StatusReserva status);

    long countByMoradorIdAndDataReservaGreaterThanEqualAndStatus(
            Long moradorId, LocalDate data, StatusReserva status);

    boolean existsByAreaId(Long areaId);

    @Modifying
    @Query("delete from Reserva r where r.morador.id = :moradorId")
    void excluirDoMorador(@Param("moradorId") Long moradorId);
}
