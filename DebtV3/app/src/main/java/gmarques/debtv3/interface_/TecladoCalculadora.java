
package gmarques.debtv3.interface_;


import android.app.Activity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.math.BigDecimal;
import java.math.RoundingMode;

import gmarques.debtv3.R;


/**
 * Criado por Gilian Marques em 09/12/2016.
 */
public class TecladoCalculadora implements View.OnClickListener {
    private Button btn0, btn1, btn2, btn3, btn4, btn5, btn6, btn7, btn8, btn9, btnPlus, btnMinus, btnMultiply, btnDiv, btnDot, btnEqual, btnClear;
    private final Activity mActivity;
    private final View rootView;
    private final KeyboardCallback callback;
    private EditText target;
    private String operatorOnScreen;
    private boolean lastDigitIsAnOperator;

    public TecladoCalculadora(Activity mActivity, KeyboardCallback callback) {
        this.mActivity = mActivity;
        rootView = View.inflate(mActivity, R.layout.layout_calculadora, null);
        this.callback = callback;
        initKeyboard();
        setListreners();
    }


    private void initKeyboard() {

        btn0 = rootView.findViewById(R.id.btn0);
        btn1 = rootView.findViewById(R.id.btn1);
        btn2 = rootView.findViewById(R.id.btn2);
        btn3 = rootView.findViewById(R.id.btn3);
        btn4 = rootView.findViewById(R.id.btn4);
        btn5 = rootView.findViewById(R.id.btn5);
        btn6 = rootView.findViewById(R.id.btn6);
        btn7 = rootView.findViewById(R.id.btn7);
        btn8 = rootView.findViewById(R.id.btn8);
        btn9 = rootView.findViewById(R.id.btn9);
        btnClear = rootView.findViewById(R.id.btnClear);
        btnPlus = rootView.findViewById(R.id.btnPlus);
        btnMinus = rootView.findViewById(R.id.btnMinus);
        btnMultiply = rootView.findViewById(R.id.btnMult);
        btnDiv = rootView.findViewById(R.id.btnDiv);
        btnDot = rootView.findViewById(R.id.btnDot);
        btnEqual = rootView.findViewById(R.id.btnEqual);


    }

    private void setListreners() {
        btn0.setOnClickListener(this);
        btn1.setOnClickListener(this);
        btn2.setOnClickListener(this);
        btn3.setOnClickListener(this);
        btn4.setOnClickListener(this);
        btn5.setOnClickListener(this);
        btn6.setOnClickListener(this);
        btn7.setOnClickListener(this);
        btn8.setOnClickListener(this);
        btn9.setOnClickListener(this);
        btnPlus.setOnClickListener(this);
        btnDot.setOnClickListener(this);
        btnMinus.setOnClickListener(this);
        btnMultiply.setOnClickListener(this);
        btnDiv.setOnClickListener(this);
        btnEqual.setOnClickListener(this);
        btnClear.setOnClickListener(this);
        btnClear.setOnLongClickListener(new View.OnLongClickListener() {
            @Override public boolean onLongClick(View v) {
                int length = target.getText().toString().length();
                for (int i = 0; i <length; i++)
                    removeOneFromScreen();

                return false;
            }
        });


    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btn0) onClickNumber(view);
        else if (view.getId() == R.id.btn1) onClickNumber(view);
        else if (view.getId() == R.id.btn2) onClickNumber(view);
        else if (view.getId() == R.id.btn3) onClickNumber(view);
        else if (view.getId() == R.id.btn4) onClickNumber(view);
        else if (view.getId() == R.id.btn5) onClickNumber(view);
        else if (view.getId() == R.id.btn6) onClickNumber(view);
        else if (view.getId() == R.id.btn7) onClickNumber(view);
        else if (view.getId() == R.id.btn8) onClickNumber(view);
        else if (view.getId() == R.id.btn9) onClickNumber(view);
        else if (view.getId() == R.id.btnDot) onClickPoint();
        else if (view.getId() == R.id.btnDiv) onClickOperator(view);
        else if (view.getId() == R.id.btnMinus) onClickOperator(view);
        else if (view.getId() == R.id.btnMult) onClickOperator(view);
        else if (view.getId() == R.id.btnPlus) onClickOperator(view);
        else if (view.getId() == R.id.btnEqual) onClickEqual();
        else if (view.getId() == R.id.btnClear) removeOneFromScreen();

        equalToCaucular();
    }

    //  eventos de clique

    private void onClickOperator(View v) {

        final Button mButton = (Button) v;

        if (operatorOnScreen == null) {

            if (!target.getText().toString().isEmpty()) {

                addToScreen(mButton.getText().toString());
                operatorOnScreen = mButton.getText().toString();

            }
        } else if (lastDigitIsAnOperator) {
            removeOneFromScreen();
            addToScreen(mButton.getText().toString());
            operatorOnScreen = mButton.getText().toString();
        } else {
            onClickEqual();
            addToScreen(mButton.getText().toString());
            operatorOnScreen = mButton.getText().toString();
        }


        //

    }

    private void onClickNumber(View v) {
        addToScreen(((Button) v).getText().toString());
    }

    private void onClickPoint() {
        //35.9*9.
        String displaying = target.getText().toString();
        if (operatorOnScreen != null && !lastDigitIsAnOperator)
            displaying = displaying.split("[-+x÷]")[1];
        if (!displaying.contains(".")) addToScreen(".");


    }

    private void onClickEqual() {
        if (btnEqual.getText().equals("=")) {
            if (operatorOnScreen != null && !lastDigitIsAnOperator) calcular();
        } else {
            // o usuario deu OK
            hide(true);
            callback.dismiss();
        }
    }


    //   Manipuladores de tela


    private void addToScreen(String add) {
        lastDigitIsAnOperator = isOperator(add);
        target.setText(target.getText() + "" + add);
        target.setSelection(target.getText().length());

    }

    /**
     * Remove um digito do atualTextview
     * <p>
     * Deve-se verificar se há conteudo na tela antes de chamar esse metodo
     */
    private void removeOneFromScreen() {
        String text = target.getText().toString();
        if (text.isEmpty()) return;
        text = text.substring(0, text.length() - 1);
        target.setText(text);
        target.setSelection(target.getText().length());

        if (text.length() > 0)
            lastDigitIsAnOperator = isOperator(text.substring(0, text.length() - 1));
        else lastDigitIsAnOperator = false;

        boolean thereAreOperator = false;

        if (text.contains("+") || text.contains("-") || text.contains("x") || text.contains("÷"))
            thereAreOperator = true;

        if (!thereAreOperator) operatorOnScreen = null;
    }


    // Relacionados a para calculos

    /**
     * Faz o calculo,  limpa a tela e seta o novo getArquivoDeSinc
     */
    private void calcular() {
        BigDecimal result = operate();
        if (result.toString().equals("erro")) {
            return;
        }
        String srtResult = result.toString();


        if (srtResult.endsWith(".00")) srtResult = srtResult.replace(".00", "");
        else if (srtResult.endsWith(".0")) srtResult = srtResult.replace(".0", "");
        target.setText("");
        addToScreen(srtResult);

    }

    /**
     * Calcula os valores inseridos  com base nos dados obtidos em getScreenContent() e  registra no log
     *
     * @return O getArquivoDeSinc do calculo
     */
    private BigDecimal operate() {
        String operation = target.getText().toString();
        String valorPrimario = operation.split("[-+x÷]")[0];
        String op = operatorOnScreen;
        String valorSecundario = operation.split("[-+x÷]")[1];

        operatorOnScreen = null;

        BigDecimal result;
        try {
            //noinspection IfCanBeSwitch
            if (op.equals("+"))
                result = new BigDecimal(valorPrimario).add(new BigDecimal(valorSecundario));
            else if (op.equals("-"))
                result = new BigDecimal(valorPrimario).subtract(new BigDecimal(valorSecundario));
            else if (op.equals("x"))
                result = new BigDecimal(valorPrimario).multiply(new BigDecimal(valorSecundario));
            else if (op.equals("÷"))
                result = new BigDecimal(valorPrimario).divide(new BigDecimal(valorSecundario), 2, RoundingMode.UP);
            else result = new BigDecimal("-1");
        } catch (Exception e) {
            e.printStackTrace();
            result = new BigDecimal("erro");
        }
        return result;
    }

    private boolean isOperator(String op) {
        switch (op) {
            case "+":
            case "-":
            case "x":
            case "÷":
                return true;
            default:
                return false;
        }
    }
    //

    /**
     * Analiza os dados inseridos e define a funçao adequada para o botao "Equals"
     */
    private void equalToCaucular() {
        if (operatorOnScreen != null) btnEqual.setText("=");
        else btnEqual.setText(mActivity.getString(R.string.OK));
    }


    public View show(EditText target) {
        this.target = target;

        BigDecimal valInitial = FormatUtils.emDecimal(target.getText().toString());
        String tInicial = valInitial.toString();
        target.setText("");

        if (tInicial.endsWith(".00")) tInicial = tInicial.replace(".00", "");
        else if (tInicial.endsWith(".0")) tInicial = tInicial.replace(".0", "");

        addToScreen(valInitial.doubleValue() > 0 ? tInicial : "");

        return rootView;

    }

    private void hide(boolean retonarValor) {
        String ultimoResultado = target.getText().toString().equals("") ? "0" : target.getText().toString();
        BigDecimal bg = new BigDecimal(ultimoResultado);
        if (bg.doubleValue() < 0) return;

        if (retonarValor) callback.valueSet(bg, FormatUtils.emReal(Float.parseFloat(ultimoResultado)));


    }

    public interface KeyboardCallback {
        void valueSet(BigDecimal decimal, String real);

        void dismiss();
    }
}
