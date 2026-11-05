package jcondo.entity;

public enum StatusOcorrencia {
    ABERTA("Aberta"),
    EM_ANDAMENTO("Em andamento"),
    RESOLVIDA("Resolvida");

    private final String descricao;

    StatusOcorrencia(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
