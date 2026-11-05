package jcondo.entity;

public enum CategoriaAviso {
    GERAL("Geral"),
    MANUTENCAO("Manutenção"),
    ASSEMBLEIA("Assembleia"),
    SEGURANCA("Segurança"),
    EVENTO("Evento");

    private final String descricao;

    CategoriaAviso(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
