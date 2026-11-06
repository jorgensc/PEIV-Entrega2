-- JCondo - script do banco de dados (versao 2.0)
-- Funciona no MySQL do XAMPP e no Azure Database for MySQL.
--
-- Nao e obrigatorio rodar: a aplicacao cria as tabelas sozinha na primeira subida
-- (JPA com ddl-auto=update) e, com SEED_DEMO=true, carrega o sindico, os moradores,
-- as areas, os avisos, as reservas e as ocorrencias de exemplo.
-- Este script serve pra quem quiser criar o banco na mao ou conferir o modelo.

CREATE DATABASE IF NOT EXISTS jcondo
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE jcondo;

-- moradores (cadastro da Entrega 1)
CREATE TABLE IF NOT EXISTS moradores (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    nome        VARCHAR(100) NOT NULL,
    apartamento VARCHAR(10)  NOT NULL,
    bloco       VARCHAR(10)  NOT NULL,
    cpf         VARCHAR(14)  NOT NULL,
    telefone    VARCHAR(20)  NOT NULL,
    email       VARCHAR(100) NOT NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB;

-- login do sistema. O morador tem um usuario vinculado, o sindico nao
-- senha guardada com hash BCrypt
CREATE TABLE IF NOT EXISTS usuarios (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    nome       VARCHAR(100) NOT NULL,
    email      VARCHAR(100) NOT NULL,
    senha      VARCHAR(100) NOT NULL,
    perfil     ENUM ('MORADOR','SINDICO') NOT NULL,
    ativo      BIT          NOT NULL,
    morador_id BIGINT,
    PRIMARY KEY (id),
    CONSTRAINT uk_usuarios_email UNIQUE (email),
    CONSTRAINT uk_usuarios_morador UNIQUE (morador_id),
    CONSTRAINT fk_usuarios_morador FOREIGN KEY (morador_id) REFERENCES moradores (id)
) ENGINE = InnoDB;

-- mural de avisos publicado pelo sindico
CREATE TABLE IF NOT EXISTS avisos (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    titulo       VARCHAR(120)  NOT NULL,
    conteudo     VARCHAR(2000) NOT NULL,
    categoria    ENUM ('ASSEMBLEIA','EVENTO','GERAL','MANUTENCAO','SEGURANCA') NOT NULL,
    importante   BIT           NOT NULL,
    publicado_em DATETIME(6)   NOT NULL,
    autor_id     BIGINT,
    PRIMARY KEY (id),
    CONSTRAINT fk_avisos_autor FOREIGN KEY (autor_id) REFERENCES usuarios (id)
) ENGINE = InnoDB;

-- areas comuns que podem ser reservadas
CREATE TABLE IF NOT EXISTS areas_comuns (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    nome            VARCHAR(60)  NOT NULL,
    descricao       VARCHAR(255),
    capacidade      INT          NOT NULL,
    hora_abertura   TIME(6)      NOT NULL,
    hora_fechamento TIME(6)      NOT NULL,
    ativa           BIT          NOT NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB;

-- reservas. A regra de conflito de horario fica no ReservaService
CREATE TABLE IF NOT EXISTS reservas (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    area_id      BIGINT       NOT NULL,
    morador_id   BIGINT       NOT NULL,
    data_reserva DATE         NOT NULL,
    hora_inicio  TIME(6)      NOT NULL,
    hora_fim     TIME(6)      NOT NULL,
    observacao   VARCHAR(255),
    status       ENUM ('CANCELADA','CONFIRMADA') NOT NULL,
    criada_em    DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_reservas_area FOREIGN KEY (area_id) REFERENCES areas_comuns (id),
    CONSTRAINT fk_reservas_morador FOREIGN KEY (morador_id) REFERENCES moradores (id),
    INDEX idx_reservas_area_data (area_id, data_reserva)
) ENGINE = InnoDB;

-- ocorrencias abertas pelos moradores
CREATE TABLE IF NOT EXISTS ocorrencias (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    morador_id    BIGINT        NOT NULL,
    titulo        VARCHAR(120)  NOT NULL,
    descricao     VARCHAR(2000) NOT NULL,
    categoria     ENUM ('BARULHO','LIMPEZA','MANUTENCAO','OUTROS','SEGURANCA') NOT NULL,
    status        ENUM ('ABERTA','EM_ANDAMENTO','RESOLVIDA') NOT NULL,
    resposta      VARCHAR(1000),
    aberta_em     DATETIME(6)   NOT NULL,
    atualizada_em DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_ocorrencias_morador FOREIGN KEY (morador_id) REFERENCES moradores (id)
) ENGINE = InnoDB;
