package com.example.technews;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;


public class MainActivity extends AppCompatActivity {

    SQLiteDatabase NewsDataBase;
    ArrayList<String> headings = new ArrayList<>();
    ArrayList<String> information = new ArrayList<>();
    ArrayAdapter arrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        NewsDataBase = this.openOrCreateDatabase("Articles", MODE_PRIVATE, null);
        NewsDataBase.execSQL("CREATE TABLE IF NOT EXISTS articles (id INTEGER PRIMARY KEY, articleId, INTEGER, title VARCHAR, content VARCHAR)");
        DownloadTask task = new DownloadTask();
        try {
            task.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");
        } catch (Exception e) {
        }
        ListView listView = findViewById(R.id.listview);
        arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, headings);
        listView.setAdapter(arrayAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent(getApplicationContext(), news.class);
                intent.putExtra("content", information.get(i));
                startActivity(intent);
            }
        });

        UpdateListView();
    }
    public void UpdateListView() {

        Cursor cursor = NewsDataBase.rawQuery("SELECT * FROM articles", null);

        int newsIndex = cursor.getColumnIndex("content");

        int headingIndex = cursor.getColumnIndex("title");

        if (cursor.moveToFirst()) {
            headings.clear();
            information.clear();

            do {
                headings.add(cursor.getString(headingIndex));
                information.add(cursor.getString(newsIndex));

            } while (cursor.moveToNext());
            arrayAdapter.notifyDataSetChanged();
        }
    }

    public class DownloadTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... urls) {

            String result = "";
            URL url;
            HttpURLConnection urlConnection = null;

            try {

                url = new URL(urls[0]);

                urlConnection = (HttpURLConnection) url.openConnection();

                InputStream inputStream = urlConnection.getInputStream();

                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);

                int data = inputStreamReader.read();

                while (data != -1) {
                    char current = (char) data;
                    result += current;
                    data = inputStreamReader.read();
                }

                JSONArray jsonArray = new JSONArray(result);

                int nbOfItems = 20;

                if (jsonArray.length() < 20) {
                    nbOfItems = jsonArray.length();
                }

                NewsDataBase.execSQL("DELETE FROM articles");

                for (int i=0;i < nbOfItems; i++) {
                    String articleId = jsonArray.getString(i);
                    url = new URL("https://hacker-news.firebaseio.com/v0/item/" + articleId + ".json?print=pretty");
                    urlConnection = (HttpURLConnection) url.openConnection();

                    inputStream = urlConnection.getInputStream();
                    inputStreamReader = new InputStreamReader(inputStream);

                    data = inputStreamReader.read();

                    String newsInfo = "";

                    while (data != -1) {
                        char current = (char) data;
                        newsInfo += current;
                        data = inputStreamReader.read();
                    }
                    JSONObject jsonObject = new JSONObject(newsInfo);
                    if (!jsonObject.isNull("title") && !jsonObject.isNull("url")) {
                        String articleTitle = jsonObject.getString("title");
                        String newsUrl = jsonObject.getString("url");

                        url = new URL(newsUrl);
                        urlConnection = (HttpURLConnection) url.openConnection();
                        inputStream = urlConnection.getInputStream();
                        inputStreamReader = new InputStreamReader(inputStream);
                        data = inputStreamReader.read();
                        String newsContent = "";
                        while (data != -1) {
                            char current = (char) data;
                            newsContent += current;
                            data = inputStreamReader.read();
                        }
                        String sql = "INSERT INTO articles (articleId, title, content) VALUES (?, ?, ?)";
                        SQLiteStatement statement = NewsDataBase.compileStatement(sql);
                        statement.bindString(1,articleId);
                        statement.bindString(2,articleTitle);
                        statement.bindString(3,newsContent);
                        statement.execute();
                    }
                }
                return result;
            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            UpdateListView();
        }
    }
}