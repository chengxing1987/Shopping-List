package com.cheng.xing.shoppinglist.ui.add;

import androidx.lifecycle.ViewModelProviders;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.Toast;

import com.cheng.xing.shoppinglist.R;
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

public class AddFragment extends Fragment {

    private AddViewModel mViewModel;
    private String fileName;
    private String filePath;
    private DropBoxUtils dropBoxUtils;
    private ProgressDialog pd;

    public static AddFragment newInstance() {
        return new AddFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.add_fragment, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = ViewModelProviders.of(this).get(AddViewModel.class);
        // TODO: Use the ViewModel

        String ACCESS_TOKEN = getResources().getString(R.string.access_token);
        fileName = getResources().getString(R.string.file_name);
        String purchasedfileName = getResources().getString(R.string.file_name_purchased);
        filePath = getActivity().getApplicationContext().getFilesDir().getAbsolutePath();
        String[] fileNames = new String[]{fileName, purchasedfileName};
        dropBoxUtils = new DropBoxUtils(filePath, ACCESS_TOKEN, fileNames);
        pd = new ProgressDialog(getActivity());
        Button btn_add = (Button) getActivity().findViewById(R.id.add_ok);
        btn_add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveNewItem(v);
            }
        });
    }

    public void saveNewItem(android.view.View view){
        LinearLayout addFrame = (LinearLayout)getActivity().findViewById(R.id.addFrame);
        EditText nameEdit = getActivity().findViewById(R.id.name);
        String name = nameEdit.getText().toString();
        Spinner shopEdit = getActivity().findViewById(R.id.fromSelect);
        String shop = (String)shopEdit.getSelectedItem();
        EditText commentEdit = getActivity().findViewById(R.id.comment);
        String comment = commentEdit.getText().toString();
        Calendar cal = Calendar.getInstance();
        int day = cal.get(Calendar.DATE);
        int month = cal.get(Calendar.MONTH) + 1;
        if(name.equals("")){
            Toast.makeText(getActivity(),
                    "物品名称不能为空",
                    Toast.LENGTH_LONG).show();
        }else if(name.contains(",") || name.contains(";")){
            Toast.makeText(getActivity(),
                    "物品中不能包含‘,’和‘;’符号，请重新输入",
                    Toast.LENGTH_LONG).show();
        }
        else if(comment.contains(",") || comment.contains(";")){
            Toast.makeText(getActivity(),
                    "备注中不能包含‘,’和‘;’符号，请重新输入",
                    Toast.LENGTH_LONG).show();
        }else {
            addItemOperation(System.currentTimeMillis() + "," + name + "," + shop + ","
                    + comment + ","+(month < 10? "0" + month: month)+"-"+
                    (day < 10? "0" + day: day) +";0", name);

        }
    }

    private void addItemOperation(String text, String name){
        new AddItemTask().execute(new String[]{text, name});
    }

    private void resetAndMessage(String name){
        EditText nameEdit = getActivity().findViewById(R.id.name);
        EditText commentEdit = getActivity().findViewById(R.id.comment);
        nameEdit.setText("");
        commentEdit.setText("");
        Toast.makeText(getActivity(),
                "添加物品‘" + name +"’成功",
                Toast.LENGTH_LONG).show();
    }

    private void writeNewItemToLocalFile(String text){
        List<String> shoppingLines = new ArrayList<String>();
        shoppingLines.add(text);
        File file = new File(filePath + "/" + fileName);

        try (FileReader fr = new FileReader(filePath + "/" + fileName);
             BufferedReader bf = new BufferedReader(fr);){
            String str;
            while ((str = bf.readLine()) != null) {
                shoppingLines.add(str);
            }
        } catch (IOException e) {
            Toast.makeText(getActivity(),
                    "添加商品发生异常：" + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }

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
                    "添加商品发生异常：" + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    class AddItemTask extends AsyncTask<String, Integer, Integer> {
        String name = "";
        @Override
        protected void onPreExecute() {
            pd.setTitle("添加新商品");
            pd.setMessage("正在处理，请稍后...");
            pd.show();
            super.onPreExecute();
        }

        protected Integer doInBackground(String... args) {
            name = args[1];
            dropBoxUtils.downloadLatestFile();
            writeNewItemToLocalFile(args[0]);
            dropBoxUtils.uploadToDropBox();
            return 1;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate();
        }

        protected void onPostExecute(Integer result) {
            dropBoxUtils.downloadLatestFile();
            pd.dismiss();
            resetAndMessage(name);
            super.onPostExecute(1);
        }
    }
}
