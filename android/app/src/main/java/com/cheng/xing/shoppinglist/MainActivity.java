package com.cheng.xing.shoppinglist;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.Gravity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.cheng.xing.shoppinglist.utils.DropBoxUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private TableLayout resultTable;
    private final String ACCESS_TOKEN = getResources().getString(R.string.access_token);
    private final String FILE_NAME = getResources().getString(R.string.file_name);
    private final String FILE_PATH = getApplicationContext().getFilesDir().getAbsolutePath();
    private DropBoxUtils dropBoxUtils;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.setTitle(getResources().getString(R.string.app_title));
        //Change Policy temporarily for avoiding network call exceptions in the main thread
        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
        dropBoxUtils = new DropBoxUtils(ACCESS_TOKEN, FILE_NAME, FILE_PATH);
        resultTable = (TableLayout) this.findViewById(R.id.resultTable);
        addData();
    }

    private void addData(){
        dropBoxUtils.downloadLatestFile();
        resultTable.setStretchAllColumns(true);
        resultTable.setShrinkAllColumns(true);
        resultTable.removeAllViewsInLayout();
        String[] titles = {"名称", "超市", "备注", "时间", "操作"};
        addLine(titles, true);
        try (FileReader fr = new FileReader(FILE_PATH + "/" + FILE_NAME);
             BufferedReader bf = new BufferedReader(fr)){
            String str;
            while ((str = bf.readLine()) != null) {
                if(!str.equals("")) {
                    addLine(convert(str), false);
                }
            }
        } catch (IOException e) {
            Toast.makeText(MainActivity.this,
                    "读取文件发生异常：" + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    private String[] convert(String s){
        return s.substring(s.indexOf(",") + 1, s.indexOf(";")).split(",");
    }

    private void addLine(String[] elements, boolean title){
        TableRow row = new TableRow(getApplicationContext());
        TableRow.LayoutParams layoutParams =
                new TableRow.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT);
        layoutParams.setMargins(1,1,1,1);
        for(String element: elements){
            TextView tv = new TextView(getApplicationContext());
            tv.setText(element);
            tv.setMaxWidth(200);
            tv.setMinHeight(100);
            tv.setBackgroundColor(Color.WHITE);
            tv.setGravity(Gravity.CENTER);
            row.addView(tv, layoutParams);
        }
        if(!title){
            CheckBox cb = new CheckBox(getApplicationContext());
            cb.setGravity(Gravity.CENTER);
            cb.setBackgroundColor(Color.WHITE);
            row.addView(cb, layoutParams);
        }
        resultTable.addView(row);
    }

    private void addItemOperation(String text){
        dropBoxUtils.downloadLatestFile();
        writeNewItemToLocalFile(text);
        dropBoxUtils.uploadToDropBox();
        refreshGrid();
    }

    private void refreshGrid() {
        dropBoxUtils.downloadLatestFile();
        addData();
    }

    private void writeNewItemToLocalFile(String text){
        File file = new File(FILE_PATH + "/" + FILE_NAME);
        try (FileWriter fw = new FileWriter(file,true);
             BufferedWriter writer = new BufferedWriter(fw)){
            writer.write(text);
            writer.newLine();
        } catch (IOException e) {
            Toast.makeText(MainActivity.this,
                    "写入新商品发生异常：" + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    public void addNewItem(android.view.View view){
        LinearLayout addFrame = (LinearLayout)findViewById(R.id.addFrame);
        addFrame.setVisibility(View.VISIBLE);
    }

    public void saveNewItem(android.view.View view){
        LinearLayout addFrame = (LinearLayout)findViewById(R.id.addFrame);
        EditText nameEdit = findViewById(R.id.name);
        String name = nameEdit.getText().toString();
        Spinner shopEdit = findViewById(R.id.fromSelect);
        String shop = (String)shopEdit.getSelectedItem();
        EditText commentEdit = findViewById(R.id.comment);
        String comment = commentEdit.getText().toString();
        Calendar cal = Calendar.getInstance();
        int day = cal.get(Calendar.DATE);
        int month = cal.get(Calendar.MONTH) + 1;
        if(name.equals("")){
            Toast.makeText(MainActivity.this,
                    "物品名称不能为空",
                    Toast.LENGTH_LONG).show();
        }else if(name.contains(",") || name.contains(";")){
            Toast.makeText(MainActivity.this,
                    "物品中不能包含‘,’和‘;’符号，请重新输入",
                    Toast.LENGTH_LONG).show();
        }else {
            addItemOperation(System.currentTimeMillis() + "," + name + "," + shop + ","
                    + comment + ","+(month < 10? "0" + month: month)+"-"+day+";");
            nameEdit.setText("");
            commentEdit.setText("");
            addFrame.setVisibility(View.GONE);
        }
    }

    public void cancelAllNewItem(android.view.View view){
        LinearLayout addFrame = (LinearLayout)findViewById(R.id.addFrame);
        addFrame.setVisibility(View.GONE);
    }

    public void deleteSelected(android.view.View view){
        String temp = "";
        List<Integer> deleteList = new ArrayList<Integer>();
        for (int i = 1; i < resultTable.getChildCount(); i++) {
            CheckBox cb = (CheckBox)((TableRow)resultTable.getChildAt(i)).getChildAt(4);
            if(cb.isChecked()){
                deleteList.add(i);
                temp += i + ";";
            }
        }

        if(deleteList.size() == 0){
            Toast.makeText(MainActivity.this, "未选中任何商品",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        final List<Integer> deleteListFinal = deleteList;
        AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setTitle("删除确认")
                .setMessage("确定要删除选中的商品么？")
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        deleteAndRefreshGrid(deleteListFinal);
                    }
                })
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Toast.makeText(MainActivity.this, "操作未执行",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .create();
        alertDialog.show();
    }

    private void deleteAndRefreshGrid(List<Integer> deleteListFinal) {
        List<String> idList = getIdForDelete(deleteListFinal);
        dropBoxUtils.downloadLatestFile();
        deleteCorrespondingItems(idList);
        dropBoxUtils.uploadToDropBox();
        refreshGrid();
        Toast.makeText(MainActivity.this, "已删除" + deleteListFinal.size() + "件商品",
                Toast.LENGTH_SHORT).show();
    }

    private void deleteCorrespondingItems(List<String> idList) {
        List<String> itemsLeftLines = new ArrayList<String>();
        try (FileReader fr = new FileReader(FILE_PATH + "/" + FILE_NAME);
             BufferedReader bf = new BufferedReader(fr);){
            String str;
            while ((str = bf.readLine()) != null) {
                if(!str.equals("") && (!idList.contains(str.substring(0, str.indexOf(","))))) {
                    itemsLeftLines.add(str);
                }
            }
        } catch (IOException e) {
            Toast.makeText(MainActivity.this,
                    "删除商品发生异常：" + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }

        File file = new File(FILE_PATH + "/" + FILE_NAME);
        try (FileWriter fw = new FileWriter(file,false);
             BufferedWriter writer = new BufferedWriter(fw)){
            if (!file.exists()) {
                file.createNewFile();
            }
            for(String s: itemsLeftLines) {
                writer.write(s);
                writer.newLine();
                writer.flush();
            }

        } catch (IOException e) {
            Toast.makeText(MainActivity.this,
                    "删除商品发生异常：" + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    private List<String> getIdForDelete(List<Integer> deleteListFinal) {
        List<String> idList = new ArrayList<String>();
        try (FileReader fr = new FileReader(FILE_PATH + "/" + FILE_NAME);
             BufferedReader bf = new BufferedReader(fr)){
            String str;
            int counter = 1;
            while ((str = bf.readLine()) != null) {
                if(!str.equals("") && deleteListFinal.contains(counter)) {
                    idList.add(str.substring(0, str.indexOf(",")));
                }
                counter++;
            }
        } catch (IOException e) {
            Toast.makeText(MainActivity.this,
                    "删除商品发生异常：" + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
        return idList;
    }
}
