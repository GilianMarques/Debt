package gmarques.debtv3.activities.perfil;

import android.app.ActivityManager;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.databinding.DataBindingUtil;

import com.pixplicity.easyprefs.library.Prefs;
import com.squareup.picasso.Picasso;

import java.text.MessageFormat;
import java.util.ArrayList;

import gmarques.debtv3.R;
import gmarques.debtv3.activities.MyActivity;
import gmarques.debtv3.databinding.ActivityPerfilBinding;
import gmarques.debtv3.especificos.ConexaoComAInternet;
import gmarques.debtv3.especificos.Usuario;
import gmarques.debtv3.interface_.AnimatedClickListener;
import gmarques.debtv3.interface_.UIUtils;
import gmarques.debtv3.interface_.dialogo.DialogoEnviarSolicitaçao;
import gmarques.debtv3.modelos.nuvem.ContaSincronizavel;
import gmarques.debtv3.nuvem.FBaseNomes;
import gmarques.debtv3.nuvem.FirebaseImpl;
import gmarques.debtv3.nuvem.SincronismoDeContas;
import gmarques.debtv3.utilitarios.C;
import jp.wasabeef.picasso.transformations.BlurTransformation;
import jp.wasabeef.picasso.transformations.CropCircleTransformation;

public class Perfil extends MyActivity {
    private ActivityPerfilBinding ui;
    private ActivityManager actServ;
    private ArrayList<ContaSincronizavel> contasDeSincronismo;/*armazena a conta com quem sincronizo ou as contas que sincronizam comigo*/
    private boolean anfitriao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ui = DataBindingUtil.setContentView(this, R.layout.activity_perfil);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                actServ = ((ActivityManager) getSystemService(ACTIVITY_SERVICE));

                inicializarBotaoSair();
                inicializarFotoDePerfilENome();
                carregarSolicitaçoesPendentes();
                carregarContasDeSincronismo();
                inicializarBotaoEnviarSolicitacao();
            }
        }, 300);
    }

    private void inicializarFotoDePerfilENome() {
        Picasso.get().load(Usuario.getFotoDePerfil()).transform(new CropCircleTransformation()).placeholder(R.drawable.vec_foto_placeholder).error(R.drawable.vec_foto_placeholder).into(ui.ivFotoDePerfil);
        Picasso.get().load(Usuario.getFotoDePerfilGrande()).transform(new BlurTransformation(Perfil.this, 5)).placeholder(R.drawable.vec_foto_placeholder).error(R.drawable.vec_foto_placeholder).into(ui.appBarImage);
        ui.tvNomeUsuario.setText(Usuario.getUsuario().getDisplayName());
        ui.tvEmail.setText(Usuario.getUsuario().getEmail());
    }

    private void inicializarBotaoEnviarSolicitacao() {
        ui.btnConvidar.setOnClickListener(new AnimatedClickListener() {
            @Override
            public void onClick(View view) {
                super.onClick(view);
                new ConexaoComAInternet().verificar(conectado -> {
                    if (conectado) new DialogoEnviarSolicitaçao(Perfil.this);
                    else UIUtils.erroToasty(getString(R.string.Naohaconexaocomainternet));
                });

            }
        });
    }

    private void inicializarBotaoSair() {
        ui.btnSair.setOnClickListener(new AnimatedClickListener() {
            @Override
            public void onClick(View view) {
                super.onClick(view);
                confirmarSaida();
            }
        });
    }

    private void confirmarSaida() {
        AlertDialog d = new AlertDialog.Builder(this)
                .setTitle(R.string.Porfavorconfirme)
                .setMessage(R.string.Aosairtodososseusdadosserao)
                .setPositiveButton(R.string.Sair, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        sair();
                    }
                })
                .create();
        d.show();

        Window window = d.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(R.drawable.back_dialogo);
        }

    }

    private void sair() {
        Usuario.sair(this, new Usuario.Callbackb() {
            @Override
            public void resultado(boolean tarefaExecutada) {
                if (tarefaExecutada) {
                    finishAffinity();
                    UIUtils.sucessoToasty(getString(R.string.Abraoappparafazerlogin));
                    //new Handler().postDelayed(() -> actServ.clearApplicationUserData(), 2000);

                } else UIUtils.erroToasty(getString(R.string.Naofoipossivelsair));

            }
        });


    }

    void carregarSolicitaçoesPendentes() {
        prepararContainerDeSolicitaçoes();

        new SincronismoDeContas().getSolicitaçoesDeSincronismoPendentes(new SincronismoDeContas.CallbackDeSolicitaçoes() {
            @Override
            public void feito(String msg, ArrayList<ContaSincronizavel> solicitaçoes) {
                ui.pbSolicitacoes.setVisibility(View.GONE);
                if (msg != null)
                    UIUtils.erroToasty(MessageFormat.format(getString(R.string.Errox), msg));
                else if (solicitaçoes.size() > 0) exibirSolicitaçoesPendentes(solicitaçoes);
                else ui.tvSemSolicitacoes.setVisibility(View.VISIBLE);
            }
        });
    }

    /**
     * este é um método utilitário para limpar a interface antes de carregar ou recarregar as solicitaçoes  de sincronismo.
     */
    private void prepararContainerDeSolicitaçoes() {

        // atualizo a interface antes de carregar ou recarregar
        ui.solicitacoesContainer.removeAllViews();

        ui.solicitacoesContainer.addView(ui.pbSolicitacoes);
        ui.solicitacoesContainer.addView(ui.tvSemSolicitacoes);

        ui.pbSolicitacoes.setVisibility(View.VISIBLE);
        ui.tvSemSolicitacoes.setVisibility(View.GONE);

    }

    private void exibirSolicitaçoesPendentes(ArrayList<ContaSincronizavel> solicitaçoes) {
        for (final ContaSincronizavel solicitaçao : solicitaçoes) {
            final View sView = getLayoutInflater().inflate(R.layout.layout_solicitacao, null);

            ui.solicitacoesContainer.addView(sView);

            Button btnAceitar = sView.findViewById(R.id.btnAceitar);
            Button btnRemover = sView.findViewById(R.id.btnRemover);
            TextView tvNomeUsuario = sView.findViewById(R.id.tvNomeUsuario);
            TextView tvEmail = sView.findViewById(R.id.tvEmail);
            ImageView ivFotoDePerfil = sView.findViewById(R.id.ivFotoDePerfil);

            tvNomeUsuario.setText(solicitaçao.nome);
            tvEmail.setText(solicitaçao.email);
            Picasso.get().load(solicitaçao.foto).transform(new CropCircleTransformation()).placeholder(R.drawable.vec_foto_placeholder).error(R.drawable.vec_foto_placeholder).into(ivFotoDePerfil);

            btnAceitar.setOnClickListener(new AnimatedClickListener() {
                @Override
                public void onClick(View view) {
                    super.onClick(view);
                    new AceitarSolicitaçao(Perfil.this, solicitaçao.email);
                }
            });

            btnRemover.setOnClickListener(new AnimatedClickListener() {
                @Override
                public void onClick(View view) {
                    super.onClick(view);
                    new ConexaoComAInternet().verificar(new ConexaoComAInternet.Callback() {
                        @Override
                        public void conclusao(boolean conectado) {
                            if (conectado)
                                removerSolicitaçaoDeSincronismo(solicitaçao.email, sView);
                            else UIUtils.erroToasty(getString(R.string.Naohaconexaocomainternet));
                        }
                    });
                }
            });
        }
    }

    private void removerSolicitaçaoDeSincronismo(String email, final View sView) {
        new SincronismoDeContas().removerSolicitaçaoDeSincronismo(email, new FirebaseImpl.CallbackDeStatus() {
            @Override
            public void feito(boolean sucesso, String msg) {
                if (sucesso) {
                    ui.solicitacoesContainer.removeView(sView);

                    if (ui.solicitacoesContainer.getChildCount() == 2)
                        ui.tvSemSolicitacoes.setVisibility(View.VISIBLE);
                } else
                    UIUtils.erroToasty(MessageFormat.format(getString(R.string.Errox), msg));
            }
        });

    }

    /**
     * carrega todas as contas de sincronismo independentemente de serem contas que sincronizam com o usuario local ou contas com quem o usuário local sincroniza
     */
    void carregarContasDeSincronismo() {

        prepararContainerDeContas();

        final Runnable getContasQueSincronizamComOUsuario = () -> {
            ui.pbContasEmSincronismo.setVisibility(View.VISIBLE);
            new SincronismoDeContas().getContasQueSincronizamComEsteEmail(Usuario.getUsuario().getEmail(), (msg, contasQueSincronizam) -> {
                Perfil.this.contasDeSincronismo = contasQueSincronizam;
                ui.pbContasEmSincronismo.setVisibility(View.GONE);

                if (msg != null)
                    UIUtils.erroToasty(MessageFormat.format(getString(R.string.Errox), msg));
                else if (contasQueSincronizam.size() > 0)
                    exibirContasQueSincronizamComOUsuario();
                else ui.tvSemContas.setVisibility(View.VISIBLE);
            });
        };

        new SincronismoDeContas().getContasComQuemSincronizo((msg, contasQueSincronizam) -> {
            Perfil.this.contasDeSincronismo = contasQueSincronizam;
            ui.pbContasEmSincronismo.setVisibility(View.GONE);
            if (msg != null) {
                UIUtils.erroToasty(MessageFormat.format(getString(R.string.Errox), msg));
            } else if (contasDeSincronismo.size() > 0)
                exibirContasComQuemOUsuarioSincroniza();
            else getContasQueSincronizamComOUsuario.run();
        });


    }

    /**
     * este é um método utilitário para limpar a interface antes de carregar ou recarregar as contas de sincronismo.
     */
    private void prepararContainerDeContas() {

        // atualizo a interface antes de carregar ou recarregar
        ui.contasContainer.removeAllViews();

        ui.contasContainer.addView(ui.pbContasEmSincronismo);
        ui.contasContainer.addView(ui.tvSemContas);

        ui.pbContasEmSincronismo.setVisibility(View.VISIBLE);
        ui.tvSemContas.setVisibility(View.GONE);
    }

    /**
     * exibe na interface uma lista de usuários com quem o usuario local sincroniza, o anfitrião e todos os seus outros convidados
     **/
    private void exibirContasComQuemOUsuarioSincroniza() {
        anfitriao = false;
        for (final ContaSincronizavel conta : contasDeSincronismo) {
            /*Ao carregar a lista com todos os usuários com quem usuário local sincroniza são carregadas a conta do anfitrião
             e todas as contas que sincronizam com ele o que automaticamente inclui o usuario local*/
            if (conta.email.equals(Usuario.getUsuario().getEmail())) continue;

            final View sView = getLayoutInflater().inflate(R.layout.layout_conta_de_sincronismo, null);

            ConstraintLayout parent = sView.findViewById(R.id.parent);

            TextView tvAnfitriao = sView.findViewById(R.id.tvAnfitriao);
            TextView tvNomeUsuario = sView.findViewById(R.id.tvNomeUsuario);
            TextView tvEmail = sView.findViewById(R.id.tvEmail);
            ImageView ivFotoDePerfil = sView.findViewById(R.id.ivFotoDePerfil);

            ui.contasContainer.addView(sView);

            tvNomeUsuario.setText(conta.nome);
            tvEmail.setText(conta.email);
            Picasso.get().load(conta.foto).transform(new CropCircleTransformation()).placeholder(R.drawable.vec_foto_placeholder).error(R.drawable.vec_foto_placeholder).into(ivFotoDePerfil);

            if (conta.anfitriao) {
                tvAnfitriao.setVisibility(View.VISIBLE);
            }
            sView.setOnClickListener(new AnimatedClickListener() {
                @Override
                public void onClick(View view) {
                    super.onClick(view);
                    exibirDialogoDeRemoçaoDeContas(conta);
                }
            });

        }
    }

    /**
     * exibe na interface uma lista de usuários que sincronizam com usuário local
     */
    private void exibirContasQueSincronizamComOUsuario() {
        anfitriao = true;
        for (final ContaSincronizavel conta : contasDeSincronismo) {
            final View sView = getLayoutInflater().inflate(R.layout.layout_conta_de_sincronismo, null);

            ConstraintLayout parent = sView.findViewById(R.id.parent);

            Button btnRemover = sView.findViewById(R.id.btnRemover);
            TextView tvNomeUsuario = sView.findViewById(R.id.tvNomeUsuario);
            TextView tvEmail = sView.findViewById(R.id.tvEmail);
            ImageView ivFotoDePerfil = sView.findViewById(R.id.ivFotoDePerfil);

            ui.contasContainer.addView(sView);

            tvNomeUsuario.setText(conta.nome);
            tvEmail.setText(conta.email);
            Picasso.get().load(conta.foto).transform(new CropCircleTransformation()).placeholder(R.drawable.vec_foto_placeholder).error(R.drawable.vec_foto_placeholder).into(ivFotoDePerfil);

            sView.setOnClickListener(new AnimatedClickListener() {
                @Override
                public void onClick(View view) {
                    super.onClick(view);
                    exibirDialogoDeRemoçaoDeContas(conta);
                }
            });

        }
    }

    private void exibirDialogoDeRemoçaoDeContas(ContaSincronizavel conta) {
        AlertDialog dialogo = new AlertDialog.Builder(this).create();

        View rView = getLayoutInflater().inflate(R.layout.layout_remover_conta, null, false);

        TextView tvNomeUsuario = rView.findViewById(R.id.tvNomeUsuario);
        TextView tvEmail = rView.findViewById(R.id.tvEmail);
        ImageView ivFotoDePerfil = rView.findViewById(R.id.ivFotoDePerfil);


        Button btnRemoverAnfitriao = rView.findViewById(R.id.btnRemoverAnfitriao);
        Button btnRemoverHospede = rView.findViewById(R.id.btnRemoverHospede);

        tvNomeUsuario.setText(conta.nome);
        tvEmail.setText(conta.email);
        Picasso.get().load(conta.foto).transform(new CropCircleTransformation()).placeholder(R.drawable.vec_foto_placeholder).error(R.drawable.vec_foto_placeholder).into(ivFotoDePerfil);

        /*O usuario local é anfitriao, logo, tem autoridade sobre a conta em questao*/
        if (anfitriao) {
            btnRemoverHospede.setVisibility(View.VISIBLE);
            btnRemoverHospede.setOnClickListener(v -> {
                new ConexaoComAInternet().verificar(conectado -> removerConta(conta));
                dialogo.dismiss();
            });
        } else if (conta.anfitriao) {
            btnRemoverAnfitriao.setVisibility(View.VISIBLE);
            btnRemoverAnfitriao.setOnClickListener(v -> {
                new ConexaoComAInternet().verificar(conectado -> removerConta(conta));
                dialogo.dismiss();
            });
        }
        dialogo.setView(rView);
        Window window = dialogo.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(R.drawable.back_dialogo);
        }

        dialogo.show();
    }

    private void removerConta(ContaSincronizavel conta) {

        /*O usuário é anfitrião logo basta ir na coleção contasQueSincronizamComigo e remover o e-mail relacionado a conta  alvo
         depois basta ir no documento da conta alvo, na coleção contaComQuemSincronizo e remover o e-mail do usuário local de lá

         É oque esta em cima porem ao contrario. preguiça é foda*/

        if (anfitriao) {
            new FirebaseImpl()
                    .getDocumentousuario()
                    .getParent()
                    .document(conta.email)
                    .collection(FBaseNomes.contaComQuemSincronizo)
                    .document(Usuario.getEmail())
                    .delete()
                    .addOnSuccessListener(aVoid1 -> {

                        new FirebaseImpl()
                                .getDocumentousuario()
                                .collection(FBaseNomes.contasQueSincronizamComigo)
                                .document(conta.email)
                                .delete()
                                .addOnSuccessListener(aVoid -> {
                                    UIUtils.sucessoToasty(getString(R.string.Sucesso));
                                    carregarContasDeSincronismo();
                                }).addOnFailureListener(e -> {
                            UIUtils.erroToasty(MessageFormat.format(getString(R.string.Errox), e.getMessage() + C.eCod17));

                        });


                    }).addOnFailureListener(e -> {
                UIUtils.erroToasty(MessageFormat.format(getString(R.string.Errox), e.getMessage() + C.eCod18));

            });


        } else {

            /*usuário não é anfitrião, nesse caso preciso ir no documento dele no campo contaComQuemSincronizo e remover o e-mail da conta alvo de lá
            depois basta ir no documento da conta recebida no campo contasQueSincronizamComigo e remover o e-mail do usuário local de lá*/

            new FirebaseImpl()
                    .getDocumentousuario()
                    .collection(FBaseNomes.contaComQuemSincronizo)
                    .document(conta.email)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        /* O usuário interrompeu o sincronismo com a conta com quem estava sincronizando (anfitrião) por tanto,
                        apago a referência a essa conta das preferências para que o usuário volte sincronizar em sua própria conta*/
                        if (conta.anfitriao) Prefs.putString(C.contaDeSincronismo, null);

                        new FirebaseImpl()
                                .getDocumentousuario()
                                .getParent()
                                .document(conta.email)
                                .collection(FBaseNomes.contasQueSincronizamComigo)
                                .document(Usuario.getEmail())
                                .delete()
                                .addOnSuccessListener(aVoid1 -> {

                                    UIUtils.sucessoToasty(getString(R.string.Sucesso));
                                    carregarContasDeSincronismo();

                                }).addOnFailureListener(e -> {
                            UIUtils.erroToasty(MessageFormat.format(getString(R.string.Errox), e.getMessage() + C.eCod18));

                        });


                    }).addOnFailureListener(e -> {
                UIUtils.erroToasty(MessageFormat.format(getString(R.string.Errox), e.getMessage() + C.eCod17));

            });

        }

    }


}