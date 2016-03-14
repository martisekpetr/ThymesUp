package martisep.thymesup;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;


public class FilterActivity extends Activity {
    private int team;
    private ArrayList<Entry> filtered_entries;
    private ListView listView;
    private EditText teamNameEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filter);

        // extract data from Intent
        Intent intent = getIntent();
        team = intent.getIntExtra(GameActivity.SUMMARY_TEAM, 0);
        String team_name = intent.getStringExtra(GameActivity.TEAM_NAME);
        filtered_entries = intent.getParcelableArrayListExtra(GameActivity.SUMMARY);

        teamNameEditText = (EditText) findViewById(R.id.editTextTeamName);
        teamNameEditText.setText(team_name);
        teamNameEditText.selectAll();

        listView = (ListView) findViewById(R.id.listViewFilter);
        ArrayAdapter<Entry> adapter = new ArrayAdapter<>(this, R.layout.list_item, filtered_entries);
        listView.setAdapter(adapter);
        listView.setItemsCanFocus(false);
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        // check guessed items
        for(int i = 0; i < adapter.getCount(); i++){
            listView.setItemChecked(i, true);
        }


        Button confirmBtn = (Button) findViewById(R.id.button_confirm_filter);
        confirmBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(listView.getCheckedItemCount() != 6){
                    Toast toast = Toast.makeText(getBaseContext(), "Choose 6 entries.",
                            Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.TOP| Gravity.CENTER_HORIZONTAL, 0, 0);
                    toast.show();
                } else {
                    SparseBooleanArray checked = listView.getCheckedItemPositions();
                    for(int i = checked.size()-1; i >= 0; i--){
                        if(!checked.get(i)){
                            filtered_entries.remove(i);
                        }
                    }
                    Intent intent = new Intent();

                    // return new filtered entries
                    intent.putParcelableArrayListExtra(GameActivity.SUMMARY, filtered_entries);
                    intent.putExtra(GameActivity.SUMMARY_TEAM, team);
                    intent.putExtra(GameActivity.TEAM_NAME, teamNameEditText.getText().toString());
                    setResult(RESULT_OK, intent);
                    finish();
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        // do nothing.
    }



}
