package gmarques.debtv3.interface_;

import android.view.View;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;

/**
 * Criado por Gilian Marques
 * Sábado, 20 de Julho de 2019  as 18:08:03.
 */
public abstract class AnimatedClickListener implements View.OnClickListener {
    @Override public void onClick(View view) {
        Animation anim = new ScaleAnimation(
                1f, 1.018f,
                1f, 1.018f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        anim.setFillAfter(true); // Needed to keep the result of the animation
        anim.setDuration(150);
        anim.setRepeatCount(1);
        anim.setRepeatMode(Animation.REVERSE);
        view.startAnimation(anim);

    }
}
