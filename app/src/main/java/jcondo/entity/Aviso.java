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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// comunicado publicado pelo sindico
@Entity
@Table(name = "avisos")
public class Aviso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "O título é obrigatório")
    @Size(max = 120, message = "O título pode ter no máximo 120 caracteres")
    @Column(nullable = false, length = 120)
    private String titulo;

    @NotBlank(message = "O conteúdo é obrigatório")
    @Size(max = 2000, message = "O conteúdo pode ter no máximo 2000 caracteres")
    @Column(nullable = false, length = 2000)
    private String conteudo;

    @NotNull(message = "Escolha uma categoria")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CategoriaAviso categoria = CategoriaAviso.GERAL;

    // aviso importante fica fixado no topo
    @Column(nullable = false)
    private boolean importante;

    @Column(name = "publicado_em", nullable = false)
    private LocalDateTime publicadoEm;

    @ManyToOne
    @JoinColumn(name = "autor_id")
    private Usuario autor;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getConteudo() { return conteudo; }
    public void setConteudo(String conteudo) { this.conteudo = conteudo; }

    public CategoriaAviso getCategoria() { return categoria; }
    public void setCategoria(CategoriaAviso categoria) { this.categoria = categoria; }

    public boolean isImportante() { return importante; }
    public void setImportante(boolean importante) { this.importante = importante; }

    public LocalDateTime getPublicadoEm() { return publicadoEm; }
    public void setPublicadoEm(LocalDateTime publicadoEm) { this.publicadoEm = publicadoEm; }

    public Usuario getAutor() { return autor; }
    public void setAutor(Usuario autor) { this.autor = autor; }
}
