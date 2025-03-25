package gmarques.debtv3.activities.add_edit_despesas;

import static gmarques.debtv3.gestores.Meses.mesAtual;

import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.CalendarView;

import androidx.appcompat.app.ActionBar;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.joda.time.LocalDate;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import gmarques.debtv3.R;
import gmarques.debtv3.activities.MyActivity;
import gmarques.debtv3.databinding.ActivityAddEditDespesasBinding;
import gmarques.debtv3.especificos.CalculadorMonetario;
import gmarques.debtv3.gestores.Categorias;
import gmarques.debtv3.gestores.Despesas;
import gmarques.debtv3.gestores.Meses;
import gmarques.debtv3.interface_.AnimatedClickListener;
import gmarques.debtv3.interface_.FormatUtils;
import gmarques.debtv3.interface_.UIUtils;
import gmarques.debtv3.modelos.Categoria;
import gmarques.debtv3.modelos.Despesa;
import gmarques.debtv3.novo_bsheet.Sheettalogo;
import gmarques.debtv3.outros.Broadcaster;
import gmarques.debtv3.outros.Tag;
import gmarques.debtv3.outros.UIRunnable;
import io.realm.Case;
import io.realm.Realm;
import io.realm.RealmResults;

public class AddEditDespesas extends MyActivity {
    private ActivityAddEditDespesasBinding ui;
    private boolean editando;
    private boolean deveSugerir;

    private Despesa despesa;
    private Despesa despesaCopia;
    private Despesa despesaSugerida;
    private String parcelas;
    private CategoriasAdapter categoriasAdapter;
    private Categoria categoriaSelecionada;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ui = DataBindingUtil.setContentView(this, R.layout.activity_add_edit_despesas);

        inicializarObjetos();

        setSupportActionBar(ui.toolbar);
        ActionBar actBar = getSupportActionBar();
        assert actBar != null;
        actBar.setTitle(editando ? getString(R.string.Editandodespesa) : getString(R.string.Novadespesa));
        actBar.setSubtitle(mesAtual.getNome());


        new Handler().postDelayed(() -> {

            inicializarBotoes();
            inicializarCampoNome();
            inicializarCampoObservaçoes();
            inicializarCampoValor();
            inicializarCampoData();
            inicializarCampoDataEmQueFoiPaga();
            inicializarCampoParcelas();
            inicializarCategorias();

            if (editando) atualizarUI();
        }, tempoDeEspera);
    }


    private void inicializarCampoObservaçoes() {
        ui.edtObs.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {

                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            ui.scrollView.smoothScrollTo(0, ui.scrollView.getHeight());

                        }
                    }, 800);

                }
            }
        });
    }

    private void inicializarCategorias() {

        categoriasAdapter = new CategoriasAdapter(Categorias.getCategorias(), new CategoriasAdapter.Callback() {

            @Override
            public void categoriaSelecionada(Categoria categoria) {
                categoriaSelecionada = categoria;
                if (ui.edtParcelas.isFocused()) ui.edtParcelas.clearFocus();
            }
        });
        ui.rvCat.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));
        ui.rvCat.setAdapter(categoriasAdapter);
    }

    private void inicializarObjetos() {


        long id = getIntent().getLongExtra("id", 0);

        despesa = mesAtual.getDespesa(id);
        if (despesa != null) {
            editando = true;
            despesaCopia = Despesas.clonar(despesa);
            /*oculto da tela partes da interface que o usuario nao pode interagir quando esta editando um objeto*/
            ui.btnRecorrente.setVisibility(View.GONE);
            ui.edtParcelas.setVisibility(View.GONE);
        } else despesa = new Despesa();
    }

    private void inicializarCampoParcelas() {
        if (editando) ui.edtParcelas.setEnabled(false);

        ui.edtParcelas.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String raw = ui.edtParcelas.getText().toString();
                if (raw.length() == 2)
                    raw = "0" + raw.substring(0, 1) + "0" + raw.substring(1, 2);
                if (raw.length() == 3) raw = "0" + raw;
                if (raw.length() == 4) {
                    parcelas = raw + "";/*nao quero fazer atribuiçao a raw, apenas copiar o valor*/
                    int atual = Integer.parseInt(raw.substring(0, 2));
                    int total = Integer.parseInt(raw.substring(2, 4));
                    if (atual >= total) ui.edtParcelas.setText("");

                    LocalDate ultimaParcela = new LocalDate(dataSelecionada).plusMonths(total - atual).withDayOfMonth(1);
                    LocalDate primeiraParcela = new LocalDate(dataSelecionada).withDayOfMonth(1);
                    if (atual > 1) primeiraParcela = primeiraParcela.minusMonths(atual - 1);
                    ui.edtParcelas.setText(FormatUtils.formatarDataEmPeriodo(primeiraParcela, ultimaParcela));

                } else {
                    ui.edtParcelas.setText("");
                    parcelas = "";
                }

                /*desativo o botao de recorrente caso estaja habilitado pois despesas com parcelas sao diferentes de despesas recorrentes*/
                if (ui.edtParcelas.getText().toString().length() > 0 && ui.btnRecorrente.isActivated())
                    ui.btnRecorrente.setActivated(false);
            } else
                ui.edtParcelas.setText(parcelas);
        });
    }

    private long dataSelecionada;

    private void inicializarCampoData() {
        ui.edtPgto.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                UIUtils.esconderTeclado();
                ui.edtPgto.clearFocus();
                /*--------------------------------------------------------------------------*/
                CalendarView calendarView = new CalendarView(AddEditDespesas.this);
                final Sheettalogo dialog = new Sheettalogo(AddEditDespesas.this);
                dialog.contentView(calendarView);
                final LocalDate dataMes = new LocalDate(mesAtual.getAno(), mesAtual.getMes(), 1);
                final LocalDate hoje = new LocalDate();


                if (despesa.getDataDePagamento() > 0)
                    calendarView.setDate(despesa.getDataDePagamento());
                else if (dataSelecionada > 0) calendarView.setDate(dataSelecionada);
                else calendarView.setDate(hoje.toDate().getTime());

                calendarView.setMinDate(dataMes.toDate().getTime());
                calendarView.setMaxDate(dataMes.plusMonths(1).minusDays(1).toDate().getTime());
                calendarView.setOnDateChangeListener((calendarView1, i, i1, i2) -> {
                    dataSelecionada = new LocalDate(i, i1 + 1, i2).toDate().getTime();
                    ui.edtPgto.setText(FormatUtils.formatarData(new LocalDate(i, i1 + 1, i2), true));
                    dialog.dismiss();
                });

                dialog.onDismissListener(dialog1 -> ui.edtPgto.clearFocus()).show();
                /*--------------------------------------------------------------------------*/
            }
        });
    }

    private long dataEmQueFoiPagaSelecionada;

    private void inicializarCampoDataEmQueFoiPaga() {
        ui.edtDataEmQueFoiPago.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                UIUtils.esconderTeclado();
                ui.edtDataEmQueFoiPago.clearFocus();
                /*--------------------------------------------------------------------------*/
                CalendarView calendarView = new CalendarView(AddEditDespesas.this);
                final Sheettalogo dialog = new Sheettalogo(AddEditDespesas.this);
                dialog.contentView(calendarView);
                dialog.titulo(getString(R.string.Atualizardataemqueadespesafoipaga));
                dialog.mensagem(getString(R.string.Asmodificacoesfeitasaquisaorefletidas));
                dialog.icone(R.drawable.vec_info);
                final LocalDate dataMes = new LocalDate(mesAtual.getAno(), mesAtual.getMes(), 1);
                final LocalDate hoje = new LocalDate();


                if (despesa.getDataEmQueFoiPaga() > 0)
                    calendarView.setDate(despesa.getDataEmQueFoiPaga());
                else if (dataEmQueFoiPagaSelecionada > 0)
                    calendarView.setDate(dataEmQueFoiPagaSelecionada);
                else calendarView.setDate(hoje.toDate().getTime());

                calendarView.setMinDate(dataMes.toDate().getTime());
                calendarView.setMaxDate(dataMes.plusMonths(1).minusDays(1).toDate().getTime());
                calendarView.setOnDateChangeListener((calendarView1, i, i1, i2) -> {
                    dataEmQueFoiPagaSelecionada = new LocalDate(i, i1 + 1, i2).toDate().getTime();
                    ui.edtDataEmQueFoiPago.setText(FormatUtils.formatarData(new LocalDate(i, i1 + 1, i2), true));
                    dialog.dismiss();

                });

                dialog.onDismissListener(dialog1 -> ui.edtDataEmQueFoiPago.clearFocus()).show();
                /*--------------------------------------------------------------------------*/
            }
        });
    }

    private void inicializarCampoValor() {

        new CalculadorMonetario(ui.edtValor, (valor, valorFormatado, editText) -> ui.edtValor.setText(valorFormatado));

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
                RealmResults<Despesa> rSemelhantes = realm.where(Despesa.class).beginsWith("nome", s.toString(), Case.INSENSITIVE).and().equalTo("removido", false).limit(5).findAll();
                List<Despesa> semelhantes = realm.copyFromRealm(rSemelhantes);

                Collections.sort(semelhantes, new Comparator<Despesa>() {
                    @Override
                    public int compare(Despesa o1, Despesa o2) {
                        return o1.getNome().length() - o2.getNome().length();
                    }
                });
                despesaSugerida = semelhantes.size() > 0 ? semelhantes.get(0) : null;
                semelhantes.clear();
                if (despesaSugerida != null) {
                    ui.edtNomeSugestao.setHint(despesaSugerida.getNome());
                    ui.ivImportar.setVisibility(View.VISIBLE);
                }

                realm.close();
            }
        });
        ui.ivImportar.setOnClickListener(v -> {
            deveSugerir = false;
            ui.edtNomeSugestao.setHint("");
            ui.ivImportar.setVisibility(View.GONE);
            despesa = Despesas.clonarComAlteraçaoDeId(despesaSugerida);
            despesa.setPaga(false);
            Despesas.mudarDataDeAcordoComMes(mesAtual.getMes(), mesAtual.getAno(), despesa);
            despesaSugerida = null;
            atualizarUI();
        });

        ui.edtNome.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                ui.edtNomeSugestao.setHint("");
                ui.ivImportar.setVisibility(View.GONE);
                deveSugerir = true;

                new Thread(new UIRunnable() {
                    Despesa duplicata = null;

                    @Override
                    public void workerThread() {
                        for (Despesa despesa : mesAtual.getDespesas()) {
                            if (despesa.getNome().equals(ui.edtNome.getText().toString())) {
                                duplicata = despesa;
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

    private void inicializarBotoes() {
        ui.fabConcluir.setOnClickListener(new AnimatedClickListener() {
            @Override
            public void onClick(View view) {
                super.onClick(view);

                concluir();
            }
        });

        ui.btnPago.setOnClickListener(new AnimatedClickListener() {
            @Override
            public void onClick(View view) {
                super.onClick(view);
                ui.btnPago.setActivated(!ui.btnPago.isActivated());

                ui.edtDataEmQueFoiPago.setVisibility(ui.btnPago.isActivated() ? View.VISIBLE : View.GONE);

                if (ui.btnPago.isActivated()) {
                    LocalDate localDate = new LocalDate();
                    dataEmQueFoiPagaSelecionada = localDate.toDate().getTime();
                    ui.edtDataEmQueFoiPago.setText(FormatUtils.formatarData(dataEmQueFoiPagaSelecionada, true));
                } else dataEmQueFoiPagaSelecionada = 0;
            }
        });

        if (editando) ui.btnRecorrente.setEnabled(false);
        else ui.btnRecorrente.setOnClickListener(new AnimatedClickListener() {
            @Override
            public void onClick(View view) {
                super.onClick(view);
                ui.btnRecorrente.setActivated(!ui.btnRecorrente.isActivated());
                if (ui.btnRecorrente.isActivated()) {
                    ui.edtParcelas.setText("");
                }
            }
        });
    }

    private void atualizarUI() {
        ui.edtNome.setText(despesa.getNome());
        ui.edtValor.setText(FormatUtils.emReal(despesa.getValor()));
        ui.edtPgto.setText(FormatUtils.formatarData(despesa.getDataDePagamento(), true));
        ui.edtDataEmQueFoiPago.setText(FormatUtils.formatarData(despesa.getDataEmQueFoiPaga(), true));
        /*necessario pra quando for verificar a data ou coletar pelo datapicker*/
        dataSelecionada = despesa.getDataDePagamento();
        dataEmQueFoiPagaSelecionada = despesa.getDataEmQueFoiPaga();

        int[] parcelas = despesa.getParcelas();
        if (parcelas != null) {
            ui.edtParcelas.setText(MessageFormat.format("{0}/{1}X", parcelas[0], parcelas[1]));
            ui.btnRecorrente.setActivated(false);
        }

        ui.edtObs.setText(despesa.getObservacoes());
        ui.btnPago.setActivated(despesa.estaPaga());
        ui.edtDataEmQueFoiPago.setVisibility(ui.btnPago.isActivated() ? View.VISIBLE : View.GONE);

        int pos = categoriasAdapter.setSeleçaoEChamarCallback(despesa.getCategoriaId());
        if (pos > 0) {
            ui.rvCat.getLayoutManager().smoothScrollToPosition(ui.rvCat, null, pos);
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
                    if (checarEAplicarDataPgto()) {
                        if (checarEAplicarParcelas()) {
                            if (checarEAplicarCategoria()) {
                                aplicarDadosNaoObrigatorios();

                                if (editando) {

                                    mesAtual.attDespesa(despesa);

                                    if (despesaRecorrenteParceladaOuComCopias())
                                        perguntarOndeAplicarAlteracoes();
                                    else finish();

                                } else {

                                    mesAtual.addDespesa(despesa);

                                    if (despesa.getParcelas() != null) {
                                        long id = Despesas.addCopiaParcelada(despesa);
                                        Meses.importarDespesaParcelada(id, despesa, mesAtual.getMes(), mesAtual.getAno());
                                    } else if (ui.btnRecorrente.isActivated()) {
                                        Log.d(Tag.AppTag, "AddEditDespesas.concluir: ----------");
                                        Despesas.addCopiaRecorrente(despesa);
                                        Meses.importarDespesaRecorrente(despesa, mesAtual.getMes(), mesAtual.getAno());
                                    }
                                    finish();
                                }

                                UIUtils.vibrar(1);
                            } else
                                UIUtils.erroToasty(getString(R.string.Voceprecisaselecionarumacategoriaparaadespesa));
                        } else UIUtils.erroNoFormulario(ui.edtParcelas);
                    } else UIUtils.erroNoFormulario(ui.edtPgto);
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

        despesa.setObservacoes(ui.edtObs.getText().toString());

        /*Deve ser setado antes do setDataEmQueFoiPago pq ao chamar setPaga
         * o metodo setDataEmQueFoiPago é automaticamente chamado para mater
         * a informação atualizada. Mas se usuario definir uma data diferente, esta deve persistir
         * por isso a seto depois de ter chamado setPaga*/
        despesa.setPaga(ui.btnPago.isActivated());

        despesa.setDataEmQueFoiPaga(new LocalDate(dataEmQueFoiPagaSelecionada));


    }

    private boolean checarObjetoRepetido() {
        //retorno true so o objeto nao for repetido
        if (editando && despesaCopia.getNome().equals(ui.edtNome.getText().toString())) return true;
        Realm realm = Realm.getDefaultInstance();
        boolean repetido = realm.where(Despesa.class).equalTo("nome", ui.edtNome.getText().toString(), Case.INSENSITIVE).and().equalTo("removido", false).and().equalTo("mesId", mesAtual.getId()).findFirst() != null;
        realm.close();
        return !repetido;
    }

    private boolean checarEAplicarCategoria() {
        if (categoriaSelecionada != null) {
            despesa.setCategoriaId(categoriaSelecionada.getId());
            return true;
        } else return false;
    }

    private boolean checarEAplicarDataPgto() {
        despesa.setDataDePagamento(new LocalDate(dataSelecionada));
        return dataSelecionada > 0;

    }

    private boolean checarEAplicarValor() {
        String valor = ui.edtValor.getText().toString();
        despesa.setValor(FormatUtils.emDecimal(valor).floatValue());
        if (despesa.getValor() <= 0) despesa.setValor(1);
        return valor.length() > 0;
    }

    private boolean checarEAplicarNome() {
        String nome = ui.edtNome.getText().toString();
        despesa.setNome(nome);
        return nome.length() > 0;


    }

    private boolean checarEAplicarParcelas() {
        if (editando) return true;
        if (parcelas == null || parcelas.length() == 0) {
            despesa.setParcelas(null, null);
            return true;
        }

        String raw = parcelas.replaceAll("[^0-9]", "");
        if (raw.length() != 4) return false;

        int atual = Integer.parseInt(raw.substring(0, 2));
        int total = Integer.parseInt(raw.substring(2, 4));
        if (atual >= total) return false;

        LocalDate ultimaParcela = new LocalDate(despesa.getDataDePagamento()).plusMonths(total - atual).withDayOfMonth(1);
        LocalDate primeiraParcela = new LocalDate(despesa.getDataDePagamento()).withDayOfMonth(1);
        if (atual > 1) primeiraParcela = primeiraParcela.minusMonths(atual - 1);

        despesa.setParcelas(primeiraParcela, ultimaParcela);

        Log.d(Tag.AppTag, "checarEAplicarParcelas: atual " + atual + " total " + total + " primeiraParcela " + primeiraParcela.toString() + " ultimaParcela " + ultimaParcela);

        return true;
    }

    private void perguntarOndeAplicarAlteracoes() {
        final Sheettalogo dialog = new Sheettalogo(this);
        dialog.titulo(getString(R.string.Despesarecorrenteparcelada))
                .mensagem(getString(R.string.Ondedesejaaplicarasmodificacoes))
                .botaoPositivo(mesAtual.getNome(), v -> {
                    finish();
                })
                .botaoNegativo(MessageFormat.format(getString(R.string.Xemdiante), mesAtual.getNome()), v -> {
                    Despesas.atualizarRecorrenteOuParcelada(despesa, despesaCopia);
                    Meses.atualizarCopias(despesa, despesaCopia, mesAtual.getMes(), mesAtual.getAno());
                    finish();
                })
                .naoCancelavel()
                .show();
        ;

    }

    /**
     * retorna true  se houverem copias dessa despesa nos meses seguintes a este ou
     * se houver uma despesa com mesId =  {@link Despesa.PARCELADA} ou {@link Despesa.RECORRENTE}
     * caso contrario retorna false
     */
    @SuppressWarnings("JavadocReference")
    private boolean despesaRecorrenteParceladaOuComCopias() {
        Realm realm = Realm.getDefaultInstance();

        String nomeParaBusca = ui.edtNome.getText().toString();
        if (editando && !despesaCopia.getNome().equals(nomeParaBusca))
            nomeParaBusca = despesaCopia.getNome(); /* se durante a ediçao de uma despesa o usuario trocar o nome, devo fazer a verificaçao abaixo usando o nome
             original para poder perguntar se ele quer atualizar o nome das das copias desta despesa caso ela seja recorrente/parcelada*/

        Despesa copiaRecorrente = realm.where(Despesa.class)
                .equalTo("nome", nomeParaBusca)
                .and()
                .equalTo("removido", false)
                .and()
                .equalTo("mesId", Despesa.RECORRENTE).findFirst();


        if (copiaRecorrente != null) {
            realm.close();
            return true;
        }

        Despesa copiaParcelada = realm.where(Despesa.class)
                .equalTo("nome", nomeParaBusca)
                .and()
                .equalTo("removido", false)
                .and()
                .equalTo("mesId", Despesa.PARCELADA).findFirst();


        if (copiaParcelada != null) {
            realm.close();
            return true;
        }

        RealmResults<Despesa> copiasNosMesesSeguintes = realm.where(Despesa.class)
                .equalTo("nome", nomeParaBusca)
                .and()
                .equalTo("removido", false)
                .and()
                .not()
                .equalTo("mesId", despesa.getMesId())
                .and()
                /*a data de pgto deve ser maior nem que seja por um dia para aparecer na busca*/
                .greaterThan("dataDePagamento", despesa.getDataDePagamento() + (24 * 60 * 60 * 1000))
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
