package gmarques.debtv3.nuvem;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;

import gmarques.debtv3.callbacks.EncerrarSincronismoCallback;
import gmarques.debtv3.especificos.Usuario;
import gmarques.debtv3.modelos.nuvem.ContaSincronizavel;
import gmarques.debtv3.utilitarios.C;


/**
 * A ideia dessa classe é não popular a {@link FirebaseImpl} com varios metodos e ao mesmo tempo nao criar metodos gigantescos e bagunçados
 **/
public class SincronismoDeContas {
    private DocumentReference raiz;


    public SincronismoDeContas() {
        /* nao inicializo aqui pq tenho que verificar se o app esta em testes ou nao pra escolher qual coleçao do firestore usar
         * aí teria que duplicar o cadigo e eu to evitando isso*/
        raiz = new FirebaseImpl().getDocumentousuario();
    }

    /**
     * Envia o email do usuario como parametro para o destinatario
     **/
    @SuppressWarnings("ConstantConditions")
    public void enviarSolicitaçaoDeSincronismo(String email, final FirebaseImpl.CallbackDeStatus callbackDeStatus) {


        HashMap<String, String> solicitaçao = new HashMap<>();
        solicitaçao.put(FBaseNomes.campoEmail, Usuario.getUsuario().getEmail());

        raiz.getParent()
                .document(email)
                .collection(FBaseNomes.solicitaçoesDeSincronismo)
                .document(Usuario.getUsuario().getEmail())
                .set(solicitaçao)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        callbackDeStatus.feito(true, null);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        callbackDeStatus.feito(false, e.getMessage() + C.eCod2);
                    }
                });
    }

    public void getSolicitaçoesDeSincronismoPendentes(final CallbackDeSolicitaçoes solicitaçoesCallbak) {

        final ArrayList<ContaSincronizavel> contas = new ArrayList<>();
        final int[] operaçoesDeCarga = {0};

        raiz.collection(FBaseNomes.solicitaçoesDeSincronismo).get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {


                        if (queryDocumentSnapshots.size() == 0) {
                            solicitaçoesCallbak.feito(null, contas);
                            return;
                        }

                        for (DocumentSnapshot snapshot : queryDocumentSnapshots.getDocuments()) {
                            ContaSincronizavel conta = new ContaSincronizavel(snapshot.getString(FBaseNomes.campoNome), snapshot.getString(FBaseNomes.campoEmail), snapshot.getString(FBaseNomes.campoFoto));

                            contas.add(conta);
                            operaçoesDeCarga[0]++;

                            carregarDadosPublicosDaConta(conta, new FirebaseImpl.CallbackDeStatus() {
                                @Override
                                public void feito(boolean sucesso, String msg) {
                                    /*não verifico se a solicitação de leitura de dados públicos foi realizada com sucesso por que em algum momento do login usuário pode não conseguir enviar seus dados para nuvem
                                     * se isso acontecer, este método não vai conseguir ler os dados públicos deste usuário em questão e o usuário que estiver tentando carregar as contas vai receber um erro de leitura
                                     * cuja única maneira de resolver é reiniciando o aplicativo do usuario que nao conseguiu enviar seus dados públicos para nuvem, com uma conexão com a internet.
                                     * para evitar esse transtorno, decidi que as contas devem ser exibidas mesmo que não seja possível obter suas informações públicas (nome e foto) principalmente pq o mais
                                     * importante é o email e este dado ja esta carregado. */
                                    operaçoesDeCarga[0]--;
                                    if (operaçoesDeCarga[0] == 0)
                                        solicitaçoesCallbak.feito(null, contas);
                                }
                            });
                        }

                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                solicitaçoesCallbak.feito(e.getMessage() + C.eCod3, null);
            }
        });


    }

    public void removerSolicitaçaoDeSincronismo(String emailAlvo, final FirebaseImpl.CallbackDeStatus status) {
        raiz.collection(FBaseNomes.solicitaçoesDeSincronismo).document(emailAlvo).delete().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                status.feito(true, null);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                status.feito(false, e.getMessage() + C.eCod4);

            }
        });
    }

    /**
     * esse método escreve no campo "contasQueSincronizamComigo" do usuário que enviou a
     * solicitação o próprio e-mail, então vai no campo "contaComQuemSincronizo" e salva o
     * e-mail do usuário que enviou o convite ao fim de tudo chama o método para remover a solicitação
     */
    public void aceitarSolicitaçaoDeSincronismo(final String emailDoUSuarioQueConvidou, final FirebaseImpl.CallbackDeStatus callbackDeStatus) {

        String emailDoUsuarioLocal = Usuario.getUsuario().getEmail();
        final HashMap<String, String> notificaçao = new HashMap<>();
        notificaçao.put(FBaseNomes.campoEmail, emailDoUsuarioLocal);

        /*primeiro notifico o usuario que enviou a solicitação de que sincronizo com ele*/
        //noinspection ConstantConditions
        raiz.getParent()
                .document(emailDoUSuarioQueConvidou)
                .collection(FBaseNomes.contasQueSincronizamComigo)
                .document(emailDoUsuarioLocal)
                .set(notificaçao)
                .addOnSuccessListener(aVoid -> {
                    /*Só então atualizo o meu documento dizendo pra sincronizar com ele*/
                    final HashMap<String, Object> solicitaçaoAceita = new HashMap<>();
                    solicitaçaoAceita.put(FBaseNomes.campoEmail, emailDoUSuarioQueConvidou);
                    /*Faço assim pra poupar bandwith, baixando um documento que tem apenas o campo que eu quero posteriormente*/
                    raiz.collection(FBaseNomes.contaComQuemSincronizo)
                            .document(emailDoUSuarioQueConvidou)
                            .set(solicitaçaoAceita)
                            .addOnSuccessListener(aVoid1 -> {
                                //uma vez que a solicitação foi aceita, devo remover ela e só entao retornar com sucesso =true
                                removerSolicitaçaoDeSincronismo(emailDoUSuarioQueConvidou, callbackDeStatus);
                            })
                            .addOnFailureListener(e -> callbackDeStatus.feito(false, e.getMessage() + C.eCod5));
                }).addOnFailureListener(e -> callbackDeStatus.feito(false, e.getMessage() + C.eCod7));


    }

    public void jaEstouSincronizandoComUmaConta(final CallbackDeStrings callback) {
        raiz.collection(FBaseNomes.contaComQuemSincronizo).get().addOnSuccessListener(documentSnapshot -> {

            if (documentSnapshot.size() == 0) {
                callback.feito(null, null);
            } else {
                String email = documentSnapshot.getDocuments().get(0).getString(FBaseNomes.campoEmail);
                callback.feito(email, null);
            }

        }).addOnFailureListener(e -> callback.feito(null, e.getMessage() + C.eCod8));
    }

    /**
     * Verifique com cautela o retorno do callback. A operação pode ser feita com sucesso mas o documento pode nao existir e no campo 'sucesso' de metodo 'feito' do callback
     * o valor será false, causando confusão. PAra ter certeza se a chamada falhou ou não verifique se a String 'msg' é nula.
     **/
    public void existeUsuarioComEsseEmail(final String email, final FirebaseImpl.CallbackDeStatus callbackDeStatus) {

        new FirebaseImpl().getDocumentousuario()
                .getParent()
                .document(email)
                .get()
                .addOnSuccessListener(documentSnapshot -> callbackDeStatus.feito(documentSnapshot.exists(), null))
                .addOnFailureListener(e -> callbackDeStatus.feito(false, e.getMessage() + C.eCod14));

    }

    /**
     * Carrega todas as contas de sincronismo.
     * Após carregar os dados chama o método para baixar os dados públicos de cada conta e retorna com as informações previamente carregadas.
     * caso algum usuário tenha ficado incapacitado de enviar seus dados públicos para nuvem, será retornado uma array de 3 posiçoes contendo a apenas o seu e-mail para exibição
     */
    public void getContasQueSincronizamComEsteEmail(String email, final CallbackDeSolicitaçoes callback) {

        final ArrayList<ContaSincronizavel> contas = new ArrayList<>();
        final int[] operaçoesDeCarga = {0};

        raiz.getParent().document(email).collection(FBaseNomes.contasQueSincronizamComigo).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    if (queryDocumentSnapshots.size() == 0) {
                        callback.feito(null, contas);
                        return;
                    }

                    for (DocumentSnapshot snapshot : queryDocumentSnapshots.getDocuments()) {
                        ContaSincronizavel conta = new ContaSincronizavel(snapshot.getString(FBaseNomes.campoNome),
                                snapshot.getString(FBaseNomes.campoEmail),
                                snapshot.getString(FBaseNomes.campoFoto));

                        contas.add(conta);
                        operaçoesDeCarga[0]++;

                        carregarDadosPublicosDaConta(conta, (sucesso, msg) -> {
                            /*não verifico se a solicitação de leitura de dados públicos foi realizada com sucesso por que em algum momento do login usuário pode não conseguir enviar seus dados para nuvem
                             * se isso acontecer, este método não vai conseguir ler os dados públicos deste usuário em questão e o usuário que estiver tentando carregar as contas vai receber um erro de leitura
                             * cuja única maneira de resolver é reiniciando o aplicativo do usuario que nao conseguiu enviar seus dados públicos para nuvem, com uma conexão com a internet.
                             * para evitar esse transtorno, decidi que as contas devem ser exibidas mesmo que não seja possível obter suas informações públicas (nome e foto) principalmente pq o mais
                             * importante é o email e este dado ja esta carregado. */
                            operaçoesDeCarga[0]--;
                            if (operaçoesDeCarga[0] == 0)
                                callback.feito(null, contas);

                        });

                    }


                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                callback.feito(e.getMessage() + C.eCod9, null);
            }
        });


    }


    /**
     * Carrega  a conta do anfitriao e todas as contas que com ele sincronizam
     * Após carregar os dados chama o método para baixar os dados públicos de cada conta e retorna com as informações previamente carregadas.
     * caso algum usuário tenha ficado incapacitado de enviar seus dados públicos para nuvem, será retornado uma array de 3 posiçoes contendo a apenas o seu e-mail para exibição
     */
    public void getContasComQuemSincronizo(final CallbackDeSolicitaçoes callback) {

        final ArrayList<ContaSincronizavel> contas = new ArrayList<>();
        final int[] operaçoesDeCarga = {0};

        raiz.collection(FBaseNomes.contaComQuemSincronizo).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    if (queryDocumentSnapshots.size() == 0) {
                        callback.feito(null, contas);
                        return;
                    }
                    DocumentSnapshot snapshot = queryDocumentSnapshots.getDocuments().get(0);

                    ContaSincronizavel anfitriao = new ContaSincronizavel(snapshot.getString(FBaseNomes.campoNome), snapshot.getString(FBaseNomes.campoEmail), snapshot.getString(FBaseNomes.campoFoto));
                    contas.add(anfitriao.setAnfitriao());
                    /*Carrego as contas que sincronizam com ele*/
                    getContasQueSincronizamComEsteEmail(anfitriao.email, (msg, hospedes) -> {

                        /*Agora carrego os dados do anfitriao e de todos os seus hospedes*/
                        if (msg != null) {
                            callback.feito(msg, null);
                            return;
                        }

                        contas.addAll(hospedes);

                        for (ContaSincronizavel conta : contas) {
                            operaçoesDeCarga[0]++;

                            carregarDadosPublicosDaConta(conta, new FirebaseImpl.CallbackDeStatus() {
                                @Override
                                public void feito(boolean sucesso, String msg) {
                                    /*não verifico se a solicitação de leitura de dados públicos foi realizada com sucesso por que em algum momento do login usuário pode não conseguir enviar seus dados para nuvem
                                     * se isso acontecer, este método não vai conseguir ler os dados públicos deste usuário em questão e o usuário que estiver tentando carregar as contas vai receber um erro de leitura
                                     * cuja única maneira de resolver é reiniciando o aplicativo do usuario que nao conseguiu enviar seus dados públicos para nuvem, com uma conexão com a internet.
                                     * para evitar esse transtorno, decidi que as contas devem ser exibidas mesmo que não seja possível obter suas informações públicas (nome e foto) principalmente pq o mais
                                     * importante é o email e este dado ja esta carregado. */
                                    operaçoesDeCarga[0]--;
                                    if (operaçoesDeCarga[0] == 0)
                                        callback.feito(null, contas);

                                }
                            });

                        }

                    });


                }).addOnFailureListener(e -> callback.feito(e.getMessage() + C.eCod9, null));


    }


    /**
     * Esse metodo faz uma busca no banco de dados para carregar as informações (nome de exibição, e-mail e foto)  da conta recebida com parâmetro direto dos documentos na nuvem
     * de forma que os dados baixados sejam os mais recentes
     */
    private void carregarDadosPublicosDaConta(final ContaSincronizavel conta, final FirebaseImpl.CallbackDeStatus callbackDeStatus) {

        raiz.getParent().document(conta.email).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                conta.nome = documentSnapshot.getString(FBaseNomes.campoNome);
                conta.foto = documentSnapshot.getString(FBaseNomes.campoFoto);
                callbackDeStatus.feito(true, null);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                callbackDeStatus.feito(false, e.getMessage() + C.eCod10);
            }
        });
    }

    public void usuarioQueMeConvidouJaEstaSincronizandoComOutroUsuario(String email, final FirebaseImpl.CallbackDeStatus callbackDeStatus) {
        raiz.getParent().document(email).collection(FBaseNomes.contaComQuemSincronizo)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        callbackDeStatus.feito(queryDocumentSnapshots.size() > 0, null);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        callbackDeStatus.feito(false, e.getMessage() + C.eCod11);
                    }
                });
    }

    public void encerrarSincronismo(String emailAlvo, EncerrarSincronismoCallback callback) {

        /*primeiro removo o email alvo salvo na  minha conta para interromper o sincronismo mesmo que a operação falhe no meio*/


        raiz.collection(FBaseNomes.contaComQuemSincronizo)
                .document(emailAlvo).delete().addOnSuccessListener(aVoid -> {
            /*Agora removo da conta com quem o usuario local sincroniza, o email dele */
            raiz.getParent()
                    .document(emailAlvo)
                    .collection(FBaseNomes.contasQueSincronizamComigo)
                    .document(Usuario.getEmail())
                    .delete()
                    .addOnSuccessListener(aVoid1 -> {
                        callback.sincronismoEncerrado();

                    }).addOnFailureListener(e ->
                    /*Neste ponto, se o firebase retornar um erro, o usuario com que o usuario local interrompeu o sincronismo  anda vai ver o email do usuario local
                     * como se nada tivesse mudado, porem o usuario local ja vai ter se desconectado.*/
                    callback.sincronismoEncerradoComRessalva(e.getMessage() + C.eCod16));

        }).addOnFailureListener(e -> {
            callback.falhaAoEncerrarSincronismo(e.getMessage() + C.eCod15);
        });
    }

    public interface CallbackDeSolicitaçoes {
        void feito(String msg, ArrayList<ContaSincronizavel> solicitaçoes);
    }

    public interface CallbackDeStrings {
        void feito(String email, String msg);
    }

}
