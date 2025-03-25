package gmarques.debtv3;

import android.util.Log;

import org.jetbrains.annotations.NotNull;
import org.joda.time.LocalDate;

import gmarques.debtv3.modelos.ContaBanco;
import gmarques.debtv3.modelos.Objetivo;
import gmarques.debtv3.modelos.Receita;
import gmarques.debtv3.outros.Tag;
import io.realm.DynamicRealm;
import io.realm.FieldAttribute;
import io.realm.RealmMigration;

class MyMigration implements RealmMigration {
    @Override
    public void migrate(@NotNull DynamicRealm realm, long oldVersion, long newVersion) {
        Log.d(Tag.AppTag, "MyMigration.migrate: " + oldVersion + "  " + newVersion);
        if (oldVersion == 1) {
            /*add novo campo em receita*/
            realm.getSchema().get(Receita.class.getSimpleName())
                    .addField("dataEmQueFoiRecebido", long.class);
            /*Defino um valor padrao*/
            realm.getSchema().get(Receita.class.getSimpleName())
                    .transform(obj -> obj.setLong("dataEmQueFoiRecebido", (new LocalDate().toDate().getTime())));
        }

        if (oldVersion == 2) {

            realm.getSchema().create(ContaBanco.class.getSimpleName())
                    .addField("id", long.class, FieldAttribute.PRIMARY_KEY)
                    .addField("ultimaAtt", long.class)
                    .addField("removido", boolean.class)
                    .addField("origem", long.class)
                    .addField("nomeConta", String.class)
                    .addField("valorConta", float.class)
                    .addField("propritario", String.class);

            realm.getSchema().create(Objetivo.class.getSimpleName())
                    .addField("contaId", long.class)
                    .addField("id", long.class, FieldAttribute.PRIMARY_KEY)
                    .addField("ultimaAtt", long.class)
                    .addField("removido", boolean.class)
                    .addField("origem", long.class)
                    .addField("dataDeCriacao", long.class)
                    .addField("dataDeConclusao", long.class)
                    .addField("concluido", boolean.class)
                    .addField("nomeObjetivo", String.class)
                    .addField("valorObjetivo", float.class);

        }


        //noinspection StatementWithEmptyBody
        if (oldVersion == 5) {
            /* qdo precisar atualizar o realm mude a versao dele mpra 6 e ponha o codigo necessario aqui*/
        }
    }
}
