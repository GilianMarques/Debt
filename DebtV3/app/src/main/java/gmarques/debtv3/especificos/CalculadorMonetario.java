package gmarques.debtv3.especificos;


import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Locale;

import gmarques.debtv3.Debt;
import gmarques.debtv3.R;
import gmarques.debtv3.interface_.FormatUtils;
import gmarques.debtv3.interface_.UIUtils;
import gmarques.debtv3.outros.Tag;

/*Faz calculos com base em entradas do softkeyboard
SE SETA COMO LISTENER DE FOCO DA VIEW ENTAO QUALQUER LISTENER DE FOCO REGISTRADO ANTES NAO VAI FUNCIONAR
E SE FOR REGISTRADO DEPOIS VAI IMPEDIR O FUNCIONAMENTO CORRETO DESSA CLASSE. USE O LISTENER DEFINIDO NESSA CLASSE
PARA SABER SE A VIEW GANHOU OU PERDEU FOCO

O MESMO VALE PARA OUVIR TECLA 'DONE' DO TECLADO

* */
public class CalculadorMonetario implements View.OnFocusChangeListener, TextView.OnEditorActionListener {

    private final EditText editText;
    private final Callback callback;
    private View.OnFocusChangeListener listenerDeFocoExterno;// para compartilhar o estado de foco da view com 3°s
    private TextView.OnEditorActionListener onEditorActionListenerExterno;
    private boolean viewTemFoco;
    private boolean ignorarProximaEntrada;
    private String valorExistenteQAdoViewGanhouFoco = "";

    public CalculadorMonetario(EditText editText, Callback callback) {
        this.editText = editText;
        this.callback = callback;
        addListenersAView();
    }

    /**
     * Queria adicionar o {@link TextWatcher} na view sem criar tantos metodos adicionais entao fiz tudo
     * dentro desse metodo
     */
    private void addListenersAView() {
        //se usuario apertar o botao 'done' do teclado (se esta for a açao definida pra view) ele limpa o foco
        editText.setOnEditorActionListener(this);
        editText.setOnFocusChangeListener(this);
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (ignorarProximaEntrada) {
                    ignorarProximaEntrada = false;
                    Log.d(Tag.AppTag, "CalculadorMonetario.afterTextChanged: entrada ignorada");
                } else if (viewTemFoco) {
                    if (s.length() > 0)
                        processarEntrada(s.toString(), String.valueOf(s.charAt(s.length() - 1)));
                }
            }
        });
    }

    private void processarEntrada(String entrada, String ultimoChar) {
        Log.d(Tag.AppTag, "CalculadorMonetario.processarEntrada: ");
        // filtro caracteres nao aceitos
        if (!ehUmCaractereValido(ultimoChar)) {
            atualizarEdittext(entrada.substring(0, entrada.length() - 1));
            return;
        }

        int operadores = 0;
        int sinalIgual = 0;
        for (String mChar : entrada.split("")) {
            Log.d(Tag.AppTag, "CalculadorMonetario.processarEntrada: " + mChar + "  " + ehUmOperador(mChar));
            if (ehUmOperador(mChar)) operadores++;
            if (ehSinalDeIgual(mChar)) sinalIgual++;
        }

        Log.d(Tag.AppTag, "CalculadorMonetario.processarEntrada: op " + operadores + "  sIgual " + sinalIgual);

        if (sinalIgual > 0) calcular(entrada);
        else if (operadores > 1) calcular(entrada);
    }

    private boolean calcular(String entrada) {
        try {
            //-------------  -----------------  ---------------------  ----------------------  -----------------
            entrada = removerPontosMultiplos(entrada);
            String ultimoChar = String.valueOf(entrada.charAt(entrada.length() - 1)); // se espera que seja um operador ou '=' se for, remove ele
            if (ehUmOperador(ultimoChar) || ehSinalDeIgual(ultimoChar))
                entrada = entrada.substring(0, entrada.length() - 1);
            String operador = entrada.replaceAll("[^*+-/]", "").replace(".", "");

            Log.d(Tag.AppTag, "CalculadorMonetario.calcular: formula:" + entrada + "  op: " + operador+"  entrada ("+entrada+")");

            if (operador.isEmpty()) return true;

            String valorPrimario = entrada.split("[-+*/]")[0];
            String valorSecundario = entrada.split("[-+*/]")[1];

            float valor;

            //noinspection IfCanBeSwitch
            if (operador.equals("+"))
                valor = new BigDecimal(valorPrimario).add(new BigDecimal(valorSecundario)).floatValue();
            else if (operador.equals("-"))
                valor = new BigDecimal(valorPrimario).subtract(new BigDecimal(valorSecundario)).floatValue();
            else if (operador.equals("*"))
                valor = new BigDecimal(valorPrimario).multiply(new BigDecimal(valorSecundario)).floatValue();
            else if (operador.equals("/"))
                valor = new BigDecimal(valorPrimario).divide(new BigDecimal(valorSecundario), 2, RoundingMode.UP).floatValue();
            else
                throw new IllegalAccessException("formula invalida: " + entrada);// faz pular para o catch


            if (ehUmOperador(ultimoChar))
                atualizarEdittext(String.format("%s %s", valor, operador));
            else atualizarEdittext(String.format("%s", valor));

            return true;
            //-------------  -----------------  ---------------------  ----------------------  -----------------
        } catch (Exception e) {
            UIUtils.erroToasty(Debt.binder.get().getString(R.string.Formulainvalida));
            e.printStackTrace();
            return false;
        }
    }

    private boolean ehUmOperador(String ultimochar) {
        return ultimochar.equals("+") || ultimochar.equals("-") || ultimochar.equals("*") || ultimochar.equals("/");
    }

    private boolean ehSinalDeIgual(String ultimoChar) {
        return "=".equals(ultimoChar);
    }

    /**
     * Verifica o ultimo digito, removendo-o da view caso seja invalido
     */
    private boolean ehUmCaractereValido(String ultimochar) {
        final String caracteresAceitos = "0123456789.+-*/=";
        return caracteresAceitos.contains(ultimochar);
    }

    /**
     * NAO SETAR TEXTO DIRETAMENTE NO EDITTEXT POIS ISSO PODE CHAMAR O METODO DE CALCULAR DESNECESSARIAMENTE
     */
    private void atualizarEdittext(String novoTexto) {
        Log.d(Tag.AppTag, "CalculadorMonetario.atualizarEdittext: " + novoTexto);
        ignorarProximaEntrada = true;
        if (novoTexto.endsWith(".00")) novoTexto = novoTexto.replace(".00", "");
        if (novoTexto.endsWith(".0")) novoTexto = novoTexto.replace(".0", "");
        editText.setText(novoTexto);
        editText.setSelection(editText.getText().length());
    }

    /**
     * QUANDO A VIEW TEM FOCO ESSA CLASSE OUVE AS ALTERAÇOES DE TEXTO DELA, QDO NAO TEM A CLASSE PARA DE OUVIR E CHAMA O CALLBACK
     * PRA INFORMAR SOBRE AS ALTERAÇOES
     */
    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        viewTemFoco = hasFocus;
        if (!viewTemFoco) {
            if (!calcular(editText.getText().toString())) {
                Log.d(Tag.AppTag, "CalculadorMonetario.onFocusChange: eero no calculo final");
                atualizarEdittext(valorExistenteQAdoViewGanhouFoco);
            }
            chamarCallback();
        } else {

            Currency moeda = Currency.getInstance(Locale.getDefault());
            String entrada = editText.getText().toString();
            valorExistenteQAdoViewGanhouFoco = entrada;
            if (entrada.contains(moeda.getSymbol())) {
                BigDecimal decimal = FormatUtils.emDecimal(entrada);
                atualizarEdittext(decimal.toString());
            }
        }
        if (listenerDeFocoExterno != null) listenerDeFocoExterno.onFocusChange(v, hasFocus);
    }

    private void chamarCallback() {
        String resultadoFinal = editText.getText().toString();

        //removo tudo que nao sao numeros e '.'  por garantia
        resultadoFinal = resultadoFinal.replaceAll("[^\\d.]", "");
        resultadoFinal = removerPontosMultiplos(resultadoFinal);
        if (resultadoFinal.length() > 0) {
            float rFinalFloat = Float.parseFloat(resultadoFinal); 
            callback.valorDefinido(rFinalFloat, FormatUtils.emReal(rFinalFloat), editText);
        } else callback.valorDefinido(0.1f, FormatUtils.emReal(0.1f), editText);
    }

    private String removerPontosMultiplos(String resultadoFinal) {
        while (resultadoFinal.contains("..")) resultadoFinal = resultadoFinal.replace("..", ".");
        return resultadoFinal;
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            editText.clearFocus();
            Log.d(Tag.AppTag, "CalculadorMonetario.addListenersAView: DONE");
        }

        if (onEditorActionListenerExterno != null)
            return onEditorActionListenerExterno.onEditorAction(v, actionId, event);
        else return false;
    }

    @SuppressWarnings("unused")
    public void registrarListenerDeFoco(View.OnFocusChangeListener onFocusChangeListener) {
        /*Caso seja necessario para uma classe saber se a view recebida nessa classe tem fpoco ou nao*/
        this.listenerDeFocoExterno = onFocusChangeListener;
    }

    @SuppressWarnings("unused")
    public void setOnEditorActionListenerExterno(TextView.OnEditorActionListener onEditorActionListenerExterno) {
        this.onEditorActionListenerExterno = onEditorActionListenerExterno;
    }

    public static interface Callback {
        void valorDefinido(float valor, String valorFormatado, EditText editText);
    }

}
