package gmarques.debtv3.gestores;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import org.joda.time.LocalDate;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import gmarques.debtv3.modelos.Despesa;
import gmarques.debtv3.modelos.Mes;
import gmarques.debtv3.outros.MyRealm;
import gmarques.debtv3.outros.Tag;
import gmarques.debtv3.utilitarios.GestorId;
import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;

public class Despesas {

    /**
     * Retorna uma copia exata da despesa recebida
     * ############## NAO ALTERA A ID DO OBJETO ##############
     */
    public static Despesa clonar(Despesa despesa) {
        return new Gson().fromJson(new Gson().toJson(despesa), Despesa.class);
    }

    public static Despesa clonarComAlteraçaoDeId(Despesa despesa) {

        Type mType = new TypeToken<Map<String, Object>>() {
        }.getType();

        Map<String, Object> despMap = new Gson().fromJson(new Gson().toJson(despesa), mType);
        despMap.put("id", GestorId.getId());
        return new Gson().fromJson(new Gson().toJson(despMap), Despesa.class);

    }

    public static void addCopiaRecorrente(Despesa despesa) {
        if (despesa.getParcelas() != null)
            throw new RuntimeException("uma despesa parcelada deve ser salva com id = Despesa.PARCELADA, nao id = Despesa.RECORRENTE. Detalhes da despesa: ->" + new GsonBuilder().setPrettyPrinting().create().toJson(despesa) + "<-");
        Despesa copiaRecorrente = clonarComAlteraçaoDeId(despesa);
        copiaRecorrente.setPaga(false);
        copiaRecorrente.setMesId(Despesa.RECORRENTE);
        MyRealm.insert(copiaRecorrente);
    }

    public static long addCopiaParcelada(Despesa despesa) {
        Despesa copia = clonarComAlteraçaoDeId(despesa);
        copia.setPaga(false);
        copia.setMesId(Despesa.PARCELADA);
        MyRealm.insert(copia);
        return copia.getId();
    }

    public static void atualizarRecorrenteOuParcelada(Despesa despesa, Despesa despesaCopia) {
        Realm realm = Realm.getDefaultInstance();

        Despesa alvo = realm.where(Despesa.class)
                .equalTo("nome", despesa.getNome())
                .and()
                .equalTo("removido", false)
                .and()
                .equalTo("mesId", Despesa.RECORRENTE)
                .findFirst();

        if (alvo == null) alvo = realm.where(Despesa.class)
                .equalTo("nome", despesa.getNome())
                .and()
                .equalTo("removido", false)
                .and()
                .equalTo("mesId", Despesa.PARCELADA)
                .findFirst();

        if (alvo != null) {
            Log.d(Tag.AppTag, "atualizarRecorrenteOuParcelada: " + alvo.getNome() + " encontrada como " + (alvo.getMesId() == Despesa.PARCELADA ? "parcelada" : "recorrente"));
            alvo = realm.copyFromRealm(alvo);
            Map<String, Object> alteracoes = getAlteracoes(despesa, despesaCopia);
            alvo = aplicarAlteracoes(alteracoes, alvo);
            MyRealm.insertOrUpdate(alvo);
        }


        realm.close();

    }

    public static Despesa aplicarAlteracoes(Map<String, Object> alteracoes, Despesa alvo) {

        Type mType = new TypeToken<Map<String, Object>>() {
        }.getType();

        Map<String, Object> obj = new Gson().fromJson(new Gson().toJson(alvo), mType);

        for (String key : alteracoes.keySet()) obj.put(key, alteracoes.get(key));

        return new Gson().fromJson(new Gson().toJson(obj), Despesa.class);
    }

    public static Map<String, Object> getAlteracoes(Despesa despAtualizada, Despesa despDesatualizada) {

        Map<String, Object> alteracoes = new HashMap<>();

        Type mType = new TypeToken<Map<String, Object>>() {
        }.getType();

        Map<String, Object> upObj = new Gson().fromJson(new Gson().toJson(despAtualizada), mType);
        Map<String, Object> outObj = new Gson().fromJson(new Gson().toJson(despDesatualizada), mType);

        for (String key : upObj.keySet()) {
            Object valueUp = upObj.get(key);
            Object valueOut = outObj.get(key);
            if (!Objects.equals(valueUp, valueOut)) alteracoes.put(key, valueUp);
        }

        Log.d(Tag.AppTag, "getalteracoes: alteracoes " + hashMapToString(alteracoes));

        return alteracoes;
    }

    private static String hashMapToString(Map<String, Object> upObj) {

        String map = "";
        for (String key : upObj.keySet()) {
            map = map.concat(key + ": " + upObj.get(key) + " ");
        }
        return map;
    }

    public static Despesa mudarDataDeAcordoComMes(int mes, int ano, Despesa novaDespesa) {

        LocalDate dataAtual = new LocalDate(novaDespesa.getDataDePagamento());
        LocalDate novaData = new LocalDate(ano, mes, 1).plusMonths(1).minusDays(1);

        int diaDoPagamento = dataAtual.getDayOfMonth();
        int ultimoDiaDoMes = novaData.getDayOfMonth();

        if (diaDoPagamento < ultimoDiaDoMes) novaData = novaData.withDayOfMonth(diaDoPagamento);

        novaDespesa.setDataDePagamento(novaData);

        return novaDespesa;
    }

    public static boolean possoImportarNesseMes(Despesa despParcelada, Mes mes) {

        LocalDate mesData = new LocalDate(mes.getAno(), mes.getMes(), 1);
        LocalDate ultimaParcela = new LocalDate(despParcelada.getUltimaParcela()).withDayOfMonth(1);
        Log.d(Tag.AppTag, "possoImportarNesseMes: despesa: " + ultimaParcela + " mes: " + mesData);
        return mesData.toDate().getTime() <= ultimaParcela.toDate().getTime();

    }

    public static ArrayList<Despesa> getDespesas(String nome) {
        ArrayList<Despesa> resultado = new ArrayList<>();
        Realm realm = Realm.getDefaultInstance();

        RealmResults<Despesa> rDespesas = realm.where(Despesa.class)
                .equalTo("removido", false)
                .and()
                .equalTo("nome", nome)
                .sort("dataDePagamento", Sort.ASCENDING)
                .findAll();

        if (rDespesas.size() > 0) resultado = (ArrayList<Despesa>) realm.copyFromRealm(rDespesas);
        realm.close();
        return resultado;

    }

    /**
     * Remove as despesas diretamente do realm sem modificar objetos 'mes' e consequentemente nao
     * atualizando a ui. Para remover uma despesa do mesAtual, atualizando seu array interno e permitindo
     * atualizaçao da UI, chame o metodo no proprio objeto mes
     */
    public static void removerCopias(Despesa despesa) {
        MyRealm.remover(despesa);
    }

    /**
     * retorna um array com todas as copias do objeto recebido do mes recebido (exclusivo) em diante
     * incluindo copia do objeto recorrente/parcelado caso haja
     */
    public static List<Despesa> getTodasAsCopiasDaDespesaIncluindoAsRecorrentesEParceladasDestaDataEmDiante(Despesa despesa) {
        Realm realm = Realm.getDefaultInstance();
        RealmResults<Despesa> rCopias = realm.where(Despesa.class)
                .equalTo("removido", false)
                .and()
                .equalTo("nome", despesa.getNome())
                .and()
                .greaterThan("dataDePagamento", despesa.getDataDePagamento())
                .findAll();

        /*Objetos recorrentes e parcelados tem a data de pagamento do mes em que foram adicionados e podem nao aparecer na pesquisa a cima*/
        Despesa recorrente = realm.where(Despesa.class)
                .equalTo("removido", false)
                .and()
                .equalTo("nome", despesa.getNome())
                .and()
                .equalTo("mesId", Despesa.RECORRENTE)
                .findFirst();

        Despesa parcelada = realm.where(Despesa.class)
                .equalTo("removido", false)
                .and()
                .equalTo("nome", despesa.getNome())
                .and()
                .equalTo("mesId", Despesa.PARCELADA)
                .findFirst();


        List<Despesa> resultado = realm.copyFromRealm(rCopias);
        /*nem todas os objetos recorrentes ou parcelados ainda tem copias recorrente ou parcelada deles mesmos entao preciso verificar a nullabilidade.*/
        if (recorrente != null) resultado.add(realm.copyFromRealm(recorrente));
        else if (parcelada != null) resultado.add(realm.copyFromRealm(parcelada));

        realm.close();


        return resultado;
    }
}
