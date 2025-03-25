package gmarques.debtv3.sincronismo.api;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import gmarques.debtv3.especificos.Data;


/**
 * Classe criada com o objetivo de generificar a tarefa de sincronizar dados
 */
/*
 *  PARA IMPLEMENTAR SINCRONISMO EM OUTROS APPS BASTA COPIAR ESTA PACOTE (api)
 * PARA O PROJETO EM QUESTAO E INSTANCIAR ESSA CLASSE PASSANDO O CALLBACK E IMPLEMENTANDO
 * OS METODOS NESSESSARIOS NELE                              .
 *
 * OS OBJETOS A SEREM SINCRONIZADOS DEVEM IMPLEMENTAR A INTERFACE Sincronizavel
 * USE A INTERFACE CallbackUI PARA ATUALIZAR A INTERFACE ENQUANTO O SINCRONIMSO É EXECUTRADO
 *
 * * * *  INSTRUÇOES PARA ADD UM NOVO OBJETO SINCRONIZAVEL A ESTE APP (DEBTV3)
 *   #1 "herde o objeto de @Sincronizavel e implemente os metodos dessa interface corretamente, use como exemplo qqer outro objeto como receita ou despesa para ter certeza de que fez tudo certo.
 *   #2 em SincAdapterImpl no metodo getDadosLocal(); escreva o codigo pro realm carregar todos os objetos do tipo do banco de dados para fazer o sincronismo.
 *   #3 em FirebaseImpl no metodo getDados(); adicione o codigo para baixar os dados do objeto da nuvem para o sincronismo. e atualize a variavel 'tiposDeDados' sobre esse metodo
 *      para refletir a quantidade de objetos que sao sincronizaveis
 *   #4 em MyRealm no metodo removerPermanentemente() adicione o codigo para remover permanentemente o objeto
 *
 *   #Dica: No geral é só implementar @Sincronizavel, e dar Crtl+C - Crtl+V nas classes citadas a cima, todas elas ja tem codigo pra sincronizar os objetos, é só copiar o metodo, renomear
 *  seguindo padrao de nomes da classe, mudar o objeto com que o metodo ta trabalhando e pronto. Sempre copie os metodos que sincroniza mas despesas, pois sao os que fazem
 * mais verificaçoes de segurança por conta das despesas dependerem das categorias etc... estes metodos vao garantir que o novo objeto seja sincronizado com sucesso
 *
 * */
public class SincAdapter {

    @NonNull
    private Callback callback;

    private ArrayList<Sincronizavel> localData;
    private ArrayList<Sincronizavel> nuvemData;
    public static final long validade = 7776000000L; /* 90 dias*/

    public SincAdapter(@NotNull Callback callback) {

        this.callback = callback;
    }

    public void executar() {
        nuvemData = callback.getDadosNuvem();
        localData = callback.getDadosLocal();
        ordenarListas();
        removerObjetosExpirados();
        sincronizar();
        callback.sincronismoConluido();

    }


    /**
     * Se assegura de que os objetos removidos sejam os primeiros das listas
     */
    private void ordenarListas() {
        Comparator<Sincronizavel> comparador = (o1, o2) -> Boolean.compare(o1.estaRemovido(), o2.estaRemovido());

        Collections.sort(localData, comparador);
        Collections.sort(nuvemData, comparador);
    }

    /**
     * Objetos removidos dos bancos de dados a mais de "X" dias devem ser removidos dos
     * arrays para sincronismo  de forma definitiva antes do começo dod sincronismo
     */
    private void removerObjetosExpirados() {
        /*a ultimaAtt de um objeto é definida usando um horario padrao UTC
        * e deve ser verificada dentro desse fuso-horario casdo contrario
        * haverá erros de calculos diferentes pra cada dispositivo com  fuso-horario diferente
        * (eu pesquisei, é haverá msm no plural) */
        long periodoAtual = Data.timeStampUTC();

        ArrayList<Sincronizavel> tempLocalData = new ArrayList<>(localData);
        ArrayList<Sincronizavel> tempNuvemData = new ArrayList<>(nuvemData);

        localData.clear();
        nuvemData.clear();

        for (Sincronizavel obj : tempNuvemData) {
            // se o obj n foi removido, ou se foi, porem ainda esta dentro da data de validade, ele é mantido no array
            if (!obj.estaRemovido() || ((periodoAtual - obj.getUltimaAtt()) < validade))
                nuvemData.add(obj);
            else callback.removerDefinitivamenteNuvem(obj);

        }
        for (Sincronizavel obj : tempLocalData) {

            // se o obj n foi removido, ou se foi, porem ainda esta dentro da data de validade, ele é mantido no array
            if (!obj.estaRemovido() || ((periodoAtual - obj.getUltimaAtt()) < validade))
                localData.add(obj);
            else callback.removerDefinitivamenteLocal(obj);

        }

    }

    private void sincronizar() {

        for (Sincronizavel nuvemObj : nuvemData) {
            Sincronizavel copiaLocal = getCopiaLocal(nuvemObj);

            if (copiaLocal != null) {
                if (nuvemObj.getUltimaAtt() > copiaLocal.getUltimaAtt())
                    callback.atualizarObjetoLocal(nuvemObj);
                /* nesse modelo de sincronismo, se um objeto de um db nao existe no outro db é pq nunca existiu ja que
                objetos removidos permancem no db ate que expirem e os que ja expiraram  sao removidos dos arrays de sincronismo
                no começo ad operaçao acabando com a possibilidade de inserir um objeto em um db do qual ele foi removido. */
            } else callback.addNovoObjetoLocal(nuvemObj);

        }

        for (Sincronizavel localObj : localData) {
            Sincronizavel copiaNuvem = getCopiaNuvem(localObj);
            if (copiaNuvem != null) {
                if (localObj.getUltimaAtt() > copiaNuvem.getUltimaAtt())
                    callback.atualizarObjetoNuvem(localObj);
                /* nesse modelo de sincronismo, se um objeto de um db nao existe no outro db é pq nunca existiu ja que
                objetos removidos permancem no db ate que expirem e os que ja expiraram  sao removidos dos arrays de sincronismo
                no começo ad operaçao acabando com a possibilidade de inserir um objeto em um db do qual ele foi removido. */
            } else callback.addNovoObjetoNuvem(localObj);

        }

    }


    /**
     * @param alvo alvo
     * @return X
     * <p>
     * Itera sobre o array de dados local para encontrar a copia correspondente
     * do objeto recebido caso haja uma.
     */
    @Nullable
    private Sincronizavel getCopiaLocal(Sincronizavel alvo) {
        for (Sincronizavel sincronizavel : localData)
            if (alvo.getId() == sincronizavel.getId()) return sincronizavel;
        return null;
    }

    /**
     * @param alvo alvo
     * @return X
     * <p>
     * Itera sobre o array de dados da nuvem para encontrar a copia correspondente
     * do objeto recebido caso haja uma.
     */
    @Nullable
    private Sincronizavel getCopiaNuvem(Sincronizavel alvo) {
        for (Sincronizavel sincronizavel : nuvemData)
            if (alvo.getId() == sincronizavel.getId()) return sincronizavel;
        return null;
    }


    public interface Callback {

        /**
         * Deve incluir todos os objetos do tipo a ser sincronizado mesmo os removidos
         *
         * @return x
         */
        @NonNull
        ArrayList<Sincronizavel> getDadosLocal();

        /**
         * Deve incluir todos os objetos do tipo a ser sincronizado mesmo os removidos
         *
         * @return x
         */
        @NonNull
        ArrayList<Sincronizavel> getDadosNuvem();

        /**
         * @param obj obj
         *            <p>
         *            Os objetos passados por esse metodo devem ser removidos definitivamente do banco de dados
         *            pois foram removidos e ja passaram da data de validade no armazenamento
         */
        void removerDefinitivamenteLocal(Sincronizavel obj);

        /**
         * @param obj obj
         *            <p>
         *            Os objetos passados por esse metodo devem ser removidos definitivamente do banco de dados
         *            pois foram removidos e ja passaram da data de validade no armazenamento
         */
        void removerDefinitivamenteNuvem(Sincronizavel obj);

        /**
         * @param nuvemObj o
         *                 <p>
         *                 Serve para atualizar os objetos locais usando os objetos da nuvem
         *                 que sao mais recentes
         */
        void atualizarObjetoLocal(Sincronizavel nuvemObj);

        /**
         * @param localObj o
         *                 <p>
         *                 Serve para atualizar os objetos da nuvem usando os objetos locais
         *                 que sao mais recentes
         */
        void atualizarObjetoNuvem(Sincronizavel localObj);

        void addNovoObjetoLocal(Sincronizavel nuvemObj);

        void addNovoObjetoNuvem(Sincronizavel localObj);

        /**
         * <p>
         * Quando esse metodo for chamado significa que todas as operaçoes de atualizaçao
         * foram executadas, e só. cabe a classe que implementa esta interface verificar
         * se todas as operaçoes feitas em nuvem ja estao conluidas e se tiveram exito,
         * antes finalizar o sincronismo.
         */
        void sincronismoConluido();
    }

    /**
     * Use nas classes que usam {@link SincAdapter} para atualziar a interface
     */
    public interface UICallback {

        /**
         * @param sucesso s
         * @param msg     m
         *                <p>
         *                chamado ao final da tarefa ou durante caso haja algum erro no meio do caminho
         */
        void feito(boolean sucesso, String msg);

        /**
         * @param titulo a
         * @param msg    a
         *               <p>
         *               Chamado a  cada operação para informar o usuario do progresso da tarefa
         */
        void status(String titulo, String msg);

    }
}
