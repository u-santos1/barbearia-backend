package agendamentoDeClienteBarbearia.infra.security;


public class ValidacaoException extends RuntimeException {
    public ValidacaoException(String mensagem) {
        super(mensagem);
    }
}