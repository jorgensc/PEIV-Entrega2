package jcondo.exception;

// regra de negocio violada (vira 422 na API)
public class RegraNegocioException extends RuntimeException {

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
