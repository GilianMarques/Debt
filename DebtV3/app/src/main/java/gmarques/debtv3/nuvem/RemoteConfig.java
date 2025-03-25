package gmarques.debtv3.nuvem;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

import gmarques.debtv3.Debt;
import gmarques.debtv3.outros.Tag;

public class RemoteConfig {

    private static final String RemConfigListenerIntentFilter = "RemConfigListener";

    public static String ultimaAttDosTermosDeUso = "ultimaAtualizacaoDosTermosDeUso";
    public static String bloquearVersoesAbaixoDe = "bloquearVersoesAbaixoDe";
    public static String versaoAtiva = "versaoAtiva";
    public static String senhaAdm = "senhaAdm";
    public static String diferencaAceitavel = "diferencaAceitavelEntreDataLocalENuvem";
    public static String minImpressoesAtePodeSuspendeAnuncios = "minImpressoesAtePodeSuspendeAnuncios";

    private Context context;
    private FirebaseRemoteConfig mFirebaseRemoteConfig;

    private boolean rcCarregadas, sucesso;

    public RemoteConfig(Context context) {
        this.context = context;
        inicializar();

    }

    private void inicializar() {
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        mFirebaseRemoteConfig.fetch(60).addOnCompleteListener(task -> {
            rcCarregadas = true;
            if (task.isSuccessful()) {
                mFirebaseRemoteConfig.activate().addOnSuccessListener(aBoolean -> {
                    Log.d(Tag.AppTag, "RemoteConfig.inicializar: remote config carregado:");
                    sucesso = true;
                    LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(RemConfigListenerIntentFilter).putExtra("Sucesso", true));
                });
            } else {
                Log.d(Tag.AppTag, "RemoteConfig.inicializar: falha ao carregar remote config");
                sucesso = false;
                LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(RemConfigListenerIntentFilter).putExtra("Sucesso", false));
            }
        });
    }

    /*Um vez que este callback é chamado pelo broadcast ele se desregistra automaticamente*/
    public abstract static class RConfigListener extends BroadcastReceiver {


        public RConfigListener(Context context) {
            /*Se as configuraçoes ja foram carregadas, chamo o listener imediatamente com os dados*/
            if (Debt.binder.get().remoteConfig.rcCarregadas) {
                configuraçoesCarregadas(Debt.binder.get().remoteConfig.sucesso, Debt.binder.get().remoteConfig.mFirebaseRemoteConfig);
            } else {
                LocalBroadcastManager.getInstance(context).registerReceiver(this, new IntentFilter(RemConfigListenerIntentFilter));
            }
        }

        @Override
        public final void onReceive(Context context, Intent intent) {
            LocalBroadcastManager.getInstance(context).unregisterReceiver(this);
            boolean sucesso = intent.getBooleanExtra("Sucesso", false);
            configuraçoesCarregadas(sucesso, Debt.binder.get().remoteConfig.mFirebaseRemoteConfig);

        }

        protected abstract void configuraçoesCarregadas(boolean sucesso, FirebaseRemoteConfig remoteConfig);
    }

}
