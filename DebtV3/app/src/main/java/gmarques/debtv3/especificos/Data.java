package gmarques.debtv3.especificos;

import android.util.Log;

import com.pixplicity.easyprefs.library.Prefs;

import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

import java.util.List;

import gmarques.debtv3.interface_.UIUtils;
import gmarques.debtv3.modelos.Despesa;
import gmarques.debtv3.modelos.Receita;
import gmarques.debtv3.outros.MyRealm;
import gmarques.debtv3.outros.Tag;
import gmarques.debtv3.utilitarios.C;
import io.realm.Realm;
import io.realm.RealmResults;

/**
 * https://www.joda.org/joda-time/timezones.html
 * final DateTimeZone fromTimeZone = DateTimeZone.forID("America/Sao_Paulo");
 * final DateTime dateTime = new DateTime("2019-01-03T01:25:00", fromTimeZone);
 */
public class Data {
    /**
     * Após alguma quebra de cabeça notei que diferentes fuso-horarios geram diferentes timeStamps mesmo
     * usando System.currentTimeMillis(); e que com isso os horarios de ultima atualização dos objetos
     * acabava embaralhado, impossibilitando o sincronismo e comprometendo a criaçao de IDs para os objetos uma
     * vez que essa funçao conta diretamente com o timeStamp gerado pelo System.currentTimeMillis(); . Então decidi
     * criar esse metodo para obter o horario base em que todos os fuso-horarios se baseiam, para criar um padrao, dessa forma
     * 2 dispositivos com  fuso-horarios diferentes e vinculados a mesma conta nao terão problema ao sincronizar seus dados
     **/
    public static long timeStampUTC() {
        return DateTimeZone.getDefault().convertLocalToUTC(new LocalDateTime().toDate().getTime(), false);
    }


    /**
     * Este metodo foi inserido na versao 11/1.1 no dia 06/09/2020
     * remova-o alguns meses depois pra evitar excesso de codigo desnecessario
     */
    public static void corretorDeCagada() {

        UIUtils.infoToasty("Corrigindo datas");

        new Thread(() -> {
            Realm realm = Realm.getDefaultInstance();

            RealmResults<Receita> realmReceitas = realm.where(Receita.class).findAll();
            List<Receita> receitas = realm.copyFromRealm(realmReceitas);


            RealmResults<Despesa> realmDespesas = realm.where(Despesa.class).findAll();
            List<Despesa> despesas = realm.copyFromRealm(realmDespesas);

            realm.close();


            for (Receita receita : receitas) {
                receita.setDataDeRecebimento(new LocalDate(receita.getDataDeRecebimento()));
                receita.setDataEmQueFoiRecebida(new LocalDate(receita.getDataEmQueFoiRecebida()));
                receita.setAutoImportarPrimeira(new LocalDate(receita.getAutoImportarPrimeira()));
                receita.setAutoImportarUltima(new LocalDate(receita.getAutoImportarUltima()));
                Log.d(Tag.AppTag, "Data.corretorDeCagada: " + receita.getNome() + " " + receita.getDataDeRecebimento() + " corrigida");
                MyRealm.insertOrUpdate(receita);
            }

            for (Despesa despesa : despesas) {
                despesa.setDataDePagamento(new LocalDate(despesa.getDataDePagamento()));
                despesa.setDataEmQueFoiPaga(new LocalDate(despesa.getDataEmQueFoiPaga()));
                despesa.setParcelas(new LocalDate(despesa.getParcelasLong()[0]), new LocalDate(despesa.getParcelasLong()[1]));
                Log.d(Tag.AppTag, "Data.corretorDeCagada: " + despesa.getNome() + " " + despesa.getDataDePagamento() + " corrigida");
                MyRealm.insertOrUpdate(despesa);
            }

            Prefs.putBoolean(C.dataCorrigida, true);
        }).start();
    }


}
