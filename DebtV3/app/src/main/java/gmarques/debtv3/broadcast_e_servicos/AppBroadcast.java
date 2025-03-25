package gmarques.debtv3.broadcast_e_servicos;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.pixplicity.easyprefs.library.Prefs;

import gmarques.debtv3.outros.Tag;
import gmarques.debtv3.sincronismo.ServicoDeSincronismo;


/**
 * Criado por Gilian Marques
 * Quinta-feira, 12 de Setembro de 2019  as 18:01:49.
 */
public class AppBroadcast extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent.getAction() == null) return;


        Log.d(Tag.AppTag, "AppBroadcast.onReceive: " + intent.getAction());
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {

            GestorDeAlarmes alarmes = new GestorDeAlarmes(context);
            if (!alarmes.estaoAsNotificaçoesLigadas()) {
                alarmes.ligarNotificaçoes();
                alarmes.alternarAutoSinc(true);
            }


        } else if ("notificar_manha".equals(intent.getAction()) || "notificar_tarde".equals(intent.getAction())) {

            new Notificador().notificarSeNecessario();

        } else if ("notificar_relatorio_do_dia".equals(intent.getAction())) {

            new RelatorioDoDia().notificarSeNecessario();

        } else if ("sinc".equals(intent.getAction())) {

            long ultimaTentativa = Prefs.getLong(ServicoDeSincronismo.ultimaTentativaDeSincronismo, 0);
            if ((System.currentTimeMillis() - ultimaTentativa) >= 60 * 60 * 1000/*1 hora*/)
                context.startForegroundService(new Intent(context, ServicoDeSincronismo.class));
            else
                Log.d(Tag.AppTag, "AppBroadcast.onReceive: Sincronismo nao pode ser executrado em intervalos curtos ");
        }


    }


}
