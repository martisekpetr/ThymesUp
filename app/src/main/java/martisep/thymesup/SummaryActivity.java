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
    int current_score;
    int round;
    String team_name;
    ArrayList<Entry> used_entries;

    TextView score_view;
    ListView list_view_summary;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_summary);

        // extract data from Intent
        Intent intent = getIntent();
        current_score = intent.getIntExtra(GameActivity.SCORE, 0);
        used_entries = intent.getParcelableArrayListExtra(GameActivity.ENTRIES_LIST);
        round = intent.getIntExtra(GameActivity.ROUND, 2);
        team_name = intent.getStringExtra(GameActivity.TEAM_NAME);

        // set ui elements
        score_view = (TextView) findViewById(R.id.textView_score);
        list_view_summary = (ListView) findViewById(R.id.listViewSummary);
        ArrayAdapter<Entry> adapter = new ArrayAdapter<>(this, R.layout.list_item, used_entries);
        list_view_summary.setAdapter(adapter);
        list_view_summary.setItemsCanFocus(false);
        list_view_summary.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        // check guessed items
        for(int i = 0; i < adapter.getCount(); i++){
            list_view_summary.setItemChecked(i, adapter.getItem(i).isGuessed());
        }
        repaintScore();

        list_view_summary.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                SparseBooleanArray checked = list_view_summary.getCheckedItemPositions();
                for (int i = 0; i < checked.size(); i++) {
                    if (checked.get(i)) {
                        ((Entry) list_view_summary.getAdapter().getItem(i)).setState(Entry.EntryState.GUESSED);
                    } else {
                        if (round == 1) {
                            ((Entry) list_view_summary.getAdapter().getItem(i)).setState(Entry.EntryState.BURNT);
                        } else {
                            ((Entry) list_view_summary.getAdapter().getItem(i)).setState(Entry.EntryState.NONE);
                        }
                    }
                }
                repaintScore();
            }
        });

        Button confirm_btn = (Button) findViewById(R.id.button_confirm_summary);
        confirm_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();

                // return new score of the team and updated entries
                intent.putExtra(GameActivity.SCORE, current_score + FilterActivity.getCheckedItemCount(list_view_summary));
                intent.putParcelableArrayListExtra(GameActivity.ENTRIES_LIST, used_entries);
                setResult(RESULT_OK, intent);
                finish();
            }
        });
    }

    private void repaintScore(){
        score_view.setText("Current score (" + team_name + "): " + (current_score + FilterActivity.getCheckedItemCount(list_view_summary)));
    }

    @Override
    public void onBackPressed() {
        // do nothing.
    }
}
