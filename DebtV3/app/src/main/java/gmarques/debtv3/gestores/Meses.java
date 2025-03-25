package gmarques.debtv3.gestores;

import android.util.ArrayMap;
import android.util.Log;

import com.google.gson.Gson;

import org.joda.time.LocalDate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import gmarques.debtv3.modelos.Despesa;
import gmarques.debtv3.modelos.Mes;
import gmarques.debtv3.modelos.Nota;
import gmarques.debtv3.modelos.Receita;
import gmarques.debtv3.outros.MyRealm;
import gmarques.debtv3.outros.Tag;
import io.realm.Realm;
import io.realm.RealmResults;


/**
 * Gerencia os meses
 */
public class Meses {

    public static Mes mesAtual;

    public static void inicializar() {
        LocalDate hoje = new LocalDate();

        verificarMesesCriados();
        carregarMesAtual(hoje);
        /*se nao houverem pendencias no mes atual, mudo para o prox mes*/
        if (!mesAtualTemPendencias()) carregarMesAtual(hoje.plusMonths(1));
    }

    /***
     *
     * retorna true se houverem despesas que nao foram pagas, receitas que nao foram recebidas e notas que nao foram concluidas
     * no mesAtual ou se o usuario ainda nao tiver adicionado receitas E despesas, caso contrario retorna false
     * */
    private static boolean mesAtualTemPendencias() {
        /*seo usuarioa ainda nao adicionou receitas E despesas no mes deve dizer que ainda ha pendencias*/
        if (mesAtual.getReceitas().size() == 0 || mesAtual.getDespesas().size() == 0) return true;

        for (Despesa despesa : mesAtual.getDespesas()) if (!despesa.estaPaga()) return true;
        for (Receita receita : mesAtual.getReceitas()) if (!receita.estaRecebido()) return true;
        for (Nota nota : mesAtual.getNotas()) if (!nota.estaConcluido()) return true;

        return false;
    }

    private static void carregarMesAtual(LocalDate hoje) {

        Realm realm = Realm.getDefaultInstance();
        /*Nesse caso é mais facil buscar pelo mes e ano do pela id*/
        mesAtual = realm.where(Mes.class)
                .equalTo("mes", hoje.getMonthOfYear())
                .and()
                .equalTo("ano", hoje.getYear())
                .findFirst();
        /*um algoritimo de gestao de meses  (verificarMesesCriados())  é executado no boot garantindo que sempre haja
         * um objeto Mes pra data atual, se não houver, algo precisa ser corrigido*/
        assert mesAtual != null;
        mesAtual = realm.copyFromRealm(mesAtual);
        realm.close();

    }

    public static void setMesAtual(Mes mesAtual) {
        Meses.mesAtual = mesAtual;
    }

    /**
     * Verifica os meses criados para a data atual + 6 meses a frente
     * e cria se necessario
     */
    private static void verificarMesesCriados() {

        LocalDate hoje = new LocalDate();
        /*cria objetos meses para o mes atual e os proximos 6 se necessario*/
        for (int i = 1; i <= 7; i++) {
            if (getMes(hoje.getMonthOfYear(), hoje.getYear()) == null) {
                Mes novoMes = criarMes(hoje.getMonthOfYear(), hoje.getYear());

                addDespesasRecorrentes(novoMes);
                addDespesasParceladas(novoMes);
                addReceitasParceladas(novoMes);
                addReceitasRecorrentes(novoMes);
            }
            hoje = hoje.plusMonths(1);
        }
    }

    private static void addReceitasParceladas(Mes novoMes) {
        List<Receita> receitas = new ArrayList<>();
        Realm realm = Realm.getDefaultInstance();
        RealmResults<Receita> rReceitas = realm.where(Receita.class).equalTo("removido", false).and().equalTo("mesId", Receita.AUTOIMPORTADA).findAll();
        if (rReceitas.size() > 0) receitas = realm.copyFromRealm(rReceitas);
        realm.close();

        for (Receita receita : receitas) {

            Receita autoImportada = Receitas.clonarComAlteraçaoDeId(receita);

            if (Receitas.possoImportarNesseMes(autoImportada, novoMes)) {
                Receitas.mudarDataDeAcordoComMes(novoMes.getMes(), novoMes.getAno(), autoImportada);
                autoImportada.setRecebida(false);
                novoMes.addReceita(autoImportada);
                Log.d(Tag.AppTag, "addReceitasParceladas: " + autoImportada.getNome() + " pgto: " + new LocalDate(autoImportada.getDataDeRecebimento()).toString() + " adicionada em " + novoMes.getNome());

            } else {
                Log.d(Tag.AppTag, "addReceitasParceladas: " + autoImportada.getNome() + " nao pode ser importada para " + novoMes.getNome() + " pois expirou. Auto-importar até: " + new LocalDate(autoImportada.getAutoImportarPrimeira()));
                MyRealm.remover(receita);
            }

        }
    }

    private static void addReceitasRecorrentes(Mes novoMes) {
        List<Receita> receitas = new ArrayList<>();
        Realm realm = Realm.getDefaultInstance();
        RealmResults<Receita> rReceitas = realm.where(Receita.class).equalTo("removido", false).and().equalTo("mesId", Receita.RECORRENTE).findAll();
        if (rReceitas.size() > 0) receitas = realm.copyFromRealm(rReceitas);
        realm.close();

        for (Receita receita : receitas) {

            Receita recorrente = Receitas.clonarComAlteraçaoDeId(receita);

            Receitas.mudarDataDeAcordoComMes(novoMes.getMes(), novoMes.getAno(), recorrente);
            recorrente.setRecebida(false);
            novoMes.addReceita(recorrente);
            Log.d(Tag.AppTag, "addReceitasRecorrentes: " + recorrente.getNome() + " pgto: " + new LocalDate(recorrente.getDataDeRecebimento()).toString() + " adicionada em " + novoMes.getNome());

        }
    }

    private static void addDespesasRecorrentes(Mes novoMes) {
        List<Despesa> despesas = new ArrayList<>();

        Realm realm = Realm.getDefaultInstance();
        RealmResults<Despesa> rDesp = realm.where(Despesa.class).equalTo("removido", false).and().equalTo("mesId", Despesa.RECORRENTE).findAll();
        if (rDesp.size() > 0) despesas = realm.copyFromRealm(rDesp);
        realm.close();

        for (Despesa despesa : despesas) {
            Despesa recorrente = Despesas.clonarComAlteraçaoDeId(despesa);
            Despesas.mudarDataDeAcordoComMes(novoMes.getMes(), novoMes.getAno(), recorrente);
            recorrente.setPaga(false);
            novoMes.addDespesa(recorrente);
            Log.d(Tag.AppTag, "addDespesasRecorrentes: " + recorrente.getNome() + " pgto: " + new LocalDate(recorrente.getDataDePagamento()).toString() + " adicionada em " + novoMes.getNome());
        }
    }

    private static void addDespesasParceladas(Mes novoMes) {
        List<Despesa> despesas = new ArrayList<>();

        Realm realm = Realm.getDefaultInstance();
        RealmResults<Despesa> rDesp = realm.where(Despesa.class).equalTo("removido", false).and().equalTo("mesId", Despesa.PARCELADA).findAll();
        if (rDesp.size() > 0) despesas = realm.copyFromRealm(rDesp);
        realm.close();

        for (Despesa despesa : despesas) {

            Despesa parcelada = Despesas.clonarComAlteraçaoDeId(despesa);
            if (Despesas.possoImportarNesseMes(parcelada, novoMes)) {
                Despesas.mudarDataDeAcordoComMes(novoMes.getMes(), novoMes.getAno(), parcelada);
                parcelada.setPaga(false);
                novoMes.addDespesa(parcelada);
                Log.d(Tag.AppTag, "addDespesasParceladas: " + parcelada.getNome() + " pgto: " + new LocalDate(parcelada.getDataDePagamento()).toString() + " adicionada em " + novoMes.getNome());

            } else {
                Log.d(Tag.AppTag, "addDespesasParceladas: " + parcelada.getNome() + " nao pode ser importada para " + novoMes.getNome() + " pois expirou. UltimaParcela: " + new LocalDate(parcelada.getUltimaParcela()));
                /*se expirar, removo a despesa com id = Despesa.PARCELADA*/
                MyRealm.remover(despesa);
            }

        }
    }

    public static Mes getMes(int mes, int ano) {
        Realm realm = Realm.getDefaultInstance();

        Mes mesObj = realm.where(Mes.class)
                .equalTo("mes", mes)
                .and()
                .equalTo("ano", ano)
                .and()
                .findFirst();

        if (mesObj != null) mesObj = realm.copyFromRealm(mesObj);

        realm.close();

        return mesObj;
    }

    public static Mes getMes(LocalDate data) {
        return getMes(data.getMonthOfYear(), data.getYear());
    }


    private static Mes criarMes(int mes, int ano) {
        final Mes mesObj = new Mes(mes, ano);
        Log.d(Tag.AppTag, "criarMes: objeto criado para " + mes + "/" + ano);
        MyRealm.insert(mesObj);
        return mesObj;

    }

    /***
     *  A data recebida serve de base e é excluida da inserção. Se for 05/2020 a inserçao começara em 06/2020
     */
    public static void atualizarCopias(Despesa despAtualizada, Despesa despDesatualizada, int mesInicio, int anoInicio) {

        LocalDate data = new LocalDate(anoInicio, mesInicio, 1).plusMonths(1);
        Map<String, Object> alteracoes = Despesas.getAlteracoes(despAtualizada, despDesatualizada);
        Realm realm = Realm.getDefaultInstance();
        Mes mes;

        while ((mes = getMes(data.getMonthOfYear(), data.getYear())) != null) {
            Despesa alvo = realm.where(Despesa.class)
                    .equalTo("removido", false)
                    .and()
                    .equalTo("mesId", mes.getId())
                    .and()
                    .equalTo("nome", despDesatualizada.getNome())
                    .findFirst();
            if (alvo != null) {
                alvo = realm.copyFromRealm(alvo);
                alvo = Despesas.aplicarAlteracoes(alteracoes, alvo);

                /*caso o usuario mude a data do pagamento, corrijo o mes para que apenas o dia seja alterado, impedindo
                 * que todas as despesas que forem atualizadas com as modificações na data sejam definidas para serem pagas no mesmo mes*/
                Despesas.mudarDataDeAcordoComMes(mes.getMes(), mes.getAno(), alvo);

                mes.attDespesa(alvo);
                Log.d(Tag.AppTag, "atualizarCopias: " + alvo.getNome() + " atualizado: " + new Gson().toJson(alvo));
            } else
                Log.d(Tag.AppTag, "atualizarCopias: não foi encontrada despesa com mesmo nome (" + despDesatualizada.getNome() + ") no mes " + mes.getNome() + ".");
            data = data.plusMonths(1).withDayOfMonth(1);
        }
        Log.d(Tag.AppTag, "atualizarCopias: feito");

        realm.close();
    }

    /***
     *  A data recebida serve de base e é excluida da inserção. Se for 05/2020 a inserçao começara em 06/2020
     */
    public static void atualizarCopias(Receita recAtualizada, Receita recDesatualizada, int mesInicio, int anoInicio) {

        LocalDate data = new LocalDate(anoInicio, mesInicio, 1).plusMonths(1);
        Map<String, Object> alteracoes = Receitas.getAlteracoes(recAtualizada, recDesatualizada);

        Realm realm = Realm.getDefaultInstance();
        Mes mes;

        while ((mes = getMes(data.getMonthOfYear(), data.getYear())) != null) {
            Receita alvo = realm.where(Receita.class)
                    .equalTo("removido", false)
                    .and()
                    .equalTo("mesId", mes.getId())
                    .and()
                    .equalTo("nome", recDesatualizada.getNome())
                    .findFirst();
            if (alvo != null) {
                alvo = realm.copyFromRealm(alvo);
                alvo = Receitas.aplicarAlteracoes(alteracoes, alvo);


                /*caso o usuario mude a data do recebimento, corrijo o mes para que apenas o dia seja alterado, impedindo
                 * que todas as recetitas que forem atualizadas com as modificações na data sejam definidas para serem recebidas no mesmo mes*/
                Receitas.mudarDataDeAcordoComMes(mes.getMes(), mes.getAno(), alvo);

                mes.attReceita(alvo);
                Log.d(Tag.AppTag, "atualizarCopias: " + alvo.getNome() + " atualizado: " + new Gson().toJson(alvo));
            } else
                Log.d(Tag.AppTag, "atualizarCopias: não foi encontrada receita com mesmo nome (" + recDesatualizada.getNome() + ") no mes " + mes.getNome() + ".");
            data = data.plusMonths(1).withDayOfMonth(1);
        }
        Log.d(Tag.AppTag, "atualizarCopias: feito");

        realm.close();
    }

    /**
     * Esse metodo nao adiciona a despesa recorrente, ele adiciona a copia convencional da recorrente nos meses
     * A data recebida serve de base e é excluida da inserção. Se for 05/2020 a inserçao começara em 06/2020
     */
    public static void importarDespesaRecorrente(Despesa despesa, int mesInicio, int anoInicio) {

        LocalDate data = new LocalDate(anoInicio, mesInicio, 1).plusMonths(1);

        Realm realm = Realm.getDefaultInstance();
        Mes mes;

        while ((mes = getMes(data.getMonthOfYear(), data.getYear())) != null) {

            Despesa novaDespesa = Despesas.mudarDataDeAcordoComMes(mes.getMes(), mes.getAno(), Despesas.clonarComAlteraçaoDeId(despesa));
            novaDespesa.setPaga(false);
            mes.addDespesa(novaDespesa);
            Log.d(Tag.AppTag, "importarDespesaRecorrente: " + novaDespesa.getNome() + " pgto: " + new LocalDate(novaDespesa.getDataDePagamento()).toString() + " adicionada em " + mes.getNome());

            data = data.plusMonths(1).withDayOfMonth(1);
        }

        Log.d(Tag.AppTag, "importarDespesaRecorrente: feito");

        realm.close();

    }

    /**
     * Esse metodo nao adiciona a receita recorrente, ele adiciona a copia convencional da recorrente nos meses
     * * A data recebida serve de base e é excluida da inserção. Se for 05/2020 a inserçao começara em 06/2020
     */
    public static void importarReceitaRecorrente(Receita receita, int mesInicio, int anoInicio) {

        LocalDate data = new LocalDate(anoInicio, mesInicio, 1).plusMonths(1);

        Realm realm = Realm.getDefaultInstance();
        Mes mes;

        while ((mes = getMes(data.getMonthOfYear(), data.getYear())) != null) {

            Receita novaReceita = Receitas.mudarDataDeAcordoComMes(mes.getMes(), mes.getAno(), Receitas.clonarComAlteraçaoDeId(receita));
            novaReceita.setRecebida(false);
            mes.addReceita(novaReceita);
            Log.d(Tag.AppTag, "importarReceitaRecorrente: " + novaReceita.getNome() + " pgto: " + new LocalDate(novaReceita.getDataDeRecebimento()).toString() + " adicionada em " + mes.getNome());

            data = data.plusMonths(1).withDayOfMonth(1);
        }

        Log.d(Tag.AppTag, "importarReceitaRecorrente: feito");

        realm.close();

    }

    /**
     * Esse metodo nao adiciona a despesa parcelada (mesId = {@link Despesa.PARCELADA}), ele adiciona a copia convencional da parcelada nos meses
     * A data recebida serve de base e é excluida da inserção. Se for 05/2020 a inserçao começara em 06/2020
     */
    @SuppressWarnings("JavadocReference")
    public static void importarDespesaParcelada(long idOriginal, Despesa despParcelada, int mesInicio, int anoInicio) {

        LocalDate data = new LocalDate(anoInicio, mesInicio, 1).plusMonths(1);

        Realm realm = Realm.getDefaultInstance();
        Mes mes;

        while ((mes = getMes(data.getMonthOfYear(), data.getYear())) != null) {

            Despesa despParcCopia = Despesas.clonarComAlteraçaoDeId(despParcelada);
            if (Despesas.possoImportarNesseMes(despParcCopia, mes)) {
                Despesas.mudarDataDeAcordoComMes(mes.getMes(), mes.getAno(), despParcCopia);
                despParcCopia.setPaga(false);
                mes.addDespesa(despParcCopia);
                Log.d(Tag.AppTag, "importarDespesaParcelada: " + despParcCopia.getNome() + " pgto: " + new LocalDate(despParcCopia.getDataDePagamento()).toString() + " adicionada em " + mes.getNome());

            } else {
                Log.d(Tag.AppTag, "importarDespesaParcelada: " + despParcCopia.getNome() + " nao pode ser importada para " + mes.getNome() + " pois expirou. UltimaParcela: " + new LocalDate(despParcCopia.getUltimaParcela()));
                Despesa parceladaOriginalNoDB = realm.where(Despesa.class).equalTo("removido", false).and().equalTo("id", idOriginal).findFirst();
                assert parceladaOriginalNoDB != null;/* se a chamada a baixo der erro, é pq tem bug no codigo*/
                MyRealm.remover(realm.copyFromRealm(parceladaOriginalNoDB));
                break;
            }

            data = data.plusMonths(1).withDayOfMonth(1);
        }
        Log.d(Tag.AppTag, "importarDespesaParcelada: feito");

        realm.close();


    }

    /**
     * Esse metodo nao adiciona a receita parcelada (mesId = {@link Receita.AUTOIMPORTADA}), ele adiciona a copia convencional da parcelada nos meses
     * A data recebida serve de base e é excluida da inserção. Se for 05/2020 a inserçao começara em 06/2020
     */
    @SuppressWarnings("JavadocReference")
    public static void importarReceitaParcelada(long idOriginal, Receita recParcelada, int mesInicio, int anoInicio) {

        LocalDate data = new LocalDate(anoInicio, mesInicio, 1).plusMonths(1);

        Realm realm = Realm.getDefaultInstance();
        Mes mes;

        while ((mes = getMes(data.getMonthOfYear(), data.getYear())) != null) {

            Receita recParcCopia = Receitas.clonarComAlteraçaoDeId(recParcelada);
            if (Receitas.possoImportarNesseMes(recParcCopia, mes)) {
                Receitas.mudarDataDeAcordoComMes(mes.getMes(), mes.getAno(), recParcCopia);
                recParcCopia.setRecebida(false);
                mes.addReceita(recParcCopia);
                Log.d(Tag.AppTag, "importarReceitaParcelada: " + recParcCopia.getNome() + " pgto: " + new LocalDate(recParcCopia.getDataDeRecebimento()).toString() + " adicionada em " + mes.getNome());

            } else {
                Log.d(Tag.AppTag, "importarReceitaParcelada: " + recParcCopia.getNome() + " nao pode ser importada para " + mes.getNome() + " pois expirou. Auto-importar até: " + new LocalDate(recParcCopia.getAutoImportarPrimeira()));
                Receita parceladaOriginalNoDB = realm.where(Receita.class).equalTo("removido", false).and().equalTo("id", idOriginal).findFirst();
                assert parceladaOriginalNoDB != null;/* se a chamada a baixo der erro, é pq tem bug no codigo*/
                MyRealm.remover(realm.copyFromRealm(parceladaOriginalNoDB));
                break;
            }

            data = data.plusMonths(1).withDayOfMonth(1);
        }
        Log.d(Tag.AppTag, "importarReceitaParcelada: feito");

        realm.close();


    }


    public static List<Mes> getTodosOsMeses() {
        Realm realm = Realm.getDefaultInstance();
        RealmResults<Mes> rMeses = realm.where(Mes.class).findAll();
        List<Mes> meses = realm.copyFromRealm(rMeses);
        realm.close();
        return meses;

    }

    /**
     * o aplicativo não sincroniza os meses pois estes são criados automaticamente e tem uma id padrão
     * que se refere ao mês e ao ano. toda vez que limpa os dados do aplicativo e faça o login novamente
     * o aplicativo cria meses a partir do mês atual o resultado é que todas as despesas e receitas
     * de meses anteriores ao mês atual deixam de ser exibidas pois esses meses não são recriados e
     * nem sincronizados. Este método serve para verificar APÓS O SINCRONISMO se todas as despesas
     * tem um objeto mês referente no banco de dados caso não, cria um mês garantindo que todos os
     * dados mesmo de meses anteriores ao mês atual sejam exibidos corretamente
     */
    public static void verificarSeTemMesesParaTodasAsDespesas() {
        ArrayMap<Long, Despesa> despesas = new ArrayMap<>();

        //pego todas as despesas nao removidas
        Realm realm = Realm.getDefaultInstance();
        RealmResults<Despesa> rDesp = realm.where(Despesa.class)
                .equalTo("removido", false)
                .findAll();
        for (Despesa despesa : rDesp) despesas.put(despesa.getMesId(), realm.copyFromRealm(despesa));

        realm.close();

        for (long key : despesas.keySet()) {
            Despesa despesa = despesas.get(key);


            Log.d(Tag.AppTag, "Meses.verificarSeTemMesesParaTodasAsDespesas: " + despesa.getNome()+" "+despesa.getMesId());
            if (despesa.getMesId() == 9999999 ||despesa.getMesId() == 9999998)
                continue;//despesas com meses dessas ids sao  parceladas ou recorrentes e n devem ter um objeto mes criado no DB

            //  --  12020 - 112020
            String smID = String.valueOf(despesa.getMesId());
            int mes = Integer.parseInt(smID.substring(0, (smID.length() - 4)));
            int ano = Integer.parseInt(smID.substring((smID.length() - 4)));


            if (getMes(mes, ano) == null) {
                Log.d(Tag.AppTag, "Meses.verificarSeTemMesesParaTodasAsDespesas: criando mes com a data/id: " + despesa.getMesId());
                criarMes(mes, ano);
            }

        }

    }


}
