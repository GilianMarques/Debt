package gmarques.debtv3.broadcast_e_servicos;

import static android.app.PendingIntent.FLAG_IMMUTABLE;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;

import org.joda.time.LocalDate;

import java.util.ArrayList;
import java.util.Collections;

import gmarques.debtv3.Debt;
import gmarques.debtv3.R;
import gmarques.debtv3.activities.relatorio_do_dia.RelatorioDoDiaAct;
import gmarques.debtv3.modelos.Despesa;
import gmarques.debtv3.modelos.Receita;
import gmarques.debtv3.sincronismo.api.Sincronizavel;
import io.realm.RealmList;

import static gmarques.debtv3.gestores.Meses.mesAtual;

@SuppressWarnings("ConstantConditions")
public class RelatorioDoDia {

    private final RealmList<Receita> receitas;
    private RealmList<Despesa> despesas;
    private long hojeMilis;
    private Context contexto;
    private final String canalPadrao = "canalPadrao";
    private NotificationManager mNotManager;

    public RelatorioDoDia() {
        contexto = Debt.binder.get().getApplicationContext();
        mNotManager = (NotificationManager) contexto.getSystemService(Context.NOTIFICATION_SERVICE);
        despesas = mesAtual.getDespesas();
        receitas = mesAtual.getReceitas();

        Collections.sort(despesas, (o1, o2) -> (int) (o1.getDataDePagamento() - o2.getDataDePagamento()));

        Collections.sort(receitas, (o1, o2) -> (int) (o1.getDataDeRecebimento() - o2.getDataDeRecebimento()));

        LocalDate hoje = new LocalDate();
        hojeMilis = hoje.toDate().getTime();
    }

    public void notificarSeNecessario() {

        RealmList<Despesa> despPraHoje = carregarDespesasPraHoje();
        if (despPraHoje.size() > 0) {
            notificarSobreORelatorioDoDia();
        } else {
            RealmList<Receita> recPraHoje = carregarReceitasPraHoje();
            if (recPraHoje.size() > 0) notificarSobreORelatorioDoDia();
        }

    }

    public ArrayList<Sincronizavel> getPendencias() {

        RealmList<Despesa> despPraHoje = carregarDespesasPraHoje();
        RealmList<Receita> recPraHoje = carregarReceitasPraHoje();

        ArrayList<Sincronizavel> sincronizavels = new ArrayList<>();

        sincronizavels.addAll(recPraHoje);
        sincronizavels.addAll(despPraHoje);

        return sincronizavels;


    }

    private RealmList<Despesa> carregarDespesasPraHoje() {
        RealmList<Despesa> desps = new RealmList<>();
        for (Despesa despesa : despesas)
            if (!despesa.estaPaga())
                if (despesa.getDataDePagamento() <= hojeMilis) desps.add(despesa);
        return desps;
    }

    private RealmList<Receita> carregarReceitasPraHoje() {
        RealmList<Receita> recs = new RealmList<>();
        for (Receita receita : receitas)
            if (!receita.estaRecebido())
                if (receita.getDataDeRecebimento() <= hojeMilis) recs.add(receita);
        return recs;
    }

    private void notificarSobreORelatorioDoDia() {


        //adicionando açao ao clicar na notificaçao
        Intent intent = new Intent(contexto, RelatorioDoDiaAct.class);


        TaskStackBuilder tsb = TaskStackBuilder.create(contexto);
        tsb.addNextIntent(intent);
        // deve ser um numero
        String id = "3";
        PendingIntent pi = tsb.getPendingIntent(Integer.parseInt(id), PendingIntent.FLAG_UPDATE_CURRENT| FLAG_IMMUTABLE);


        NotificationCompat.Builder nBuilder = new NotificationCompat.Builder(contexto, id);
        nBuilder.setContentTitle(contexto.getString(R.string.Relatoriododia));
        nBuilder.setContentText(contexto.getString(R.string.Atualizeoappcomasmodificacoesfeitashoje));
        nBuilder.setAutoCancel(true);
        nBuilder.setSmallIcon(R.drawable.vec_notificacao_info);


        nBuilder.setContentIntent(pi);
        nBuilder.setOngoing(true);
        nBuilder.setStyle(new NotificationCompat.BigTextStyle());
        nBuilder.setPriority(NotificationCompat.PRIORITY_HIGH);

        Notification notification = nBuilder.build();

        NotificationChannel mChannel = new NotificationChannel(id, canalPadrao, NotificationManager.IMPORTANCE_DEFAULT);
        mChannel.enableVibration(true);
        mChannel.enableLights(true);
        mChannel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        mNotManager.createNotificationChannel(mChannel);
        nBuilder.setChannelId(id);


        mNotManager.notify(Integer.parseInt(id), notification);
    }


}
