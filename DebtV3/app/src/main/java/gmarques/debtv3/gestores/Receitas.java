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

import gmarques.debtv3.outros.MyRealm;
import gmarques.debtv3.modelos.Mes;
import gmarques.debtv3.modelos.Receita;
import gmarques.debtv3.outros.Tag;
import gmarques.debtv3.utilitarios.GestorId;
import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;

public class Receitas {

    /**
     * Retorna uma copia exata da receita recebida
     * ############## NAO ALTERA A ID DO OBJETO ##############
     */
    public static Receita clonar(Receita receita) {
        return new Gson().fromJson(new Gson().toJson(receita), Receita.class);
    }

    public static Receita clonarComAlteraçaoDeId(Receita receita) {

        Type mType = new TypeToken<Map<String, Object>>() {
        }.getType();

        Map<String, Object> recMap = new Gson().fromJson(new Gson().toJson(receita), mType);
        recMap.put("id", GestorId.getId());
        return new Gson().fromJson(new Gson().toJson(recMap), Receita.class);

    }

    public static Receita aplicarAlteracoes(Map<String, Object> alteracoes, Receita alvo) {

        Type mType = new TypeToken<Map<String, Object>>() {
        }.getType();

        Map<String, Object> obj = new Gson().fromJson(new Gson().toJson(alvo), mType);

        for (String key : alteracoes.keySet()) obj.put(key, alteracoes.get(key));

        return new Gson().fromJson(new Gson().toJson(obj), Receita.class);
    }

    public static Map<String, Object> getAlteracoes(Receita recAtualizada, Receita recDesatualizada) {

        Map<String, Object> alteracoes = new HashMap<>();

        Type mType = new TypeToken<Map<String, Object>>() {
        }.getType();

        Map<String, Object> upObj = new Gson().fromJson(new Gson().toJson(recAtualizada), mType);
        Map<String, Object> outObj = new Gson().fromJson(new Gson().toJson(recDesatualizada), mType);

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

    public static void addCopiaRecorrente(Receita receita) {
        if (receita.getAutoImportarPrimeira() > 0)
            throw new RuntimeException("uma receita parcelada deve ser salva com id = Receita.AUTOIMPORTADA, nao id = Receita.RECORRENTE. Detalhes da receita: ->" + new GsonBuilder().setPrettyPrinting().create().toJson(receita) + "<-");
        Receita copiaRecorrente = clonarComAlteraçaoDeId(receita);
        copiaRecorrente.setRecebida(false);
        copiaRecorrente.setMesId(Receita.RECORRENTE);
        MyRealm.insert(copiaRecorrente);
    }

    public static long addCopiaAutoImportada(Receita receita) {
        Receita copia = clonarComAlteraçaoDeId(receita);
        copia.setRecebida(false);
        copia.setMesId(Receita.AUTOIMPORTADA);
        MyRealm.insert(copia);
        return copia.getId();
    }

    public static void atualizarRecorrenteOuParcelada(Receita receita, Receita receitaCopia) {
        Realm realm = Realm.getDefaultInstance();

        Receita alvo = realm.where(Receita.class)
                .equalTo("nome", receita.getNome())
                .and()
                .equalTo("removido", false)
                .and()
                .equalTo("mesId", Receita.RECORRENTE)
                .findFirst();

        if (alvo == null) alvo = realm.where(Receita.class)
                .equalTo("nome", receita.getNome())
                .and()
                .equalTo("removido", false)
                .and()
                .equalTo("mesId", Receita.AUTOIMPORTADA)
                .findFirst();

        if (alvo != null) {
            Log.d(Tag.AppTag, "atualizarRecorrenteOuParcelada: " + alvo.getNome() + " encontrada como " + (alvo.getMesId() == Receita.AUTOIMPORTADA ? "auto-importada" : "recorrente"));
            alvo = realm.copyFromRealm(alvo);
            Map<String, Object> alteracoes = getAlteracoes(receita, receitaCopia);
            alvo = aplicarAlteracoes(alteracoes, alvo);
            MyRealm.insertOrUpdate(alvo);
        }


        realm.close();

    }

    public static Receita mudarDataDeAcordoComMes(int mes, int ano, Receita novaReceita) {

        LocalDate dataAtual = new LocalDate(novaReceita.getDataDeRecebimento());
        LocalDate novaData = new LocalDate(ano, mes, 1).plusMonths(1).minusDays(1);

        int diaDoPagamento = dataAtual.getDayOfMonth();
        int ultimoDiaDoMes = novaData.getDayOfMonth();

        if (diaDoPagamento < ultimoDiaDoMes) novaData = novaData.withDayOfMonth(diaDoPagamento);

        novaReceita.setDataDeRecebimento(novaData);

        return novaReceita;
    }

    public static boolean possoImportarNesseMes(Receita recParcelada, Mes mes) {

        LocalDate mesData = new LocalDate(mes.getAno(), mes.getMes(), 1);
        LocalDate importatAte = new LocalDate(recParcelada.getAutoImportarUltima()).withDayOfMonth(1);
        Log.d(Tag.AppTag, "possoImportarNesseMes: receita: " + importatAte + " mes: " + mesData);
        return mesData.toDate().getTime() <= importatAte.toDate().getTime();

    }

    /**
     * retorna um array com todas as copias do objeto recebido do mes recebido (exclusivo) em diante
     * incluindo copia do objeto recorrente/parcelado caso haja
     */
    public static List<Receita> getTodasAsCopiasDaReceitaIncluindoAsRecorrentesEParceladasDestaDataEmDiante(Receita receita) {
        Realm realm = Realm.getDefaultInstance();
        RealmResults<Receita> rCopias = realm.where(Receita.class)
                .equalTo("removido", false)
                .and()
                .equalTo("nome", receita.getNome())
                .and()
                .greaterThan("dataDeRecebimento", receita.getDataDeRecebimento())
                .findAll();

        /*Objetos recorrentes e parcelados tem a data de pagamento do mes em que foram adicionados e podem nao aparecer na pesquisa a cima*/
        Receita recorrente = realm.where(Receita.class)
                .equalTo("removido", false)
                .and()
                .equalTo("nome", receita.getNome())
                .and()
                .equalTo("mesId", Receita.RECORRENTE)
                .findFirst();

        Receita parcelada = realm.where(Receita.class)
                .equalTo("removido", false)
                .and()
                .equalTo("nome", receita.getNome())
                .and()
                .equalTo("mesId", Receita.AUTOIMPORTADA)
                .findFirst();


        List<Receita> resultado = realm.copyFromRealm(rCopias);
        /*nem todas os objetos recorrentes ou parcelados ainda tem copias recorrente ou parcelada deles mesmos entao preciso verificar a nullabilidade.*/
        if (recorrente != null) resultado.add(realm.copyFromRealm(recorrente));
        else if (parcelada != null) resultado.add(realm.copyFromRealm(parcelada));

        realm.close();


        return resultado;
    }


    /**
     * Remove as receitas diretamente do realm sem modificar objetos 'mes' e consequentemente nao
     * atualizando a ui. Para remover uma receita do mesAtual, atualizando seu array interno e permitindo
     * atualizaçao da UI, chame o metodo no proprio objeto mes
     */ public static void removerCopias(Receita receita) {
        MyRealm.remover(receita);
    }

    public static ArrayList<Receita> getReceitas(String nome) {

        ArrayList<Receita> resultado = new ArrayList<>();
        Realm realm = Realm.getDefaultInstance();

        RealmResults<Receita> rReceitas = realm.where(Receita.class)
                .equalTo("removido", false)
                .and()
                .equalTo("nome", nome)
                .sort("dataDeRecebimento", Sort.ASCENDING)
                .findAll();

        if (rReceitas.size() > 0) resultado = (ArrayList<Receita>) realm.copyFromRealm(rReceitas);
        realm.close();
        return resultado;
    }

    public static boolean temCopiaRecorrenteOuAutoImportada(Receita receita) {


        Realm realm = Realm.getDefaultInstance();
        Receita rec = realm.where(Receita.class)
                .equalTo("removido", false)
                .and()
                .equalTo("nome", receita.getNome())
                .and()
                .equalTo("mesId", Receita.RECORRENTE)
                .findFirst();

        if (rec != null) {
            realm.close();
            return true;
        }

        rec = realm.where(Receita.class)
                .equalTo("removido", false)
                .and()
                .equalTo("nome", receita.getNome())
                .and()
                .equalTo("mesId", Receita.AUTOIMPORTADA)
                .findFirst();


        realm.close();

        return rec != null;
    }
}
