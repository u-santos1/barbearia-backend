package agendamentoDeClienteBarbearia.dtos;

import jakarta.validation.constraints.*;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CadastroBarbeiroDTO(

        @NotBlank(message = "Nome é obrigatório")
        String nome,

        @NotBlank(message = "E-mail é obrigatório")
        @Email(message = "Formato de e-mail inválido")
        String email,

        @NotBlank(message = "A senha é obrigatória")
        @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!_]).{8,}$",
                message = "A senha deve ter no mínimo 8 caracteres, contendo letras maiúsculas, minúsculas, números e caracteres especiais")
        String senha,

        @NotBlank(message = "A especialidade é obrigatória")
        String especialidade, // Ex: "Corte", "Barba", "Colorimetria"

        @NotBlank(message = "O WhatsApp de contato é obrigatório")
        @Pattern(regexp = "\\d{10,11}", message = "O WhatsApp deve conter apenas números, entre 10 e 11 dígitos (com DDD)")
        String whatsappContato,

        Boolean vaiCortarCabelo,
             // Para saber se agenda serviços
        Double comissaoPorcentagem    // Para o cálculo financeiro (ex: 50.0)
) {}