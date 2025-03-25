package gmarques.debtv3.activities.ver_receitas;

import android.graphics.Color;
import android.view.View;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;

import org.joda.time.LocalDate;

import java.math.BigDecimal;
import java.util.ArrayList;

import gmarques.debtv3.R;
import gmarques.debtv3.gestores.Receitas;
import gmarques.debtv3.interface_.FormatUtils;
import gmarques.debtv3.interface_.UIUtils;
import gmarques.debtv3.modelos.Receita;

/**
 * Criado por Gilian Marques
 * Domingo, 28 de Julho de 2019  as 19:36:26.
 */
public class GraficoDeLinhaComInfo {

    private LineChart lineChart;
    private final int windowBackground = UIUtils.corAttr(android.R.attr.windowBackground);
    private final int primary = UIUtils.corAttr(android.R.attr.colorPrimary);
    private ArrayList<Entry> dados;

    private Receita receita;

    public GraficoDeLinhaComInfo(LineChart lineChart, Receita receita) {
        this.lineChart = lineChart;
        this.receita = receita;
        lineChart.setVisibility(View.GONE);
        carregarDados();

        if (dados.size() > 1) atualizarGrafico();

    }

    private void carregarDados() {

        dados = new ArrayList<>();

        ArrayList<Receita> receitas = Receitas.getReceitas(receita.getNome());

        for (Receita receita : receitas) {
            if (receita.getMesId() == Receita.RECORRENTE || receita.getMesId() == Receita.AUTOIMPORTADA)
                continue;
            dados.add(new Entry(receita.getDataDeRecebimento(), receita.getValor()));
        }

    }


    private void atualizarGrafico() {
        LineDataSet dataSet;

        if (lineChart.getData() != null) {
            dataSet = (LineDataSet) lineChart.getData().getDataSetByIndex(0);
            dataSet.setValues(dados);
            lineChart.getData().notifyDataChanged();
            lineChart.notifyDataSetChanged();
            lineChart.setVisibility(View.VISIBLE);
            lineChart.animateY(650, Easing.EaseInOutExpo);
        } else {


            // dataset ----------------------------------------------------
            dataSet = new LineDataSet(dados, "");

            dataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
            dataSet.setColor(primary);
            dataSet.setCircleColor(primary);
            dataSet.setLineWidth(3f);

            dataSet.setCircleRadius(3f);
            dataSet.setMode(LineDataSet.Mode.HORIZONTAL_BEZIER);
            /*o grad*/
            // dataSet.setFillDrawable(ContextCompat.getDrawable(Debt.binder.get(), R.drawable.back_gradiente_grafico_linha));
            dataSet.setHighLightColor(Color.RED);

            dataSet.setDrawCircleHole(false);

            dataSet.setValueFormatter(new ValueFormatter() {
                @Override
                public String getFormattedValue(float value) {
                    return "";//FormatUtils.emReal(value ) + "";
                }
            });
            dataSet.setDrawFilled(false);
            dataSet.setDrawHorizontalHighlightIndicator(false);
            dataSet.setVisible(true);
            //     dataSet.setCircleHoleColor(primary);
            //-------------------------------------------------------------------
            // create a data object with the data sets
            LineData data = new LineData(dataSet);
            data.setValueTextColor(primary);
            data.setValueTextSize(9f);

            //customize -------------------------------------------------------------
            XAxis xAxis = lineChart.getXAxis();

            xAxis.setDrawAxisLine(false);
            xAxis.setDrawGridLines(true);
            xAxis.setDrawLimitLinesBehindData(false);
            xAxis.setDrawGridLinesBehindData(false);
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM_INSIDE);
            xAxis.setYOffset(5);
            xAxis.setTextColor(primary);
            xAxis.setValueFormatter(new ValueFormatter() {
                @Override
                public String getAxisLabel(float value, AxisBase axis) {
                    String date = FormatUtils.formatarDataCurta(new LocalDate(new BigDecimal(value).longValue()));
                    return date.substring(2);//remove the day from string. '22 AGO' becomes 'AGO'

                }
            });
            //-----------------------------------------------------------------------
            YAxis leftYAxis = lineChart.getAxisLeft();
            leftYAxis.setEnabled(false);

            YAxis rightYAxis = lineChart.getAxisRight();
            rightYAxis.setEnabled(false);

            // xAxis.setEnabled(false);

            Description description = new Description();
            description.setText("");
            lineChart.setDescription(description);

            // dismiss legend (that colored square at bottom of chart)
            Legend legend = lineChart.getLegend();
            legend.setEnabled(false);

            float offset = UIUtils.dp(10);
            lineChart.setViewPortOffsets(offset, offset * 2, offset, offset);


            // lineChart.setAutoScaleMinMaxEnabled(true);
            // avoid repeated x values
            lineChart.getAxisLeft().setGranularityEnabled(false);
            lineChart.getAxisLeft().setGranularity(1f);
            lineChart.getAxisLeft().setLabelCount(dados.size(), true);

            lineChart.setDoubleTapToZoomEnabled(false);
            lineChart.setHighlightPerDragEnabled(true);
            lineChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
                @Override
                public void onValueSelected(Entry e, Highlight h) {
                    UIUtils.infoToasty(FormatUtils.emReal(e.getY()));
                    UIUtils.vibrar(0);
                }

                @Override
                public void onNothingSelected() {

                }
            });


            // set data
            lineChart.setData(data);
            lineChart.invalidate();
            lineChart.setVisibility(View.VISIBLE);
            lineChart.animateY(650, Easing.EaseInOutExpo);
            lineChart.fitScreen();


        }

    }

}
