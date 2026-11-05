package jcondo.entity;

import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// salao de festas, churrasqueira, quadra...
@Entity
@Table(name = "areas_comuns")
public class AreaComum {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "O nome é obrigatório")
    @Size(max = 60)
    @Column(nullable = false, length = 60)
    private String nome;

    @Size(max = 255)
    @Column(length = 255)
    private String descricao;

    @NotNull(message = "Informe a capacidade")
    @Min(value = 1, message = "A capacidade mínima é 1 pessoa")
    @Max(value = 500, message = "A capacidade máxima é 500 pessoas")
    @Column(nullable = false)
    private Integer capacidade;

    @NotNull(message = "Informe o horário de abertura")
    @Column(name = "hora_abertura", nullable = false)
    private LocalTime horaAbertura;

    @NotNull(message = "Informe o horário de fechamento")
    @Column(name = "hora_fechamento", nullable = false)
    private LocalTime horaFechamento;

    @Column(nullable = false)
    private boolean ativa = true;

    public AreaComum() {
    }

    public AreaComum(String nome, String descricao, int capacidade,
                     LocalTime horaAbertura, LocalTime horaFechamento) {
        this.nome = nome;
        this.descricao = descricao;
        this.capacidade = capacidade;
        this.horaAbertura = horaAbertura;
        this.horaFechamento = horaFechamento;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public Integer getCapacidade() { return capacidade; }
    public void setCapacidade(Integer capacidade) { this.capacidade = capacidade; }

    public LocalTime getHoraAbertura() { return horaAbertura; }
    public void setHoraAbertura(LocalTime horaAbertura) { this.horaAbertura = horaAbertura; }

    public LocalTime getHoraFechamento() { return horaFechamento; }
    public void setHoraFechamento(LocalTime horaFechamento) { this.horaFechamento = horaFechamento; }

    public boolean isAtiva() { return ativa; }
    public void setAtiva(boolean ativa) { this.ativa = ativa; }
}
