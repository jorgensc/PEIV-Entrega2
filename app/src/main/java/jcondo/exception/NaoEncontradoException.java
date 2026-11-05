package jcondo.exception;

// registro que nao existe (vira 404 na API)
public class NaoEncontradoException extends RuntimeException {

    public NaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
