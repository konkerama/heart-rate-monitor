package com.example.kostas.heartratemonitor;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.text.NumberFormat;
import java.util.Collections;

import static com.example.kostas.heartratemonitor.R.id.date;
import static com.example.kostas.heartratemonitor.R.id.graph;

public class History extends AppCompatActivity {

    DBHelper dbHelper;
    private ListView listView;
    private LineGraphSeries<DataPoint> mSeries;
    GraphView graph;
    //Button reset;
    TextView dateText,bpmText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        dateText=(TextView)findViewById(R.id.date);
        bpmText=(TextView)findViewById(R.id.bpm);
        listView=(ListView)findViewById(R.id.listView);

        graph = (GraphView) findViewById(R.id.graph);
        graph.getGridLabelRenderer().setVerticalLabelsVisible(false);
        graph.getViewport().setScrollableY(true);
        graph.getViewport().setScalable(true);


        dbHelper = new DBHelper(this);
        mSeries = new LineGraphSeries<>();
        graph.addSeries(mSeries);
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(1200);
        graph.getViewport().setXAxisBoundsManual(true);

        final Cursor cursor = dbHelper.getAllPersons();
        String [] columns = new String[] {
                DBHelper.PERSON_COLUMN_ID,
                DBHelper.PERSON_COLUMN_DATE
        };
        int [] widgets = new int[] {
                R.id.graphID,
                R.id.graphDate
        };

        final SimpleCursorAdapter cursorAdapter = new SimpleCursorAdapter(this, R.layout.graph_info,
                cursor, columns, widgets, 0);

        listView.setAdapter(cursorAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Cursor itemCursor = (Cursor) History.this.listView.getItemAtPosition(position);
                String values = itemCursor.getString(itemCursor.getColumnIndex(DBHelper.PERSON_COLUMN_VALUE));
                String date = itemCursor.getString(itemCursor.getColumnIndex(DBHelper.PERSON_COLUMN_DATE));
                Log.d("graph",values);
                mSeries.resetData(generateData(values));
                Bpm(values);
                dateText.setText(date);
            }
        });


        /*
        reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // db.delete(String tableName, String whereClause, String[] whereArgs);
                // If whereClause is null, it will delete all rows.
                SQLiteDatabase db = dbHelper.getWritableDatabase(); // helper is object extends SQLiteOpenHelper
                db.delete(DBHelper.PERSON_COLUMN_VALUE, null, null);
                db.delete(DBHelper.PERSON_COLUMN_DATE, null, null);
                db.delete(DBHelper.PERSON_COLUMN_ID,null,null);
            }
        });
        */

    }

    private DataPoint[] generateData(String values) {
        String[] parts=values.split(" ");
        int count = parts.length;
        DataPoint[] dataValues = new DataPoint[count];
        for (int i=0; i<count; i++) {
            double x = i;
            double y= Integer.parseInt(parts[i]);
            DataPoint v = new DataPoint(x, y);
            dataValues[i] = v;
        }
        return dataValues;
    }

    private void Bpm(String values) {
        int beats=0;
        String[] parts=values.split(" ");
        int count = parts.length;
        //Log.d("Length", String.valueOf(count));
        for (int i=0; i<count; i++) {
            int max=0;
            while(Integer.parseInt(parts[i])>650){
                if(Integer.parseInt(parts[i])>max){
                    max=Integer.parseInt(parts[i]);
                }
                i++;
            }
            if (max!=0){
                beats++;
            }
        }
        //Log.d("Beats", String.valueOf(beats));
        float time = (float) (count*0.006);
        int bpm= (int) (beats*60/time);
        bpmText.setText(Integer.toString(bpm));
    }


}
