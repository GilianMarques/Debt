package gmarques.debtv3.activities.ver_receitas;

import android.app.Activity;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.google.android.flexbox.AlignSelf;
import com.google.android.flexbox.FlexboxLayoutManager;

import org.joda.time.LocalDate;

import java.util.List;

import gmarques.debtv3.R;
import gmarques.debtv3.gestores.Receitas;
import gmarques.debtv3.modelos.Receita;
import gmarques.debtv3.interface_.AnimatedClickListener;
import gmarques.debtv3.interface_.FormatUtils;
import gmarques.debtv3.interface_.UIUtils;
import io.realm.RealmList;


/**
 * Criado por Gilian Marques
 * Sábado, 20 de Julho de 2019  as 16:51:59.
 */
public class ReceitasAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<Receita> receitas;
    private final Callback callback;
    private final Activity activity;

    ReceitasAdapter(List<Receita> receitas, Activity activity, Callback callback) {
        this.receitas = receitas;
        this.activity = activity;
        this.callback = callback;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = activity.getLayoutInflater().inflate(R.layout.layout_receita_rv, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

        YoYo.with(Techniques.FadeOut).duration(1).playOn(holder.itemView);

        final MyViewHolder mHolder = (MyViewHolder) holder;
        final Receita receita = receitas.get(position);

        mHolder.tvDate.setText(FormatUtils.formatarData(new LocalDate(receita.getDataDeRecebimento()), false));
        mHolder.tvValue.setText(FormatUtils.emReal(receita.getValor()));
        mHolder.tvName.setText(receita.getNome());
        mHolder.tvIcon.setText(receita.getNome().substring(0, 1).toUpperCase());
        mHolder.ivRecurrent.setVisibility(View.INVISIBLE);


        mHolder.tvIcon.setTextColor(Color.WHITE);
        UIUtils.aplicarTema(mHolder.tvIcon.getBackground(), UIUtils.corAttr(android.R.attr.colorPrimary));

        if (receita.estaRecebido()) {
            mHolder.tvRecebido.setVisibility(View.VISIBLE);
            UIUtils.aplicarTema(mHolder.tvRecebido.getBackground(), UIUtils.corAttr(android.R.attr.colorPrimary));

        } else {

            mHolder.tvRecebido.setVisibility(View.INVISIBLE);
        }


        if (Receitas.temCopiaRecorrenteOuAutoImportada(receita))
            mHolder.ivRecurrent.setVisibility(View.VISIBLE);


        YoYo.with(Techniques.FadeIn).duration(200).playOn(mHolder.itemView);

        ViewGroup.LayoutParams lp = mHolder.parent.getLayoutParams();

        if (lp instanceof FlexboxLayoutManager.LayoutParams) {
            FlexboxLayoutManager.LayoutParams flexboxLp = (FlexboxLayoutManager.LayoutParams) lp;
            flexboxLp.setFlexGrow(1.0f);
            flexboxLp.setAlignSelf(AlignSelf.AUTO);
        }


    }


    @Override
    public int getItemCount() {
        return receitas.size();
    }

    public void atualizarItem(Receita receita, int adapterPosition) {
        receitas.set(adapterPosition, receita);
        notifyItemChanged(adapterPosition);
    }

    public void atualizar(RealmList<Receita> receitas) {
        this.receitas.clear();
        this.receitas.addAll(receitas);
        notifyDataSetChanged();
    }


    // stores and recycles views as they are scrolled off screen
    public class MyViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvName, tvIcon, tvValue, tvDate, tvRecebido;
        public View parent;
        private ImageView ivRecurrent;

        MyViewHolder(final View itemView) {
            super(itemView);
            tvRecebido = itemView.findViewById(R.id.tvRecebido);
            tvValue = itemView.findViewById(R.id.tvValue);
            tvDate = itemView.findViewById(R.id.tvDate);
            ivRecurrent = itemView.findViewById(R.id.ivRecurrent);
            tvName = itemView.findViewById(R.id.tvName);
            tvIcon = itemView.findViewById(R.id.tvIcon);
            parent = itemView.findViewById(R.id.parent);
            itemView.setOnClickListener(new AnimatedClickListener() {
                @Override
                public void onClick(View view) {
                    super.onClick(view);

                    callback.onClick(getAdapterPosition(), receitas.get(getAdapterPosition()), itemView);
                }
            });
        }


    }

    public void removerItem(int pos) {
        receitas.remove(pos);
        notifyItemRemoved(pos);
    }

    public interface Callback {


        void onClick(int adapterPosition, Receita receita, View itemView);
    }
}
