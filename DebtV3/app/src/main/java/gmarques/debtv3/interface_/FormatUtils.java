package gmarques.debtv3.interface_;

import org.joda.time.LocalDate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.Format;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Currency;
import java.util.Date;
import java.util.Locale;

/**
 * Criado por Gilian Marques
 * Sábado, 20 de Julho de 2019  as 17:18:34.
 */
public class FormatUtils {


    public static String emReal(float valor) {
        return emReal(new BigDecimal(valor));
    }

    public static String emReal(BigDecimal valor) {
        if (valor == null) return "";
        if (valor.doubleValue() > 999999.99) valor = new BigDecimal("999999.99");
        try {
            String returnValue;
            Currency usd = Currency.getInstance(Locale.getDefault());
            NumberFormat format = NumberFormat.getCurrencyInstance(Locale.getDefault());
            format.setCurrency(usd);
            returnValue = format.format(valor);
            return returnValue;
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    /**
     * Nao deve receber valores decimais
     *
     * @param amount o valor a ser formatado
     * @return o valor formatado ou zero em caso de exceçao
     */
    public static BigDecimal emDecimal(String amount) {
        if (amount == null || amount.isEmpty()) return new BigDecimal("0");
        try {
            amount = amount.replaceAll("[^\\d.,]", "");
            if (amount.equals("")) return new BigDecimal("0");
            final NumberFormat format = NumberFormat.getNumberInstance(Locale.getDefault());
            if (format instanceof DecimalFormat) ((DecimalFormat) format).setParseBigDecimal(true);
            return new BigDecimal(format.parse(amount).toString());
        } catch (ParseException e) {
            e.printStackTrace();
            return new BigDecimal("0");
        }
    }

    /**
     * converte a data simples em uma data completa com nomes e etc...
     * é necessario remover 1 do mes ja que o indice dos meses começa com 0
     * jan=0, fev=1, etc...
     *
     * @param data um objeto do tipo {@link Date} como meses de 0-11
     * @return uma String com a data formatada equivalente a data recebida
     */
    public static String formatarData(LocalDate data, boolean full) {
        if (data == null) return null;
        return formatarData(new Date(data.toDate().getTime()), full);
    }

    public static String formatarData(long data, boolean full) {
        return formatarData(new Date(data), full);
    }

    /**
     * converte a data simples em uma data completa com nomes e etc...
     * é necessario remover 1 do mes ja que o indice dos meses começa com 0
     * jan=0, fev=1, etc...
     *
     * @param date um objeto do tipo {@link LocalDate} como meses de 1-12
     * @return uma String com a data formatada equivalente a data recebida
     */
    private static String formatarData(Date date, boolean full) {
        if (date == null) return null;
        try {
            String result;
            if (full) result = DateFormat.getDateInstance(DateFormat.FULL).format(date);
            else result = DateFormat.getDateInstance(DateFormat.MEDIUM).format(date);
            String c = result.charAt(0) + "";
            c = c.toUpperCase();
            result = result.substring(1, result.length());
            result = c.concat(result);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR";
        }
    }

    public static String formatarDataMesEAno(long longData) {
        /*se usar a data em longData sem criar um LocalDate, ao selecionar no dataPicker 31 de dezembro de 2020
         * o formatador retornou DEZEMBRO de 2021. resolvi criando o lDate abaixo*/
        LocalDate lDate = new LocalDate(longData).withDayOfMonth(1);
        Format formatter = new SimpleDateFormat("MMMM-YYYY", Locale.getDefault());

        /*Aplico a gambiarra só se for no Brasil*/
        if (Locale.getDefault().getCountry().equalsIgnoreCase("BR")) {
            String sData = formatter.format(lDate.toDate());
            String mNome = sData.split("-")[0];
            return (mNome.substring(0, 1).toUpperCase() + mNome.substring(1)) + " de " + sData.split("-")[1];
        } else return formatter.format(lDate.toDate());
    }


    public static String formatarDataEmPeriodo(LocalDate comecoPeriodo, LocalDate fimPeriodo) {

        String data1 = formatarDataMesEAno(comecoPeriodo.toDate().getTime());
        String data2 = formatarDataMesEAno(fimPeriodo.toDate().getTime());
        return data1 + " - " + data2;
    }


    /**
     * Calcula  quantos % valA é de valB
     *
     * @param valA d
     * @param valB r
     * @return r
     */
    public static BigDecimal getPorcentagem(String valA, String valB) {
        if (valB.equals("0")) return new BigDecimal("100");// R$35 é 100% de 0
        BigDecimal a;
        BigDecimal b;
        try {
            a = new BigDecimal(valA).multiply(new BigDecimal("100"));
            b = a.divide(new BigDecimal(valB), 1, RoundingMode.HALF_UP);
            return b;
        } catch (Exception e) {
            return new BigDecimal("0");
        }
    }

    public static String formatarDataComHora(Date date, boolean seconds) {
        String fDate = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.DEFAULT, Locale.getDefault()).format(date);
        if (!seconds) fDate = fDate.substring(0, fDate.length() - 3);
        return fDate;
    }

    public static String formatarPorcentagem(BigDecimal percent) {

        String strPercent = percent.toString();

        if (strPercent.endsWith(".0") || strPercent.endsWith(".00"))
            strPercent = strPercent.split("\\.")[0];
        return strPercent + "%";


    }

    public static String formatarDataCurta(LocalDate date) {

        String monthName = date.monthOfYear().getAsText(Locale.getDefault()).substring(0, 3).toUpperCase();
        int day = date.getDayOfMonth();
        return (day < 10 ? "0" + day : +day + "") + " " + monthName;

    }

    public static String comoNoCartaoDeCredito(long receivingDate) {
        LocalDate localDate = new LocalDate(receivingDate);

        String day = String.valueOf(localDate.getDayOfMonth());
        String month = String.valueOf(localDate.getMonthOfYear());

        day = day.length() == 1 ? "0" + day : day;
        month = month.length() == 1 ? "0" + month : month;

        return day + "/" + month;
    }

    public static String formatarParcelas(int[] parcelas) {
        /*Quando recebidos do vetor a 1° parcela = 0 e a última é = (seu valor-1) */
        int atual = parcelas[0] + 1;
        int total = parcelas[1] + 1;

        return (atual < 10 ? "0" + atual : atual) + "/" + (total < 10 ? "0" + total : total);

    }

    public static String formatarDataBasica(long dataDeRecebimento) {

        LocalDate localDate = new LocalDate(dataDeRecebimento);

        return localDate.getDayOfMonth() + "/" + localDate.getMonthOfYear() + "/" + localDate.getYear();
    }
}
