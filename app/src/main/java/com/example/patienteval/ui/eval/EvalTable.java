package com.example.patienteval.ui.eval;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.ConditionVariable;
import android.os.Looper;
import android.os.Message;
import android.text.Layout;
import android.util.AttributeSet;
import android.util.JsonReader;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
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
    AutoCompleteTextView edit_patientnum = null;
    AutoCompleteTextView edit_patientname = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_eval_table);
        edit_patientnum = (AutoCompleteTextView) findViewById(R.id.edittext_patientnum);
        edit_patientname = (AutoCompleteTextView) findViewById(R.id.edittext_patientname);
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpsConnection httpsConnection = new HttpsConnection(pCongext);
                JSONArray patientArray = httpsConnection.getPatientList();
                if(patientArray != null){
                    ArrayAdapter<String> patientname_hints_adapter = new ArrayAdapter<String>(pCongext,android.R.layout.simple_spinner_dropdown_item);
                    ArrayAdapter<String> patientnum_hints_adapter = new ArrayAdapter<String>(pCongext,android.R.layout.simple_spinner_dropdown_item);
                    for(int patient_index = 0;patient_index < patientArray.length();patient_index++){
                        try{
                            JSONObject patientjson = patientArray.getJSONObject(patient_index);
                            patientnum_hints_adapter.insert(patientjson.optString("patientNum"),patient_index);
                            patientname_hints_adapter.insert(patientjson.optString("patientName"),patient_index);
                        }catch (JSONException e){
                            e.printStackTrace();
                        }
                    }
                    edit_patientnum.post(new Runnable() {
                        @Override
                        public void run() {
                            edit_patientname.setAdapter(patientname_hints_adapter);
                            edit_patientnum.setAdapter(patientnum_hints_adapter);
                            edit_patientnum.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                @Override
                                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                                    edit_patientname.setText(patientname_hints_adapter.getItem(i));
                                }
                            });
                            edit_patientname.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                @Override
                                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                                    edit_patientnum.setText(patientnum_hints_adapter.getItem(i));

                                }
                            });
                        }
                    });
               }
            }
        }).start();
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
        MenuInflater inflater = getMenuInflater(); // 前者在thread里，此处并不知菜单是否已经完善
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
    private final int QUESTION_ID_BIAS = 928;
    private int question_count = 0;
    private int[] question_type = new int[128];
    private int current_tableid = 0;
    private View RenderSubview(JSONObject currentItem, Context pContext, int textSize){
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.bottomMargin = 5;
        if(textSize < 20)textSize = 20;// 标题字号逐级递减5，最小不小于20，选项字号使用15
        try {
            if (currentItem != null && currentItem.optInt(key_haveson) > 0) {  // subtitle
                LinearLayout currentLayout = new LinearLayout(pContext);
                currentLayout.setOrientation(LinearLayout.VERTICAL);
                JSONArray itemArray = currentItem.optJSONArray(key_subitems);
                for (int itemIndex = 0; itemIndex < itemArray.length(); itemIndex++) {
                    JSONObject subItem = itemArray.getJSONObject(itemIndex);
                    View subView = RenderSubview(subItem, pContext, textSize-5);
                    TextView descriptionView = new TextView(pContext);
                    descriptionView.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
                    descriptionView.setTextSize(textSize);
                    descriptionView.setText(subItem.optString(key_description));
                    CardView cardView = new CardView(pContext);
                    LinearLayout linearLayout = new LinearLayout(pContext);
                    linearLayout.setOrientation(LinearLayout.VERTICAL);
                    linearLayout.addView(descriptionView, layoutParams);
                    linearLayout.addView(subView, layoutParams);
                    cardView.addView(linearLayout,layoutParams);
                    cardView.setRadius(30);
                    cardView.setPadding(5,5,5,5);
                    cardView.setContentPadding(5,5,5,5);
                    // border
                    cardView.setCardElevation(2);
                    cardView.setUseCompatPadding(true);
                    currentLayout.addView(cardView,layoutParams);
                }
                return currentLayout;
            } else {
                LinearLayout questionLayout = new LinearLayout(pContext);
                questionLayout.setOrientation(LinearLayout.VERTICAL);
                //topLayer.addView(questionView,layoutParams);
                JSONArray optionArray = currentItem.optJSONArray(key_subitems);
                RadioGroup optionGroup = new RadioGroup(pContext);
                int optionIndex = 0;
                for (; optionIndex < optionArray.length(); optionIndex++) {
                    JSONObject optionItem = optionArray.getJSONObject(optionIndex);
                    RadioButton optionButton = new RadioButton(pContext);
                    optionButton.setId(QUESTION_ID_BIAS + question_count*10 + optionIndex + 1);
                    optionButton.setText(optionItem.optString(key_description));
                    optionButton.setTextSize(15);
                    optionGroup.addView(optionButton, optionIndex);
                }
                if(optionIndex > 0){
                    optionGroup.setId(QUESTION_ID_BIAS + question_count*10); // 此处
                    questionLayout.addView(optionGroup, layoutParams);
                    question_type[question_count] = 0;
                }else { // 如果没有选项，则以编辑框或其他组件替代
                    EditText editText = new EditText(pContext);
                    editText.setId(QUESTION_ID_BIAS + question_count * 10);
                    questionLayout.addView(editText,layoutParams);
                    question_type[question_count] = 1;
                }
                question_count ++;
                return questionLayout;
            }
        } catch (JSONException e){
            e.printStackTrace();
        }
        return null;
    }


    private void RenderPage(int tableid){
        final Context pContext = this;
        current_tableid = tableid;
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
                        topLayer.removeAllViews();  //  remove == deleter ?
                        question_count = 0;
                        for(int titleIndex = 0; titleIndex < titleArray.length(); titleIndex ++) {
                            try {
                                JSONObject topItem = titleArray.getJSONObject(titleIndex);    // top title
                                TextView titleView = new TextView(pContext);
                                titleView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                                titleView.setTextSize(30);
                                layoutParams.bottomMargin = 10;
                                titleView.setText(topItem.optString(key_description));
                                topLayer.setPadding(8,8,8,8);
                                topLayer.addView(titleView,layoutParams);
                                if(topItem != null && topItem.optInt(key_haveson) > 0) {
                                    View layoutView = RenderSubview(topItem, pContext, 30-5);
                                    if(layoutView != null)
                                        topLayer.addView(layoutView,layoutParams);
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                        Log.d("question count",question_count + "");
                        Button commitButton = new Button(pContext);
                        commitButton.setText("提交");
                        commitButton.setTextSize(15);
                        commitButton.setTextColor(getResources().getColor(R.color.white,getTheme()));
                        commitButton.setBackgroundColor(getResources().getColor(R.color.design_default_color_primary,getTheme()));
                        commitButton.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                        commitButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                JSONObject json_payload = new JSONObject();
                                JSONObject json_paylaod_scoringcol = new JSONObject();
                                try{
                                    json_payload.put("ScoringTableId",current_tableid);

                                    json_paylaod_scoringcol.put("patientName",edit_patientname.getText());
                                    json_paylaod_scoringcol.put("patientNum",edit_patientnum.getText());
                                    RadioButton btn_bfsurgery = findViewById(R.id.radiobtn_bfsurgery);
                                    if(btn_bfsurgery.isChecked())
                                        json_paylaod_scoringcol.put("surgeryStatus","1");//str/int
                                    else
                                        json_paylaod_scoringcol.put("surgeryStatus","0");
                                    LinearLayout topLayer = findViewById(R.id.table_top_layer);
                                    for(int question_index = 0; question_index < question_count; question_index ++){
                                        switch (question_type[question_index]){
                                            case 0:// type option
                                                RadioGroup optionGroup = topLayer.findViewById(QUESTION_ID_BIAS + question_index * 10);
                                                int scoreVal = optionGroup.getCheckedRadioButtonId();
                                                if(scoreVal >= 0){
                                                    scoreVal = (scoreVal - QUESTION_ID_BIAS) % 10 - 1;
                                                }
                                                json_paylaod_scoringcol.put("c"+(question_index+1),scoreVal);
                                                break;
                                            case 1:// type text
                                                EditText editText = topLayer.findViewById(QUESTION_ID_BIAS + question_index * 10);
                                                String textVal = editText.getText().toString();
                                                json_paylaod_scoringcol.put("c"+(question_index+1),textVal);
                                                break;
                                        }
                                    }
                                    json_payload.putOpt("ScoringCol",json_paylaod_scoringcol);
                                    Log.d("jsonpayload",json_payload.toString());
                                }
                                catch (JSONException e){
                                    e.printStackTrace();
                                }
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        HttpsConnection postM = new HttpsConnection(pContext);
                                        final String resultString = postM.postTable(json_payload);
                                        Looper.prepare();
                                        Toast.makeText(pContext,resultString,Toast.LENGTH_LONG).show();
                                        Looper.loop();
                                    }
                                }).start();

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