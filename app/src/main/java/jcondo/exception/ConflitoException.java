package jcondo.exception;

// conflito com dado que ja existe, ex: horario reservado ou e-mail repetido (vira 409)
public class ConflitoException extends RuntimeException {

    public ConflitoException(String mensagem) {
        super(mensagem);
    }
}
