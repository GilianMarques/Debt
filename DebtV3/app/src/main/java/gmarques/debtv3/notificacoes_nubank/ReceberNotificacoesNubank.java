package gmarques.debtv3.notificacoes_nubank;

import android.os.Bundle;
import android.os.Handler;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.pixplicity.easyprefs.library.Prefs;

import gmarques.debtv3.BuildConfig;
import gmarques.debtv3.interface_.FormatUtils;
import gmarques.debtv3.outros.Tag;
import gmarques.debtv3.utilitarios.C;

public class ReceberNotificacoesNubank extends NotificationListenerService {
/*
*   :: com.nu.production  ;; Transferência recebida
    Você recebeu uma transferência de R$ 10,00 de Elaine Morais da Silva Queiroz.
    *
    *
* */

    private String mensagem;
    private String titulo;
    private long stamp;
    // a id das notificações muda toda vez... fiz 2 testes. só da pra confiar no pacote
    //id da notificaçao de compra -1212332469, -1322715052
    //id da notificaçao de estorno -1259573501, 1568845914
    // pacote com.nu.production

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        super.onNotificationPosted(sbn);
        String idDoApp = sbn.getPackageName();
//        if (idDoApp.equals("gmarques.debtv3")) return;
        if (!BuildConfig.DEBUG && !idDoApp.equals("com.nu.production")) return;

        carregarDadosNotificaçao(sbn);

        if (NotificacaoDuplicada()) return;

        // como nao da pra confiar na id das notificaçoes tenho que checar se sao as que eu quero por palavras chave
        if (titulo.equals("Compra no débito aprovada"))
            operaçaoEmDebito();
        else if (mensagem.contains("APROVADA") || mensagem.contains("estornada"))
            operaçaoEmCredito();


    }

    /**
     * Este metodo tem como proposito impedir que notificaçoes multiplas do nubank afetem os dados no app.
     * Ele compara a notificaçao recebida no momento atual com a ultima ocorrido verificando titulo, mensagem e data
     * se certificando de que se  o nubank  emitiu a mesma notiuficaçao 2 duas vezes ele n atualize a despesa tambem
     * duas vezes
     * <p>
     * Tambem salva os dados da notificação atual nas preferencias pra verificar posteriormente
     *
     * @return true se a notificação é repetida
     */
    private boolean NotificacaoDuplicada() {

        // "P" se refere ao Preferencia ou prefs

        String pTitulo = Prefs.getString(C.nubankUltimoTitulo, null);
        String pMensagem = Prefs.getString(C.nubankUltimaMensagem, null);
        long pStamp = Prefs.getLong(C.nubankUltimaStamp, 0);

        /* Salvar os dados da notificaçao atual nas prefs ja pra verificar na proxima vez
         */
        Prefs.putString(C.nubankUltimoTitulo, titulo);
        Prefs.putString(C.nubankUltimaMensagem, mensagem);
        Prefs.putLong(C.nubankUltimaStamp, stamp);

        /*
          Verificar notificação
        */
        // ainda nao ha registro de notficaçoes nas preferencias
        if (pTitulo == null && pMensagem == null) return false;

        // A notificação pode ser repetida apenas depois de um intervalo de 10 segundos. Se esse intervalo ja se passou n preciso nem verificar o titulo e a mensagem
        // as notificaçoes repetidas ocorrem em fraçao de segundo entao 10 segundos é uma folga
        // muito boa, alem do mais o usuario pode comprar o mesmo produto logo em seguida
        if ((stamp - pStamp) > 10000) return false;

        return pTitulo.equals(titulo) && pMensagem.equals(mensagem);
    }

    private void operaçaoEmDebito() {

    }

    private void operaçaoEmCredito() {

        //Se valorNotificacao for ==-1, houve um erro ao ler o valor da notificação
        float valorNotificacao = obterValorDaNotificaçao(mensagem);
        boolean estorno = mensagem.toLowerCase().contains("estornada");

        new Handler().postDelayed(() -> {
            new FaturaNubank().notificarCredito(valorNotificacao, estorno);
        }, 2000);
    }


    private void carregarDadosNotificaçao(StatusBarNotification sbn) {

        Bundle extras = sbn.getNotification().extras;
        mensagem = (extras.getCharSequence("android.text") == null ? "" : extras.getCharSequence("android.text")).toString();
        titulo = (extras.getCharSequence("android.title") == null ? "" : extras.getCharSequence("android.title")).toString();
        stamp = sbn.getPostTime();
        Log.d(Tag.AppTag, "ReceberNotificacoesNubank.carregarDadosNotificaçao:  :: " + sbn.getPackageName() + "  ;; " + titulo + "\n" + mensagem);
        Log.d(Tag.AppTag, "ReceberNotificacoesNubank.carregarDadosNotificaçao: ------------------------------------");
    }


    private float obterValorDaNotificaçao(String msg) {

        try {
            String localMensagem = msg.toLowerCase();
            //Compra de R$ 10,00 APROVADA em 'estabelecimento'
            // A compra em 'estabelecimento' no valor de R$ 10,00 foi estornada.
            String strValor = localMensagem.split("r\\$")[1];
            strValor = strValor.split(" ")[1];
            return FormatUtils.emDecimal(strValor).floatValue();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return -1;
    }


}
