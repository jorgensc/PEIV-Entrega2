package jcondo.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

// testa a API de ponta a ponta com os dados de demonstracao
@SpringBootTest
@AutoConfigureMockMvc
class ApiIntegracaoTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    private String login(String email, String senha) throws Exception {
        String corpo = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"senha\":\"" + senha + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + mapper.readTree(corpo).get("token").asText();
    }

    @Test
    void loginDevolveTokenEDadosDoUsuario() throws Exception {
        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ana.souza@email.com\",\"senha\":\"morador123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.tipo", is("Bearer")))
                .andExpect(jsonPath("$.usuario.perfil", is("MORADOR")))
                .andExpect(jsonPath("$.usuario.morador.apartamento", is("101")))
                .andExpect(jsonPath("$.usuario.senha").doesNotExist());
    }

    @Test
    void senhaErradaDa401() throws Exception {
        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ana.souza@email.com\",\"senha\":\"errada\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail", is("E-mail ou senha inválidos")));
    }

    @Test
    void semTokenDa401EmJson() throws Exception {
        mvc.perform(get("/api/v1/avisos"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Content-Type", containsString("application/problem+json")));
    }

    @Test
    void moradorLeAvisosMasNaoPublica() throws Exception {
        String token = login("ana.souza@email.com", "morador123");
        mvc.perform(get("/api/v1/avisos").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", greaterThan(0)))
                .andExpect(jsonPath("$[0].importante", is(true)));

        mvc.perform(post("/api/v1/avisos").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"x\",\"conteudo\":\"y\",\"categoria\":\"GERAL\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void moradorNaoAcessaCadastroDeMoradores() throws Exception {
        String token = login("ana.souza@email.com", "morador123");
        mvc.perform(get("/api/v1/moradores").header("Authorization", token))
                .andExpect(status().isForbidden());
    }

    @Test
    void sindicoListaMoradores() throws Exception {
        String token = login("sindico@jcondo.com", "sindico123");
        mvc.perform(get("/api/v1/moradores").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", greaterThan(4)));
    }

    @Test
    void moradorSoVeAsProprias() throws Exception {
        String token = login("bruno.lima@email.com", "morador123");
        mvc.perform(get("/api/v1/reservas").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].morador", everyItem(is("Bruno Oliveira Lima"))));
        mvc.perform(get("/api/v1/ocorrencias").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].morador", everyItem(is("Bruno Oliveira Lima"))));
    }

    @Test
    void reservaNoMesmoHorarioDa409() throws Exception {
        String ana = login("ana.souza@email.com", "morador123");
        String carla = login("carla.rocha@email.com", "morador123");
        JsonNode areas = mapper.readTree(mvc.perform(get("/api/v1/areas").header("Authorization", ana))
                .andReturn().getResponse().getContentAsString());
        long quadraId = 0;
        for (JsonNode a : areas) {
            if (a.get("nome").asText().startsWith("Quadra")) {
                quadraId = a.get("id").asLong();
            }
        }
        String data = LocalDate.now(ZoneId.of("America/Sao_Paulo")).plusDays(20).toString();
        String corpo = "{\"areaId\":" + quadraId + ",\"data\":\"" + data
                + "\",\"horaInicio\":\"15:00\",\"horaFim\":\"17:00\"}";

        mvc.perform(post("/api/v1/reservas").header("Authorization", ana)
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/reservas/")))
                .andExpect(jsonPath("$.horaInicio", is("15:00")));

        String sobreposta = "{\"areaId\":" + quadraId + ",\"data\":\"" + data
                + "\",\"horaInicio\":\"16:00\",\"horaFim\":\"18:00\"}";
        mvc.perform(post("/api/v1/reservas").header("Authorization", carla)
                        .contentType(MediaType.APPLICATION_JSON).content(sobreposta))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail", containsString("indisponível")));

        mvc.perform(get("/api/v1/reservas/disponibilidade").header("Authorization", carla)
                        .param("areaId", String.valueOf(quadraId)).param("data", data))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ocupados[0].inicio", is("15:00")))
                .andExpect(jsonPath("$.ocupados[0].fim", is("17:00")));
    }

    @Test
    void camposInvalidosDa400ComMensagemPorCampo() throws Exception {
        String token = login("ana.souza@email.com", "morador123");
        mvc.perform(post("/api/v1/ocorrencias").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"\",\"descricao\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.titulo", is("O título é obrigatório")))
                .andExpect(jsonPath("$.campos.categoria", is("Escolha uma categoria")));
    }

    @Test
    void moradorAbreOcorrenciaESindicoResponde() throws Exception {
        String morador = login("diego.alves@email.com", "morador123");
        String sindico = login("sindico@jcondo.com", "sindico123");

        String criada = mvc.perform(post("/api/v1/ocorrencias").header("Authorization", morador)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Interfone mudo\",\"descricao\":\"O interfone do 104 C não toca\",\"categoria\":\"MANUTENCAO\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("ABERTA")))
                .andReturn().getResponse().getContentAsString();
        long id = mapper.readTree(criada).get("id").asLong();

        mvc.perform(patch("/api/v1/ocorrencias/" + id).header("Authorization", morador)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"RESOLVIDA\"}"))
                .andExpect(status().isForbidden());

        mvc.perform(patch("/api/v1/ocorrencias/" + id).header("Authorization", sindico)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"EM_ANDAMENTO\",\"resposta\":\"Técnico agendado\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusDescricao", is("Em andamento")));

        mvc.perform(get("/api/v1/ocorrencias/" + id).header("Authorization", morador))
                .andExpect(jsonPath("$.resposta", is("Técnico agendado")));
    }

    @Test
    void resumoDoMorador() throws Exception {
        String token = login("ana.souza@email.com", "morador123");
        mvc.perform(get("/api/v1/resumo").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perfil", is("MORADOR")))
                .andExpect(jsonPath("$.moradores").doesNotExist())
                .andExpect(jsonPath("$.reservasFuturas", greaterThan(0)));
    }

    @Test
    void healthCheckEDocumentacaoSaoPublicos() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")));
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title", is("JCondo API")));
    }
}
