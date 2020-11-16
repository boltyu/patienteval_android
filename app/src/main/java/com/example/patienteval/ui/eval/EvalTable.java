package com.example.patienteval.ui.eval;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.content.Context;
import android.os.Bundle;
import android.os.ConditionVariable;
import android.os.Message;
import android.util.AttributeSet;
import android.util.JsonReader;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.example.patienteval.R;
import com.example.patienteval.tools.HttpsConnection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class EvalTable extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_eval_table);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.evaltable, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final Context fContext = this;
        int tableid = 0;
        if(item.getItemId() == R.id.evaltable_new_id1){
            tableid = 1;
        }else if(item.getItemId() == R.id.evaltable_new_id2){
            tableid = 2;
        }else if(item.getItemId() == R.id.evaltable_new_id3){
            tableid = 3;
        }else if(item.getItemId() == R.id.evaltable_new_id4){
            tableid = 4;
        }else if(item.getItemId() == R.id.evaltable_new_id5){
            tableid = 5;
        }
        if(tableid > 0){
            setTitle(item.toString());
            RenderPage(tableid);
        }
        return true;
    }

    private final String key_haveson = "haveSon", key_description = "description", key_subitems = "znode";
    private void RenderPage(int tableid){
        final Context pContext = this;
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpsConnection httpsConnection = new HttpsConnection(pContext);
                JSONArray titleArray = httpsConnection.getTable(tableid);
                LinearLayout topLayer = findViewById(R.id.table_top_layer);

                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT);
                if(titleArray != null)
                topLayer.post(new Runnable() {
                    @Override
                    public void run() {
                        topLayer.removeAllViews();
                        CardView cardView = new CardView(pContext);
                        for(int titleIndex = 0; titleIndex < titleArray.length(); titleIndex ++) {
                            try {
                                JSONObject topItem = titleArray.getJSONObject(titleIndex);    // top title
                                TextView titleView = new TextView(pContext);
                                titleView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                                titleView.setTextSize(30);
                                titleView.setText(topItem.getString(key_description));
                                layoutParams.bottomMargin = 5;
                                topLayer.addView((View)titleView,layoutParams);
                                //layoutParams.gravity = Gravity.LEFT;
                                if(topItem.optInt(key_haveson) > 0){
                                    JSONArray questionArray = topItem.getJSONArray(key_subitems);
                                    for(int questionIndex = 0; questionIndex < questionArray.length(); questionIndex ++){
                                        JSONObject questionItem = questionArray.getJSONObject(questionIndex);   // question title
                                        TextView questionView = new TextView(pContext);
                                        questionView.setTextSize(20);
                                        questionView.setText(questionItem.optString(key_description));
                                        topLayer.addView(questionView,layoutParams);
                                        JSONArray optionArray = questionItem.optJSONArray(key_subitems);
                                        RadioGroup optionGroup = new RadioGroup(pContext);
                                        for(int optionIndex = 0; optionIndex < optionArray.length(); optionIndex ++){
                                            JSONObject optionItem = optionArray.getJSONObject(optionIndex);
                                            RadioButton optionButton = new RadioButton(pContext);
                                            optionButton.setText(optionItem.optString(key_description));
                                            optionButton.setTextSize(15);
                                            optionGroup.addView(optionButton,optionIndex);
                                        }
                                        topLayer.addView(optionGroup,layoutParams);
                                    }

                                }

                                topLayer.invalidate();

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                        }


                    }
                });
            }
        }).start();
    }
}