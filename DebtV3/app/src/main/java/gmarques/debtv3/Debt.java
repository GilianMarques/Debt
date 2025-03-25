package gmarques.debtv3;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.ContextWrapper;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.pixplicity.easyprefs.library.Prefs;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import gmarques.debtv3.activities.MyActivity;
import gmarques.debtv3.activities.dashboard.MainActivity;
import gmarques.debtv3.broadcast_e_servicos.GestorDeAlarmes;
import gmarques.debtv3.gestores.Meses;
import gmarques.debtv3.nuvem.RemoteConfig;
import gmarques.debtv3.outros.Tag;
import gmarques.debtv3.sincronismo.LiveSinc;
import gmarques.debtv3.utilitarios.C;
import io.github.inflationx.calligraphy3.CalligraphyConfig;
import io.github.inflationx.calligraphy3.CalligraphyInterceptor;
import io.github.inflationx.viewpump.ViewPump;
import io.realm.Realm;
import io.realm.RealmConfiguration;

public class Debt extends Application {
    /* TODO: 12/06/2020    checklist pre lançamento: (Fazer essas verificações com o app em release)
     *   atualizar os termo pra ver se sao exibidos :OK
     * verificar se a proteção por mudança de hora funciona :OK
     * verificar como os relatorios de erro esta sendo exibidos :OK
     * testar o sincronimo em outra conta e ver se volta ao normal ao parar de sincronizar com outra conta :OK
     * remover a conta com quem sincronizo pra ver se volto a sincronizar na minha conta:OK
     *
     *  adicionar nesta lista oque for lembrando*/
    public static iBinder binder;
    public static int DIFERENÇA_SEG;
    public static int MIN_IMP_ATE_SUSPENDER;
    public static String ADM_SENHA;
    public static boolean ADMINISTRADOR;
    public static boolean DESLIGAR_ADS;
    /*muda o local onde os dados sao salvos na nuvem em caso de testes. Mude manualmente*/
    public static boolean MODO_DE_TESTES = false;//QUANDO ENTRAR/SAIR DOS TESTES APAGAR OS DADOS DO APP PRA N SINCRONIZAR E CAGAR COM OS DADOS NA NUVEM
    private MyActivity activityNatela;
    public LiveSinc liveSinc;
    private boolean appMinimizado = true;
    public RemoteConfig remoteConfig;

    @Override
    public void onCreate() {
        binder = () -> Debt.this;
// TODO: 25/03/2025 mudar o projeto do firebase para outra conta
        if (!BuildConfig.DEBUG) inicializarCapturadorDeExcessoesGlobal();

        iniciarPrefs();
        verificarPrivilegios();
        iniciarRealm();
        inicializarRemoteConfig();
        liveSinc = new LiveSinc();
        inicializarMeses();
        iniciarCalligraphy();
        verificarNotificacoes();

        super.onCreate();
    }

    private void verificarNotificacoes() {
        GestorDeAlarmes gestorDeAlarmes = new GestorDeAlarmes(this);
        if (!gestorDeAlarmes.estaoAsNotificaçoesLigadas()) {
            gestorDeAlarmes.ligarNotificaçoes();
            gestorDeAlarmes.alternarAutoSinc(true);
        }
    }

    private void inicializarRemoteConfig() {
        remoteConfig = new RemoteConfig(getApplicationContext());
        new RemoteConfig.RConfigListener(this) {
            @Override
            protected void configuraçoesCarregadas(boolean sucesso, FirebaseRemoteConfig remoteConfig) {
                if (sucesso) {
                    ADM_SENHA = remoteConfig.getString(RemoteConfig.senhaAdm);
                    DIFERENÇA_SEG = (int) remoteConfig.getLong(RemoteConfig.diferencaAceitavel);
                    MIN_IMP_ATE_SUSPENDER = (int) remoteConfig.getLong(RemoteConfig.minImpressoesAtePodeSuspendeAnuncios);
                } else {
                    ADM_SENHA = null;
                    DIFERENÇA_SEG = (5 * 60);
                    MIN_IMP_ATE_SUSPENDER = 10;
                }
            }
        };
    }

    private void verificarPrivilegios() {
        //sempre é administrador qdo em DEBUG a menos que mude manualmente
        ADMINISTRADOR = Prefs.getBoolean(C.administrador, BuildConfig.DEBUG);
        DESLIGAR_ADS = Prefs.getBoolean(C.desligarAnuncios, false);
    }

    public void reiniciar() {
        Log.d(Tag.AppTag, "Debt.reiniciar: App vai ser reinicaido? " + (activityNatela != null));
        liveSinc.pararDeOuvir();
        if (activityNatela == null) return;

        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        int mPendingIntentId = 123474321;
        PendingIntent mPendingIntent = PendingIntent.getActivity(getApplicationContext(), mPendingIntentId, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager mgr = (AlarmManager) getApplicationContext().getSystemService(ALARM_SERVICE);
        if (mgr != null) {
            mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
        }

        System.exit(0);

    }

    private void inicializarCapturadorDeExcessoesGlobal() {


        Thread.setDefaultUncaughtExceptionHandler((paramThread, e) -> {
            //Catch your exception
            // Without System.exit() this will not work.
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            e.printStackTrace(new PrintStream(out));
            String trace = new String(out.toByteArray());

            Prefs.edit().putString("trace", trace).commit();

            Log.d(Tag.AppTag, "Debt.uncaughtException: exception capturada!");
            if (!BuildConfig.DEBUG) System.exit(2);
        });


    }

    private void inicializarMeses() {
        Meses.inicializar();
    }

    private void iniciarRealm() {
        Realm.init(this);
        RealmConfiguration config = new RealmConfiguration.Builder()
                .schemaVersion(5) // Must be bumped when the schema changes  -- olha a classe MyMigration antes de mexer aqui
                .migration(new MyMigration())
                .build();


        Realm.setDefaultConfiguration(config);

    }

    private void iniciarPrefs() {

        new Prefs.Builder()
                .setContext(this)
                .setMode(ContextWrapper.MODE_PRIVATE)
                .setUseDefaultSharedPreference(true)
                .build();
    }

    private void iniciarCalligraphy() {

        ViewPump.init(ViewPump.builder()
                .addInterceptor(new CalligraphyInterceptor(
                        new CalligraphyConfig.Builder()
                                .setDefaultFontPath("ProductSans")
                                .setFontAttrId(io.github.inflationx.calligraphy3.R.attr.fontPath)
                                .build()))
                .build());
    }

    public MyActivity activity() {
        return activityNatela;
    }

    public void setActivity(MyActivity myActivity) {
        this.activityNatela = myActivity;
    }

    public void setAppMinimizado(boolean appMinimizado) {

        this.appMinimizado = appMinimizado;
    }

    public boolean estaAppMinimizado() {
        return appMinimizado;
    }

    public interface iBinder {
        Debt get();

    }

    public void runOnUiThread(Runnable runnable) {
        new Handler(Looper.getMainLooper()).post(runnable);
    }
}
