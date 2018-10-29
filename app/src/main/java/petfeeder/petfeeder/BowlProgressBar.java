package petfeeder.petfeeder;

import android.widget.ImageView;

public class BowlProgressBar {

    private ImageView bowlProgressImage = null;
    private int pr = 0;
    private int[] images  = new int[] { R.drawable.progress_bar_0,
                                        R.drawable.progress_bar_1,
                                        R.drawable.progress_bar_2,
                                        R.drawable.progress_bar_3,
                                        R.drawable.progress_bar_4,
                                        R.drawable.progress_bar_5,
                                        R.drawable.progress_bar_6,
                                        R.drawable.progress_bar_7,
                                        R.drawable.progress_bar_8,
                                        R.drawable.progress_bar_9,
                                        R.drawable.progress_bar_10};

    public void setBowlProgressImage(ImageView progressView){
        bowlProgressImage   = progressView;
    }

    public void setProgress(int pr){
        if (pr > 10 || pr < 0){
            throw new IllegalArgumentException("progress value must be between 0 and 10");
        }
        bowlProgressImage.setImageResource(images[pr]);
    }

    public void increment(){
        pr = (pr+1)%11;
        setProgress(pr);
    }
}
