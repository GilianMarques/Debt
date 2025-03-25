package gmarques.debtv3.sincronismo;

import static android.app.PendingIntent.FLAG_IMMUTABLE;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.pixplicity.easyprefs.library.Prefs;

import gmarques.debtv3.Debt;
import gmarques.debtv3.R;
import gmarques.debtv3.especificos.ConexaoComAInternet;
import gmarques.debtv3.especificos.Data;
import gmarques.debtv3.gestores.Meses;
import gmarques.debtv3.interface_.UIUtils;
import gmarques.debtv3.outros.Tag;
import gmarques.debtv3.sincronismo.api.SincAdapter;
import gmarques.debtv3.utilitarios.C;

public class ServicoDeSincronismo extends Service implements SincAdapter.UICallback {
    public static String ultimaTentativaDeSincronismo = "ultimaTentativaDeSincronismo";
    private NotificationManager mNotManager;
    private int id = 123321;
    private static String estadoDoSincronismo = "estadoDoSincronismo";
    public ServicoDeSincronismo() {



    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();


        mNotManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        startForeground(id, criarNotificaçao("Sincronizando", "Por favor, aguarde...", R.drawable.vec_notificacao_info));

        if (!Prefs.getBoolean(C.usuarioAceitouOsTermosDeUso, false)) {
            stopSelf();
        } else {
            verificarConexaoESincronizarSePossivel();
        }
    }

    private void verificarConexaoESincronizarSePossivel() {

        new ConexaoComAInternet().verificar(conectado -> {
            if (conectado) {
                rodarVerfificaçaoPreSincronismo();
            } else feito(false, getString(R.string.Naohaconexaocomainternet));
        });


    }


    private void rodarVerfificaçaoPreSincronismo() {


        Runnable sincronizar = () -> {

            Debt.binder.get().liveSinc.pararDeOuvir();
            SincAdapterImpl sincAdapterImpl = new SincAdapterImpl(ServicoDeSincronismo.this);
            sincAdapterImpl.executar();

        };

           /*antes de fazer o ChecagemPreSincronismo eu verificava com a classe Usuario se estava sincronizando os dados com outra conta
         porém parei de fazer essa verificação porque nem sempre essa informação está disponível nas preferências do aplicativo.
         estva usando aplicativo no celular então fiz logoff e então loguei novamente e quando fez o sincronismo para restaurar os dados
          mesmo estamos sincronizando com uma outra conta, o aplicativo sincronizou com a minha conta porque eu ainda não tinha baixado
          o e-mail do usuario com quem eu sincronizava para as preferências do app, então a classe Usuario sempre retornavam que estava sincronizando com a minha
           própria conta. Moral da história: antes de sincronizar é necessário sempre checar na nuvem se o usuário sincroniza ou não com outro usuário.
            A classe ChecagemPreSincronismo faz essa verificação e atualiza as preferências por que o e-mail do usuário anfitrião pode não estar sempre disponível lá*/
        /*Usuario.estaSincronizandoComOutraConta()*/
        new ChecagemPreSincronismo(new ChecagemPreSincronismo.Callback() {
            @Override
            public void feito() {
                sincronizar.run();
            }

            @Override
            public void falha(String erro) {
                UIUtils.erroToasty(Debt.binder.get().getString(R.string.Naofoipossivelverificarcontadesincronismo) + ": " + erro);
            }
        });


    }

    private Notification criarNotificaçao(String titulo, String msg, int icone) {

        Intent intent = new Intent(this, ServicoDeSincronismo.class);


        TaskStackBuilder tsb = TaskStackBuilder.create(this);
        tsb.addNextIntent(intent);
        String sincServiceId = "4123";
        PendingIntent pi = tsb.getPendingIntent(Integer.parseInt(sincServiceId), PendingIntent.FLAG_UPDATE_CURRENT| FLAG_IMMUTABLE);


        NotificationCompat.Builder nBuilder = new NotificationCompat.Builder(this, sincServiceId);
        nBuilder.setContentTitle(titulo);
        nBuilder.setContentText(msg);
        nBuilder.setAutoCancel(true);
        nBuilder.setSmallIcon(icone);


        nBuilder.setContentIntent(pi);
        nBuilder.setOngoing(false);
        nBuilder.setStyle(new NotificationCompat.BigTextStyle());
        nBuilder.setPriority(NotificationCompat.PRIORITY_HIGH);

        Notification notification = nBuilder.build();

        NotificationChannel mChannel = new NotificationChannel(sincServiceId, "Sincronismo", NotificationManager.IMPORTANCE_NONE);
        mChannel.enableVibration(false);
        mChannel.enableLights(false);

        mChannel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PRIVATE);
        mNotManager.createNotificationChannel(mChannel);
        nBuilder.setChannelId(sincServiceId);


        return nBuilder.build();
    }

    @Override
    public void feito(boolean sucesso, String msg) {

        status("Removendo duplicatas", "Por favor aguarde...");
        new RemovedorDeDuplicatas().executar();

        status("Verificando os meses", "Por favor aguarde...");
        /*caso novos meses relacionados a despesas anteriores ao mês atual sejam adicionados no banco
         de dados a interface não será atualizada isso não é um bug, é preguiça. A mensagem após o
         sincronismo diz que pode ser necessário reiniciar o aplicativo e é isso que o usuário deve
         fazer se for o caso, por que eu estava com preguiça de escrever o código para atualizar a interface
         afinal de contas provavelmente isso só vai acontecer comigo */
        Meses.verificarSeTemMesesParaTodasAsDespesas();

        if (!sucesso) {
            int erroId = 30003219;
            Notification notificaçao = criarNotificaçao("Falha no sincronismo", msg, R.drawable.vec_notificacao_sinc_erro);
            mNotManager.notify(erroId, notificaçao);
        }

        /*O serviço de sincronismo n deve se iniciar automaticamente com intervalos curtos (<1 hora atualmente)  */
        long data = Data.timeStampUTC();
        Prefs.edit().putLong(ServicoDeSincronismo.ultimaTentativaDeSincronismo, data).commit();

        LocalBroadcastManager.getInstance(Debt.binder.get().getApplicationContext()).sendBroadcast(new Intent(estadoDoSincronismo).putExtra("sucesso", sucesso).putExtra("mensagem", msg));
        Debt.binder.get().liveSinc.inicializar();

        stopSelf();

    }

    @Override
    public void status(String titulo, String msg) {
        mNotManager.notify(id, criarNotificaçao(titulo, msg, R.drawable.vec_notificacao_sinc));
        Log.d(Tag.AppTag + " ServicoDeSincronismo: ", " titulo = [" + titulo + "], msg = [" + msg + "]");
    }

    public abstract static class Callback extends BroadcastReceiver {
        public Callback() {
            LocalBroadcastManager.getInstance(Debt.binder.get().getApplicationContext()).registerReceiver(this, new IntentFilter(estadoDoSincronismo));
        }

        @Override
        public final void onReceive(Context context, Intent intent) {
            boolean sucesso = intent.getBooleanExtra("sucesso", false);
            String mensagem = intent.getStringExtra("mensagem");
            feito(sucesso, mensagem);
            LocalBroadcastManager.getInstance(Debt.binder.get().getApplicationContext()).unregisterReceiver(this);
        }

        public abstract void feito(boolean sucesso, String mensagem);
    }

}

