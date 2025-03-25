package gmarques.debtv3.sincronismo;

import android.util.Log;

import com.pixplicity.easyprefs.library.Prefs;

import gmarques.debtv3.nuvem.FBaseNomes;
import gmarques.debtv3.nuvem.FirebaseImpl;
import gmarques.debtv3.nuvem.SincronismoDeContas;
import gmarques.debtv3.outros.Tag;
import gmarques.debtv3.utilitarios.C;

/**
 * essa classe verifica se usuário está sincronizando seus dados com um hóspede caso sim
 * verifica se esse hóspede existe e se ele não interrompeu o sincronismo com o usuário,
 * atualizando as preferências do app  caso necessario.
 * <p>
 * Quando o usuário interrompe sincronismo com o hóspede, as preferências do APP são atualizadas
 * (é necessario pq a classe {@link gmarques.debtv3.nuvem.FirebaseImpl} usa o e-mail salvo nas preferencias pra determinar onde deve sincronizar os dados do usuario)
 * mas quando o hóspede interrompe sincronismo com usuário não é possível (para o usuario) virificar (em tempo real) se houve ou não interrupção, portanto, as preferências
 * não são atualizadas e se nao houver a checagem feita por esta classe o usuario continua sincronizando seus dados com o hospede até que os dados do app sejam limpos ou
 * o usuario por conta propria se desconecte do hospede. também é necessário verificar se a conta do hóspede existe antes de cada sincronismo e atualizar as preferencias
 */
public class ChecagemPreSincronismo {
    private Callback callback;

    public ChecagemPreSincronismo(Callback callback) {
        this.callback = callback;
        verificarSeUsuarioEHospedeDeAlguem();
    }

    /**
     * verifica se o usuario sincroniza seus dados com outra conta
     */
    private void verificarSeUsuarioEHospedeDeAlguem() {
        new SincronismoDeContas().jaEstouSincronizandoComUmaConta((email, msg) -> {
            if (msg != null) callback.falha(msg);
            else if (email != null) verificarSeContaDoAnfitriaoExiste(email);
            else {
                /*O usuario nao sincroniza com ninguem (pode ter sido expluso pelo hospede)
                 * logo, deve sincronizar na propria conta. Salvar null nas preferencias vai
                 * fazer a classe @Usuario retornar o email do proprio usuario quando a classe @FirebaseImpl
                 * solicitar a conta de sincronismo para definir onde salvar os dados durante o sincronismo*/
                Prefs.putString(C.contaDeSincronismo, null);
                callback.feito();
            }
            Log.d(Tag.AppTag, "ChecagemPreSincronismo.verificarSeUsuarioEHospedeDeAlguem: "+email+" : "+msg);
        });
    }

    /**
     * Verifica se a conta com quem o usuario sincroniza (caso ele sincronize com alguma conta) existe
     */
    private void verificarSeContaDoAnfitriaoExiste(String email) {
        new SincronismoDeContas().existeUsuarioComEsseEmail(email, (existe, msg) -> {
            Log.d(Tag.AppTag, "ChecagemPreSincronismo.verificarSeContaDoAnfitriaoExiste: "+existe+" : "+msg);  if (msg != null) callback.falha(msg);

            else if (existe) {
                Prefs.putString(C.contaDeSincronismo, email);
                callback.feito();
            } else {
                /*A conta do hospede ja nao existe mais logo, o usuario deve sincronizar na propria conta.
                 Salvar null nas preferencias vai fazer a classe @Usuario retornar o email do proprio usuario quando a classe @FirebaseImpl
                 * solicitar a conta de sincronismo para definir onde salvar os dados durante o sincronismo*/
                Prefs.putString(C.contaDeSincronismo, null);
                /*A conta do anfitriao nao existe, devo remover ela dos dados do usuario local
                * tento a remoção sem criar um metodo especifico pra isso na classe SincronismoDeContas pq acho desnecessario
                * um metodo só pra isso, tbm n defino listeners, se a op falhar agora, tento de novo na proxima e assim sucessivamente
                * (enquanto nao remover a conta ela vai aparecer na tela de perfil do usuario como conta anfitria.)*/
                new FirebaseImpl().getDocumentousuario().collection(FBaseNomes.contaComQuemSincronizo).document(email).delete();
                callback.feito();
            }
        });

    }

    public static interface Callback {
        void feito();

        void falha(String erro);
    }
}
