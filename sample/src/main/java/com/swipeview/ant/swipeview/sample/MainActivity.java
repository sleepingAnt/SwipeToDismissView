package com.swipeview.ant.swipeview.sample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.swipeview.ant.swipeview.R;
import com.swipeview.ant.swipeview.SwipeView;

public class MainActivity extends AppCompatActivity {
    private SwipeView swipeView;
    private Button button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        swipeView = (SwipeView) findViewById(R.id.swipe_view);
        button = (Button) findViewById(R.id.bt_remove_top);
        swipeView.setAnimationDuration(1000);

        for (int i = 0; i < 10; i++) {
            View view = View.inflate(this, R.layout.item_swipe, null);
            final int finalI = i;
            TextView textView = (TextView) view.findViewById(R.id.tv_title);
            textView.setText("Hello Swipe " + (i + 1) + " !");
            swipeView.addItem(view, true, new SwipeView.OnSwipeViewCallback() {
                @Override
                public void onSwiped() {
                    Toast.makeText(getApplicationContext(), "item " + (finalI + 1) + " swiped!", Toast.LENGTH_SHORT).show();
                }
            });
        }

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                swipeView.removeTopItem(true);
            }
        });
    }
}
