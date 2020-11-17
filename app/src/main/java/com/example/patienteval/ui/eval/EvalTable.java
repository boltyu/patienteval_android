package com.example.patienteval.ui.eval;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
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
import android.view.SubMenu;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.patienteval.R;
import com.example.patienteval.tools.HttpsConnection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class EvalTable extends AppCompatActivity {

    final private Context pCongext = this;
    final private int MENU_ID_BIAS = 35;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_eval_table);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpsConnection httpsConnection = new HttpsConnection(pCongext);
                JSONArray tableArray = httpsConnection.getTableList();
                try{
                    MenuItem menu_add = menu.getItem(0);
                    SubMenu menu_add_submenu = menu_add.getSubMenu(); // returns may be null ptr
                    if(menu_add.getTitle().equals("新建评估量表")){
                        for(int tableindex = 0; tableindex < tableArray.length(); tableindex ++){
                            JSONObject tableObject = tableArray.getJSONObject(tableindex);
                            int tableid = tableObject.optInt("id");
                            String tablename = tableObject.optString("scaleName");
                            //public abstract MenuItem add (int groupId,int itemId,int order,CharSequence title)
                            menu_add_submenu.add(Menu.NONE,MENU_ID_BIAS + tableid,tableid,tablename);
                        }
                    }

                }catch(Exception e){
                    e.printStackTrace();
                }

            }
        }).start();
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.evaltable, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final Context fContext = this;
        int tableid = item.getItemId()-MENU_ID_BIAS;
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
                        for(int titleIndex = 0; titleIndex < titleArray.length(); titleIndex ++) {
                            try {
                                JSONObject topItem = titleArray.getJSONObject(titleIndex);    // top title
                                TextView titleView = new TextView(pContext);
                                titleView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                                titleView.setTextSize(30);
                                layoutParams.bottomMargin = 10;
                                titleView.setText(topItem.getString(key_description));
                                topLayer.addView(titleView,layoutParams);
                                //layoutParams.gravity = Gravity.LEFT;
                                if(topItem.optInt(key_haveson) > 0){
                                    JSONArray questionArray = topItem.getJSONArray(key_subitems);
                                    for(int questionIndex = 0; questionIndex < questionArray.length(); questionIndex ++){
                                        JSONObject questionItem = questionArray.getJSONObject(questionIndex);   // question title
                                        TextView questionView = new TextView(pContext);
                                        questionView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                                        questionView.setTextSize(20);
                                        questionView.setText(questionItem.optString(key_description));
                                        CardView cardView = new CardView(pContext);
                                        cardView.setContentPadding(5,5,5,5);
                                        LinearLayout questionLayout = new LinearLayout(pContext);
                                        questionLayout.setOrientation(LinearLayout.VERTICAL);
                                        questionLayout.addView(questionView,layoutParams);
                                        //topLayer.addView(questionView,layoutParams);
                                        JSONArray optionArray = questionItem.optJSONArray(key_subitems);
                                        RadioGroup optionGroup = new RadioGroup(pContext);
                                        for(int optionIndex = 0; optionIndex < optionArray.length(); optionIndex ++){
                                            JSONObject optionItem = optionArray.getJSONObject(optionIndex);
                                            RadioButton optionButton = new RadioButton(pContext);
                                            optionButton.setText(optionItem.optString(key_description));
                                            optionButton.setTextSize(15);
                                            optionGroup.addView(optionButton,optionIndex);
                                        }
                                        questionLayout.addView(optionGroup,layoutParams);
                                        cardView.addView(questionLayout,layoutParams);
                                        cardView.setRadius(30);
                                        topLayer.addView(cardView,layoutParams);
                                    }

                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                        Button commitButton = new Button(pContext);
                        commitButton.setText("提交");
                        commitButton.setTextSize(15);
                        commitButton.setTextColor(getResources().getColor(R.color.white,getTheme()));
                        commitButton.setBackgroundColor(getResources().getColor(R.color.design_default_color_primary,getTheme()));
                        commitButton.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                        commitButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                Toast.makeText(pContext,"已提交",Toast.LENGTH_LONG).show();
                            }
                        });
                        topLayer.addView(commitButton,layoutParams);
                        topLayer.invalidate();

                    }
                });
            }
        }).start();
    }
}