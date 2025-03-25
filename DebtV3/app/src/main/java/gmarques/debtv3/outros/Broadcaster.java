package gmarques.debtv3.outros;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import gmarques.debtv3.Debt;

/**
 * Criado por Gilian Marques
 * Domingo, 04 de Agosto de 2019  as 22:31:17.
 */
public class Broadcaster {


    public static void registrar(final String action, final Callback callback) {
        //cancel this action in case it's been set up earlier
        cancel(action);

        final BroadcastReceiver broadcaster = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //     Log.d(Debt.myFuckingUniqueTAG + "Broadcaster", "onReceive: calling " + action);
                callback.execute();
            }
        };

        LocalBroadcastManager.getInstance(Debt.binder.get().getApplicationContext()).registerReceiver(broadcaster, new IntentFilter(action));

        LocalBroadcastManager.getInstance(Debt.binder.get().getApplicationContext()).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(Tag.AppTag + "Broadcaster", "onReceive: cancelando registro anterior -> " + action + " <-");
                LocalBroadcastManager.getInstance(Debt.binder.get().getApplicationContext()).unregisterReceiver(broadcaster);
                LocalBroadcastManager.getInstance(Debt.binder.get().getApplicationContext()).unregisterReceiver(this);
            }
        }, new IntentFilter(action + "_cancelar"));

    }

    public static void enviar(String... actions) {
        for (String action : actions)
            LocalBroadcastManager.getInstance(Debt.binder.get().getApplicationContext()).sendBroadcast(new Intent(action));
    }



    public static void cancel(String action) {
        LocalBroadcastManager.getInstance(Debt.binder.get().getApplicationContext()).sendBroadcast(new Intent(action + "_cancelar"));
    }

    public static final String atualizarFragDespesas = "atualizarFragDespesas";
    public static final String atualizarFragNotas = "atualizarFragNotas";
    public static final String atualizarFragContas = "atualizarFragContas";
    public static final String atualizarFragCategorias = "atualizarFragCategorias";
    public static final String atualizarGraficoDeBarras = "atualizarGraficoDeBarras";
    public static final String atualizarCardsDeDados = "atualizarCardsDeDados";
    public static final String sincronismoConcluido = "sincronismoConcluido";
    public static final String fecharTodasActivities = "fecharTodasActivities";

    public static void atualizarMainActivity() {
        enviar(atualizarFragDespesas, atualizarFragCategorias, atualizarGraficoDeBarras, atualizarCardsDeDados, atualizarFragContas,atualizarFragNotas);
    }

    public interface Callback {
        void execute();
    }
}
