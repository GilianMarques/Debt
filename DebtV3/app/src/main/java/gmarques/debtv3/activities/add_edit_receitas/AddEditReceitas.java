package gmarques.debtv3.activities.add_edit_receitas;

import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.CalendarView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.databinding.DataBindingUtil;

import org.joda.time.LocalDate;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import gmarques.debtv3.R;
import gmarques.debtv3.activities.MyActivity;
import gmarques.debtv3.databinding.ActivityAddEditReceitasBinding;
import gmarques.debtv3.especificos.CalculadorMonetario;
import gmarques.debtv3.gestores.Meses;
import gmarques.debtv3.gestores.Receitas;
import gmarques.debtv3.interface_.AnimatedClickListener;
import gmarques.debtv3.interface_.FormatUtils;
import gmarques.debtv3.interface_.UIUtils;
import gmarques.debtv3.modelos.Receita;
import gmarques.debtv3.novo_bsheet.Sheettalogo;
import gmarques.debtv3.outros.Broadcaster;
import gmarques.debtv3.outros.Tag;
import gmarques.debtv3.outros.UIRunnable;
import io.realm.Case;
import io.realm.Realm;
import io.realm.RealmResults;

import static gmarques.debtv3.gestores.Meses.mesAtual;


@SuppressWarnings("JavadocReference")
public class AddEditReceitas extends MyActivity {
    private ActivityAddEditReceitasBinding ui;

    private Receita receita;
    private Receita receitaCopia;
    private Receita receitaSugerida;

    private boolean editando;
    private boolean deveSugerir;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ui = DataBindingUtil.setContentView(this, R.layout.activity_add_edit_receitas);

        inicializarObjetos();

        setSupportActionBar(ui.toolbar);
        ActionBar actBar = getSupportActionBar();
        assert actBar != null;
        actBar.setTitle(editando ? getString(R.string.Editandoreceita) : getString(R.string.Novareceita));
        actBar.setSubtitle(mesAtual.getNome());


        new Handler().postDelayed(() -> {

            inicializarBotoes();
            inicializarCampoNome();
            inicializarCampoObservaçoes();
            inicializarCampoValor();
            inicializarCampoData();
            inicializarCampoDataEmQueFoiRecebida();
            inicializarImportracao();
            if (editando) atualizarUI();

        }, tempoDeEspera);
    }

    private void inicializarObjetos() {

        long id = getIntent().getLongExtra("id", 0);
        receita = mesAtual.getReceita(id);
        if (receita != null) {
            editando = true;
            receitaCopia = Receitas.clonar(receita);
        } else receita = new Receita();

    }

    private void inicializarBotoes() {
        ui.btnRecebido.setOnClickListener(new AnimatedClickListener() {
            @Override
            public void onClick(View view) {
                super.onClick(view);

                ui.btnRecebido.setActivated(!ui.btnRecebido.isActivated());
                ui.edtDataEmQueFoiRecebida.setVisibility(ui.btnRecebido.isActivated() ? View.VISIBLE : View.GONE);

                if (ui.btnRecebido.isActivated()) {
                    LocalDate localDate = new LocalDate();
                    dataEmQueFoiRecebidaSelecionada = localDate.toDate().getTime();
                    ui.edtDataEmQueFoiRecebida.setText(FormatUtils.formatarData(dataEmQueFoiRecebidaSelecionada, true));
                } else dataEmQueFoiRecebidaSelecionada = 0;

            }
        });

        ui.btnRecorrente.setOnClickListener(new AnimatedClickListener() {
            @Override
            public void onClick(View view) {
                super.onClick(view);

                ui.btnRecorrente.setActivated(!ui.btnRecorrente.isActivated());
                if (ui.btnRecorrente.isActivated()) {
                    ui.edtDataImport.setText("");
                    dataAutoImportarSelecionada = 0;
                }

            }
        });

        ui.fabConcluir.setOnClickListener(new AnimatedClickListener() {
            @Override
            public void onClick(View view) {
                super.onClick(view);

                concluir();
            }
        });

    }

    private long dataAutoImportarSelecionada;

    private void inicializarImportracao() {
        ui.edtDataImport.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    UIUtils.esconderTeclado();
                    ui.edtDataImport.clearFocus();
                    /*--------------------------------------------------------------------------*/
                    final Sheettalogo dialog = new Sheettalogo(AddEditReceitas.this);
                    CalendarView calendarView = new CalendarView(AddEditReceitas.this);

                    final LocalDate dataMes = new LocalDate(mesAtual.getAno(), mesAtual.getMes(), 1);
                    final LocalDate hoje = new LocalDate();


                    if (receita.getDataDeRecebimento() > 0)
                        calendarView.setDate(receita.getDataDeRecebimento());
                    else if (dataAutoImportarSelecionada > 0)
                        calendarView.setDate(dataAutoImportarSelecionada);
                    else calendarView.setDate(hoje.toDate().getTime());

                    calendarView.setMinDate(dataMes.toDate().getTime());
                    calendarView.setMaxDate(dataMes.plusYears(3).plusMonths(1).minusDays(1).toDate().getTime());
                    calendarView.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
                        @Override
                        public void onSelectedDayChange(@NonNull CalendarView calendarView, int i, int i1, int i2) {
                            dataAutoImportarSelecionada = new LocalDate(i, i1 + 1, i2).toDate().getTime();
                            ui.edtDataImport.setText(FormatUtils.formatarDataMesEAno(new LocalDate(i, i1 + 1, i2).toDate().getTime()));
                            ui.btnRecorrente.setActivated(false);
                            dialog.dismiss();
                        }
                    });


                    dialog.contentView(calendarView)
                            .onDismissListener(dialogInterface -> ui.edtDataImport.clearFocus())

                            .show();
                    /*--------------------------------------------------------------------------*/
                }
            }
        });
    }

    private long dataSelecionada;

    private void inicializarCampoData() {
        ui.edtDataReceb.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    UIUtils.esconderTeclado();
                    ui.edtDataReceb.clearFocus();
                    /*--------------------------------------------------------------------------*/
                    final Sheettalogo dialog = new Sheettalogo(AddEditReceitas.this);
                    CalendarView calendarView = new CalendarView(AddEditReceitas.this);

                    final LocalDate dataMes = new LocalDate(mesAtual.getAno(), mesAtual.getMes(), 1);
                    final LocalDate hoje = new LocalDate();


                    if (receita.getDataDeRecebimento() > 0)
                        calendarView.setDate(receita.getDataDeRecebimento());
                    else if (dataSelecionada > 0) calendarView.setDate(dataSelecionada);
                    else calendarView.setDate(hoje.toDate().getTime());

                    calendarView.setMinDate(dataMes.toDate().getTime());
                    calendarView.setMaxDate(dataMes.plusMonths(1).minusDays(1).toDate().getTime());
                    calendarView.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
                        @Override
                        public void onSelectedDayChange(@NonNull CalendarView calendarView, int i, int i1, int i2) {
                            dataSelecionada = new LocalDate(i, i1 + 1, i2).toDate().getTime();
                            ui.edtDataReceb.setText(FormatUtils.formatarData(new LocalDate(i, i1 + 1, i2), true));
                            dialog.dismiss();
                        }
                    });


                    dialog.contentView(calendarView)
                            .onDismissListener(dialogInterface -> ui.edtDataReceb.clearFocus())

                            .show();
                    /*--------------------------------------------------------------------------*/
                }
            }
        });
    }

    private long dataEmQueFoiRecebidaSelecionada;

    private void inicializarCampoDataEmQueFoiRecebida() {
        ui.edtDataEmQueFoiRecebida.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                UIUtils.esconderTeclado();
                ui.edtDataEmQueFoiRecebida.clearFocus();
                /*--------------------------------------------------------------------------*/
                final Sheettalogo dialog = new Sheettalogo(AddEditReceitas.this);
                dialog.titulo(getString(R.string.Atualizardataemqueareceitafoirecebida));
                dialog.mensagem(getString(R.string.Asmodificacoesfeitasaquisaorefletidas));
                dialog.icone(R.drawable.vec_info);
                CalendarView calendarView = new CalendarView(AddEditReceitas.this);

                final LocalDate dataMes = new LocalDate(mesAtual.getAno(), mesAtual.getMes(), 1);
                final LocalDate hoje = new LocalDate();


                if (receita.getDataDeRecebimento() > 0)
                    calendarView.setDate(receita.getDataDeRecebimento());
                else if (dataEmQueFoiRecebidaSelecionada > 0)
                    calendarView.setDate(dataEmQueFoiRecebidaSelecionada);
                else calendarView.setDate(hoje.toDate().getTime());

                calendarView.setMinDate(dataMes.toDate().getTime());
                calendarView.setMaxDate(dataMes.plusMonths(1).minusDays(1).toDate().getTime());
                calendarView.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
                    @Override
                    public void onSelectedDayChange(@NonNull CalendarView calendarView, int i, int i1, int i2) {
                        dataEmQueFoiRecebidaSelecionada = new LocalDate(i, i1 + 1, i2).toDate().getTime();
                        ui.edtDataEmQueFoiRecebida.setText(FormatUtils.formatarData(new LocalDate(i, i1 + 1, i2), true));
                        dialog.dismiss();
                    }
                });


                dialog.contentView(calendarView)
                        .onDismissListener(dialogInterface -> ui.edtDataEmQueFoiRecebida.clearFocus())

                        .show();
                /*--------------------------------------------------------------------------*/
            }
        });
    }

    private void inicializarCampoValor() {
        new CalculadorMonetario(ui.edtValor, (valor, valorFormatado, editText) -> ui.edtValor.setText(valorFormatado));

    }

    private void inicializarCampoObservaçoes() {
        ui.edtObs.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        ui.sView.smoothScrollTo(0, ui.sView.getHeight());

                    }
                }, 800);

            }
        });
    }

    private void inicializarCampoNome() {
        if (editando) return;
        deveSugerir = true;
        ui.edtNome.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                ui.edtNomeSugestao.setHint("");
                ui.ivImportar.setVisibility(View.GONE);
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {

                if (!deveSugerir || s.length() < 1) return;
                Realm realm = Realm.getDefaultInstance();
                RealmResults<Receita> rSemelhantes = realm.where(Receita.class).beginsWith("nome", s.toString(), Case.INSENSITIVE).and().equalTo("removido", false).limit(5).findAll();
                List<Receita> semelhantes = realm.copyFromRealm(rSemelhantes);

                Collections.sort(semelhantes, new Comparator<Receita>() {
                    @Override
                    public int compare(Receita o1, Receita o2) {
                        return o1.getNome().length() - o2.getNome().length();
                    }
                });
                receitaSugerida = semelhantes.size() > 0 ? semelhantes.get(0) : null;
                semelhantes.clear();
                if (receitaSugerida != null) {
                    ui.edtNomeSugestao.setHint(receitaSugerida.getNome());
                    ui.ivImportar.setVisibility(View.VISIBLE);
                }

                realm.close();
            }
        });
        ui.ivImportar.setOnClickListener(v -> {
            deveSugerir = false;
            ui.edtNomeSugestao.setHint("");
            ui.ivImportar.setVisibility(View.GONE);
            receita = Receitas.clonarComAlteraçaoDeId(receitaSugerida);
            receita.setRecebida(false);
            Receitas.mudarDataDeAcordoComMes(mesAtual.getMes(), mesAtual.getAno(), receita);
            receitaSugerida = null;
            atualizarUI();
        });

        ui.edtNome.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                ui.edtNomeSugestao.setHint("");
                ui.ivImportar.setVisibility(View.GONE);
                deveSugerir = true;
                new Thread(new UIRunnable() {
                    Receita duplicata = null;

                    @Override
                    public void workerThread() {
                        for (Receita receita : mesAtual.getReceitas()) {
                            if (receita.getNome().equals(ui.edtNome.getText().toString())) {
                                duplicata = receita;
                                break;
                            }
                        }
                    }

                    @Override
                    public void uiThread() {
                        if (duplicata != null)
                            UIUtils.avisoToasty(MessageFormat.format(getString(R.string.Xjaestaregistradaemy), duplicata.getNome(), mesAtual.getNome()));

                    }
                }).start();

            }
        });
    }

    private void atualizarUI() {
        ui.edtNome.setText(receita.getNome());
        ui.edtValor.setText(FormatUtils.emReal(receita.getValor()));
        ui.edtDataReceb.setText(FormatUtils.formatarData(receita.getDataDeRecebimento(), true));
        ui.edtDataEmQueFoiRecebida.setText(FormatUtils.formatarData(receita.getDataEmQueFoiRecebida(), true));
        /*necessario pra quando for verificar a data ou coletar pelo datapicker*/
        dataSelecionada = receita.getDataDeRecebimento();
        dataEmQueFoiRecebidaSelecionada = receita.getDataEmQueFoiRecebida();
        dataAutoImportarSelecionada = receita.getAutoImportarPrimeira();

        ui.edtObs.setText(receita.getObservacoes());
        ui.btnRecebido.setActivated(receita.estaRecebido());
        ui.edtDataEmQueFoiRecebida.setVisibility(ui.btnRecebido.isActivated() ? View.VISIBLE : View.GONE);

        if (editando) {

            /*oculto da tela partes da interface que o usuario nao pode interagir quando esta editando um objeto*/
            ui.autoImportarContainer.setVisibility(View.GONE);

        }
    }

    private void concluir() {
        /*limpo o foco de todas as views*/
        ui.edtObs.requestFocus();
        ui.edtObs.clearFocus();
        /*começo a verificação*/

        if (checarObjetoRepetido()) {
            if (checarEAplicarNome()) {
                if (checarEAplicarValor()) {
                    if (checarEAplicarDataRecebimento()) {
                        if (checarEAplicarAutoImportacao()) {
                            aplicarDadosNaoObrigatorios();
                            if (editando) {
                                mesAtual.attReceita(receita);

                                if (receitaRecorrenteParceladaOuComCopias())
                                    perguntarOndeAplicarAlteracoes();
                                else finish();

                            } else {

                                mesAtual.addReceita(receita);

                                /*checarEAplicarAutoImportacao()*/
                                if (receita.getAutoImportarPrimeira() > 0) {

                                    long id = Receitas.addCopiaAutoImportada(receita);
                                    Meses.importarReceitaParcelada(id, receita, mesAtual.getMes(), mesAtual.getAno());

                                } else if (ui.btnRecorrente.isActivated()) {

                                    Receitas.addCopiaRecorrente(receita);
                                    Meses.importarReceitaRecorrente(receita, mesAtual.getMes(), mesAtual.getAno());
                                }

                                finish();
                            }

                            UIUtils.vibrar(1);


                        } else UIUtils.erroNoFormulario(ui.edtDataImport);
                    } else UIUtils.erroNoFormulario(ui.edtDataReceb);
                } else UIUtils.erroNoFormulario(ui.edtValor);
            } else {
                UIUtils.erroNoFormulario(ui.edtNome);
                UIUtils.erroNoFormulario(ui.edtNomeSugestao);
            }
        } else {
            UIUtils.erroToasty(MessageFormat.format(getString(R.string.Xjaestaregistradaemy), ui.edtNome.getText().toString(), mesAtual.getNome()));
        }

    }

    private void aplicarDadosNaoObrigatorios() {
        receita.setObservacoes(ui.edtObs.getText().toString());

        /*Deve ser setado antes do setDataEmQueFoiRecebida pq ao chamar setRecebida
         * o metodo setDataEmQueFoiRecebida é automaticamente chamado para mater
         * a informação atualizada. Mas se usuario definir uma data diferente, esta deve persistir
         * por isso a seto depois de ter chamado setRecebida*/
        receita.setRecebida(ui.btnRecebido.isActivated());

        receita.setDataEmQueFoiRecebida(new LocalDate(dataEmQueFoiRecebidaSelecionada));
    }

    private boolean checarObjetoRepetido() {
        //retorno true so o objeto nao for repetido
        if (editando && receitaCopia.getNome().equals(ui.edtNome.getText().toString())) return true;
        Realm realm = Realm.getDefaultInstance();
        boolean repetido = realm.where(Receita.class).equalTo("nome", ui.edtNome.getText().toString(), Case.INSENSITIVE).and().equalTo("removido", false).and().equalTo("mesId", mesAtual.getId()).findFirst() != null;
        realm.close();
        return !repetido;
    }

    private boolean checarEAplicarDataRecebimento() {
        receita.setDataDeRecebimento(new LocalDate(dataSelecionada));
        return dataSelecionada > 0;

    }

    private boolean checarEAplicarValor() {
        String valor = ui.edtValor.getText().toString();
        receita.setValor(FormatUtils.emDecimal(valor).floatValue());
        if (receita.getValor() <= 0) receita.setValor(1);
        return valor.length() > 0;
    }

    private boolean checarEAplicarNome() {
        String nome = ui.edtNome.getText().toString();
        receita.setNome(nome);
        return nome.length() > 0;


    }

    private boolean checarEAplicarAutoImportacao() {

        if (editando) return true;
        if (dataAutoImportarSelecionada > 0 && ui.edtDataImport.getText().length() > 0) {

            if (dataSelecionada == 0)
                throw new RuntimeException("É NECESSARIO QUE USUARIO TENHA SELECIONADO UMA DATA DE PAGAMENTO PARA FUNCIONAR >" + dataSelecionada + "<");

            LocalDate ultima = new LocalDate(dataAutoImportarSelecionada).withDayOfMonth(1);
            receita.setAutoImportarPrimeira(new LocalDate(dataSelecionada));
            receita.setAutoImportarUltima(ultima);

            Log.d(Tag.AppTag, "checarEAplicarAutoImportacao: " + ultima);
        } else {
            Log.d(Tag.AppTag, "AddEditReceitas.checarEAplicarAutoImportacao: limpando auto importaçao de " + receita.getNome());
            receita.setAutoImportarPrimeira(null);
            receita.setAutoImportarUltima(null);
        }
        return true;
    }

    private void perguntarOndeAplicarAlteracoes() {
        final Sheettalogo dialog = new Sheettalogo(this);
        dialog.titulo(getString(R.string.Receitarecorrenteparcelada))
                .mensagem(getString(R.string.Ondedesejaaplicarasmodificacoes))
                .botaoPositivo(mesAtual.getNome(), v -> finish())
                .botaoNegativo(MessageFormat.format(getString(R.string.Xemdiante), mesAtual.getNome()), v -> {
                    Receitas.atualizarRecorrenteOuParcelada(receita, receitaCopia);
                    Meses.atualizarCopias(receita, receitaCopia, mesAtual.getMes(), mesAtual.getAno());
                    dialog.dismiss();
                    finish();
                })
                .naoCancelavel()
                .show();
        ;

    }

    /**
     * retorna true  se houverem copias dessa receita nos meses seguintes a este ou
     * se houver uma receita com mesId =  {@link Receita.AUTOIMPORTADA} ou {@link Receita.RECORRENTE}
     * caso contrario retorna false
     */
    private boolean receitaRecorrenteParceladaOuComCopias() {

        Realm realm = Realm.getDefaultInstance();


        String nomeParaBusca = ui.edtNome.getText().toString();
        if (editando && !receitaCopia.getNome().equals(nomeParaBusca))
            nomeParaBusca = receitaCopia.getNome(); /* se durante a ediçao de uma receita o usuario trocar o nome, devo fazer a verificaçao abaixo usando o nome
             original para poder perguntar se ele quer atualizar o nome das das copias desta receita caso ela seja recorrente/parcelada*/


        Receita copiaRecorrente = realm.where(Receita.class)
                .equalTo("nome", nomeParaBusca)
                .and()
                .equalTo("removido", false)
                .and()
                .equalTo("mesId", Receita.RECORRENTE).findFirst();


        if (copiaRecorrente != null) {
            realm.close();
            return true;
        }

        Receita copiaParcelada = realm.where(Receita.class)
                .equalTo("nome", nomeParaBusca)
                .and()
                .equalTo("removido", false)
                .and()
                .equalTo("mesId", Receita.AUTOIMPORTADA).findFirst();


        if (copiaParcelada != null) {
            realm.close();
            return true;
        }

        RealmResults<Receita> copiasNosMesesSeguintes = realm.where(Receita.class)
                .equalTo("nome", nomeParaBusca)
                .and()
                .equalTo("removido", false)
                .and()
                .not()
                .equalTo("mesId", receita.getMesId())
                .and()
                /*a data de pgto deve ser maior nem que seja por um dia para aparecer na busca*/
                .greaterThan("dataDeRecebimento", receita.getDataDeRecebimento() + (24 * 60 * 60 * 1000))
                .limit(1)
                .findAll();

        if (copiasNosMesesSeguintes.size() > 0) {
            realm.close();
            return true;
        }
        return false;
    }

    @Override
    protected void onPause() {
        /*garante que em caso de adicçao ou atualizaçao a maiactivity sera atualizada*/
        Broadcaster.atualizarMainActivity();
        super.onPause();
    }
}
