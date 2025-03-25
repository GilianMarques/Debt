package gmarques.debtv3.activities.dashboard;

import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.databinding.DataBindingUtil;

import org.jetbrains.annotations.NotNull;
import org.joda.time.LocalDate;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import gmarques.debtv3.Debt;
import gmarques.debtv3.R;
import gmarques.debtv3.databinding.FragmentVariacaoDaReceitaBinding;
import gmarques.debtv3.interface_.FormatUtils;
import gmarques.debtv3.interface_.UIUtils;
import gmarques.debtv3.modelos.Despesa;
import gmarques.debtv3.modelos.Receita;
import gmarques.debtv3.outros.Broadcaster;
import gmarques.debtv3.outros.MyFragment;
import io.realm.RealmList;
import lecho.lib.hellocharts.listener.ColumnChartOnValueSelectListener;
import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.AxisValue;
import lecho.lib.hellocharts.model.Column;
import lecho.lib.hellocharts.model.ColumnChartData;
import lecho.lib.hellocharts.model.SubcolumnValue;
import lecho.lib.hellocharts.view.ColumnChartView;

import static gmarques.debtv3.gestores.Meses.mesAtual;


public class VariacaoDaReceita extends MyFragment {
    private FragmentVariacaoDaReceitaBinding ui;
    private RealmList<Receita> receitas;
    private RealmList<Despesa> despesas;
    private ColumnChartView chart;
    private List<Column> colunas;
    private List<AxisValue> eixoInfo;
    private LocalDate hoje;
    private int colorPrimary;
    private int colorAccent;

    public VariacaoDaReceita() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ui = DataBindingUtil.inflate(inflater, R.layout.fragment_variacao_da_receita, container, false);
        return ui.getRoot();
    }

    @Override
    protected void atualizar() {
        inicializar();
    }

    @Override
    protected String getAçaoBroadcast() {
        return Broadcaster.atualizarGraficoDeBarras;
    }

    @Override
    protected void inicializar() {
        chart = ui.barChart;
        receitas = mesAtual.getReceitas();
        despesas = mesAtual.getDespesas();
        if (receitas.size() == 0 || despesas.size() == 0) {
            ((MainActivity) getActivity()).atualizarBalançoDoDia(-999);
            ui.getRoot().setVisibility(View.GONE);
            return;
        } else ui.getRoot().setVisibility(View.VISIBLE);


        hoje = new LocalDate();
        colorPrimary = UIUtils.corAttr(android.R.attr.colorPrimary);
        colorAccent = UIUtils.corAttr(android.R.attr.colorAccent);
        calcularDados();
        inicializarGrafico();

    }


    private void inicializarGrafico() {

        Axis eixoX = new Axis();
        eixoX.setValues(eixoInfo);
        eixoX.setHasLines(true);
        eixoX.setHasSeparationLine(false);
        eixoX.setTextColor(UIUtils.corAttr(android.R.attr.colorPrimary));
        eixoX.setTypeface(Typeface.createFromAsset(Debt.binder.get().getAssets(), "ProductSansBold"));


        Axis eixoY = new Axis();
        eixoY.setHasLines(true);
        eixoY.setHasSeparationLine(false);

        eixoY.setTextColor(0);
        eixoY.setInside(true);

        ColumnChartData data = new ColumnChartData(colunas);
        data.setAxisXBottom(eixoX);
        data.setAxisYLeft(eixoY);
        data.setFillRatio(0.6f);/*1.0 = barras coladas umas com as outras 0.0f barras com grossura de 2px*/


        chart.setZoomEnabled(true);
        chart.setOnValueTouchListener(new ColumnChartOnValueSelectListener() {
            @Override
            public void onValueSelected(int columnIndex, int subcolumnIndex, SubcolumnValue value) {
                UIUtils.vibrar(0);
            }

            @Override
            public void onValueDeselected() {

            }
        });
        chart.setColumnChartData(data);
        chart.resetViewports();

    }


    private void calcularDados() {
        colunas = new ArrayList<>();
        eixoInfo = new ArrayList<>();
        LinkedHashMap<Integer, Float> valores = new LinkedHashMap<>();

        LocalDate dia = new LocalDate(mesAtual.getAno(), mesAtual.getMes(), 1);
        long diaMillis = dia.toDate().getTime();

        float despesaDoDia = 0;
        float receitaDoDia = 0;

        do {

            int varDia = 0;

            for (Receita receita : receitas)
                if (receita.estaRecebido()) {
                    if (new LocalDate(receita.getDataEmQueFoiRecebida()).toDate().getTime() == diaMillis) {
                        varDia++;
                        receitaDoDia = new BigDecimal(receitaDoDia).add(new BigDecimal(receita.getValor())).floatValue();
                    }
                } else {
                    if (new LocalDate(receita.getDataDeRecebimento()).toDate().getTime() == diaMillis) {
                        Log.d("USUK", "VariacaoDaReceita.calcularDados: n "+receita.getNome()+"  "+receita.getValor());
                        varDia++;
                        receitaDoDia = new BigDecimal(receitaDoDia).add(new BigDecimal(receita.getValor())).floatValue();
                    }
                }

            for (Despesa despesa : despesas)
                if (despesa.estaPaga()) {
                    if (new LocalDate(despesa.getDataEmQueFoiPaga()).toDate().getTime()  == diaMillis) {
             //           Log.d(Tag.AppTag, "VariacaoDaReceita.calcularDados: " + dia.getDayOfMonth() + " " + despesa.getNome() + "------------------------------- 1592697600000 utc hoje");
                        varDia++;
                        despesaDoDia = new BigDecimal(despesaDoDia).add(new BigDecimal(despesa.getValor())).floatValue();
                    }

                } else {
                    if (new LocalDate(despesa.getDataDePagamento()).toDate().getTime()  == diaMillis) {
                        Log.d("USUK", "VariacaoDaReceita.calcularDados: n "+despesa.getNome()+"  "+despesa.getValor());
                        varDia++;
                        despesaDoDia = new BigDecimal(despesaDoDia).add(new BigDecimal(despesa.getValor())).floatValue();
                    }
                }


            float balancoDoDia = new BigDecimal(receitaDoDia).subtract(new BigDecimal(despesaDoDia)).floatValue();
         //   Log.d(Tag.AppTag, "VariacaoDaReceita.calcularDados: " + dia.getDayOfMonth() + ": " + balancoDoDia);
            if (varDia > 0)
                valores.put(dia.getDayOfMonth(), balancoDoDia);

            Log.d("USUK", "VariacaoDaReceita.calcularDados: "+balancoDoDia);


            if (dia.getDayOfMonth() == hoje.getDayOfMonth())
                ((MainActivity) getActivity()).atualizarBalançoDoDia(balancoDoDia);
            dia = dia.plusDays(1);
            diaMillis = dia.toDate().getTime();

        } while (dia.getMonthOfYear() == mesAtual.getMes());

        colunas.clear();


        for (int diaN : valores.keySet()) {
            float valorDoDia = valores.get(diaN);

            ArrayList<SubcolumnValue> subColunas = new ArrayList<>();
            subColunas.add(new SubcolumnValue(valorDoDia, (valorDoDia < 0 ? colorAccent : colorPrimary)).setLabel(FormatUtils.emReal(valorDoDia)));

            Column coluna = new Column(subColunas);
            coluna.setHasLabelsOnlyForSelected(true);

            colunas.add(coluna);
            eixoInfo.add(new AxisValue(eixoInfo.size()).setLabel(MessageFormat.format("Dia {0}", diaN)));
        }

    }


}
