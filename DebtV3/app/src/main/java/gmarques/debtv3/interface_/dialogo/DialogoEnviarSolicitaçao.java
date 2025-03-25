package gmarques.debtv3.interface_.dialogo;

import android.app.AlertDialog;
import android.os.Handler;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.text.MessageFormat;

import gmarques.debtv3.R;
import gmarques.debtv3.activities.perfil.Perfil;
import gmarques.debtv3.especificos.Usuario;
import gmarques.debtv3.interface_.UIUtils;
import gmarques.debtv3.nuvem.FirebaseImpl;
import gmarques.debtv3.nuvem.SincronismoDeContas;

public class DialogoEnviarSolicitaçao {

    private View sView;
    private TextView tvTitulo;
    private TextView tvInfo;
    private EditText edtEmail;
    private Button btnConcluido;
    private Button btnConfirmarConvite;
    private ProgressBar progressBar;
    private Perfil activity;
    private AlertDialog dialogo;
    private String email;


    public DialogoEnviarSolicitaçao(Perfil perfil) {
        this.activity = perfil;
        inicializarViews();
        configurarCampoDeEmail();
        definirEventosDeClique();
        inicializarEExibirDialogo();
    }

    private void configurarCampoDeEmail() {
        edtEmail.addTextChangedListener(new TextWatcher() {
            int ultimoTamanho = 0;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().endsWith("@") && ultimoTamanho < s.length()) {
                    edtEmail.setText(s.toString().concat("gmail.com"));
                    edtEmail.setSelection(edtEmail.getText().length());
                }
                ultimoTamanho = s.length();
            }
        });
    }

    private void definirEventosDeClique() {

        btnConcluido.setOnClickListener(v -> {
            email = edtEmail.getText().toString().toLowerCase();
            if (email.endsWith("@gmail.com") && !email.equals(Usuario.getEmail())) {
                confirmarEnvio();
            } else UIUtils.erroNoFormulario(edtEmail);
        });

        btnConfirmarConvite.setOnClickListener(v -> enviarSolicitaçao(email));
    }

    private void enviarSolicitaçao(final String email) {
        progressBar.setVisibility(View.VISIBLE);
        btnConfirmarConvite.setVisibility(View.GONE);
        tvInfo.setVisibility(View.GONE);

        Runnable enviarSolicitaçao = new Runnable() {
            @Override
            public void run() {

                new SincronismoDeContas().enviarSolicitaçaoDeSincronismo(email, new FirebaseImpl.CallbackDeStatus() {
                    @Override
                    public void feito(boolean solicitaçaoEnviada, String msg) {
                        dialogo.dismiss();

                        if (solicitaçaoEnviada && msg == null)
                            UIUtils.sucessoToasty(activity.getString(R.string.Solicitacaoenviadacomsucesso));
                        else
                            UIUtils.erroToasty(MessageFormat.format(activity.getString(R.string.Errox), msg));
                    }
                });
            }
        };


        verificarSeUsuarioExiste(email, enviarSolicitaçao);
    }

    private void verificarSeUsuarioExiste(String email, final Runnable enviarSolicitaçao) {
        new SincronismoDeContas().existeUsuarioComEsseEmail(email, (usuarioExiste, msg) -> {
            /*verificação ocorreu com sucesso. Veja o javadoc do metodo*/
            if (msg == null) {
                if (usuarioExiste) enviarSolicitaçao.run();
                else {
                    UIUtils.erroToasty(activity.getString(R.string.Usuarionaoencontrado));
                    dialogo.dismiss();
                }
            } else {
                UIUtils.erroToasty(activity.getString(R.string.Naofoipossivelconcluiraoperacao));
                dialogo.dismiss();
            }
        });
    }
    //1045,91 out
    //1207,51
    //690,57
    //

    private void confirmarEnvio() {

        btnConcluido.setVisibility(View.GONE);
        edtEmail.setVisibility(View.GONE);
        tvTitulo.setVisibility(View.GONE);                                      //                                                                          <font color=#2bb1ff> .... read more</font>
        tvInfo.setText(Html.fromHtml(MessageFormat.format(activity.getString(R.string.Sexaceitarasolicitacaoseusdados), edtEmail.getText().toString()), Html.FROM_HTML_MODE_COMPACT));
        tvInfo.setVisibility(View.VISIBLE);
        btnConfirmarConvite.setVisibility(View.VISIBLE);
        UIUtils.esconderTeclado();


        /*Execuçao continua quando usuario clicar no botao de confirmar envio, volte para o metodo definirEventosDeClique(); */
    }

    private void inicializarViews() {
        sView = activity.getLayoutInflater().inflate(R.layout.layout_enviar_solicitacao, null, false);
        tvTitulo = sView.findViewById(R.id.tvTitulo);
        tvInfo = sView.findViewById(R.id.tvInfo);
        edtEmail = sView.findViewById(R.id.edtEmail);
        btnConcluido = sView.findViewById(R.id.btnConcluido);
        btnConfirmarConvite = sView.findViewById(R.id.btnConfirmarConvite);
        progressBar = sView.findViewById(R.id.progressBar);
    }

    private void inicializarEExibirDialogo() {
        dialogo = new AlertDialog.Builder(activity).create();
        dialogo.setView(sView);
        dialogo.show();
        Window window = dialogo.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(R.drawable.back_dialogo);
        }
        new Handler().postDelayed(() -> {
            edtEmail.requestFocus();
            UIUtils.mostrarTeclado(edtEmail);
        }, 200);
    }

    public interface Callback {
        void enviarSolicitaçao(String email);
    }
}
