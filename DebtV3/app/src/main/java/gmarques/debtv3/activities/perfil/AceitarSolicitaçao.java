package gmarques.debtv3.activities.perfil;

import android.text.Html;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.pixplicity.easyprefs.library.Prefs;

import java.text.MessageFormat;
import java.util.Locale;

import gmarques.debtv3.R;
import gmarques.debtv3.callbacks.EncerrarSincronismoCallback;
import gmarques.debtv3.especificos.ConexaoComAInternet;
import gmarques.debtv3.especificos.Usuario;
import gmarques.debtv3.interface_.AnimatedClickListener;
import gmarques.debtv3.interface_.UIUtils;
import gmarques.debtv3.nuvem.SincronismoDeContas;
import gmarques.debtv3.utilitarios.C;

public class AceitarSolicitaçao {

    private final Perfil perfil;
    /*Este campo armazena o email que esta em sincronismo no momento
     * caso o usurio esteja sincronizando com alguma conta */
    private String emailEmSincronismo;

    public AceitarSolicitaçao(final Perfil perfil, final String emailDeQuemSolicitou) {

        this.perfil = perfil;
        new ConexaoComAInternet().verificar(conectado -> {
            if (conectado)
                verificarSeAlguemSincronizaComOusuarioLocal(emailDeQuemSolicitou);
            else UIUtils.erroToasty(perfil.getString(R.string.Naohaconexaocomainternet));
        });

    }

    /**
     * verifica se já existem contas sincronizando com usuário local, caso sim, o avisa de que não pode aceitar solicitações por conta deste motivo, caso não, prossegue com o processo
     *
     * @param emailDeQuemSolicitou
     */
    private void verificarSeAlguemSincronizaComOusuarioLocal(String emailDeQuemSolicitou) {
        new SincronismoDeContas().getContasQueSincronizamComEsteEmail(Usuario.getEmail(), (msg, contas) -> {
            if (msg != null) {
                UIUtils.erroToasty(MessageFormat.format(perfil.getString(R.string.Errox), msg));
            } else if (contas.size() > 0) {
                UIUtils.dialogo(perfil, "", String.format(Locale.getDefault(), perfil.getString(R.string.Vocenaopodesincronizarcomxporquejatemcontassincronizandocomvoce), emailDeQuemSolicitou));
            } else informarSobreRiscosDeAceitarSolicitaçao(emailDeQuemSolicitou);


        });
    }

    /**
     * notifica o usuário através de uma caixa de diálogo dos riscos que ele corre ao aceitar solicitações de sincronismo
     */
    private void informarSobreRiscosDeAceitarSolicitaçao(final String email) {

        final androidx.appcompat.app.AlertDialog dialogo = new AlertDialog.Builder(perfil).create();
        View dView = perfil.getLayoutInflater().inflate(R.layout.layout_dialogo_aceitar_solicitacao, null, false);

        final ProgressBar progressBar = dView.findViewById(R.id.progressBar);
        final TextView tvInfo = dView.findViewById(R.id.tvInfo);
        final Button btnAceitar = dView.findViewById(R.id.btnAceitar);

        tvInfo.setText(Html.fromHtml(String.format(Locale.getDefault(), perfil.getString(R.string.Aoaceitarasolicitacaodexseusdadosse), email), Html.FROM_HTML_MODE_COMPACT));

        btnAceitar.setOnClickListener(new AnimatedClickListener() {
            @Override
            public void onClick(View view) {
                super.onClick(view);
                progressBar.setVisibility(View.VISIBLE);
                tvInfo.setVisibility(View.GONE);
                btnAceitar.setVisibility(View.GONE);

                verificarSeJaEstouSincronizandoComUmaConta(email, dialogo);
            }
        });
        dialogo.setView(dView);
        dialogo.show();
        Window window = dialogo.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(R.drawable.back_dialogo);
        }

    }


    /**
     * verifica se o usuario já está sincronizando seu banco de dados com alguma conta, caso sim chama o metodo confirmarSeDeveAceitarSincronismo para confirmar a decisao do usuario
     */
    private void verificarSeJaEstouSincronizandoComUmaConta(final String emailSolicitante, final AlertDialog dialogo) {
        new SincronismoDeContas().jaEstouSincronizandoComUmaConta((emailEmSincronismo, erroMsg) -> {
            if (erroMsg != null) {
                dialogo.dismiss();
                UIUtils.erroToasty(MessageFormat.format(perfil.getString(R.string.Errox), erroMsg));
            } else if (emailEmSincronismo == null)
                possoAceitarSolicitaçaoDeSincronismo(emailSolicitante, dialogo);
            else {
                this.emailEmSincronismo = emailEmSincronismo;
                confirmarSeDeveAceitarSincronismo(emailEmSincronismo, emailSolicitante, dialogo);
            }
        });

    }

    /**
     * avisa o usuário que se ele aceitar a solicitação, o sincronismo com a conta com quem ele está sincronizando atualmente será interrompido
     * e pergunta se ele quer aceitar mesmo assim
     */
    private void confirmarSeDeveAceitarSincronismo(String emailEmSincronismo, final String emailSolicitante, final AlertDialog dialogoPrincipal) {
        final AlertDialog dialogoDeConfirmaçao = new AlertDialog.Builder(perfil).create();
        View dView = perfil.getLayoutInflater().inflate(R.layout.layout_dialogo_aceitar_solicitacao, null, false);

        final TextView tvInfo = dView.findViewById(R.id.tvInfo);
        final Button btnAceitar = dView.findViewById(R.id.btnAceitar);
        final boolean[] usuarioQuerProsseguirMesmoAssim = new boolean[]{false};

        tvInfo.setText(Html.fromHtml(String.format(Locale.getDefault(), perfil.getString(R.string.Aoaceitarasolicitacaodexvocedeixara), emailSolicitante, emailEmSincronismo), Html.FROM_HTML_MODE_COMPACT));
        btnAceitar.setOnClickListener(new AnimatedClickListener() {
            @Override
            public void onClick(View view) {
                super.onClick(view);
                usuarioQuerProsseguirMesmoAssim[0] = true;
                dialogoDeConfirmaçao.dismiss();
                possoAceitarSolicitaçaoDeSincronismo(emailSolicitante, dialogoPrincipal);
            }
        });
        dialogoDeConfirmaçao.setView(dView);
        dialogoDeConfirmaçao.setOnDismissListener(dialog -> {
            if (!usuarioQuerProsseguirMesmoAssim[0]) dialogoPrincipal.dismiss();
        });
        dialogoDeConfirmaçao.show();
        Window window = dialogoDeConfirmaçao.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(R.drawable.back_dialogo);
        }
    }

    /**
     * Verifica se o usuario que enviou a solicitaçao ja esta sincronizando o proprio banco de dados com outra pessoa, caso sim nao permite que o usuario aceite
     * a solicitaçao pois isso quebraria a regra de sincronismo de contas que é de "1 para MUITOS" que significa que eu posso ter varias pessoas
     * sincronizando comigo mas só se eu nao estiver sincronizando com a conta de alguem
     */
    private void possoAceitarSolicitaçaoDeSincronismo(final String email, final androidx.appcompat.app.AlertDialog dialogo) {

        new SincronismoDeContas().usuarioQueMeConvidouJaEstaSincronizandoComOutroUsuario(email, (sim, msg) -> {

            if (msg != null) {
                dialogo.dismiss();
                UIUtils.erroToasty(MessageFormat.format(perfil.getString(R.string.Errox), msg));

            } else if (sim) {
                UIUtils.dialogo(perfil, "", String.format(Locale.getDefault(), perfil.getString(R.string.Vocenaopodesincronizarcomxporqueesteemailjaestasincronizandocomoutraconta), email));
                dialogo.dismiss();
            } else removerContaDesincronismoAtualCasoHaja(email, dialogo);

        });
    }

    /**
     * Caso o usuario local já esteja sincronizando seus dados com outra conta, esse método encerra o sincronismo com esta conta antes de aceitar uma nova solicitação
     */
    private void removerContaDesincronismoAtualCasoHaja(final String email, final AlertDialog dialogo) {

        if (emailEmSincronismo == null) aceitarSolicitaçaoDeSincronismo(email, dialogo);

        else new SincronismoDeContas()
                .encerrarSincronismo(emailEmSincronismo, new EncerrarSincronismoCallback() {
                    @Override
                    public void sincronismoEncerrado() {
                        /*removo a referência á conta com quem o usuário local acabou de interromper o sincronismo para manter consistência dos dados caso
                        a operação falhe no meio do caminho enquanto tenta aceitar a solicitação de sincronismo de uma outra conta*/
                        Prefs.putString(C.contaDeSincronismo, null);
                        aceitarSolicitaçaoDeSincronismo(email, dialogo);
                    }

                    @Override
                    public void falhaAoEncerrarSincronismo(String erro) {
                        UIUtils.erroToasty(MessageFormat.format(perfil.getString(R.string.Errox), erro));
                        dialogo.dismiss();
                    }

                    @Override
                    public void sincronismoEncerradoComRessalva(String erro) {
                        UIUtils.erroToasty(MessageFormat.format(perfil.getString(R.string.Errox), erro));
                        UIUtils.infoToasty(String.format(Locale.getDefault(), perfil.getString(R.string.OsincronismocomXfoiinterrompidocomresaslvas), emailEmSincronismo));
                        /*removo a referência á conta com quem o usuário local acabou de interromper o sincronismo para manter consistência dos dados caso
                        a operação falhe no meio do caminho enquanto tenta aceitar a solicitação de sincronismo de uma outra conta*/
                        Prefs.putString(C.contaDeSincronismo, null);
                        aceitarSolicitaçaoDeSincronismo(email, dialogo);
                    }
                });
    }

    /**
     * aceita a solicitação de sincronismo e atualiza interface de acordo
     */
    private void aceitarSolicitaçaoDeSincronismo(final String email, final AlertDialog dialogo) {

        new SincronismoDeContas().aceitarSolicitaçaoDeSincronismo(email, (sucesso, msg) -> {
            dialogo.dismiss();

            if (sucesso) {
                Prefs.putString(C.contaDeSincronismo, email);
                perfil.carregarContasDeSincronismo();
                perfil.carregarSolicitaçoesPendentes();
            } else
                UIUtils.erroToasty(MessageFormat.format(perfil.getString(R.string.Errox), msg));
        });
    }


}
