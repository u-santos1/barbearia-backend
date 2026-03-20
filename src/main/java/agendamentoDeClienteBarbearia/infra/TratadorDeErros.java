package agendamentoDeClienteBarbearia.infra;


import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class TratadorDeErros {

    // Trata erros de validação (@Valid - campos nulos, email inválido)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity tratarErro400(MethodArgumentNotValidException ex) {
        var erros = ex.getFieldErrors();
        return ResponseEntity.badRequest().body(erros.stream().map(DadosErroValidacao::new).toList());
    }

    // Trata nossas regras de negócio (Conflito de horário, data passada)
    @ExceptionHandler(RegraDeNegocioException.class)
    public ResponseEntity tratarRegraDeNegocio(RegraDeNegocioException ex) {
        return ResponseEntity.badRequest().body(new DadosErroSimples(ex.getMessage()));
    }
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity tratarErro404(EntityNotFoundException exception){
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new DadosErroSimples(exception.getMessage()));
    }
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity tratarErro401(BadCredentialsException exception){
        log.warn("Tentativa de acesso nao autorizada {}", exception.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new DadosErroSimples(exception.getMessage()));
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity tratarErro500(Exception e){
        log.error("Erro interno: ", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new DadosErroSimples("Erro interno. Tente novamente mais tarde"));
    }

    // DTOs internos para a resposta de erro
    private record DadosErroValidacao(String campo, String mensagem) {
        public DadosErroValidacao(FieldError erro) {
            this(erro.getField(), erro.getDefaultMessage());
        }
    }


    private record DadosErroSimples(String mensagem) {}
}