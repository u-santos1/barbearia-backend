package agendamentoDeClienteBarbearia.service;



import agendamentoDeClienteBarbearia.dtos.RelatorioFinanceiroCompletoDTO;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;

@Service
public class GeradorPdfService {

    @Autowired
    private TemplateEngine templateEngine;

    public byte[] gerarRelatorioFinanceiroPdf(RelatorioFinanceiroCompletoDTO dadosFinanceiros, String dataInicio, String dataFim) {
        Context context = new Context();
        context.setVariable("dataInicio", dataInicio);
        context.setVariable("dataFim", dataFim);
        context.setVariable("receitaTotal", dadosFinanceiros.totalFaturamento());
        context.setVariable("lucroCasa", dadosFinanceiros.totalCasa());
        context.setVariable("totalComissoes", dadosFinanceiros.totalComissoes());
        context.setVariable("quantidadeAtendimentos", dadosFinanceiros.quantidadeAtendimentos());
        context.setVariable("extrato", dadosFinanceiros.extrato());

        String htmlProcessado = templateEngine.process("relatorio", context);

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(htmlProcessado, null);
            builder.toStream(outputStream);
            builder.run();

            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar o PDF", e);
        }
    }
}