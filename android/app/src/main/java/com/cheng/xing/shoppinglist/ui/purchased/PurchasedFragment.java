package com.cheng.xing.shoppinglist.ui.purchased;

import androidx.lifecycle.ViewModelProviders;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.cheng.xing.shoppinglist.R;
import com.cheng.xing.shoppinglist.ui.home.HomeFragment;
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

public class PurchasedFragment extends Fragment {

    private PurchasedViewModel mViewModel;
    private TableLayout resultTable;
    private String fileName;
    private String filePath;
    private List<String> shownItemIdList;
    private String purchasedfileName;
    private DropBoxUtils dropBoxUtils;
    private List<Boolean> highlightedList = new ArrayList<Boolean>();
    private ProgressDialog pd;
    private ProgressDialog pdh;

    public static PurchasedFragment newInstance() {
        return new PurchasedFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.purchased_fragment, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = ViewModelProviders.of(this).get(PurchasedViewModel.class);
        String ACCESS_TOKEN = getResources().getString(R.string.access_token);
        fileName = getResources().getString(R.string.file_name);
        purchasedfileName = getResources().getString(R.string.file_name_purchased);
        filePath = getActivity().getApplicationContext().getFilesDir().getAbsolutePath();
        String[] fileNames = new String[]{fileName, purchasedfileName};
        dropBoxUtils = new DropBoxUtils(filePath, ACCESS_TOKEN, fileNames);
        resultTable = (TableLayout) getActivity().findViewById(R.id.purchasedTable);
        setButtonStatus(false);
        pd = new ProgressDialog(getActivity());
        pdh = new ProgressDialog(getActivity());
        pdh.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        addData();

        Button btn_del = (Button) getActivity().findViewById(R.id.deletePurchasedSelected);
        btn_del.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteSelected(v);
                setButtonStatus(false);
            }
        });

        Button btn_purchased = (Button) getActivity().findViewById(R.id.purchaseAgain);
        btn_purchased.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                purchaseSelected(v);
                setButtonStatus(false);
            }
        });

        Button btn_highlighted = (Button) getActivity().findViewById(R.id.highlightPurchased);
        btn_highlighted.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                highlightSelected(v);
                //setButtonStatus(false);
            }
        });
    }

    private void purchaseSelected(View v) {
        String temp = "";
        List<Integer> purchaseList = new ArrayList<Integer>();
        for (int i = 1; i < resultTable.getChildCount(); i++) {
            CheckBox cb = (CheckBox)((TableRow)resultTable.getChildAt(i)).getChildAt(4);
            if(cb.isChecked()){
                purchaseList.add(i);
                temp += i + ";";
            }
        }

        if(purchaseList.size() == 0){
            Toast.makeText(getActivity(), "未选中任何商品",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        final List<Integer> purchaseListFinal = purchaseList;
        AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
                .setTitle("购买确认")
                .setMessage("确定再次购买选中的商品么？")
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        purchaseAgain(purchaseListFinal);
                    }
                })
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Toast.makeText(getActivity(), "操作未执行",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .create();
        alertDialog.show();
    }

    private void purchaseAgain(List<Integer> purchaseListFinal) {
        List<String> idList = getIdForOperation(purchaseListFinal);
        new PurchaseTask().execute(idList);
    }

    private void purchaseCorrespondingItems(List<String> idList) {
        Calendar cal = Calendar.getInstance();
        int day = cal.get(Calendar.DATE);
        int month = cal.get(Calendar.MONTH) + 1;
        List<String> shoppingLines = new ArrayList<String>();
        try (FileReader fr = new FileReader(filePath + "/" + purchasedfileName);
             BufferedReader bf = new BufferedReader(fr);){
            String str;
            while ((str = bf.readLine()) != null) {
                if(!str.equals("") && (idList.contains(str.substring(0, str.indexOf(","))))) {
                    String[] elements = str.substring(0, str.indexOf(";")).split(",");
                    shoppingLines.add(elements[0] + "," + elements[1] + "," +
                            elements[2] + "," + elements[3] + "," +
                            (month < 10? "0" + month: month) + "-"+
                            (day < 10? "0" + day: day) +";0");
                }
            }
        } catch (IOException e) {
            Toast.makeText(getActivity(),
                    "添加已购商品发生异常：" + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }

        try (FileReader fr = new FileReader(filePath + "/" + fileName);
             BufferedReader bf = new BufferedReader(fr);){
            String str;
            while ((str = bf.readLine()) != null) {
                shoppingLines.add(str);
            }
        } catch (IOException e) {
            Toast.makeText(getActivity(),
                    "添加已购商品发生异常：" + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }

        File file = new File(filePath + "/" + fileName);
        try (FileWriter fw = new FileWriter(file,false);
             BufferedWriter writer = new BufferedWriter(fw)){
            if (!file.exists()) {
                file.createNewFile();
            }
            for(String s: shoppingLines) {
                writer.write(s);
                writer.newLine();
                writer.flush();
            }

        } catch (IOException e) {
            Toast.makeText(getActivity(),
                    "添加已购商品发生异常：" + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void highlightSelected(View v) {
        String temp = "";
        List<Integer> list = new ArrayList<Integer>();
        for (int i = 1; i < resultTable.getChildCount(); i++) {
            CheckBox cb = (CheckBox)((TableRow)resultTable.getChildAt(i)).getChildAt(4);
            if(cb.isChecked()){
                list.add(i);
                temp += i + ";";
            }
        }

        if(list.size() == 0){
            Toast.makeText(getActivity(), "未选中任何商品",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        new HighlightTask().execute(getIdForOperation(list));
        
    }

    private void highlightCorrespondingItems(List<String> targetList) {
        List<String> lines = new ArrayList<String>();
        Button btn_highlighted = (Button) getActivity().findViewById(R.id.highlightPurchased);
        String append = (btn_highlighted.getText().equals("高亮"))? "1":"0";
        try (FileReader fr = new FileReader(filePath + "/" + purchasedfileName);
             BufferedReader bf = new BufferedReader(fr);){
            String str;
            while ((str = bf.readLine()) != null) {
                if(!str.equals("") && (targetList.contains(str.substring(0, str.indexOf(","))))) {
                    lines.add(str.substring(0, str.indexOf(';')+1) + append);
                } else {
                    lines.add(str);
                }
            }
        } catch (IOException e) {
            Toast.makeText(getActivity(),
                    "高亮商品发生异常：" + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }

        File file = new File(filePath + "/" + purchasedfileName);
        try (FileWriter fw = new FileWriter(file,false);
             BufferedWriter writer = new BufferedWriter(fw)){
            if (!file.exists()) {
                file.createNewFile();
            }
            for(String s: lines) {
                writer.write(s);
                writer.newLine();
                writer.flush();
            }

        } catch (IOException e) {
            Toast.makeText(getActivity(),
                    "高亮商品发生异常：" + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void addData(){
        resultTable.setStretchAllColumns(true);
        resultTable.setShrinkAllColumns(true);
        resultTable.removeAllViewsInLayout();
        highlightedList = new ArrayList<Boolean>();
        shownItemIdList = new ArrayList<String>();
        String[] titles = {"名称", "超市", "备注", "购买时间", "操作"};
        addTitleLine(titles);
        new AddDataTask().execute("");
    }

    private String[] convert(String s){
        return s.substring(s.indexOf(",") + 1, s.indexOf(";")).split(",");
    }

    private void addTitleLine(String[] elements){
        TableRow row = new TableRow(getActivity().getApplicationContext());
        TableRow.LayoutParams layoutParams =
                new TableRow.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT);
        layoutParams.setMargins(1,1,1,1);
        for(String element: elements){
            TextView tv = new TextView(getActivity().getApplicationContext());
            tv.setText(element);
            tv.setMaxWidth(200);
            tv.setMinHeight(100);
            tv.setBackgroundColor(Color.WHITE);
            tv.setGravity(Gravity.CENTER);
            row.addView(tv, layoutParams);
        }
        resultTable.addView(row);
    }

    private void addLine(String[] elements, boolean highlighted){
        TableRow row = new TableRow(getActivity().getApplicationContext());
        TableRow.LayoutParams layoutParams =
                new TableRow.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT);
        layoutParams.setMargins(1,1,1,1);
        for(int i = 0; i < 4; i++){
            TextView tv = new TextView(getActivity().getApplicationContext());
            tv.setText(elements[i]);
            tv.setMaxWidth(200);
            tv.setMinHeight(100);
            if (highlighted){
                tv.setBackgroundColor(Color.YELLOW);
            }else {
                tv.setBackgroundColor(Color.WHITE);
            }
            tv.setGravity(Gravity.CENTER);
            row.addView(tv, layoutParams);
        }
        CheckBox cb = new CheckBox(getActivity().getApplicationContext());
        cb.setGravity(Gravity.CENTER);
        if (highlighted){
            cb.setBackgroundColor(Color.YELLOW);
        }else {
            cb.setBackgroundColor(Color.WHITE);
        }
        cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
              boolean all_highlighted = true;
             List<Integer> list = new ArrayList<Integer>();
             for (int i = 1; i < resultTable.getChildCount(); i++) {
                 CheckBox cb = (CheckBox)((TableRow)resultTable.getChildAt(i)).getChildAt(4);
                 if(cb.isChecked()){
                     list.add(i);
                     all_highlighted = all_highlighted && highlightedList.get(i-1);
                 }
             }
             Button btn_highlighted = (Button) getActivity().findViewById(R.id.highlightPurchased);
             if(all_highlighted){
                 btn_highlighted.setText("取消高亮");
             } else{
                 btn_highlighted.setText("高亮");
             }
             if(list.size() == 0){
                 setButtonStatus(false);
             } else {
                 setButtonStatus(true);
             }
            }
        });
        row.addView(cb, layoutParams);
        resultTable.addView(row);
    }

    private void setButtonStatus(boolean available) {
        Button btn_del = (Button) getActivity().findViewById(R.id.deletePurchasedSelected);
        Button btn_purchasedAgain = (Button) getActivity().findViewById(R.id.purchaseAgain);
        Button btn_highlighted = (Button) getActivity().findViewById(R.id.highlightPurchased);
        //Button btn_purchaseAgainEdit = (Button) getActivity().findViewById(R.id.purchaseAgainEdit);
        if(available){
            btn_del.setAlpha(1);
            btn_purchasedAgain.setAlpha(1);
            btn_highlighted.setAlpha(1);
            //btn_purchaseAgainEdit.setAlpha(1);
        } else {
            btn_del.setAlpha(0.5F);
            btn_purchasedAgain.setAlpha(0.5F);
            btn_highlighted.setAlpha(0.5F);
            //btn_purchaseAgainEdit.setAlpha(0.5F);
            btn_highlighted.setText("高亮");
        }
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
            Toast.makeText(getActivity(), "未选中任何商品",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        final List<Integer> deleteListFinal = deleteList;
        AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
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
                        Toast.makeText(getActivity(), "操作未执行",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .create();
        alertDialog.show();
    }

    private void deleteAndRefreshGrid(List<Integer> deleteListFinal) {
        List<String> ids = getIdForOperation(deleteListFinal);
        new DeleteTask().execute(ids);
    }

    private void refreshGrid() {
        //dropBoxUtils.downloadLatestPurchasedFile();
        addData();
    }

    private void deleteCorrespondingItems(List<String> targetList) {
        List<String> itemsLeftLines = new ArrayList<String>();
        try (FileReader fr = new FileReader(filePath + "/" + purchasedfileName);
             BufferedReader bf = new BufferedReader(fr);){
            String str;
            while ((str = bf.readLine()) != null) {
                if(!str.equals("") && (!targetList.contains(str.substring(0, str.indexOf(","))))) {
                    itemsLeftLines.add(str);
                }
            }
        } catch (IOException e) {
            Toast.makeText(getActivity(),
                    "删除商品发生异常：" + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }

        File file = new File(filePath + "/" + purchasedfileName);
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
            Toast.makeText(getActivity(),
                    "删除商品发生异常：" + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    private List<String> getIdForOperation(List<Integer> lineNumbers) {
        List<String> targetIdList = new ArrayList<String>();
        for(Integer i: lineNumbers){
            targetIdList.add(shownItemIdList.get(i-1));
        }
        return targetIdList;
    }

    class AddDataTask extends AsyncTask<String, Integer, Integer> {
        @Override
        protected void onPreExecute() {
            pd.setTitle("提示");
            pd.setMessage("正在加载，请稍后...");
            pd.show();
            super.onPreExecute();
        }

        protected Integer doInBackground(String... args) {
            dropBoxUtils.downloadLatestPurchasedFile();
            return 1;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate();
        }

        protected void onPostExecute(Integer result) {
            try (FileReader fr = new FileReader(filePath + "/" + purchasedfileName);
                 BufferedReader bf = new BufferedReader(fr)){
                String str;
                while ((str = bf.readLine()) != null) {
                    if(!str.equals("")) {
                        boolean highlighted = (str.substring(str.indexOf(';')).length() >= 2 &&
                                str.substring(str.indexOf(';')).charAt(1) == '1');
                        shownItemIdList.add(str.substring(0, str.indexOf(",")));
                        highlightedList.add(highlighted);
                        addLine(convert(str), highlighted);
                    }
                }
            } catch (IOException e) {
                Toast.makeText(getActivity(),
                        "读取文件发生异常：" + e.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
            pd.dismiss();
            super.onPostExecute(1);
        }
    }

    class PurchaseTask extends AsyncTask<List<String>, Integer, Integer> {
        private List<String> list;
        @Override
        protected void onPreExecute() {
            pdh.setTitle("移动到已购清单");
            pdh.setMessage("正在处理，请稍后...");
            pdh.setProgress(0);
            pdh.setMax(100);
            pdh.show();
            super.onPreExecute();
        }

        protected Integer doInBackground(List<String>... args) {
            list = args[0];
            dropBoxUtils.downloadLatestFile();
            publishProgress(30);
            pdh.setMessage("获取清单...");
            purchaseCorrespondingItems(list);
            deleteCorrespondingItems(list);
            pdh.setMessage("上传清单...");
            dropBoxUtils.uploadToDropBox();
            publishProgress(60);
            pdh.setMessage("更新列表...");
            dropBoxUtils.downloadLatestFile();
            publishProgress(0);
            return 1;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            pdh.setProgress(values[0]);
            super.onProgressUpdate();
        }

        protected void onPostExecute(Integer result) {
            pdh.dismiss();
            Toast.makeText(getActivity(), "添加" + list.size() + "件商品到已购列表",
                    Toast.LENGTH_SHORT).show();
            super.onPostExecute(1);
        }
    }

    class DeleteTask extends AsyncTask<List<String>, Integer, Integer> {
        private List<String> list;
        @Override
        protected void onPreExecute() {
            pdh.setTitle("删除物品");
            pdh.setMessage("正在处理，请稍后...");
            pdh.setMax(100);
            pdh.setProgress(0);
            pdh.show();
            super.onPreExecute();
        }

        protected Integer doInBackground(List<String>... args) {
            list = args[0];
            pdh.setMessage("获取最新清单...");
            dropBoxUtils.downloadLatestPurchasedFile();
            publishProgress(30);
            deleteCorrespondingItems(list);
            pdh.setMessage("上传清单...");
            dropBoxUtils.uploadPurchasedToDropBox();
            publishProgress(70);
            pdh.setMessage("更新列表...");
            dropBoxUtils.downloadLatestPurchasedFile();
            publishProgress(0);
            return 1;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            pdh.setProgress(values[0]);
            super.onProgressUpdate();
        }

        protected void onPostExecute(Integer result) {
            pdh.dismiss();
            refreshGrid();
            Toast.makeText(getActivity(), "已删除" + list.size() + "件商品",
                    Toast.LENGTH_SHORT).show();
            super.onPostExecute(1);
        }
    }

    class HighlightTask extends AsyncTask<List<String>, Integer, Integer> {
        private List<String> list;
        @Override
        protected void onPreExecute() {
            pd.setTitle("高亮处理");
            pd.setMessage("正在处理，请稍后...");
            pd.show();
            super.onPreExecute();
        }

        protected Integer doInBackground(List<String>... args) {
            list = args[0];
            dropBoxUtils.downloadLatestPurchasedFile();
            highlightCorrespondingItems(list);
            dropBoxUtils.uploadPurchasedToDropBox();
            return 1;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            pdh.setProgress(values[0]);
            super.onProgressUpdate();
        }

        protected void onPostExecute(Integer result) {
            dropBoxUtils.downloadLatestPurchasedFile();
            pdh.dismiss();
            refreshGrid();
            Toast.makeText(getActivity(), "已修改" + list.size() + "件商品高亮显示",
                    Toast.LENGTH_SHORT).show();
            setButtonStatus(false);
            super.onPostExecute(1);
        }
    }
}
