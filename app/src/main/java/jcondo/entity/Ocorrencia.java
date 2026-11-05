package jcondo.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

// problema registrado pelo morador e acompanhado pelo sindico
@Entity
@Table(name = "ocorrencias")
public class Ocorrencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String titulo;

    @Column(nullable = false, length = 2000)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CategoriaOcorrencia categoria;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusOcorrencia status = StatusOcorrencia.ABERTA;

    // retorno do sindico pro morador
    @Column(length = 1000)
    private String resposta;

    @ManyToOne(optional = false)
    @JoinColumn(name = "morador_id", nullable = false)
    private Morador morador;

    @Column(name = "aberta_em", nullable = false)
    private LocalDateTime abertaEm;

    @Column(name = "atualizada_em", nullable = false)
    private LocalDateTime atualizadaEm;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public CategoriaOcorrencia getCategoria() { return categoria; }
    public void setCategoria(CategoriaOcorrencia categoria) { this.categoria = categoria; }

    public StatusOcorrencia getStatus() { return status; }
    public void setStatus(StatusOcorrencia status) { this.status = status; }

    public String getResposta() { return resposta; }
    public void setResposta(String resposta) { this.resposta = resposta; }

    public Morador getMorador() { return morador; }
    public void setMorador(Morador morador) { this.morador = morador; }

    public LocalDateTime getAbertaEm() { return abertaEm; }
    public void setAbertaEm(LocalDateTime abertaEm) { this.abertaEm = abertaEm; }

    public LocalDateTime getAtualizadaEm() { return atualizadaEm; }
    public void setAtualizadaEm(LocalDateTime atualizadaEm) { this.atualizadaEm = atualizadaEm; }
}
