package jcondo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import jcondo.entity.Usuario;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmailIgnoreCase(String email);

    Optional<Usuario> findByMoradorId(Long moradorId);

    boolean existsByEmailIgnoreCase(String email);
}
