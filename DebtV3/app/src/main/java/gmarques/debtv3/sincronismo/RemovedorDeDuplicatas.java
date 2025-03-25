package gmarques.debtv3.sincronismo;

import android.util.Log;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

import gmarques.debtv3.nuvem.FirebaseImpl;
import gmarques.debtv3.outros.MyRealm;
import gmarques.debtv3.modelos.Categoria;
import gmarques.debtv3.modelos.Despesa;
import gmarques.debtv3.modelos.Nota;
import gmarques.debtv3.modelos.Receita;
import gmarques.debtv3.outros.Tag;
import io.realm.Realm;
import io.realm.RealmResults;

public class RemovedorDeDuplicatas {

    private final FirebaseImpl firebase;

    public RemovedorDeDuplicatas() {
        firebase = new FirebaseImpl();
    }

    public void executar() {
        verificarReceitas();
        verificarDespesas();
        verificarCategorias();
        verificarNotas();
    }

    private void verificarDespesas() {
        List<Despesa> despesas;

        Realm realm = Realm.getDefaultInstance();
        RealmResults<Despesa> rd = realm.where(Despesa.class).equalTo("removido", false).findAll();
        despesas = new ArrayList<>(realm.copyFromRealm(rd));
        realm.close();


        for (Despesa despesa : despesas) {
            Despesa objetoDuplicado = getDuplicataDespesa(despesa, despesas);
            if (objetoDuplicado != null) {
                Log.d(Tag.AppTag, "RemovedorDeDuplicatas.verificarDespesas:  removendo duplicata de" + despesa.getNome() + "\n" + new Gson().toJson(despesa) + "\n" + new Gson().toJson(objetoDuplicado));
                MyRealm.removerPermanentemente(objetoDuplicado);
                firebase.removerObjetoPermanentemente(objetoDuplicado, new FirebaseImpl.CallbackDeStatus() {
                    @Override
                    public void feito(boolean sucesso, String msg) {

                    }
                });
            }
        }

    }

    private Despesa getDuplicataDespesa(Despesa original, List<Despesa> despesas) {

        for (Despesa duplicata : despesas)
            if (duplicata.getNome().equals(original.getNome())
                    && duplicata.getMesId() == original.getMesId()
                    && duplicata.getUltimaAtt() > original.getUltimaAtt())
                return duplicata;/*retorno a despesa duplicada mais recente para ser removida*/

        return null;
    }

    private void verificarReceitas() {
        List<Receita> receitas;

        Realm realm = Realm.getDefaultInstance();
        RealmResults<Receita> rd = realm.where(Receita.class).equalTo("removido", false).findAll();
        receitas = new ArrayList<>(realm.copyFromRealm(rd));
        realm.close();


        for (Receita receita : receitas) {
            Receita objetoDuplicado = getDuplicataReceita(receita, receitas);
            if (objetoDuplicado != null) {
                Log.d(Tag.AppTag, "RemovedorDeDuplicatas.verificarReceitas:  removendo duplicata de" + receita.getNome() + "\n" + new Gson().toJson(receita) + "\n" + new Gson().toJson(objetoDuplicado));
                MyRealm.removerPermanentemente(objetoDuplicado);
                firebase.removerObjetoPermanentemente(objetoDuplicado, new FirebaseImpl.CallbackDeStatus() {
                    @Override
                    public void feito(boolean sucesso, String msg) {

                    }
                });
            }
        }

    }

    private Receita getDuplicataReceita(Receita original, List<Receita> receitas) {

        for (Receita duplicata : receitas)
            if (duplicata.getNome().equals(original.getNome())
                    && duplicata.getMesId() == original.getMesId()
                    && duplicata.getUltimaAtt() > original.getUltimaAtt())
                return duplicata;/*retorno a receita duplicada mais recente para ser removida*/

        return null;
    }

    private void verificarNotas() {
        List<Nota> notas;

        Realm realm = Realm.getDefaultInstance();
        RealmResults<Nota> rd = realm.where(Nota.class).equalTo("removido", false).findAll();
        notas = new ArrayList<>(realm.copyFromRealm(rd));
        realm.close();


        for (Nota nota : notas) {
            Nota objetoDuplicado = getDuplicataNota(nota, notas);
            if (objetoDuplicado != null) {
                Log.d(Tag.AppTag, "RemovedorDeDuplicatas.verificarNotas:  removendo duplicata de" + nota.getNome() + "\n" + new Gson().toJson(nota) + "\n" + new Gson().toJson(objetoDuplicado));
                MyRealm.removerPermanentemente(objetoDuplicado);
                firebase.removerObjetoPermanentemente(objetoDuplicado, new FirebaseImpl.CallbackDeStatus() {
                    @Override
                    public void feito(boolean sucesso, String msg) {

                    }
                });
            }
        }

    }

    private Nota getDuplicataNota(Nota original, List<Nota> notas) {

        for (Nota duplicata : notas)
            if (duplicata.getNome().equals(original.getNome())
                    && duplicata.getMesId() == original.getMesId()
                    && duplicata.getUltimaAtt() > original.getUltimaAtt())
                return duplicata;/*retorno a nota duplicada mais recente para ser removida*/

        return null;
    }

    private void verificarCategorias() {
        List<Categoria> categorias;

        Realm realm = Realm.getDefaultInstance();
        RealmResults<Categoria> rd = realm.where(Categoria.class).equalTo("removido", false).findAll();
        categorias = new ArrayList<>(realm.copyFromRealm(rd));
        realm.close();

        for (Categoria categoria : categorias) {
            final Categoria objetoDuplicado = getDuplicataQueNaoEstejaEmUso(categoria);
            if (objetoDuplicado != null) {

                Log.d(Tag.AppTag, "RemovedorDeDuplicatas.verificarCategorias:  removendo duplicata de: " + categoria.getNome() + "\n original:" + new Gson().toJson(categoria) + "\nduplicata:" + new Gson().toJson(objetoDuplicado));
                MyRealm.removerPermanentemente(objetoDuplicado);
                /*nao tem importancia se esta açao falhar, no prox sincronismo ela tentara novamente*/
                firebase.removerObjetoPermanentemente(objetoDuplicado, new FirebaseImpl.CallbackDeStatus() {
                    @Override
                    public void feito(boolean sucesso, String msg) {
                        Log.d(Tag.AppTag + " RemovedorDeDuplicatas:", objetoDuplicado.getNome() + " id: " + objetoDuplicado.getId() + " removido da nuvem? sucesso = [" + sucesso + "], msg = [" + msg + "]");
                    }
                });
            }
        }

    }


    private Categoria getDuplicataQueNaoEstejaEmUso(Categoria original) {
        Realm realm = Realm.getDefaultInstance();

        Categoria dup = realm.where(Categoria.class)
                .equalTo("removido", false)
                .and()
                .equalTo("nome", original.getNome())
                .and()
                .greaterThan("ultimaAtt", original.getUltimaAtt())
                .findFirst();

        if (dup == null) {
            realm.close();
            return null;
        } else dup = realm.copyFromRealm(dup);

        Despesa uso = realm.where(Despesa.class).equalTo("removido", false)
                .and()
                .equalTo("categoriaId", dup.getId())
                .findFirst();


        realm.close();

        if (uso == null) return dup;
        else return null;

    }


}
