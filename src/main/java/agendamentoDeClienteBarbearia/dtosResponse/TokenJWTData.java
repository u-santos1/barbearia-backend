package agendamentoDeClienteBarbearia.dtosResponse;



public record TokenJWTData(
        String token,
        Long id,        // Necessário para buscar "meus agendamentos"
        String nome,
        String email,
        String perfil,   // Necessário para o Front saber se mostra o Dashboard de Dono
        String barbeariaNome // Para mostrar no topo do painel
) {}