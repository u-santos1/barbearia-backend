package agendamentoDeClienteBarbearia.dtos;

public record ResumoFinanceiroDTO(
        Double faturamentoTotal,
        Double lucroCasa,
        Double repasseBarbeiros,
        Integer totalCortes
) {}
