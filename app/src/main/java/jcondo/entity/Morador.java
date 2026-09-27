package jcondo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// Entidade principal do sistema - representa um morador do condominio
@Entity
@Table(name = "moradores")
public class Morador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "O nome é obrigatório")
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String nome;

    @NotBlank(message = "O apartamento é obrigatório")
    @Size(max = 10)
    @Column(nullable = false, length = 10)
    private String apartamento;

    @NotBlank(message = "O bloco é obrigatório")
    @Size(max = 10)
    @Column(nullable = false, length = 10)
    private String bloco;

    // aceita com ou sem pontuacao: 000.000.000-00 ou 00000000000
    @NotBlank(message = "O CPF é obrigatório")
    @Pattern(regexp = "\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}",
             message = "CPF inválido. Use o formato 000.000.000-00")
    @Column(nullable = false, length = 14)
    private String cpf;

    @NotBlank(message = "O telefone é obrigatório")
    @Size(max = 20)
    @Column(nullable = false, length = 20)
    private String telefone;

    @NotBlank(message = "O e-mail é obrigatório")
    @Email(message = "Informe um e-mail válido")
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String email;

    // JPA precisa do construtor vazio
    public Morador() {
    }

    public Morador(String nome, String apartamento, String bloco,
                   String cpf, String telefone, String email) {
        this.nome = nome;
        this.apartamento = apartamento;
        this.bloco = bloco;
        this.cpf = cpf;
        this.telefone = telefone;
        this.email = email;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getApartamento() {
        return apartamento;
    }

    public void setApartamento(String apartamento) {
        this.apartamento = apartamento;
    }

    public String getBloco() {
        return bloco;
    }

    public void setBloco(String bloco) {
        this.bloco = bloco;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
