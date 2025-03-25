package gmarques.debtv3.broadcast_e_servicos;

import static android.app.PendingIntent.FLAG_IMMUTABLE;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.text.Html;

import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;

import org.joda.time.LocalDate;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import gmarques.debtv3.Debt;
import gmarques.debtv3.R;
import gmarques.debtv3.activities.dashboard.MainActivity;
import gmarques.debtv3.modelos.Despesa;
import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmResults;

@SuppressWarnings("ConstantConditions")
public class Notificador {

    private List<Despesa> despesas;
    private long hojeMilis;
    private Context contexto;
    private final String canalPadrao = "canalPadrao";
    private NotificationManager mNotManager;

    public Notificador() {
        contexto = Debt.binder.get().getApplicationContext();
        mNotManager = (NotificationManager) contexto.getSystemService(Context.NOTIFICATION_SERVICE);

        Realm realm = Realm.getDefaultInstance();

        /*pego todas as despesas */
        RealmResults<Despesa> rDesp = realm.where(Despesa.class)
                .equalTo("removido", false)
                .and()
                .notEqualTo("mesId", Despesa.RECORRENTE)
                .and()
                .notEqualTo("mesId", Despesa.PARCELADA)
                .findAll();
        despesas = realm.copyFromRealm(rDesp);
        realm.close();

        Collections.sort(despesas, (o1, o2) -> Long.compare(o1.getDataDePagamento(), o2.getDataDePagamento()));

        LocalDate hoje = new LocalDate();
        hojeMilis = hoje.toDate().getTime();
    }

    public void notificarSeNecessario() {

        RealmList<Despesa> despEmAtraso = carregarDespesasEmAtraso();
        if (despEmAtraso.size() > 0) notificarSobreDespesasEmAtraso(despEmAtraso);

        RealmList<Despesa> proxDesp = carregarProximasDespesas();
        if (proxDesp.size() > 0) notificarSobreProximasDespesas(proxDesp);

        RealmList<Despesa> praHj = carregarDespesasDeHoje();
        if (praHj.size() > 0) notificarSobreDespesasDehoje(praHj);
    }

    private RealmList<Despesa> carregarDespesasEmAtraso() {
        RealmList<Despesa> despesasEmAtraso = new RealmList<>();
        for (Despesa despesa : despesas)
            if (!despesa.estaPaga())
                if ( (despesa.getDataDePagamento()) < hojeMilis)
                    despesasEmAtraso.add(despesa);
        return despesasEmAtraso;
    }

    private void notificarSobreDespesasEmAtraso(RealmList<Despesa> despEmAtraso) {
        String titulo = MessageFormat.format(contexto.getString(R.string.Vocetemxdespesaspendentes), despEmAtraso.size());
        String msg;

        if (despEmAtraso.size() == 1) msg = despEmAtraso.get(0).getNome();
        else if (despEmAtraso.size() == 2)
            msg = despEmAtraso.get(0).getNome() + " e " + despEmAtraso.get(1).getNome();
        else if (despEmAtraso.size() == 3)
            msg = despEmAtraso.get(0).getNome() + ", " + despEmAtraso.get(1).getNome() + " e " + despEmAtraso.get(2).getNome();
        else
            msg = despEmAtraso.get(0).getNome() + ", " + despEmAtraso.get(1).getNome() + " e outras " + (despEmAtraso.size() - 2) + " despesas";


        //adicionando açao ao clicar na notificaçao
        Intent intent = new Intent(contexto, MainActivity.class);


        TaskStackBuilder tsb = TaskStackBuilder.create(contexto);
        tsb.addNextIntent(intent);
        // deve ser um numero
        String despEmAtrasoId = "1";
        PendingIntent pi = tsb.getPendingIntent(Integer.parseInt(despEmAtrasoId), PendingIntent.FLAG_UPDATE_CURRENT| FLAG_IMMUTABLE);


        NotificationCompat.Builder nBuilder = new NotificationCompat.Builder(contexto, despEmAtrasoId);
        nBuilder.setContentTitle(titulo);
        nBuilder.setContentText(msg);
        nBuilder.setAutoCancel(true);
        nBuilder.setSmallIcon(R.drawable.vec_notificacoes_despesa);


        nBuilder.setContentIntent(pi);
        nBuilder.setOngoing(false);
        nBuilder.setStyle(new NotificationCompat.BigTextStyle());
        nBuilder.setPriority(NotificationCompat.PRIORITY_HIGH);

        Notification notification = nBuilder.build();

        NotificationChannel mChannel = new NotificationChannel(despEmAtrasoId, canalPadrao, NotificationManager.IMPORTANCE_HIGH);
        mChannel.enableVibration(true);
        mChannel.enableLights(true);
        mChannel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PRIVATE);
        mNotManager.createNotificationChannel(mChannel);
        nBuilder.setChannelId(despEmAtrasoId);


        mNotManager.notify(Integer.parseInt(despEmAtrasoId), notification);
    }

    private RealmList<Despesa> carregarProximasDespesas() {
        RealmList<Despesa> proxDesp = new RealmList<>();
        long proxDoisDias = new LocalDate().plusDays(3).toDate().getTime();
        for (Despesa despesa : despesas)
            if (!despesa.estaPaga())
                if ( (despesa.getDataDePagamento()) > hojeMilis &&  (despesa.getDataDePagamento()) <= proxDoisDias)/*vence amanha ou depois de*/
                    proxDesp.add(despesa);
        return proxDesp;
    }

    private void notificarSobreProximasDespesas(RealmList<Despesa> proxDesp) {
        String titulo = MessageFormat.format(contexto.getString(R.string.Despesasparaosproxdias), proxDesp.size());
        String msg;

        if (proxDesp.size() == 1)
            msg = proxDesp.get(0).getNome() + " (" + getNomeDia(proxDesp.get(0)) + ")";
        else if (proxDesp.size() == 2)
            msg = proxDesp.get(0).getNome() + " (" + getNomeDia(proxDesp.get(0)) + ")" + " e " + proxDesp.get(1).getNome() + " (" + getNomeDia(proxDesp.get(1)) + ")";
        else if (proxDesp.size() == 3)
            msg = proxDesp.get(0).getNome() + " (" + getNomeDia(proxDesp.get(0)) + ")" + ", " + proxDesp.get(1).getNome() + " (" + getNomeDia(proxDesp.get(1)) + ")" + " e " + proxDesp.get(2).getNome() + " (" + getNomeDia(proxDesp.get(2)) + ")";
        else
            msg = proxDesp.get(0).getNome() + " (" + getNomeDia(proxDesp.get(0)) + ")" + ", " + proxDesp.get(1).getNome() + " (" + getNomeDia(proxDesp.get(1)) + ")" + " e outras " + (proxDesp.size() - 2) + " despesas";


        boolean temDespesaProfinalDeSemana = false;
        for (Despesa despesa : proxDesp) {
            String nomeDia = getNomeDia(despesa);
            /*sabado e domingo nao tem '-' no nome como segunda-feira, terça-feira, qurta-feira, etc...*/
            if (!nomeDia.contains("-")) {
                temDespesaProfinalDeSemana = true;
                break;
            }
        }


        if (temDespesaProfinalDeSemana)
            msg = msg + ("<br><br><b>Lembre-se de antecipar os pagamentos se necessário.");

        //adicionando açao ao clicar na notificaçao
        Intent intent = new Intent(contexto, MainActivity.class);


        TaskStackBuilder tsb = TaskStackBuilder.create(contexto);
        tsb.addNextIntent(intent);
        String proxDespId = "2";
        PendingIntent pi = tsb.getPendingIntent(Integer.parseInt(proxDespId), PendingIntent.FLAG_UPDATE_CURRENT| FLAG_IMMUTABLE);


        NotificationCompat.Builder nBuilder = new NotificationCompat.Builder(contexto, proxDespId);
        nBuilder.setContentTitle(titulo);
        nBuilder.setContentText(Html.fromHtml(msg, Html.FROM_HTML_MODE_COMPACT));
        nBuilder.setAutoCancel(true);
        nBuilder.setSmallIcon(R.drawable.vec_notificacoes_despesa);


        nBuilder.setContentIntent(pi);
        nBuilder.setOngoing(false);
        nBuilder.setStyle(new NotificationCompat.BigTextStyle());
        nBuilder.setPriority(NotificationCompat.PRIORITY_HIGH);

        Notification notification = nBuilder.build();

        NotificationChannel mChannel = new NotificationChannel(proxDespId, canalPadrao, NotificationManager.IMPORTANCE_HIGH);
        mChannel.enableVibration(true);
        mChannel.enableLights(true);
        mChannel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PRIVATE);
        mNotManager.createNotificationChannel(mChannel);
        nBuilder.setChannelId(proxDespId);


        mNotManager.notify(Integer.parseInt(proxDespId), notification);
    }

    /**
     * retorna o nome do dia em que a despesa vence
     */
    private String getNomeDia(Despesa despesa) {

        return new LocalDate(despesa.getDataDePagamento()).dayOfWeek().getAsText(Locale.getDefault());
    }

    private RealmList<Despesa> carregarDespesasDeHoje() {
        RealmList<Despesa> praHj = new RealmList<>();
        for (Despesa despesa : despesas)
            if (!despesa.estaPaga() &&  (despesa.getDataDePagamento()) == hojeMilis) {
                praHj.add(despesa);
            }
        return praHj;
    }

    private void notificarSobreDespesasDehoje(RealmList<Despesa> hj) {
        String titulo = String.format(Locale.getDefault(), Debt.binder.get().getString(R.string.xDespesasvencemhoje), hj.size());
        String msg;

        if (hj.size() == 1) msg = hj.get(0).getNome();
        else if (hj.size() == 2)
            msg = hj.get(0).getNome() + " e " + hj.get(1).getNome();
        else if (hj.size() == 3)
            msg = hj.get(0).getNome() + ", " + hj.get(1).getNome() + " e " + hj.get(2).getNome();
        else
            msg = hj.get(0).getNome() + ", " + hj.get(1).getNome() + " e outras " + (hj.size() - 2) + " despesas";


        //adicionando açao ao clicar na notificaçao
        Intent intent = new Intent(contexto, MainActivity.class);


        TaskStackBuilder tsb = TaskStackBuilder.create(contexto);
        tsb.addNextIntent(intent);
        String despHjId = "3";
        PendingIntent pi = tsb.getPendingIntent(Integer.parseInt(despHjId), PendingIntent.FLAG_UPDATE_CURRENT| FLAG_IMMUTABLE);


        NotificationCompat.Builder nBuilder = new NotificationCompat.Builder(contexto, despHjId);
        nBuilder.setContentTitle(Html.fromHtml("<b><font color=#E91E63>" + titulo, Html.FROM_HTML_MODE_COMPACT));
        nBuilder.setContentText(msg);
        nBuilder.setAutoCancel(true);
        nBuilder.setSmallIcon(R.drawable.vec_notificacoes_despesa);


        nBuilder.setContentIntent(pi);
        nBuilder.setOngoing(false);
        nBuilder.setStyle(new NotificationCompat.BigTextStyle());
        nBuilder.setPriority(NotificationCompat.PRIORITY_HIGH);

        Notification notification = nBuilder.build();

        NotificationChannel mChannel = new NotificationChannel(despHjId, canalPadrao, NotificationManager.IMPORTANCE_HIGH);
        mChannel.enableVibration(true);
        mChannel.enableLights(true);
        mChannel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PRIVATE);
        mNotManager.createNotificationChannel(mChannel);
        nBuilder.setChannelId(despHjId);


        mNotManager.notify(Integer.parseInt(despHjId), notification);
    }


}
