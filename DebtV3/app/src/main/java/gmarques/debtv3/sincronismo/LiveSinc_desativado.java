package gmarques.debtv3.sincronismo;

import android.util.Log;

import androidx.annotation.Nullable;

import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.gson.Gson;
import com.pixplicity.easyprefs.library.Prefs;

import org.jetbrains.annotations.NotNull;

import gmarques.debtv3.Debt;
import gmarques.debtv3.R;
import gmarques.debtv3.especificos.RealmBackup;
import gmarques.debtv3.interface_.UIUtils;
import gmarques.debtv3.modelos.Categoria;
import gmarques.debtv3.modelos.Despesa;
import gmarques.debtv3.modelos.Nota;
import gmarques.debtv3.modelos.Receita;
import gmarques.debtv3.nuvem.FirebaseImpl;
import gmarques.debtv3.outros.Broadcaster;
import gmarques.debtv3.outros.MyRealm;
import gmarques.debtv3.outros.Tag;
import gmarques.debtv3.sincronismo.api.Sincronizavel;
import gmarques.debtv3.utilitarios.C;
import gmarques.debtv3.utilitarios.GestorId;
import io.realm.Realm;
import io.realm.RealmObject;

import static gmarques.debtv3.gestores.Meses.mesAtual;

public class LiveSinc_desativado {
    private FirebaseImpl firebase;

    private Gson gson;

    private boolean jasFezAPrimeiraChamadaReceita;
    private boolean jasFezAPrimeiraChamadaDespesa;
    private boolean jasFezAPrimeiraChamadaCategoria;
    private boolean jasFezAPrimeiraChamadaNota;

    private ListenerRegistration receitasLc;
    private ListenerRegistration despesasasLc;
    private ListenerRegistration categoriasLc;
    private ListenerRegistration notasLc;

    public LiveSinc_desativado() {

        try {
            new RealmBackup().fazerBackup(null);
        } catch (Exception ignored) {
        }
    }

    public void ouvirAtualizaçoesDaNuvem() {

        if (!Debt.ADMINISTRADOR || !Debt.MODO_DE_TESTES) return;
        if (!Prefs.getBoolean(C.executarLiveSinc, true)) return;

        if (firebase != null) return;
        Runnable ligarLiveSinc = () -> {

            Log.d(Tag.AppTag, "LiveSinc.inicializar: ligando liveSinc");

            gson = new Gson();
            firebase = new FirebaseImpl();

            receitasLc = firebase.sincronismo.collection(Receita.class.getSimpleName()).addSnapshotListener(getListenerReceitas());
            despesasasLc = firebase.sincronismo.collection(Despesa.class.getSimpleName()).addSnapshotListener(getListenerDespesas());
            categoriasLc = firebase.sincronismo.collection(Categoria.class.getSimpleName()).addSnapshotListener(getListenerCategorias());
            notasLc = firebase.sincronismo.collection(Nota.class.getSimpleName()).addSnapshotListener(getListenerNotas());

        };

        /*antes de fazer o ChecagemPreSincronismo eu verificava com a classe Usuario se estava sincronizando os dados com outra conta
         porém parei de fazer essa verificação porque nem sempre essa informação está disponível nas preferências do aplicativo.
         estva usando aplicativo no celular então fiz logoff e então loguei novamente e quando fez o sincronismo para restaurar os dados
          mesmo estamos sincronizando com uma outra conta, o aplicativo sincronizou com a minha conta porque eu ainda não tinha baixado
          o e-mail do usuario com quem eu sincronizava para as preferências do app, então a classe Usuario sempre retornavam que estava sincronizando com a minha
           própria conta. Moral da história: antes de sincronizar é necessário sempre checar na nuven se o usuário sincroniza ou não com outro usuário.
            A classe ChecagemPreSincronismo faz essa verificação e atualiza as preferências por que o e-mail do usuário anfitrião pode não estar sempre disponível lá*/
        /*Usuario.estaSincronizandoComOutraConta()*/
        new ChecagemPreSincronismo(new ChecagemPreSincronismo.Callback() {
            @Override
            public void feito() {
                ligarLiveSinc.run();
            }

            @Override
            public void falha(String erro) {
                UIUtils.erroToasty(Debt.binder.get().getString(R.string.Naofoipossivelverificarcontadesincronismo) + ": " + erro);
            }
        });


    }

    public void addObjeto(final RealmObject rObj) {

        if (firebase != null)
            firebase.addObjeto((Sincronizavel) rObj, (sucesso, msg) -> Log.d(Tag.AppTag + " LiveSinc: ", "addObjeto() uplaod do objeto " + ((Sincronizavel) rObj).getNome() + ":  sucesso = [" + sucesso + "], msg = [" + msg + "]"));
    }

    public void attObjeto(final RealmObject rObj) {

        if (firebase != null)
            firebase.attObjeto((Sincronizavel) rObj, (sucesso, msg) -> Log.d(Tag.AppTag + " LiveSinc: ", "attObjeto() uplaod do objeto " + ((Sincronizavel) rObj).getNome() + ":  sucesso = [" + sucesso + "], msg = [" + msg + "]"));
    }

    private EventListener<QuerySnapshot> getListenerReceitas() {
        return (queryDocumentSnapshots, e) -> {

            if (!jasFezAPrimeiraChamadaReceita) {
                jasFezAPrimeiraChamadaReceita = true;
                Log.d(Tag.AppTag, "LiveSinc.onEvent: Prim chamada Receitas");
                return;
            } else if (e != null) {
                UIUtils.erroToasty(Debt.binder.get().getString(R.string.Livesincfalhou));
                e.printStackTrace();
                return;

            }

            if (queryDocumentSnapshots != null)
                for (DocumentChange snap : queryDocumentSnapshots.getDocumentChanges()) {

                    @SuppressWarnings("ConstantConditions")
                    boolean local = GestorId.getIdDoDispositivo() == snap.getDocument().getLong("origem");

                    if (local) continue;

                    Receita receita = gson.fromJson(gson.toJson(snap.getDocument().getData()), Receita.class);

                    DocumentChange.Type tipoOp = snap.getType();
                    if (tipoOp != DocumentChange.Type.REMOVED) {
                        if (verificarReceita(receita, tipoOp))
                            atualizarReceitas(receita, tipoOp);
                    } else {
                        removerDuplicataPermanentemente(receita.getNome(), receita.getId(), Receita.class);
                    }

                }
        };
    }

    private EventListener<QuerySnapshot> getListenerDespesas() {
        return (queryDocumentSnapshots, e) -> {

            if (!jasFezAPrimeiraChamadaDespesa) {
                jasFezAPrimeiraChamadaDespesa = true;
                Log.d(Tag.AppTag, "LiveSinc.onEvent: Prim chamada Despesas");
                return;
            } else if (e != null) {
                UIUtils.erroToasty(Debt.binder.get().getString(R.string.Livesincfalhou));
                e.printStackTrace();
                return;

            }

            if (queryDocumentSnapshots != null)
                for (DocumentChange snap : queryDocumentSnapshots.getDocumentChanges()) {

                    @SuppressWarnings("ConstantConditions")
                    boolean local = GestorId.getIdDoDispositivo() == snap.getDocument().getLong("origem");

                    if (local) continue;

                    Despesa despesa = gson.fromJson(gson.toJson(snap.getDocument().getData()), Despesa.class);


                    DocumentChange.Type tipoOp = snap.getType();
                    if (tipoOp != DocumentChange.Type.REMOVED) {
                        if (verificarDespesa(despesa, tipoOp))
                            atualizarDespesas(despesa, tipoOp);
                    } else {
                        removerDuplicataPermanentemente(despesa.getNome(), despesa.getId(), Despesa.class);
                    }
                }
        };
    }

    private EventListener<QuerySnapshot> getListenerCategorias() {
        return new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {

                if (!jasFezAPrimeiraChamadaCategoria) {
                    jasFezAPrimeiraChamadaCategoria = true;
                    Log.d(Tag.AppTag, "LiveSinc.onEvent: Prim chamada Categorias");
                    return;
                } else if (e != null) {
                    UIUtils.erroToasty(Debt.binder.get().getString(R.string.Livesincfalhou));
                    e.printStackTrace();
                    return;

                }

                if (queryDocumentSnapshots != null)
                    for (DocumentChange snap : queryDocumentSnapshots.getDocumentChanges()) {

                        @SuppressWarnings("ConstantConditions")
                        boolean local = GestorId.getIdDoDispositivo() == snap.getDocument().getLong("origem");

                        if (local) continue;

                        Categoria categoria = gson.fromJson(gson.toJson(snap.getDocument().getData()), Categoria.class);

                        DocumentChange.Type tipoOp = snap.getType();
                        if (tipoOp != DocumentChange.Type.REMOVED) {
                            if (verificarCategoria(categoria, tipoOp))
                                atualizarCategorias(categoria, tipoOp);
                        } else {
                            removerDuplicataPermanentemente(categoria.getNome(), categoria.getId(), Categoria.class);
                        }

                    }
            }


        };
    }

    private EventListener<QuerySnapshot> getListenerNotas() {
        return new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {

                if (!jasFezAPrimeiraChamadaNota) {
                    jasFezAPrimeiraChamadaNota = true;
                    Log.d(Tag.AppTag, "LiveSinc.onEvent: Prim chamada Notas");
                    return;
                } else if (e != null) {
                    UIUtils.erroToasty(Debt.binder.get().getString(R.string.Livesincfalhou));
                    e.printStackTrace();
                    return;

                }

                if (queryDocumentSnapshots != null)
                    for (DocumentChange snap : queryDocumentSnapshots.getDocumentChanges()) {

                        @SuppressWarnings("ConstantConditions")
                        boolean local = GestorId.getIdDoDispositivo() == snap.getDocument().getLong("origem");

                        if (local) continue;

                        Nota nota = gson.fromJson(gson.toJson(snap.getDocument().getData()), Nota.class);

                        DocumentChange.Type tipoOp = snap.getType();
                        if (tipoOp != DocumentChange.Type.REMOVED) {
                            if (verificarNota(nota, tipoOp))
                                atualizarNotas(nota, tipoOp);
                        } else {
                            removerDuplicataPermanentemente(nota.getNome(), nota.getId(), Nota.class);
                        }

                    }
            }


        };
    }

    private void atualizarReceitas(@NotNull Receita receita, DocumentChange.Type tipoDeOp) {
        /*Se o objeto for do mes atual, atualizo o objeto mes diretamente e a UI tbm */
        if (receita.getMesId() == mesAtual.getId()) {
            if (tipoDeOp == DocumentChange.Type.ADDED) mesAtual.addReceita(receita);
            else if (tipoDeOp == DocumentChange.Type.MODIFIED) mesAtual.attReceita(receita);
            Broadcaster.enviar(Broadcaster.atualizarCardsDeDados, Broadcaster.atualizarGraficoDeBarras);
        } else {
            /*Se nao apenas modifico o DB*/
            if (tipoDeOp == DocumentChange.Type.ADDED) MyRealm.insert(receita);
            else if (tipoDeOp == DocumentChange.Type.MODIFIED) MyRealm.insertOrUpdate(receita);
        }
    }

    private void atualizarDespesas(@NotNull Despesa despesa, DocumentChange.Type tipoDeOp) {

        /*Se o objeto for do mes atual, atualizo o objeto mes diretamente e a UI tbm */
        if (despesa.getMesId() == mesAtual.getId()) {
            if (tipoDeOp == DocumentChange.Type.ADDED) mesAtual.addDespesa(despesa);
            else if (tipoDeOp == DocumentChange.Type.MODIFIED) mesAtual.attDespesa(despesa);
            Broadcaster.enviar(Broadcaster.atualizarGraficoDeBarras, Broadcaster.atualizarCardsDeDados, Broadcaster.atualizarFragDespesas);
        } else {
            /*Se nao apenas modifico o DB*/
            if (tipoDeOp == DocumentChange.Type.ADDED) MyRealm.insert(despesa);
            else if (tipoDeOp == DocumentChange.Type.MODIFIED) MyRealm.insertOrUpdate(despesa);
        }
    }

    private void atualizarCategorias(@NotNull Categoria categoria, DocumentChange.Type tipoDeOp) {

        /* AS categoriaspodem ser inseridas direto no db sem atualiza a interface pois nao tem id de mes*/
        if (tipoDeOp == DocumentChange.Type.ADDED) MyRealm.insert(categoria);
        else if (tipoDeOp == DocumentChange.Type.MODIFIED) MyRealm.insertOrUpdate(categoria);

    }

    private void atualizarNotas(@NotNull Nota nota, DocumentChange.Type tipoDeOp) {

        /*Se o objeto for do mes atual, atualizo o objeto mes diretamente e a UI tbm */
        if (nota.getMesId() == mesAtual.getId()) {
            if (tipoDeOp == DocumentChange.Type.ADDED) mesAtual.addNota(nota);
            else if (tipoDeOp == DocumentChange.Type.MODIFIED) mesAtual.attNota(nota);
            Broadcaster.enviar(Broadcaster.atualizarFragNotas);
        } else {
            /*Se nao apenas modifico o DB*/
            if (tipoDeOp == DocumentChange.Type.ADDED) MyRealm.insert(nota);
            else if (tipoDeOp == DocumentChange.Type.MODIFIED) MyRealm.insertOrUpdate(nota);
        }
    }

    /**
     * Verifica se as condiçoes do objeto batem com o descrito, se de fato o objeto foi inserido/atualizado ou se é bug do firebase.
     * se o objeto foi inserido, verifico se ele é uma duplicata (inserido pelo sistem automaticamente), se for, n permito que a operaçao continue.
     * <p>
     * retorna true se a classe pode seguir com a adiçao ou atualizaçao do objeto, false se
     * divergencias foram encontradas e a operaçao deve ser interrompida.
     */
    private boolean verificarReceita(Receita nuvemObjeto, DocumentChange.Type tipoDeOp) {

        Log.d(Tag.AppTag + " LiveSinc:", "verificarReceita() called with: nuvemObjeto = [" + nuvemObjeto.toString() + "], tipoDeOp = [" + tipoDeOp + "]");
        // obj teoricamente adicionado por outro dispositivo
        if (tipoDeOp == DocumentChange.Type.ADDED) {

            /*Se este obj foi inserido na nuvem por outro dispositivo ele ainda nao existe aqui*/
            Realm realm = Realm.getDefaultInstance();
            /*Nao interessa se o objeto foi removido ou nao pq se o objeto foi inserido, simplesmente nao pode haver outro objeto com mesma id */
            Receita localObjeto = realm.where(Receita.class)/*.equalTo("removido", false).and()*/.equalTo("id", nuvemObjeto.getId()).findFirst();

            if (localObjeto != null) {
                // objeto existe entao é um bug do firebase
                realm.close();
                return false;
            }
            /*um objeto com mesma id nao foi encontrado, entao de fato é um objeto adicionado por outro dispositivo, preciso
            verificar agora se este objeto é uma duplicata inserida automaticamente pelo dispositivo*/

            // aqui tento achar um objeto do mesmo mes seja recorrente/auto-importado/parcelado com o mesmo nome do objeto da nuvem.
            localObjeto = realm.where(Receita.class).equalTo("removido", false).and().equalTo("mesId", nuvemObjeto.getMesId()).and().equalTo("nome", nuvemObjeto.getNome()).findFirst();

            if (localObjeto != null) {
                // objeto existe entao é uma duplicata
                Log.d(Tag.AppTag, "LiveSinc.verificarReceita: " + nuvemObjeto.getNome() + " é uma possivel duplicata, abortando operação.\nnuvem: " + nuvemObjeto.toString() + "\nlocal: " + localObjeto.toString());
                realm.close();
                return false;
            } else {
                realm.close();
                return true;
            }


        } else if (tipoDeOp == DocumentChange.Type.MODIFIED) {
            // obj teoricamente atualizado por outro dispositivo

            /*Se este obj foi atualizado na nuvem por outro dispositivo ele  deve ter sua timestamp local menor que a da nuvem*/
            Realm realm = Realm.getDefaultInstance();
            Receita localObjeto = realm.where(Receita.class).equalTo("removido", false).and().equalTo("id", nuvemObjeto.getId()).findFirst();
            if (localObjeto != null) localObjeto = realm.copyFromRealm(localObjeto);
            realm.close();
            // objeto nao existe localmente. É necessario que o mesmo seja adicionado primeiro para entao depois ser atualizado
            if (localObjeto == null) return false;
                // se o objeto na nuvem é mais recente a operação deve seguir se nao, deve ser interrompida.
            else return localObjeto.getUltimaAtt() < nuvemObjeto.getUltimaAtt();

        }
        return false;
    }

    /**
     * Verifica se as condiçoes do objeto batem com o descrito, se de fato o objeto foi inserido/atualizado ou se é bug do firebase.
     * se o objeto foi inserido, verifico se ele é uma duplicata (inserido pelo sistem automaticamente), se for, n permito que a operaçao continue.
     * <p>
     * retorna true se a classe pode seguir com a adiçao ou atualizaçao do objeto, false se
     * divergencias foram encontradas e a operaçao deve ser interrompida.
     */
    private boolean verificarDespesa(Despesa nuvemObjeto, DocumentChange.Type tipoDeOp) {

        Log.d(Tag.AppTag + " LiveSinc:", "verificarDespesa() called with: nuvemObjeto = [" + nuvemObjeto.toString() + "], tipoDeOp = [" + tipoDeOp + "]");
        // obj teoricamente adicionado por outro dispositivo
        if (tipoDeOp == DocumentChange.Type.ADDED) {

            /*Se este obj foi inserido na nuvem por outro dispositivo ele ainda nao existe aqui*/
            Realm realm = Realm.getDefaultInstance();
            /*Nao interessa se o objeto foi removido ou nao pq se o objeto foi inserido, simplesmente nao pode haver outro objeto com mesma id */
            Despesa localObjeto = realm.where(Despesa.class).equalTo("id", nuvemObjeto.getId()).findFirst();


            if (localObjeto != null) {
                // objeto existe entao é um bug do firebase
                realm.close();
                return false;
            }
            /*um objeto com mesma id nao foi encontrado, entao de fato é um objeto adicionado por outro dispositivo, preciso
            verificar agora se este objeto é uma duplicata inserida automaticamente pelo dispositivo*/

            // aqui tento achar um objeto do mesmo mes seja recorrente/auto-importado/parcelado com o mesmo nome do objeto da nuvem.
            localObjeto = realm.where(Despesa.class).equalTo("removido", false).and().equalTo("mesId", nuvemObjeto.getMesId()).and().equalTo("nome", nuvemObjeto.getNome()).findFirst();

            if (localObjeto != null) {
                // objeto existe entao é uma duplicata
                Log.d(Tag.AppTag, "LiveSinc.verificarDespesa: " + nuvemObjeto.getNome() + " é uma possivel duplicata, abortando operação.\nnuvem: " + nuvemObjeto.toString() + "\nlocal: " + localObjeto.toString());
                realm.close();
                return false;
            } else {
                realm.close();
                return true;
            }


        } else if (tipoDeOp == DocumentChange.Type.MODIFIED) {
            // obj teoricamente atualizado por outro dispositivo

            /*Se este obj foi atualizado na nuvem por outro dispositivo ele  deve ter sua timestamp local menor que a da nuvem*/
            Realm realm = Realm.getDefaultInstance();
            Despesa localObjeto = realm.where(Despesa.class).equalTo("removido", false).and().equalTo("id", nuvemObjeto.getId()).findFirst();
            if (localObjeto != null) localObjeto = realm.copyFromRealm(localObjeto);
            realm.close();
            // objeto nao existe localmente. É necessario que o mesmo seja adicionado primeiro para entao depois ser atualizado
            if (localObjeto == null) return false;
                // se o objeto na nuvem é mais recente a operação deve seguir se nao, deve ser interrompida.
            else return localObjeto.getUltimaAtt() < nuvemObjeto.getUltimaAtt();

        }
        return false;
    }

    /**
     * Verifica se as condiçoes do objeto batem com o descrito, se de fato o objeto foi inserido/atualizado ou se é bug do firebase.
     * se o objeto foi inserido, verifico se ele é uma duplicata (inserido pelo sistem automaticamente), se for, n permito que a operaçao continue.
     * <p>
     * retorna true se a classe pode seguir com a adiçao ou atualizaçao do objeto, false se
     * divergencias foram encontradas e a operaçao deve ser interrompida.
     */
    private boolean verificarCategoria(Categoria nuvemObjeto, DocumentChange.Type tipoDeOp) {

        Log.d(Tag.AppTag + " LiveSinc:", "verificarCategoria() called with: nuvemObjeto = [" + nuvemObjeto.toString() + "], tipoDeOp = [" + tipoDeOp + "]");
        // obj teoricamente adicionado por outro dispositivo
        if (tipoDeOp == DocumentChange.Type.ADDED) {

            /*Se este obj foi inserido na nuvem por outro dispositivo ele ainda nao existe aqui*/
            Realm realm = Realm.getDefaultInstance();
            /*Nao interessa se o objeto foi removido ou nao pq se o objeto foi inserido, simplesmente nao pode haver outro objeto com mesma id */
            Categoria localObjeto = realm.where(Categoria.class).equalTo("id", nuvemObjeto.getId()).findFirst();

            if (localObjeto != null) {
                // objeto existe entao é um bug do firebase
                realm.close();
                return false;
            }
            /*um objeto com mesma id nao foi encontrado, entao de fato é um objeto adicionado por outro dispositivo, preciso
            verificar agora se este objeto é uma duplicata inserida automaticamente pelo dispositivo*/

            // aqui tento achar um objeto com o mesmo nome do objeto da nuvem.
            localObjeto = realm.where(Categoria.class).equalTo("removido", false).and().equalTo("nome", nuvemObjeto.getNome()).findFirst();

            if (localObjeto != null) {
                // objeto existe entao é uma duplicata
                Log.d(Tag.AppTag, "LiveSinc.verificarCategoria: " + nuvemObjeto.getNome() + " é uma possivel duplicata, abortando operação.\nnuvem: " + nuvemObjeto.toString() + "\nlocal: " + localObjeto.toString());
                realm.close();
                return false;
            } else {
                realm.close();
                return true;
            }


        } else if (tipoDeOp == DocumentChange.Type.MODIFIED) {
            // obj teoricamente atualizado por outro dispositivo

            /*Se este obj foi atualizado na nuvem por outro dispositivo ele  deve ter sua timestamp local menor que a da nuvem*/
            Realm realm = Realm.getDefaultInstance();
            Categoria localObjeto = realm.where(Categoria.class).equalTo("removido", false).and().equalTo("id", nuvemObjeto.getId()).findFirst();
            if (localObjeto != null) localObjeto = realm.copyFromRealm(localObjeto);
            realm.close();
            // objeto nao existe localmente. É necessario que o mesmo seja adicionado primeiro para entao depois ser atualizado
            if (localObjeto == null) return false;
                // se o objeto na nuvem é mais recente a operação deve seguir se nao, deve ser interrompida.
            else return localObjeto.getUltimaAtt() < nuvemObjeto.getUltimaAtt();

        }
        return false;
    }

    /**
     * Verifica se as condiçoes do objeto batem com o descrito, se de fato o objeto foi inserido/atualizado ou se é bug do firebase.
     * <p>
     * retorna true se a classe pode seguir com a adiçao ou atualizaçao do objeto, false se
     * divergencias foram encontradas e a operaçao deve ser interrompida.
     */
    private boolean verificarNota(Nota nuvemObjeto, DocumentChange.Type tipoDeOp) {

        Log.d(Tag.AppTag + " LiveSinc:", "verificarNota() called with: nuvemObjeto = [" + nuvemObjeto.toString() + "], tipoDeOp = [" + tipoDeOp + "]");
        // obj teoricamente adicionado por outro dispositivo
        if (tipoDeOp == DocumentChange.Type.ADDED) {

            /*Se este obj foi inserido na nuvem por outro dispositivo ele ainda nao existe aqui*/
            Realm realm = Realm.getDefaultInstance();
            /*Nao interessa se o objeto foi removido ou nao pq se o objeto foi inserido, simplesmente nao pode haver outro objeto com mesma id */
            Nota localObjeto = realm.where(Nota.class).equalTo("id", nuvemObjeto.getId()).findFirst();
            realm.close();
            return localObjeto == null;

        } else if (tipoDeOp == DocumentChange.Type.MODIFIED) {
            // obj teoricamente atualizado por outro dispositivo

            /*Se este obj foi atualizado na nuvem por outro dispositivo ele  deve ter sua timestamp local menor que a da nuvem*/
            Realm realm = Realm.getDefaultInstance();
            Nota localObjeto = realm.where(Nota.class).equalTo("removido", false).and().equalTo("id", nuvemObjeto.getId()).findFirst();
            if (localObjeto != null) localObjeto = realm.copyFromRealm(localObjeto);
            realm.close();
            // objeto nao existe localmente. É necessario que o mesmo seja adicionado primeiro para entao depois ser atualizado
            if (localObjeto == null) return false;
                // se o objeto na nuvem é mais recente a operação deve seguir se nao, deve ser interrompida.
            else return localObjeto.getUltimaAtt() < nuvemObjeto.getUltimaAtt();

        }
        return false;
    }

    /**
     * Remove localmente de forma definitiva um objeto que foi removido do banco de dos do firebase.
     * <p>
     * Esta solução foi implementada para evitar duplicatas criadas pela auto criação de receitas e despesas
     * de outros dispositivos na virada do mes ou de categorias quando um novo dispositivo  se conecta, enviando
     * suas categorias recem adicionadas a nuvem, c sabe, as categorias padrao, pois é. Apos o sincronismo deste
     * dispositivo (recem-conectado) ele remove as duplicatas  dele mesmo e as remove da nuvem tbm, este metodo é
     * chamado para remover estes objetos duplicados do realm assim que eles forem removidos  da nuvem.
     * <p>
     * Logo após criar esse metodo escrevi um codigo de atualizazaçao nos metodos de verificaçao  (verificarDespesa, verificarReceita, etc..)
     * nessa atualizaçao os metodos passam a verificar se o objeto é uma duplicata com base no seu mes e no seu nome
     * e se forem duplicatas esses objetos nao sao inseridos. Com isso esse metodo aqui ficou meio sem uso mas vou
     * mante-lo por precauçao, se os metodos de verificaçao deixarem passar alguma duplicata esse metodo aqui remove ela
     * qdo o dispositivo que a enviou remover a duplicata local e a da nuvem
     */
    private void removerDuplicataPermanentemente(String nome, long id,
                                                 @SuppressWarnings("rawtypes") Class classe) {

        Realm realm = Realm.getDefaultInstance();

        RealmObject obj = (RealmObject) realm.where(classe).equalTo("id", id).findFirst();

        if (obj != null) {
            obj = realm.copyFromRealm(obj);
            realm.close();
            MyRealm.removerPermanentemente(obj);
            Log.d(Tag.AppTag + " LiveSinc:", "removerObjeto() called with: nome = [" + nome + "], id = [" + id + "], classe = [" + classe.getSimpleName() + "]" + " -> removido.");

        } else {
            Log.d(Tag.AppTag + " LiveSinc:", "removerObjeto() called with: nome = [" + nome + "], id = [" + id + "], classe = [" + classe.getSimpleName() + "] -> Não encontrado pra remover");
            realm.close();
        }
    }

    public void pararDeOuvir() {

        if (firebase == null) return;

        Log.d(Tag.AppTag, "LiveSinc.pararDeOuvir: desligando liveSinc");

        if (receitasLc != null) receitasLc.remove();
        if (despesasasLc != null) despesasasLc.remove();
        if (categoriasLc != null) categoriasLc.remove();
        if (notasLc != null) notasLc.remove();

        jasFezAPrimeiraChamadaReceita = false;
        jasFezAPrimeiraChamadaDespesa = false;
        jasFezAPrimeiraChamadaCategoria = false;
        jasFezAPrimeiraChamadaNota = false;

        firebase = null;
        gson = null;
    }
}
