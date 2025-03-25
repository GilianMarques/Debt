package gmarques.debtv3.sincronismo;

import android.util.Log;

import gmarques.debtv3.Debt;
import gmarques.debtv3.R;
import gmarques.debtv3.especificos.RealmBackup;
import gmarques.debtv3.interface_.UIUtils;
import gmarques.debtv3.nuvem.FirebaseImpl;
import gmarques.debtv3.outros.Tag;
import gmarques.debtv3.sincronismo.api.Sincronizavel;
import io.realm.RealmObject;

public class LiveSinc {
    private FirebaseImpl firebase;

    public LiveSinc() {

        try {
            new RealmBackup().fazerBackup(null);
        } catch (Exception ignored) {
        }
    }

    public void inicializar() {

        if (firebase != null) return;
        Runnable ligarLiveSinc = () -> {

            Log.d(Tag.AppTag, "LiveSinc.inicializar: ligando liveSinc");

            firebase = new FirebaseImpl();
        };

        /*antes de fazer o ChecagemPreSincronismo eu verificava com a classe Usuario se estava sincronizando os dados com outra conta
         porém parei de fazer essa verificação porque nem sempre essa informação está disponível nas preferências do aplicativo.
         estva usando aplicativo no celular então fiz logoff e então loguei novamente e quando fez o sincronismo para restaurar os dados
          mesmo estamos sincronizando com uma outra conta, o aplicativo sincronizou com a minha conta porque eu ainda não tinha baixado
          o e-mail do usuario com quem eu sincronizava para as preferências do app, então a classe Usuario sempre retornavam que estava sincronizando com a minha
           própria conta. Moral da história: antes de sincronizar é necessário sempre checar na nuven se o usuário sincroniza ou não com outro usuário.
            A classe ChecagemPreSincronismo faz essa verificação e atualiza as preferências por que o e-mail do usuário anfitrião pode não estar sempre disponível lá*/
        /*Usuario.estaSincronizandoComOutraConta()*/
        new ChecagemPreSincronismo(new ChecagemPreSincronismo.Callback() {
            @Override
            public void feito() {
                ligarLiveSinc.run();
            }

            @Override
            public void falha(String erro) {
                UIUtils.erroToasty(Debt.binder.get().getString(R.string.Naofoipossivelverificarcontadesincronismo) + ": " + erro);
            }
        });

    }

    public void pararDeOuvir() {

        if (firebase == null) return;
        Log.d(Tag.AppTag, "LiveSinc.pararDeOuvir: desligando liveSinc");
        firebase = null;
    }


    public void addObjeto(final RealmObject rObj, long ultimaAtualizacaoDBLocal) {

        if (firebase != null) {
            firebase.addObjeto((Sincronizavel) rObj, (sucesso, msg) -> Log.d(Tag.AppTag + " LiveSinc: ", "addObjeto() uplaod do objeto " + ((Sincronizavel) rObj).getNome() + ":  sucesso = [" + sucesso + "], msg = [" + msg + "]"));
            new FirebaseImpl().attData(ultimaAtualizacaoDBLocal, (sucesso1, msg1) -> Log.d(Tag.AppTag, "LiveSinc.addObjeto: stamp do da att no db local enviada pra nuvem"));
        }
    }

    public void attObjeto(final RealmObject rObj, long ultimaAtualizacaoDBLocal) {

        if (firebase != null) {
            firebase.attObjeto((Sincronizavel) rObj, (sucesso, msg) -> Log.d(Tag.AppTag + " LiveSinc: ", "attObjeto() uplaod do objeto " + ((Sincronizavel) rObj).getNome() + ":  sucesso = [" + sucesso + "], msg = [" + msg + "]"));
            new FirebaseImpl().attData(ultimaAtualizacaoDBLocal, (sucesso1, msg1) -> Log.d(Tag.AppTag, "LiveSinc.attObjeto: stamp do da att no db local enviada pra nuvem"));

        }

    }


}
