package gmarques.debtv3.interface_;


import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AnticipateOvershootInterpolator;
import android.widget.TextView;

import com.google.android.flexbox.FlexboxLayout;

import org.joda.time.LocalDate;

import java.util.Locale;

import gmarques.debtv3.R;
import gmarques.debtv3.activities.MyActivity;

public class CalendarioMensal {

    private final FlexboxLayout container;
    private final View calendarioView;
    private MyActivity myActivity;
    private LocalDate data = new LocalDate();
    private TextView celulaHoje;
    private Callback callback;
    private View marcador;


    public CalendarioMensal(MyActivity myActivity, ViewGroup container, Callback callback) {
        this.callback = callback;

        calendarioView = myActivity.getLayoutInflater().inflate(R.layout.view_calendario_mensal, container, true);

        this.myActivity = myActivity;
        this.container = calendarioView.findViewById(R.id.container);

        inicializarNomeMes();
        inicializarCelulas();
        inicializarMarcador();
    }

    private void inicializarMarcador() {
        marcador = calendarioView.findViewById(R.id.marcador);
        marcador.setVisibility(View.GONE);

        celulaHoje.post(() -> {
            marcador.setVisibility(View.VISIBLE);
            celulaHoje.performClick();
        });
    }

    private TextView getCelula(int diaAlvo) {

        for (int i = 0; i < container.getChildCount(); i++) {
            TextView textView = (TextView) container.getChildAt(i);
            int dia = Integer.parseInt(textView.getText().toString());
            if (dia == diaAlvo) return textView;
        }
        return null;
    }

    private void inicializarNomeMes() {
        TextView tvMes = calendarioView.findViewById(R.id.tvMesNome);
        tvMes.setText(data.monthOfYear().getAsText(Locale.getDefault()) + ", " + data.year().getAsText());

    }

    private void inicializarCelulas() {

        LocalDate dataLimite = data.plusMonths(1).withDayOfMonth(1).minusDays(1);


        for (int i = 0; i < container.getChildCount(); i++) {
            TextView textView = (TextView) container.getChildAt(i);
            int dia = Integer.parseInt(textView.getText().toString());

            if (dia > dataLimite.getDayOfMonth()) {
                textView.setTextColor(UIUtils.cor(R.color.brancoA50));
            } else textView.setOnClickListener(getEventoDeClique());

            if (dia == data.getDayOfMonth()) celulaHoje = textView;

        }

    }

    private View.OnClickListener getEventoDeClique() {
        return new AnimatedClickListener() {
            @Override
            public void onClick(View view) {
                super.onClick(view);
                int dia = Integer.parseInt(((TextView) view).getText().toString());
                moverMarcador(view);
                celulaHoje = (TextView) view;
                callback.dataSelecionada(data.withDayOfMonth(dia));

            }
        };
    }

    private void moverMarcador(View alvo) {
        long duraçao = 350;
        ObjectAnimator x = ObjectAnimator.ofFloat(marcador, "x", marcador.getX(), alvo.getX());
        x.setInterpolator(new AnticipateOvershootInterpolator());
        x.setDuration(duraçao);
        x.start();

        ObjectAnimator y = ObjectAnimator.ofFloat(marcador, "y", marcador.getY(), alvo.getY());
        y.setInterpolator(new AnticipateOvershootInterpolator());
        y.setDuration(duraçao);
        y.start();

        ObjectAnimator escalaX = ObjectAnimator.ofFloat(marcador, "scaleX", 1.0f, 0.8f);
        ObjectAnimator escalaY = ObjectAnimator.ofFloat(marcador, "scaleY", 1.0f, 0.8f);
        escalaX.setDuration(duraçao / 4);
        escalaX.setRepeatCount(1);
        escalaX.setRepeatMode(ValueAnimator.REVERSE);
        escalaX.setInterpolator(new AccelerateDecelerateInterpolator());

        escalaY.setDuration(duraçao / 4);
        escalaY.setRepeatCount(1);
        escalaY.setRepeatMode(ValueAnimator.REVERSE);
        escalaY.setInterpolator(new AccelerateDecelerateInterpolator());

        escalaX.start();
        escalaY.start();
    }

    public interface Callback {
        void dataSelecionada(LocalDate data);
    }
}
