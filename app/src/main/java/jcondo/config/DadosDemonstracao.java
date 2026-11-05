package jcondo.config;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jcondo.entity.AreaComum;
import jcondo.entity.Aviso;
import jcondo.entity.CategoriaAviso;
import jcondo.entity.CategoriaOcorrencia;
import jcondo.entity.Morador;
import jcondo.entity.Ocorrencia;
import jcondo.entity.Perfil;
import jcondo.entity.Reserva;
import jcondo.entity.StatusOcorrencia;
import jcondo.entity.StatusReserva;
import jcondo.entity.Usuario;
import jcondo.repository.AreaComumRepository;
import jcondo.repository.AvisoRepository;
import jcondo.repository.MoradorRepository;
import jcondo.repository.OcorrenciaRepository;
import jcondo.repository.ReservaRepository;
import jcondo.repository.UsuarioRepository;

// popula o banco vazio com dados de exemplo pra apresentacao
// desliga com SEED_DEMO=false
@Component
@ConditionalOnProperty(name = "jcondo.seed-demo", havingValue = "true")
public class DadosDemonstracao implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DadosDemonstracao.class);

    private final UsuarioRepository usuarios;
    private final MoradorRepository moradores;
    private final AreaComumRepository areas;
    private final AvisoRepository avisos;
    private final ReservaRepository reservas;
    private final OcorrenciaRepository ocorrencias;
    private final PasswordEncoder encoder;
    private final Clock relogio;

    public DadosDemonstracao(UsuarioRepository usuarios, MoradorRepository moradores,
                             AreaComumRepository areas, AvisoRepository avisos,
                             ReservaRepository reservas, OcorrenciaRepository ocorrencias,
                             PasswordEncoder encoder, Clock relogio) {
        this.usuarios = usuarios;
        this.moradores = moradores;
        this.areas = areas;
        this.avisos = avisos;
        this.reservas = reservas;
        this.ocorrencias = ocorrencias;
        this.encoder = encoder;
        this.relogio = relogio;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (usuarios.count() > 0) {
            return; // banco ja tem dados
        }
        log.info("Banco vazio: carregando dados de demonstracao");

        Usuario sindico = new Usuario("Carlos Henrique Batista", "sindico@jcondo.com",
                encoder.encode("sindico123"), Perfil.SINDICO);
        usuarios.save(sindico);

        List<Morador> lista = moradores.findAll();
        if (lista.isEmpty()) {
            lista = moradores.saveAll(List.of(
                    new Morador("Ana Paula Souza", "101", "A", "123.456.789-01", "(49) 99911-1111", "ana.souza@email.com"),
                    new Morador("Bruno Oliveira Lima", "202", "B", "234.567.890-12", "(49) 99922-2222", "bruno.lima@email.com"),
                    new Morador("Carla Mendes Rocha", "303", "A", "345.678.901-23", "(49) 99933-3333", "carla.rocha@email.com"),
                    new Morador("Diego Ferreira Alves", "104", "C", "456.789.012-34", "(49) 99944-4444", "diego.alves@email.com"),
                    new Morador("Eduarda Costa Martins", "205", "B", "567.890.123-45", "(49) 99955-5555", "eduarda.martins@email.com")));
        }
        // todo morador ganha acesso ao app com a senha morador123
        String senhaMorador = encoder.encode("morador123");
        for (Morador m : lista) {
            if (usuarios.findByMoradorId(m.getId()).isEmpty() && !usuarios.existsByEmailIgnoreCase(m.getEmail())) {
                Usuario u = new Usuario(m.getNome(), m.getEmail(), senhaMorador, Perfil.MORADOR);
                u.setMorador(m);
                usuarios.save(u);
            }
        }

        if (areas.count() == 0) {
            areas.saveAll(List.of(
                    new AreaComum("Salão de festas", "Salão com cozinha, mesas para 80 pessoas e som ambiente", 80,
                            LocalTime.of(8, 0), LocalTime.of(23, 0)),
                    new AreaComum("Churrasqueira", "Churrasqueira coberta com pia e geladeira", 25,
                            LocalTime.of(10, 0), LocalTime.of(22, 0)),
                    new AreaComum("Quadra poliesportiva", "Quadra coberta para futsal, vôlei e basquete", 20,
                            LocalTime.of(7, 0), LocalTime.of(22, 0)),
                    new AreaComum("Espaço gourmet", "Cozinha equipada e mesa para 12 pessoas", 12,
                            LocalTime.of(9, 0), LocalTime.of(23, 0))));
        }

        LocalDateTime agora = LocalDateTime.now(relogio);
        if (avisos.count() == 0) {
            avisos.saveAll(List.of(
                    aviso("Assembleia geral ordinária",
                            "Convocamos todos os condôminos para a assembleia geral no salão de festas, no dia 20, às 19h30. "
                                    + "Pauta: prestação de contas do semestre, previsão orçamentária e pintura da fachada.",
                            CategoriaAviso.ASSEMBLEIA, true, agora.minusDays(1), sindico),
                    aviso("Manutenção dos elevadores",
                            "Na próxima terça-feira, das 8h às 12h, os elevadores do bloco B passarão por manutenção preventiva. "
                                    + "Pedimos que utilizem as escadas ou o elevador do bloco A nesse período.",
                            CategoriaAviso.MANUTENCAO, false, agora.minusDays(3), sindico),
                    aviso("Festa de fim de ano",
                            "A confraternização dos moradores será no dia 13 de dezembro, a partir das 18h, na área da piscina. "
                                    + "Cada família pode levar um prato. Confirme presença na portaria.",
                            CategoriaAviso.EVENTO, false, agora.minusDays(5), sindico),
                    aviso("Horário de silêncio",
                            "Lembramos que o horário de silêncio é das 22h às 7h, conforme o regimento interno. "
                                    + "Contamos com a colaboração de todos.",
                            CategoriaAviso.GERAL, false, agora.minusDays(9), sindico)));
        }

        if (reservas.count() == 0 && lista.size() >= 4) {
            List<AreaComum> as = areas.findAllByOrderByNomeAsc();
            AreaComum churrasqueira = buscarArea(as, "Churrasqueira");
            AreaComum salao = buscarArea(as, "Salão de festas");
            AreaComum quadra = buscarArea(as, "Quadra poliesportiva");
            LocalDate hoje = LocalDate.now(relogio);
            List<Reserva> novas = new ArrayList<>();
            novas.add(reserva(salao, lista.get(0), hoje.plusDays(3), 14, 18, "Aniversário da filha", StatusReserva.CONFIRMADA, agora));
            novas.add(reserva(churrasqueira, lista.get(1), hoje.plusDays(5), 11, 15, "Almoço de família", StatusReserva.CONFIRMADA, agora));
            novas.add(reserva(quadra, lista.get(2), hoje.plusDays(1), 19, 21, "Futsal com amigos", StatusReserva.CONFIRMADA, agora));
            novas.add(reserva(churrasqueira, lista.get(0), hoje.plusDays(10), 12, 16, null, StatusReserva.CONFIRMADA, agora));
            novas.add(reserva(salao, lista.get(3), hoje.plusDays(7), 19, 23, "Confraternização", StatusReserva.CONFIRMADA, agora));
            novas.add(reserva(quadra, lista.get(1), hoje.plusDays(2), 8, 10, null, StatusReserva.CANCELADA, agora));
            novas.add(reserva(churrasqueira, lista.get(2), hoje.minusDays(6), 11, 15, null, StatusReserva.CONFIRMADA, agora.minusDays(12)));
            reservas.saveAll(novas);
        }

        if (ocorrencias.count() == 0 && lista.size() >= 4) {
            ocorrencias.saveAll(List.of(
                    ocorrencia(lista.get(0), "Vazamento na garagem",
                            "Tem uma infiltração pingando no teto da garagem, perto da vaga 15 do bloco A. O chão fica molhado.",
                            CategoriaOcorrencia.MANUTENCAO, StatusOcorrencia.ABERTA, null, agora.minusHours(5)),
                    ocorrencia(lista.get(1), "Barulho depois das 22h",
                            "Música alta no bloco B depois das 22h nos últimos três dias.",
                            CategoriaOcorrencia.BARULHO, StatusOcorrencia.EM_ANDAMENTO,
                            "Enviamos uma notificação ao apartamento responsável. Vamos acompanhar.", agora.minusDays(2)),
                    ocorrencia(lista.get(2), "Lâmpada queimada no hall",
                            "A lâmpada do hall de entrada do bloco A está queimada e o corredor fica escuro à noite.",
                            CategoriaOcorrencia.MANUTENCAO, StatusOcorrencia.RESOLVIDA,
                            "Lâmpada trocada pelo zelador. Obrigado pelo aviso!", agora.minusDays(4)),
                    ocorrencia(lista.get(3), "Portão da garagem lento",
                            "O portão da garagem está demorando muito para abrir e às vezes para no meio.",
                            CategoriaOcorrencia.SEGURANCA, StatusOcorrencia.ABERTA, null, agora.minusDays(1))));
        }
        log.info("Dados de demonstracao carregados. Sindico: sindico@jcondo.com / Moradores: senha morador123");
    }

    private AreaComum buscarArea(List<AreaComum> lista, String nome) {
        return lista.stream().filter(a -> a.getNome().equals(nome)).findFirst().orElse(lista.get(0));
    }

    private Aviso aviso(String titulo, String conteudo, CategoriaAviso cat, boolean importante,
                        LocalDateTime quando, Usuario autor) {
        Aviso a = new Aviso();
        a.setTitulo(titulo);
        a.setConteudo(conteudo);
        a.setCategoria(cat);
        a.setImportante(importante);
        a.setPublicadoEm(quando);
        a.setAutor(autor);
        return a;
    }

    private Reserva reserva(AreaComum area, Morador m, LocalDate data, int inicio, int fim,
                            String obs, StatusReserva status, LocalDateTime criada) {
        Reserva r = new Reserva();
        r.setArea(area);
        r.setMorador(m);
        r.setDataReserva(data);
        r.setHoraInicio(LocalTime.of(inicio, 0));
        r.setHoraFim(LocalTime.of(fim, 0));
        r.setObservacao(obs);
        r.setStatus(status);
        r.setCriadaEm(criada);
        return r;
    }

    private Ocorrencia ocorrencia(Morador m, String titulo, String descricao, CategoriaOcorrencia cat,
                                  StatusOcorrencia status, String resposta, LocalDateTime abertaEm) {
        Ocorrencia o = new Ocorrencia();
        o.setMorador(m);
        o.setTitulo(titulo);
        o.setDescricao(descricao);
        o.setCategoria(cat);
        o.setStatus(status);
        o.setResposta(resposta);
        o.setAbertaEm(abertaEm);
        o.setAtualizadaEm(resposta == null ? abertaEm : abertaEm.plusHours(6));
        return o;
    }
}
