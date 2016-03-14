package martisep.thymesup;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseBooleanArray;
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
    int round;
    String team_name;
    TextView score_view;
    ArrayList<Entry> used_entries;
    ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_summary);

        // extract data from Intent
        Intent intent = getIntent();
        current_score = intent.getIntExtra(GameActivity.SUMMARY_SCORE, 0);
        current_team = intent.getIntExtra(GameActivity.SUMMARY_TEAM, 0);
        used_entries = intent.getParcelableArrayListExtra(GameActivity.SUMMARY);
        round = intent.getIntExtra(GameActivity.ROUND, 2);
        team_name = intent.getStringExtra(GameActivity.TEAM_NAME);

        score_view = (TextView) findViewById(R.id.textView_score);
        listView = (ListView) findViewById(R.id.listViewSummary);
        ArrayAdapter<Entry> adapter = new ArrayAdapter<>(this, R.layout.list_item, used_entries);
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
                    if(checked.get(i)){
                        ((Entry) listView.getAdapter().getItem(i)).setState(Entry.EntryState.GUESSED);
                    } else {
                        if(round == 1){
                            ((Entry) listView.getAdapter().getItem(i)).setState(Entry.EntryState.BURNT);
                        } else{
                            ((Entry) listView.getAdapter().getItem(i)).setState(Entry.EntryState.NONE);
                        }
                    }
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
        score_view.setText("Current score (Team "+ team_name +"): "+ (current_score + listView.getCheckedItemCount()));
    }

    @Override
    public void onBackPressed() {
        // do nothing.
    }
}
