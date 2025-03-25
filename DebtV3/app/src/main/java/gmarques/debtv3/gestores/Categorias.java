package gmarques.debtv3.gestores;

import android.util.Log;

import androidx.core.content.ContextCompat;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import gmarques.debtv3.Debt;
import gmarques.debtv3.outros.MyRealm;
import gmarques.debtv3.R;
import gmarques.debtv3.modelos.Categoria;
import gmarques.debtv3.modelos.Despesa;
import gmarques.debtv3.outros.Tag;
import gmarques.debtv3.utilitarios.GestorId;
import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;

@SuppressWarnings("NonAsciiCharacters")
public class Categorias {
    private static Categoria placeHolder;

    /**
     * Retorna uma copia exata da categoria recebida
     * ############## NAO ALTERA A ID DO OBJETO ##############
     */
    public static Categoria clonar(Categoria categoria) {
        return new Gson().fromJson(new Gson().toJson(categoria), Categoria.class);
    }

    public static Categoria clonarComAlteraçaoDeId(Categoria categoria) {

        Type mType = new TypeToken<Map<String, Object>>() {
        }.getType();

        Map<String, Object> catMap = new Gson().fromJson(new Gson().toJson(categoria), mType);
        catMap.put("id", GestorId.getId());
        return new Gson().fromJson(new Gson().toJson(catMap), Categoria.class);

    }

    public static Categoria aplicarAlteracoes(Map<String, Object> alteracoes, Categoria alvo) {

        Type mType = new TypeToken<Map<String, Object>>() {
        }.getType();

        Map<String, Object> obj = new Gson().fromJson(new Gson().toJson(alvo), mType);

        for (String key : alteracoes.keySet()) obj.put(key, alteracoes.get(key));

        return new Gson().fromJson(new Gson().toJson(obj), Categoria.class);
    }

    public static Map<String, Object> getAlteracoes(Categoria catAtualizada, Categoria catDesatualizada) {

        Map<String, Object> alteracoes = new HashMap<>();

        Type mType = new TypeToken<Map<String, Object>>() {
        }.getType();

        Map<String, Object> upObj = new Gson().fromJson(new Gson().toJson(catAtualizada), mType);
        Map<String, Object> outObj = new Gson().fromJson(new Gson().toJson(catDesatualizada), mType);

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

    public static Categoria getCategoria(long id) {
        Realm realm = Realm.getDefaultInstance();
        RealmResults<Categoria> rCategorias = realm.where(Categoria.class).equalTo("removido", false).findAll();

        for (Categoria categoria : rCategorias) {
            if (categoria.getId() == id) {
                Categoria categoria2 = realm.copyFromRealm(categoria);
                realm.close();
                return categoria2;
            }
        }
        realm.close();
        /* O LiveSinc causa inconsistencias temporarias que podem gerarm erros e parar o app, como por exemplo, sincronizar uma despesa
         * sem que sua categorria esteja sincronizada. dependendo do caso esse erro pode ser corrigido apenas qdo um sincronismo
         * completo for executado. Pro app nao parar, eu crio uma categoria paadrao temporaria, caso a original nao seja encontrada no momento.
         *
         * Claro que esse comportamento só é aceitavel em uma versao do app com sincronismo habilitado em outros casos pode significar que existe um bug no codigo */
        if (!Debt.ADMINISTRADOR) {
            if (placeHolder == null) {
                placeHolder = new Categoria();
                placeHolder.setCor(R.color.colorAccent);
                placeHolder.setIcone(getStringIcone(R.drawable.vec_categoria));
                placeHolder.setNome("Não encontrada");

                /*preciso setar uma id padrao pra todas as despesas sem categoria referenciarem a mesma categoria placeholder*/
                Type mType = new TypeToken<Map<String, Object>>() {
                }.getType();
                Map<String, Object> catMap = new Gson().fromJson(new Gson().toJson(placeHolder), mType);
                catMap.put("id", 1111111111);
                placeHolder = new Gson().fromJson(new Gson().toJson(catMap), Categoria.class);
            }
            return placeHolder;
        }


        return null;

    }

    public static String getStringIcone(int icone) {
        String endereço = "ic_cat_61";

        try {
            endereço = Debt.binder.get().getResources().getResourceEntryName(icone);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return endereço;
    }

    public static int getIntIcone(String icone) {
        return Debt.binder.get().getResources().getIdentifier(icone, "drawable", Debt.binder.get().getPackageName());
    }

    public static void attCategoria(Categoria catAtualizada) {
        MyRealm.insertOrUpdate(catAtualizada);
    }

    public static void addCategoria(Categoria categoria) {
        MyRealm.insert(categoria);
    }

    public static boolean categoriaEstaEmUso(Categoria categoria) {
        boolean emUso;
        Realm realm = Realm.getDefaultInstance();
        Despesa uso = realm.where(Despesa.class).equalTo("removido", false).and().equalTo("categoriaId", categoria.getId()).findFirst();
        emUso = uso != null;
        realm.close();
        return emUso;

    }

    public static void remover(Categoria categoria) {
        MyRealm.remover(categoria);
    }

    public static ArrayList<Categoria> getCategorias() {
        Realm realm = Realm.getDefaultInstance();
        RealmResults<Categoria> rCat = realm.where(Categoria.class).equalTo("removido", false).sort("nome", Sort.ASCENDING).findAll();
        List<Categoria> categorias = realm.copyFromRealm(rCat);
        realm.close();
        return (ArrayList<Categoria>) categorias;


    }

    @SuppressWarnings("StatementWithEmptyBody")
    public static void addPrimeirasCategorias() {
        Log.d(Tag.AppTag, "Categorias.addPrimeirasCategorias: " + getCategorias().size());
        if (getCategorias().size() > 0) return;

        final Categoria Compras = new Categoria();
        Compras.setNome(Debt.binder.get().getString(R.string.compras));
        Compras.setIcone(Categorias.getStringIcone(R.drawable.ic_cat_81));
        Compras.setCor(ContextCompat.getColor(Debt.binder.get(), R.color.flat_color_11));

        final Categoria Pagamentos = new Categoria();
        Pagamentos.setNome(Debt.binder.get().getString(R.string.pagamentos));
        Pagamentos.setIcone(Categorias.getStringIcone(R.drawable.ic_cat_82));
        Pagamentos.setCor(ContextCompat.getColor(Debt.binder.get(), R.color.flat_color_12));

        final Categoria educacao = new Categoria();
        educacao.setNome(Debt.binder.get().getString(R.string.educacao));
        educacao.setIcone(Categorias.getStringIcone(R.drawable.ic_cat_76));
        educacao.setCor(ContextCompat.getColor(Debt.binder.get(), R.color.flat_color_13));

        final Categoria alimentacao = new Categoria();
        alimentacao.setNome(Debt.binder.get().getString(R.string.alimentacao));
        alimentacao.setIcone(Categorias.getStringIcone(R.drawable.ic_cat_3));
        alimentacao.setCor(ContextCompat.getColor(Debt.binder.get(), R.color.flat_color_15));

        final Categoria Transporte = new Categoria();
        Transporte.setNome(Debt.binder.get().getString(R.string.transporte));
        Transporte.setIcone(Categorias.getStringIcone(R.drawable.ic_cat_77));
        Transporte.setCor(ContextCompat.getColor(Debt.binder.get(), R.color.flat_color_14));

        final Categoria saude = new Categoria();
        saude.setNome(Debt.binder.get().getString(R.string.saude));
        saude.setIcone(Categorias.getStringIcone(R.drawable.ic_cat_79));
        saude.setCor(ContextCompat.getColor(Debt.binder.get(), R.color.flat_color_16));

        final Categoria Lazer = new Categoria();
        Lazer.setNome(Debt.binder.get().getString(R.string.lazer));
        Lazer.setIcone(Categorias.getStringIcone(R.drawable.ic_cat_75));
        Lazer.setCor(ContextCompat.getColor(Debt.binder.get(), R.color.flat_color_17));

        final Categoria Moradia = new Categoria();
        Moradia.setNome(Debt.binder.get().getString(R.string.moradia));
        Moradia.setIcone(Categorias.getStringIcone(R.drawable.ic_cat_80));
        Moradia.setCor(ContextCompat.getColor(Debt.binder.get(), R.color.flat_color_18));


        MyRealm.insert(Compras);
        MyRealm.insert(Pagamentos);
        MyRealm.insert(educacao);
        MyRealm.insert(alimentacao);
        MyRealm.insert(Transporte);
        MyRealm.insert(saude);
        MyRealm.insert(Lazer);
        MyRealm.insert(Moradia);
    }
}
