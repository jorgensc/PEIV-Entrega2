-- JCondo - script do banco de dados
-- Testado no MySQL do XAMPP (importar pelo phpMyAdmin ou rodar no terminal)

CREATE DATABASE IF NOT EXISTS jcondo
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE jcondo;

CREATE TABLE IF NOT EXISTS moradores (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    nome        VARCHAR(100) NOT NULL,
    apartamento VARCHAR(10)  NOT NULL,
    bloco       VARCHAR(10)  NOT NULL,
    cpf         VARCHAR(14)  NOT NULL,
    telefone    VARCHAR(20)  NOT NULL,
    email       VARCHAR(100) NOT NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- alguns registros de exemplo pra tela nao comecar vazia
INSERT INTO moradores (nome, apartamento, bloco, cpf, telefone, email) VALUES
('Ana Paula Souza',      '101', 'A', '123.456.789-01', '(49) 99911-1111', 'ana.souza@email.com'),
('Bruno Oliveira Lima',  '202', 'B', '234.567.890-12', '(49) 99922-2222', 'bruno.lima@email.com'),
('Carla Mendes Rocha',   '303', 'A', '345.678.901-23', '(49) 99933-3333', 'carla.rocha@email.com'),
('Diego Ferreira Alves', '104', 'C', '456.789.012-34', '(49) 99944-4444', 'diego.alves@email.com'),
('Eduarda Costa Martins','205', 'B', '567.890.123-45', '(49) 99955-5555', 'eduarda.martins@email.com');
