package jcondo.entity;

public enum CategoriaOcorrencia {
    MANUTENCAO("Manutenção"),
    BARULHO("Barulho"),
    SEGURANCA("Segurança"),
    LIMPEZA("Limpeza"),
    OUTROS("Outros");

    private final String descricao;

    CategoriaOcorrencia(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
