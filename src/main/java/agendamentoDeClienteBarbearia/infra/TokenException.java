package agendamentoDeClienteBarbearia.infra;

public class TokenException extends RuntimeException {
    public TokenException(String message) {
        super(message);
    }
    public TokenException(String mensagem, Throwable throwable){
        super(mensagem, throwable);
    }
}
