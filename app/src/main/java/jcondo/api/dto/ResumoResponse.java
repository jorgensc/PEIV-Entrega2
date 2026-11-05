package jcondo.api.dto;

import java.util.List;

import jcondo.api.dto.AvisoDtos.AvisoResponse;
import jcondo.api.dto.ReservaDtos.ReservaResponse;
import jcondo.entity.Perfil;

// numeros da tela inicial. Pro morador, reservas e ocorrencias sao so as dele
public record ResumoResponse(Perfil perfil, long avisos, long reservasFuturas,
                             long ocorrenciasEmAberto, Long moradores,
                             List<AvisoResponse> ultimosAvisos,
                             List<ReservaResponse> proximasReservas) {
}
