package martisep.timesup;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;


public class SummaryActivity extends Activity {
    int score_gain;
    int current_score;
    int current_team;
    TextView score_view;
    ArrayList<Entry> used_entries;
    ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_summary);

        // extract data from Intent
        Intent intent = getIntent();
        int start = intent.getIntExtra(GameActivity.SUMMARY_START, 0);
        int count = intent.getIntExtra(GameActivity.SUMMARY_COUNT, 0);
        current_score = intent.getIntExtra(GameActivity.SUMMARY_SCORE, 0);
        current_team = intent.getIntExtra(GameActivity.SUMMARY_TEAM, 0);
        used_entries = intent.getParcelableArrayListExtra(GameActivity.SUMMARY);

        score_view = (TextView) findViewById(R.id.textView_score);
        Log.d("", Integer.toString(used_entries.size()));
        listView = (ListView) findViewById(R.id.listViewSummary);
        ArrayAdapter<Entry> adapter = new ArrayAdapter<Entry>(this, R.layout.list_item, used_entries);
        listView.setAdapter(adapter);
        listView.setItemsCanFocus(false);
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        // check guessed items
        for(int i = 0; i < adapter.getCount(); i++){
            listView.setItemChecked(i, adapter.getItem(i).isGuessed());
        }
        score_gain = 0;
        repaintScore();

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d(null, Integer.toString(listView.getCheckedItemCount()));
                SparseBooleanArray checked = listView.getCheckedItemPositions();
                for (int i = 0; i < checked.size(); i++) {
                    ((Entry) listView.getAdapter().getItem(i)).setGuessed(checked.get(i));
                }
                repaintScore();
            }
        });

        Button confirmBtn = (Button) findViewById(R.id.button_confirm_summary);
        confirmBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                // return new score of the team and updated entries
                intent.putExtra(GameActivity.SUMMARY_SCORE, current_score + listView.getCheckedItemCount());
                intent.putParcelableArrayListExtra(GameActivity.SUMMARY, used_entries);
                setResult(RESULT_OK, intent);
                finish();
            }
        });
    }

    private void repaintScore(){
        score_view.setText("Aktuální skóre (Tým "+ current_team +"): "+ (current_score + listView.getCheckedItemCount()));
    }

    @Override
    public void onBackPressed() {
        // do nothing.
    }
}
