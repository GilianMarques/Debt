package gmarques.debtv3.activities;


import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.databinding.DataBindingUtil;

import com.google.android.flexbox.FlexboxLayout;

import org.joda.time.LocalDate;

import java.math.BigDecimal;
import java.util.ArrayList;

import gmarques.debtv3.R;
import gmarques.debtv3.databinding.ActivityGerenciarContasBinding;
import gmarques.debtv3.especificos.CalculadorMonetario;
import gmarques.debtv3.gestores.ContaBancos;
import gmarques.debtv3.interface_.AnimatedClickListener;
import gmarques.debtv3.interface_.FormatUtils;
import gmarques.debtv3.interface_.UIUtils;
import gmarques.debtv3.modelos.ContaBanco;
import gmarques.debtv3.modelos.Objetivo;
import gmarques.debtv3.novo_bsheet.Sheettalogo;
import gmarques.debtv3.outros.Broadcaster;
import gmarques.debtv3.outros.UIRunnable;


public class VerContas extends MyActivity {
    private ActivityGerenciarContasBinding ui;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ui = DataBindingUtil.setContentView(this, R.layout.activity_gerenciar_contas);
        setSupportActionBar(ui.toolbar);
        ActionBar actBar = getSupportActionBar();
        assert actBar != null;
        actBar.setTitle(getString(R.string.Vercontas));
        inicializarBotoes();

        new Handler().postDelayed(this::carregarContas, tempoDeEspera);

    }

    private void inicializarBotoes() {
        ui.fabAdd.setOnClickListener(new AnimatedClickListener() {
            @Override
            public void onClick(View view) {
                super.onClick(view);
                addContaBanco();
            }
        });

    }

    private void carregarContas() {
        //houve alguma atualizaçao das contas, devo atualizar o fragmento
        Broadcaster.enviar(Broadcaster.atualizarFragContas);
        if (ui.container.getChildCount() > 0) {
            ui.container.removeAllViews();
        }
        ArrayList<View> views = new ArrayList<>();
        final ArrayList<ContaBanco> contas = new ArrayList<>();

        new Handler().postDelayed(new UIRunnable() {
            @Override
            public void workerThread() {
                contas.addAll(ContaBancos.getContas());

                for (ContaBanco conta : contas) {
                    View vContaBanco = carregarViewDaConta(conta);
                    views.add(vContaBanco);

                }
            }

            @Override
            public void uiThread() {
                for (int i = 0; i < views.size(); i++) {
                    View vContaBanco = views.get(i);
                    ui.container.addView(vContaBanco);
                    carregarViewsDosObjetivosDaConta(vContaBanco, contas.get(i));
                }


            }
        }, 1);
    }

    private View carregarViewDaConta(final ContaBanco conta) {
        View contaView = getLayoutInflater().inflate(R.layout.layout_contas, null);


        ImageView ivMenu = contaView.findViewById(R.id.ivMenu);

        TextView tvNomeConta = contaView.findViewById(R.id.tvNomeConta);
        TextView tvValorContaLivre = contaView.findViewById(R.id.tvValorContaLivre);
        TextView tvValorContaTotal = contaView.findViewById(R.id.tvValorContaTotal);

        ivMenu.setOnClickListener(new AnimatedClickListener() {
            @Override
            public void onClick(View view) {

                mostrarMenuDaContaDeBanco(conta);

                super.onClick(view);
            }
        });


        BigDecimal valorObjetivos = new BigDecimal("0");

        for (Objetivo objetivo : ContaBancos.getObjetivos(conta))
            if (!objetivo.estaConcluido())
                valorObjetivos = valorObjetivos.add(BigDecimal.valueOf(objetivo.getValor()));

        float valorContaLivre = BigDecimal.valueOf(conta.getValor()).subtract(valorObjetivos).floatValue();

        tvNomeConta.setText(conta.getNome());
        tvValorContaTotal.setText(FormatUtils.emReal(conta.getValor()));
        tvValorContaLivre.setText(FormatUtils.emReal(valorContaLivre));

        if (valorContaLivre > 0) tvValorContaLivre.setTextColor(UIUtils.cor(R.color.flat_color_11));
        else tvValorContaLivre.setTextColor(UIUtils.cor(R.color.flat_color_12));

        return contaView;
    }

    private void mostrarMenuDaContaDeBanco(ContaBanco conta) {

        Sheettalogo dialogo = new Sheettalogo(VerContas.this);
        dialogo.titulo(conta.getNome())
                .mensagem(getString(R.string.Selecioneumaopcao))
                .icone(R.drawable.vec_conta_banco)
                .botaoPositivo(getString(R.string.NovoObjetivo), v -> {

                    addObjetivo(conta);

                })
                .botaoNegativo(getString(R.string.EditarConta), v -> {

                    editarContaBanco(conta);

                })
                .botaoNeutro(getString(R.string.Removerconta), v -> confirmarEremoverContaBanco(conta)).show();

    }

    private void carregarViewsDosObjetivosDaConta(View vContaBanco, ContaBanco conta) {

        FlexboxLayout container = vContaBanco.findViewById(R.id.container);

        new Handler().post(new UIRunnable() {
            ArrayList<Objetivo> objetivos;

            @Override
            public void workerThread() {
                objetivos = ContaBancos.getObjetivos(conta);
                objetivos.sort((o1, o2) -> Boolean.compare(o1.estaConcluido(), o2.estaConcluido()));

            }

            @Override
            public void uiThread() {
                //------------------------------------
                for (Objetivo objetivo : objetivos) {
                    View vObjetivo = getLayoutInflater().inflate(R.layout.layout_objetivos, null);

                    ImageView ivStatus = vObjetivo.findViewById(R.id.ivStatus);

                    TextView tvNomeConta = vObjetivo.findViewById(R.id.tvNomeConta);
                    TextView tvValorConta = vObjetivo.findViewById(R.id.tvValorConta);
                    TextView tvPeriodo = vObjetivo.findViewById(R.id.tvPeriodo);

                    tvNomeConta.setText(objetivo.getNome());
                    tvValorConta.setText(FormatUtils.emReal(objetivo.getValor()));

                    if (objetivo.estaConcluido()) {

                        ivStatus.setImageResource(R.drawable.vec_check);

                        String periodo = FormatUtils.formatarDataEmPeriodo(new LocalDate(objetivo.getDataDeCriacao()), new LocalDate(objetivo.getDataDeConclusao()));
                        tvPeriodo.setText(periodo);

                    } else {

                        String periodo = FormatUtils.formatarDataCurta(new LocalDate(objetivo.getDataDeCriacao())) + " - em aberto";
                        tvPeriodo.setText(periodo);

                    }
                    vObjetivo.setOnLongClickListener(v -> {
                        editarObjetivo(objetivo, conta);
                        return true;
                    });
                    container.addView(vObjetivo);
                }

                for (int i = 0; i < container.getChildCount(); i++) {
                    View view = container.getChildAt(i);
                    FlexboxLayout.LayoutParams lp = (FlexboxLayout.LayoutParams) view.getLayoutParams();
                    lp.setOrder(-1);
                    lp.setFlexGrow(2);
                    view.setLayoutParams(lp);
                }
                // ------------------------------------
            }
        });


    }

    private void addObjetivo(ContaBanco conta) {

        Objetivo novoObjetivo = new Objetivo();

        View vAddObjetivo = getLayoutInflater().inflate(R.layout.layout_add_edit_objetivo, null);

        final EditText edtTitulo = vAddObjetivo.findViewById(R.id.edtNome);
        final EditText edtValor = vAddObjetivo.findViewById(R.id.edtValor);
        final Switch sConcluido = vAddObjetivo.findViewById(R.id.sConcluido);


        new CalculadorMonetario(edtValor, (valor, valorFormatado, editText) -> {
            editText.setText(valorFormatado);
            novoObjetivo.setValor(valor);
        });


        Sheettalogo dialogo = new Sheettalogo(VerContas.this);
        dialogo.contentView(vAddObjetivo)
                .titulo(getString(R.string.NovoObjetivo))
                .icone(R.drawable.vec_objetivo)
                .botaoPositivo(getString(R.string.Concluir), v -> {
                    novoObjetivo.setNomeObjetivo(edtTitulo.getText().toString());
                    if (novoObjetivo.getValor() <= 0) novoObjetivo.setValor(1);
                    novoObjetivo.setConcluido(sConcluido.isChecked());
                    conta.addObjetivo(novoObjetivo);
                    carregarContas();

                })
                .botaoNegativo(getString(R.string.Cancelar), v -> {

                })

                .show();

    }

    private void editarObjetivo(Objetivo objetivo, ContaBanco conta) {


        View vAttObjetivo = getLayoutInflater().inflate(R.layout.layout_add_edit_objetivo, null);

        final EditText edtTitulo = vAttObjetivo.findViewById(R.id.edtNome);
        final EditText edtValor = vAttObjetivo.findViewById(R.id.edtValor);
        final Switch sConcluido = vAttObjetivo.findViewById(R.id.sConcluido);

        edtTitulo.setText(objetivo.getNome());
        edtValor.setText(FormatUtils.emReal(objetivo.getValor()));
        sConcluido.setChecked(objetivo.estaConcluido());

        new CalculadorMonetario(edtValor, (valor, valorFormatado, editText) -> {
            editText.setText(valorFormatado);
            objetivo.setValor(valor);
        });
        Sheettalogo dialogo = new Sheettalogo(VerContas.this);
        dialogo.contentView(vAttObjetivo)
                .titulo(getString(R.string.EditarObjetivo))
                .icone(R.drawable.vec_objetivo)
                .botaoPositivo(getString(R.string.Concluir), v -> {
                    objetivo.setNomeObjetivo(edtTitulo.getText().toString());
                    if (objetivo.getValor() <= 0) objetivo.setValor(1);
                    objetivo.setConcluido(sConcluido.isChecked());
                    conta.attObjetivo(objetivo);
                    carregarContas();

                })
                .botaoNeutro(getString(R.string.Remover), v -> removerObjetivo(conta, objetivo))
                .botaoNegativo(getString(R.string.Cancelar), v -> {

                })

                .show();


    }

    private void removerObjetivo(ContaBanco conta, Objetivo objetivo) {

        Sheettalogo dialogo = new Sheettalogo(VerContas.this);
        dialogo.titulo(getString(R.string.Porfavorconfirme))
                .mensagem(getString(R.string.Desejamesmoremoveresteobjetivo))
                .icone(R.drawable.vec_objetivo)
                .botaoPositivo(getString(R.string.Cancelar), v -> {
                })
                .botaoNegativo(getString(R.string.RemoverObjetivo), v -> {
                    conta.removerObjetivo(objetivo);
                    carregarContas();
                })

                .show();

    }

    private void addContaBanco() {

        ContaBanco novaConta = new ContaBanco();

        View addContaBancoView = getLayoutInflater().inflate(R.layout.layout_add_edit_conta, null);

        final EditText edtTitulo = addContaBancoView.findViewById(R.id.edtNome);
        final EditText edtValor = addContaBancoView.findViewById(R.id.edtValor);
        new CalculadorMonetario(edtValor, (valor, valorFormatado, editText) -> {
            editText.setText(valorFormatado);
            novaConta.setValorConta(valor);
        });


        Sheettalogo dialogo = new Sheettalogo(VerContas.this);
        dialogo.contentView(addContaBancoView)
                .titulo(getString(R.string.Novaconta))
                .icone(R.drawable.vec_conta_banco)
                .botaoPositivo(getString(R.string.Concluir), v -> {

                    novaConta.setNomeConta(edtTitulo.getText().toString());
                    if (novaConta.getValor() <= 0) novaConta.setValorConta(1);
                    ContaBancos.addConta(novaConta);
                    carregarContas();

                })
                .botaoNegativo(getString(R.string.Cancelar), v -> {
                }).show();


    }

    private void editarContaBanco(ContaBanco conta) {


        View edtContaBancoView = getLayoutInflater().inflate(R.layout.layout_add_edit_conta, null);

        final EditText edtTitulo = edtContaBancoView.findViewById(R.id.edtNome);
        final EditText edtValor = edtContaBancoView.findViewById(R.id.edtValor);

        new CalculadorMonetario(edtValor, (valor, valorFormatado, editText) -> {
            editText.setText(valorFormatado);
            conta.setValorConta(valor);
        });
        edtTitulo.setText(conta.getNome());
        edtValor.setText(FormatUtils.emReal(conta.getValor()));

        Sheettalogo dialogo = new Sheettalogo(VerContas.this);
        dialogo.contentView(edtContaBancoView)
                .titulo(getString(R.string.EditarConta))
                .icone(R.drawable.vec_conta_banco)
                .botaoPositivo(getString(R.string.Concluir), v -> {

                    conta.setNomeConta(edtTitulo.getText().toString());
                    if (conta.getValor() <= 0) conta.setValorConta(1);
                    ContaBancos.attConta(conta);
                    carregarContas();

                })
                .botaoNegativo(getString(R.string.Cancelar), v -> {
                }).show();
    }

    private void confirmarEremoverContaBanco(final ContaBanco conta) {


        Sheettalogo dialogo = new Sheettalogo(VerContas.this);
        dialogo.titulo(getString(R.string.Porfavorconfirme))
                .icone(R.drawable.vec_conta_banco)
                .mensagem((getString(R.string.Desejamesmoremoverestaconta)))
                .botaoPositivo(getString(R.string.Cancelar), v -> {
                })
                .botaoNegativo(getString(R.string.Removerconta), v -> {
                    ContaBancos.removerContaBanco(conta);
                    carregarContas();

                }).show();


    }


}
