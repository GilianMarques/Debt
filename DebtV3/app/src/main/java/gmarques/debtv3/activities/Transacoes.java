package gmarques.debtv3.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.databinding.DataBindingUtil;

import org.joda.time.Days;
import org.joda.time.LocalDate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Locale;

import gmarques.debtv3.R;
import gmarques.debtv3.databinding.ActivityTransacoesBinding;
import gmarques.debtv3.gestores.Categorias;
import gmarques.debtv3.interface_.AnimatedClickListener;
import gmarques.debtv3.interface_.FormatUtils;
import gmarques.debtv3.interface_.UIUtils;
import gmarques.debtv3.modelos.Categoria;
import gmarques.debtv3.modelos.Despesa;
import gmarques.debtv3.modelos.Receita;
import gmarques.debtv3.outros.Tag;
import io.realm.RealmList;

import static gmarques.debtv3.gestores.Meses.mesAtual;

public class Transacoes extends MyActivity {
    private ActivityTransacoesBinding ui;
    private RealmList<Receita> receitas;
    private RealmList<Despesa> despesas;
    private boolean exibirTudo = false;
    private int indiceDiaSelecionado;
    private View viewDiaSelecionada;
    private LocalDate hoje = new LocalDate();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ui = DataBindingUtil.setContentView(this, R.layout.activity_transacoes);

        ui.toolbar.setTitle(String.format(Locale.getDefault(), getString(R.string.TransacoesdeX), mesAtual.getNome()));

        inicializarObjetos();
        calcularDadosEchamarMetodosPraAtualizarUI();
        inicializarFabAlternar();
    }

    private void inicializarFabAlternar() {
        ui.fabAlertnerVista.setOnClickListener(new AnimatedClickListener() {
            @Override
            public void onClick(View view) {
                super.onClick(view);
                exibirTudo = !exibirTudo;

                ui.containerDias.removeAllViews();
                ui.containerDespesasReceitas.removeAllViews();

                calcularDadosEchamarMetodosPraAtualizarUI();

            }
        });
    }

    private void inicializarObjetos() {
        receitas = mesAtual.getReceitas();
        despesas = mesAtual.getDespesas();
    }

    private void calcularDadosEchamarMetodosPraAtualizarUI() {

        indiceDiaSelecionado = -1;

        LocalDate dia = new LocalDate(mesAtual.getAno(), mesAtual.getMes(), 1);
        long diaMillis = dia.toDate().getTime();

        float despesaDoDia = 0;
        float receitaDoDia = 0;

        do {

            ArrayList<Despesa> despesasDoDia = new ArrayList<>();
            ArrayList<Receita> receitasDoDia = new ArrayList<>();

            for (Receita receita : receitas) {
                if (receita.estaRecebido()) {
                    if (new LocalDate(receita.getDataEmQueFoiRecebida()).toDate().getTime() == diaMillis) {
                        receitasDoDia.add(receita);
                        receitaDoDia = new BigDecimal(receitaDoDia).add(new BigDecimal(receita.getValor())).floatValue();
                    }
                } else {
                    if (new LocalDate(receita.getDataDeRecebimento()).toDate().getTime() == diaMillis) {
                        receitasDoDia.add(receita);
                        receitaDoDia = new BigDecimal(receitaDoDia).add(new BigDecimal(receita.getValor())).floatValue();
                    }
                }
            }

            for (Despesa despesa : despesas)
                if (despesa.estaPaga()) {
                    Log.d(Tag.AppTag, "Transacoes.calcularDadoa: " + despesa.getNome() + " d2: " + new LocalDate(despesa.getDataEmQueFoiPaga()).toString("dd/MM/YYYY") + " " + despesa.getDataEmQueFoiPaga()
                            + " " + dia.toString("dd/MM/YYYY") + " " + diaMillis);
                    if (new LocalDate(despesa.getDataEmQueFoiPaga()).toDate().getTime() == diaMillis) {
                        despesasDoDia.add(despesa);
                        despesaDoDia = new BigDecimal(despesaDoDia).add(new BigDecimal(despesa.getValor())).floatValue();
                    }
                } else {
                    if (new LocalDate(despesa.getDataDePagamento()).toDate().getTime()== diaMillis) {
                        despesasDoDia.add(despesa);
                        despesaDoDia = new BigDecimal(despesaDoDia).add(new BigDecimal(despesa.getValor())).floatValue();
                    }
                }

            float balancoDoDia = new BigDecimal(receitaDoDia).subtract(new BigDecimal(despesaDoDia)).floatValue();

            if (receitasDoDia.size() > 0 || despesasDoDia.size() > 0) {


                if (exibirTudo) {
                    ui.containerDias.setVisibility(View.GONE);
                    ui.detalhesContainer.setVisibility(View.GONE);
                    carregarMovimentaçoesDoDia(dia, balancoDoDia, receitasDoDia, despesasDoDia);

                } else {

                    if (dia.getDayOfMonth() <= hoje.getDayOfMonth()) indiceDiaSelecionado++;
                    criarViewDia(dia, balancoDoDia, receitasDoDia, despesasDoDia);
                }

            }

            dia = dia.plusDays(1);
            diaMillis = dia.toDate().getTime();

        } while (dia.getMonthOfYear() == mesAtual.getMes());

        /*estas views sao ocultadas quando o usuario decide ver todos os registros*/
        if (!exibirTudo && ui.containerDias.getVisibility() == View.GONE) {
            ui.containerDias.setVisibility(View.VISIBLE);
            ui.detalhesContainer.setVisibility(View.VISIBLE);
        }

        /*simula um clique no dia selecionado, caso esteja exibindo as transaçoes por dia*/
        if (indiceDiaSelecionado >= 0) {
            ui.containerDias.getChildAt(indiceDiaSelecionado).performClick();
        }

    }

    private void criarViewDia(LocalDate dia, float balancoDoDia, ArrayList<Receita> receitasDoDia, ArrayList<Despesa> despesasDoDia) {
        int cor = UIUtils.cor(R.color.colorPrimary);
        View diaView = getLayoutInflater().inflate(R.layout.layout_transacao_dia, null, false);

        TextView tvDiaNome = diaView.findViewById(R.id.tvDiaNome);
        TextView tvDiaNumero = diaView.findViewById(R.id.tvDiaNumero);
        View fundo = diaView.findViewById(R.id.fundo);

        String diaNome = dia.dayOfWeek().getAsShortText();
        tvDiaNome.setText(diaNome.replace(String.valueOf(diaNome.charAt(0)), String.valueOf(diaNome.charAt(0)).toUpperCase()));
        tvDiaNumero.setText(dia.getDayOfMonth() + "");

        diaView.setOnClickListener(new AnimatedClickListener() {
            @Override
            public void onClick(View view) {
                super.onClick(view);

                if (dia.getDayOfMonth() > new LocalDate().getDayOfMonth())
                    ui.tvInfo2.setVisibility(View.VISIBLE);
                else ui.tvInfo2.setVisibility(View.GONE);

                ui.containerDespesasReceitas.removeAllViews();

                if (viewDiaSelecionada != null) {

                    TextView tvDiaNome = viewDiaSelecionada.findViewById(R.id.tvDiaNome);
                    TextView tvDiaNumero = viewDiaSelecionada.findViewById(R.id.tvDiaNumero);
                    View fundo = viewDiaSelecionada.findViewById(R.id.fundo);

                    fundo.setVisibility(View.GONE);
                    tvDiaNome.setTextColor(Color.WHITE);
                    tvDiaNumero.setTextColor(Color.WHITE);
                }

                fundo.setVisibility(View.VISIBLE);
                tvDiaNome.setTextColor(cor);
                tvDiaNumero.setTextColor(cor);
                viewDiaSelecionada = diaView;
                carregarMovimentaçoesDoDia(dia, balancoDoDia, receitasDoDia, despesasDoDia);
            }
        });
        ui.containerDias.addView(diaView);

    }

    private void carregarMovimentaçoesDoDia(LocalDate dia, float balancoDoDia, ArrayList<Receita> receitasDoDia, ArrayList<Despesa> despesasDoDia) {

        criarViewsDasReceitas(dia, receitasDoDia);
        criarViewsDasDespesas(dia, despesasDoDia);
        atualizarCards(balancoDoDia, receitasDoDia, despesasDoDia);


    }

    private void atualizarCards(float balancoDoDia, ArrayList<Receita> receitasDoDia, ArrayList<Despesa> despesasDoDia) {
        BigDecimal despesas = new BigDecimal("0");
        BigDecimal receitas = new BigDecimal("0");

        for (Despesa despesa : despesasDoDia)
            despesas = despesas.add(new BigDecimal(despesa.getValor()));

        for (Receita receita : receitasDoDia)
            receitas = receitas.add(new BigDecimal(receita.getValor()));

        ui.tvReceitaDia.setText(FormatUtils.emReal(receitas.floatValue()));
        ui.tvDespesaDia.setText(FormatUtils.emReal(despesas.floatValue()));
        ui.tvBalancoDia.setText(FormatUtils.emReal(balancoDoDia));
    }

    private void criarViewsDasReceitas(LocalDate dia, ArrayList<Receita> receitasDoDia) {
        for (Receita receita : receitasDoDia) {
            View receitaView = getLayoutInflater().inflate(R.layout.layout_transacao_receita, null, false);

            TextView tvNome = receitaView.findViewById(R.id.tvNome);
            TextView tvValor = receitaView.findViewById(R.id.tvValor);
            TextView tvInfo = receitaView.findViewById(R.id.tvInfo);

            TextView tvDiaNome = receitaView.findViewById(R.id.tvDiaNome);
            TextView tvDiaNumero = receitaView.findViewById(R.id.tvDiaNumero);
            View dataparent = receitaView.findViewById(R.id.dataParent);

            /*Se exibirTudo for true, a data da transação deve ser exibida junto com ela*/
            if (exibirTudo) {
                String diaNome = dia.dayOfWeek().getAsShortText();
                tvDiaNome.setText(diaNome.replace(String.valueOf(diaNome.charAt(0)), String.valueOf(diaNome.charAt(0)).toUpperCase()));
                tvDiaNumero.setText(dia.getDayOfMonth() + "");
            } else dataparent.setVisibility(View.GONE);

            tvNome.setText(receita.getNome());
            tvValor.setText(FormatUtils.emReal(receita.getValor()));
            if (receita.estaRecebido()) {

                int diasEntre = Days.daysBetween(new LocalDate(receita.getDataEmQueFoiRecebida()), new LocalDate(receita.getDataDeRecebimento())).getDays();

                if (diasEntre == 0) tvInfo.setText(R.string.Empossenoprazoestipuilado);
                else if (diasEntre > 0)
                    tvInfo.setText(String.format(Locale.getDefault(), getString(R.string.ReceitaempossecomXdiasde), diasEntre));
                else {
                    diasEntre = -1 * diasEntre;
                    tvInfo.setText(String.format(Locale.getDefault(), getString(R.string.ReceitaempossecomXdiasdeAtraso), diasEntre));
                }
            } else {

                int diasEntre = Days.daysBetween(new LocalDate(), new LocalDate(receita.getDataDeRecebimento())).getDays();

                if (diasEntre >= 0) tvInfo.setText(R.string.Areceber);
                else {
                    diasEntre = -1 * diasEntre;
                    tvInfo.setText(String.format(Locale.getDefault(), getString(R.string.NaorecebidaXdiasdeatraso), diasEntre));
                }
            }

            ui.containerDespesasReceitas.addView(receitaView);

        }

    }

    private void criarViewsDasDespesas(LocalDate dia, ArrayList<Despesa> despesasDoDia) {
        for (Despesa despesa : despesasDoDia) {

            Categoria categoria = Categorias.getCategoria(despesa.getCategoriaId());

            View despesaView = getLayoutInflater().inflate(R.layout.layout_transacao_despesa, null, false);

            TextView tvNome = despesaView.findViewById(R.id.tvNome);
            TextView tvValor = despesaView.findViewById(R.id.tvValor);
            TextView tvInfo = despesaView.findViewById(R.id.tvInfo);

            ImageView ivIcone = despesaView.findViewById(R.id.ivIcone);
            ivIcone.setImageDrawable(UIUtils.aplicarTema(Categorias.getIntIcone(categoria.getIcone()), UIUtils.cor(R.color.colorPrimary)));


            TextView tvDiaNome = despesaView.findViewById(R.id.tvDiaNome);
            TextView tvDiaNumero = despesaView.findViewById(R.id.tvDiaNumero);
            View dataparent = despesaView.findViewById(R.id.dataParent);

            /*Se exibirTudo for true, a data da transação deve ser exibida junto com ela*/
            if (exibirTudo) {
                String diaNome = dia.dayOfWeek().getAsShortText();
                tvDiaNome.setText(diaNome.replace(String.valueOf(diaNome.charAt(0)), String.valueOf(diaNome.charAt(0)).toUpperCase()));
                tvDiaNumero.setText(dia.getDayOfMonth() + "");
            } else dataparent.setVisibility(View.GONE);


            tvNome.setText(despesa.getNome());
            tvValor.setText(FormatUtils.emReal(-1 * despesa.getValor()));
            if (despesa.estaPaga()) {

                int diasEntre = Days.daysBetween(new LocalDate(despesa.getDataEmQueFoiPaga()), new LocalDate(despesa.getDataDePagamento())).getDays();

                if (diasEntre == 0) tvInfo.setText(R.string.Pagonoprazoestipuilado);
                else if (diasEntre > 0)
                    tvInfo.setText(String.format(Locale.getDefault(), getString(R.string.DespesapagacomXdiasde), diasEntre));
                else {
                    diasEntre = -1 * diasEntre;
                    tvInfo.setText(String.format(Locale.getDefault(), getString(R.string.DespesapagacomXdiasdeAtraso), diasEntre));
                }
            } else {

                int diasEntre = Days.daysBetween(new LocalDate(), new LocalDate(despesa.getDataDePagamento())).getDays();

                if (diasEntre >= 0) tvInfo.setText(R.string.Apagar);
                else {
                    diasEntre = -1 * diasEntre;
                    tvInfo.setText(String.format(Locale.getDefault(), getString(R.string.NaopagaXdiasdeatraso), diasEntre));
                }
            }

            ui.containerDespesasReceitas.addView(despesaView);

        }

    }

}