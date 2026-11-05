package jcondo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import jcondo.entity.StatusOcorrencia;
import jcondo.service.AvisoService;
import jcondo.service.MoradorService;
import jcondo.service.OcorrenciaService;
import jcondo.service.ReservaService;

// tela inicial do sindico com os numeros do condominio
@Controller
public class PainelController {

    private final MoradorService moradores;
    private final AvisoService avisos;
    private final ReservaService reservas;
    private final OcorrenciaService ocorrencias;

    public PainelController(MoradorService moradores, AvisoService avisos,
                            ReservaService reservas, OcorrenciaService ocorrencias) {
        this.moradores = moradores;
        this.avisos = avisos;
        this.reservas = reservas;
        this.ocorrencias = ocorrencias;
    }

    @GetMapping("/painel")
    public String painel(Model model) {
        model.addAttribute("totalMoradores", moradores.contar());
        model.addAttribute("totalAvisos", avisos.contar());
        model.addAttribute("reservasFuturas", reservas.contarFuturas());
        model.addAttribute("ocorrenciasAbertas", ocorrencias.contar(StatusOcorrencia.ABERTA));
        model.addAttribute("ocorrenciasAndamento", ocorrencias.contar(StatusOcorrencia.EM_ANDAMENTO));
        model.addAttribute("ocorrenciasResolvidas", ocorrencias.contar(StatusOcorrencia.RESOLVIDA));
        model.addAttribute("proximasReservas", reservas.proximas().stream().limit(5).toList());
        model.addAttribute("pendentes", ocorrencias.listar(StatusOcorrencia.ABERTA).stream().limit(5).toList());
        model.addAttribute("ultimosAvisos", avisos.ultimos());
        return "painel";
    }
}
