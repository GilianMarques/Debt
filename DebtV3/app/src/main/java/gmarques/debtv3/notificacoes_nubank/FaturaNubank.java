package gmarques.debtv3.notificacoes_nubank;

import static android.app.PendingIntent.FLAG_IMMUTABLE;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.text.Html;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;

import com.google.gson.GsonBuilder;
import com.pixplicity.easyprefs.library.Prefs;

import org.joda.time.LocalDate;

import java.math.BigDecimal;

import gmarques.debtv3.Debt;
import gmarques.debtv3.R;
import gmarques.debtv3.activities.dashboard.MainActivity;
import gmarques.debtv3.gestores.Meses;
import gmarques.debtv3.interface_.FormatUtils;
import gmarques.debtv3.modelos.Despesa;
import gmarques.debtv3.modelos.Mes;
import gmarques.debtv3.outros.Tag;
import gmarques.debtv3.utilitarios.C;

import static gmarques.debtv3.nuvem.FBaseNomes.dados;

/**
 * Notifica o usuario sobre compras feitas no cartao e executa o codigo pra atualizar a despesa referente a fatura
 * o ajudando a manter a fatura do nubank atualizada aqui no app
 */
@SuppressWarnings("ConstantConditions")
public class FaturaNubank {

    private Context contexto;

    private final NotificationManager mNotManager;
    private final String canalPadrao = "canalPadrao";
    private BigDecimal balanço = new BigDecimal("0");

    private Despesa nubank;
    private boolean erroDeDespesa;// nao foi encontrada despesa referente a fatura nubank
    private boolean faturaFechada;

    public FaturaNubank() {
        contexto = Debt.binder.get();
        mNotManager = (NotificationManager) contexto.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    public void notificarCredito(float valorNotificacao, boolean estorno) {

        if (valorNotificacao == -1) {
            notificarSobreFatura(contexto.getString(R.string.Naoconseguimosobterovalordatransaçaoparaatualizarsuafatura));
        } else {
            atualizarFatura(valorNotificacao, estorno);
            String texto = carregarDadosDasMovimentaçoes(valorNotificacao, estorno);
            notificarSobreFatura(texto);
        }

        Log.d(Tag.AppTag, "FaturaNubank.notificarSeNecessario: ---------------------------------------");
        if (dados != null)
            Log.d(Tag.AppTag, "FaturaNubank.notificarSeNecessario: " + new GsonBuilder().setPrettyPrinting().create().toJson(dados));
        Log.d(Tag.AppTag, "FaturaNubank.notificarSeNecessario: ---------------------------------------");
    }

    /**
     * Localiza a despesa referente ao nubank no mes atual e a atualiza com o valor das compras feitas hoje
     *
     * @param valorNotificacao valorNotificacao
     * @param estorno          estorno
     */
    private void atualizarFatura(float valorNotificacao, boolean estorno) {

        //pode ser o mes atual ou nao
        Mes mesDaDespesa = Meses.getMes(new LocalDate());// obtem  mes atual de acordo com a data pra caso o app estja aberto e com um outro mes selecionado como atual

        nubank = null;
        String nomeDespesa = Prefs.getString(C.nomeDespesaNubank, "");


        // a fatura ja fechou tenho que atualizar a despesa do prox mes
        if (new LocalDate().getDayOfMonth() > Prefs.getInt(C.dataDespesaNubank, 10)) {
            faturaFechada = true;
            Mes proxMes = Meses.getMes(new LocalDate(mesDaDespesa.getAno(), mesDaDespesa.getMes(), 1).plusMonths(1));

            if (proxMes == null) {
                erroDeDespesa = true;
                Log.d(Tag.AppTag, "FaturaNubank.atualizarFatura: A fatura ja fechou e nao foi possivel achar a fatura do mes seguinte para atualizar ");
                return;
            } else {
                mesDaDespesa = proxMes;
                Log.d(Tag.AppTag, "FaturaNubank.atualizarFatura: a fatura ja fechou, buscando a fatura do prox mes");
            }
        }


        for (Despesa despesa : mesDaDespesa.getDespesas()) {
            //      Log.d(Tag.AppTag, "FaturaNubank.atualizarFatura: " + nomeDespesa + " " + despesa.getNome() + " " + mesDaDespesa.getMes() + " " + mesDaDespesa.getAno());
            if (despesa.getNome().equals(nomeDespesa)) {
                nubank = despesa;
                break;
            }
        }
        if (nubank != null) {

            BigDecimal valorDaCompra = new BigDecimal(valorNotificacao);
            if (estorno)
                nubank.setValor(BigDecimal.valueOf(nubank.getValor()).subtract(valorDaCompra).floatValue());
            else
                nubank.setValor(BigDecimal.valueOf(nubank.getValor()).add(valorDaCompra).floatValue());
            mesDaDespesa.attDespesa(nubank);

        } else erroDeDespesa = true;

    }

    private String carregarDadosDasMovimentaçoes(float valorDaNotificaçao, boolean estorno) {

        String texto = "";

        if (erroDeDespesa) {
            texto += "Não encontramos uma despesa com o nome '" + Prefs.getString(C.nomeDespesaNubank, "'não definido'") + "' para atualizar.</b>";
        } else {
            if (estorno)
                texto += "Estorno de <b>" + FormatUtils.emReal(valorDaNotificaçao) + "</b>  já consta na fatura. O novo valor é <b>" + FormatUtils.emReal(nubank.getValor()) + "</b>";
            else
                texto += "Sua compra de <b>" + FormatUtils.emReal(valorDaNotificaçao) + "</b>  já consta na fatura. O novo valor é <b>" + FormatUtils.emReal(nubank.getValor()) + "</b>";
            Log.d(Tag.AppTag, "FaturaNubank.carregarDadosDasMovimentaçoes: tudo certo pra notificar: " + nubank.toString());
        }


        return texto;
    }

    private void notificarSobreFatura(String mensagem) {


        //adicionando açao ao clicar na notificaçao
        Intent intent = new Intent(contexto, MainActivity.class);


        TaskStackBuilder tsb = TaskStackBuilder.create(contexto);
        tsb.addNextIntent(intent);
        // deve ser um numero
        String id = "99";
        PendingIntent pi = tsb.getPendingIntent(Integer.parseInt(id), PendingIntent.FLAG_UPDATE_CURRENT| FLAG_IMMUTABLE);


        NotificationCompat.Builder nBuilder = new NotificationCompat.Builder(contexto, id);
        String titulo = contexto.getString(R.string.Faturanubank) + (faturaFechada ? " (fechada)" : "");
        nBuilder.setContentTitle(titulo);
        nBuilder.setContentText(Html.fromHtml(mensagem, Html.FROM_HTML_MODE_COMPACT));
        nBuilder.setAutoCancel(true);
        nBuilder.setSmallIcon(R.drawable.vec_notificacao_info);


        nBuilder.setContentIntent(pi);
        nBuilder.setStyle(new NotificationCompat.BigTextStyle());
        nBuilder.setPriority(NotificationCompat.PRIORITY_HIGH);

        Notification notification = nBuilder.build();

        NotificationChannel mChannel = new NotificationChannel(id, canalPadrao, NotificationManager.IMPORTANCE_DEFAULT);
        mChannel.enableVibration(true);
        mChannel.enableLights(true);
        mChannel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PRIVATE);
        mNotManager.createNotificationChannel(mChannel);
        nBuilder.setChannelId(id);


        mNotManager.notify(Integer.parseInt(id), notification);
    }

}
