package martisep.thymesup;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Filter;


public class GameActivity extends Activity {
    // string constants
    public final static String TEAM_COUNT_MESSAGE = "martisep.thymesup.TEAM_COUNT_MESSAGE";
    public final static String TOPICS_MESSAGE = "martisep.thymesup.TOPICS_MESSAGE";
    public final static String SUMMARY_START = "martisep.thymesup.SUMMARY_START";
    public final static String SUMMARY_COUNT = "martisep.thymesup.SUMMARY_COUNT";
    public final static String SUMMARY= "martisep.thymesup.SUMMARY";
    public final static String SUMMARY_SCORE= "martisep.thymesup.SUMMARY_SCORE";
    public static final String SUMMARY_TEAM = "martisep.thymesup.SUMMARY_TEAM";
    public static final String ROUND1 = "Popis";
    public static final String ROUND2 = "Jedno slovo";
    public static final String ROUND3 = "Šarády";
    private static final int SUMMARY_REQUEST_CODE = 8;
    private static final int FILTER_REQUEST_CODE = 9;
    public static final String ROUND = "martisep.thymesup.ROUND";
    public static final String TEAM_NAME = "martisep.thymesup.TEAM_NAME";


    // game variables
    private int team_count;
    private int current_team;
    private int current_index;
    private int turn_start_index;
    private int turn_counter;
    private int remaining_words;
    private int[] score;
    private String[] team_names;
    private int round;
    private ArrayList<Entry> words;
    private ArrayList<Entry> filtered_words;
    private ArrayList<Entry> round_words;

    //ui elements
    Intent intent;
    TextView name_text;
    TextView keywords_text;
    ProgressBar mProgressBar;
    CountDownTimer mCountDownTimer;
    TextView mCountDown;
    Button btn_correct;
    Button btn_skip;
    Button btn_burn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        // get parameters of the game from intent
        intent = getIntent();
        team_count = intent.getIntExtra(TEAM_COUNT_MESSAGE,2);
        String[] topics = (intent.getStringArrayListExtra(TOPICS_MESSAGE)).toArray(new String[0]);
        if(topics.length == 0){
            return;
        }

        loadWordsFromDB(topics, 15 * team_count);
        initUIElements();
        //start game
        newGame();
    }

    private void initUIElements(){
        // init ui elements
        name_text = (TextView)findViewById(R.id.guessedWord);
        keywords_text = (TextView)findViewById(R.id.guessedKeywords);

        btn_correct = (Button) findViewById(R.id.button_correct);
        btn_skip = (Button) findViewById(R.id.button_skip);
        btn_burn = (Button) findViewById(R.id.button_burn);

        btn_correct.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                round_words.get(current_index).setState(Entry.EntryState.GUESSED);
                remaining_words--;
                if (remaining_words == 0){
                    turn_counter++;
                    endTurn(); // not yet endRound! some words may be corrected in summary
                } else {
                    nextWord();
                }
            }
        });
        btn_skip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nextWord();
            }
        });
        btn_burn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                round_words.get(current_index).setState(Entry.EntryState.BURNT);
                remaining_words--;
                if (remaining_words == 0){
                    turn_counter++;
                    endTurn(); // not yet endRound! some words may be corrected in summary
                } else {
                    nextWord();
                }
            }
        });

        mProgressBar=(ProgressBar)findViewById(R.id.progressBar);
        mCountDown = (TextView) findViewById(R.id.coundown);
        mProgressBar.setRotation(180);

        mCountDownTimer = new CountDownTimer(30000,500) {
            @Override
            public void onTick(long millisUntilFinished) {
                mProgressBar.setProgress((int) (millisUntilFinished / 1000 + 1));
                mCountDown.setText(Integer.toString((int) (millisUntilFinished / 1000 + 1)));
            }

            @Override
            public void onFinish() {
                mProgressBar.setProgress(0);
                mCountDown.setText(Integer.toString(0));
                endTurn();
            }
        };

    }

    private void loadWordsFromDB(String[] topics, int word_limit) {
        // get random set of words of given size from database according to selected topics
        DBController dbController = new DBController(this);
        SQLiteDatabase db = dbController.getReadableDatabase();
        String query = "SELECT DISTINCT "
                + DBController.NAME_COLUMN
                + ", "
                + DBController.KEYWORDS_COLUMN
                + " FROM "
                + DBController.ENTRIES_TABLE
                + " WHERE "
                + DBController.TOPIC_COLUMN
                + " IN ("
                + makePlaceholders(topics.length)
                + ") ORDER BY RANDOM() LIMIT "
                + Integer.toString(word_limit);
        Cursor c = db.rawQuery(query, topics);
        c.moveToFirst();
        words = new ArrayList<>();
        while (!c.isAfterLast()) {
            Entry entry = new Entry(
                    c.getString(c.getColumnIndex(DBController.NAME_COLUMN)),
                    c.getString(c.getColumnIndex(DBController.KEYWORDS_COLUMN))
            );
            words.add(entry);
            c.moveToNext();
        }
        c.close();
    }

    private void newGame(){
        score = new int[team_count];
        team_names = new String[team_count];
        filtered_words = new ArrayList<>();
        current_team = -1;
        round = 1; //TODO or 0?

        callFilterActivity(0);
    }

    private void getTeamName(final int team){
        final EditText teamName = new EditText(this);
        teamName.setText("Team " + team);

        new AlertDialog.Builder(this)
                .setTitle("Team " + Integer.toString(team))
                .setMessage("Name of your team?")
                .setCancelable(false)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setView(teamName)
                .setPositiveButton("Done", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        team_names[team] = teamName.getText().toString();
                        if (team + 1 == team_count) {
                            newRound();
                        } else {
                            getTeamName(team + 1);
                        }
                    }
                }).show();
    }

    private void newRound(){
        round_words = new ArrayList<>();
        for(Entry p : filtered_words){
            round_words.add(p.clone());
        }
        Collections.shuffle(round_words);

        if(round == 1){
            btn_burn.setVisibility(View.VISIBLE);
            btn_skip.setVisibility(View.GONE);
        } else{
            btn_burn.setVisibility(View.GONE);
            btn_skip.setVisibility(View.VISIBLE);
        }

        remaining_words = round_words.size();
        current_index = -1;
        turn_start_index = 0;
        nextWord();
        newTurn();
    }

    private void newTurn(){
        current_team = (current_team + 1) % team_count;
        turn_start_index = current_index;
        turn_counter = 0;

        //reset countdown
        mProgressBar.setProgress(30);
        mCountDown.setText("30");

        //hide texts until countdown started
        name_text.setVisibility(View.INVISIBLE);
        keywords_text.setVisibility(View.INVISIBLE);

        //ready?
        new AlertDialog.Builder(this)
                .setTitle("Team " + team_names[current_team])
                .setMessage("Ready?")
                .setCancelable(false)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("Start!", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        name_text.setVisibility(View.VISIBLE);
                        keywords_text.setVisibility(View.VISIBLE);
                        mCountDownTimer.start();
                    }}).show();
    }

    private void nextWord(){
        if(round_words.size() == 0){ return; }

        // get next "not yet guessed" word (if exists)
        do{
            current_index = (current_index + 1) % round_words.size();
        } while(round_words.get(current_index).getState() != Entry.EntryState.NONE);

        turn_counter++; // count how many words the player cycled through

        name_text.setText(round_words.get(current_index).getName());
        keywords_text.setText(round_words.get(current_index).getKeywords());
    }

    private void endTurn(){
        mCountDownTimer.cancel(); //should already be stopped, except in the end of the round
        /*
        MediaPlayer mPlayer = MediaPlayer.create(getApplicationContext(), R.raw.fifth_short);
        mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mPlayer.setVolume(1.0f, 1.0f);
        mPlayer.start();

        Vibrator v = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
        long[] pattern = {100,60,110,60,110,60,100, 2000};
        // The '-1' here means to vibrate once, as '-1' is out of bounds in the pattern array
        v.vibrate(pattern, -1);
        */
        if(remaining_words == 0){
            callSummaryActivity();
        } else{
            new AlertDialog.Builder(this)
                    .setTitle("Discard last word?")
                    .setMessage("Do you want to discard the last word or leave it for the next team?")
                    .setCancelable(false)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setNegativeButton("Discard", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            if(round == 1){
                                round_words.get(current_index).setState(Entry.EntryState.BURNT);
                                remaining_words--;
                                if (remaining_words > 0){
                                    nextWord();
                                }
                            } else{
                                nextWord();
                            }
                            callSummaryActivity();
                        }})
                    .setPositiveButton("Leave", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            callSummaryActivity();
                        }})
                    .show();
        }

    }

    private void callFilterActivity(int team){
        Intent filter_intent = new Intent(getApplicationContext(), FilterActivity.class);
        filter_intent.putExtra(SUMMARY_TEAM, current_team);

        ArrayList<Entry> words_to_filter = new ArrayList<>();

        int num_entries = Math.round((float) words.size() / team_count);

        for(int i = team*num_entries; i < (team+1)*num_entries && i < words.size(); i++){
            words_to_filter.add(words.get(i));
        }
        filter_intent.putParcelableArrayListExtra(SUMMARY, words_to_filter);

        try {
            startActivityForResult(filter_intent, FILTER_REQUEST_CODE);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(getBaseContext(), "No summary activity found.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void callSummaryActivity(){
        Intent summary_intent = new Intent(getApplicationContext(), SummaryActivity.class);
        summary_intent.putExtra(SUMMARY_START, turn_start_index);
        summary_intent.putExtra(SUMMARY_COUNT, turn_counter);
        summary_intent.putExtra(SUMMARY_SCORE, score[current_team]);
        summary_intent.putExtra(SUMMARY_TEAM, current_team );
        summary_intent.putExtra(TEAM_NAME, team_names[current_team]);
        summary_intent.putExtra(ROUND, round);

        // copy only words which player cycled through (without repeat)
        ArrayList<Entry> used_words = new ArrayList<>();
        turn_counter = Math.min(turn_counter,round_words.size());

        for(int i = 0; i < turn_counter; i++){
            used_words.add(round_words.get((turn_start_index + i) % round_words.size()));
        }
        summary_intent.putParcelableArrayListExtra(SUMMARY, used_words);

        try {
            startActivityForResult(summary_intent, SUMMARY_REQUEST_CODE);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(getBaseContext(), "No summary activity found.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void endRound(){
        //displayscore
        StringBuilder stringBuilder = new StringBuilder();
        for(int i = 0; i < team_count; i++){
            stringBuilder.append("Team ").append(team_names[i]).append(": ").append(score[i]).append("\n");
        }
        new AlertDialog.Builder(this)
                .setTitle("End of round " + Integer.toString(round))
                .setMessage("Score:\n" + stringBuilder.toString())
                .setIcon(android.R.drawable.ic_dialog_info)
                .setCancelable(false)
                .setPositiveButton("Next round", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        round++;
                        if (round == 4) {
                            finish();
                        } else {
                            newRound();
                        }
                    }
                }).show();
    }

    String makePlaceholders(int len) {
        if (len < 1) {
            // It will lead to an invalid query anyway ..
            throw new RuntimeException("No placeholders");
        } else {
            StringBuilder sb = new StringBuilder(len * 2 - 1);
            sb.append("?");
            for (int i = 1; i < len; i++) {
                sb.append(",?");
            }
            return sb.toString();
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        if(data == null){
            return;
        }
        switch(requestCode){
            case SUMMARY_REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    score[current_team] = data.getIntExtra(GameActivity.SUMMARY_SCORE, score[current_team]);

                    ArrayList<Entry> corrected_entries = data.getParcelableArrayListExtra(GameActivity.SUMMARY);

                    // correct guessed/not-guessed words
                    for(int i = 0; i < turn_counter; i++){
                        round_words.get((turn_start_index + i) % round_words.size()).setState(corrected_entries.get(i).getState());
                    }

                    // repoint current_pointer to next non-guessed, non-burnt word
                    int limit = 0;
                    while(round_words.get(current_index).getState() != Entry.EntryState.NONE){
                        current_index = (current_index + 1) % round_words.size();
                        limit++;
                        if(limit == round_words.size()){
                            // prevent endless cycle
                            break;
                        }
                    }

                    // CAREFULLY! remove guessed and burnt words (in reverse order) and correctly manage current_index
                    for(int i = round_words.size()-1; i >= current_index; i--){
                        if(round_words.get(i).getState() != Entry.EntryState.NONE){
                            round_words.remove(i);
                        }
                    }
                    for(int i = current_index-1; i >= 0; i--){
                        if(round_words.get(i).getState() != Entry.EntryState.NONE){
                            round_words.remove(i);
                            current_index--;
                        }
                    }
                    remaining_words = round_words.size();

                    // all words guessed -> new round
                    if(round_words.isEmpty()){
                        endRound();
                    } else{
                        name_text.setText(round_words.get(current_index).getName());
                        keywords_text.setText(round_words.get(current_index).getKeywords());
                        newTurn();
                    }
                }
            case FILTER_REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    ArrayList<Entry> filtered_entries = data.getParcelableArrayListExtra(GameActivity.SUMMARY);
                    int team = data.getIntExtra(GameActivity.SUMMARY_TEAM, 0);
                    team_names[team] = data.getStringExtra(GameActivity.TEAM_NAME);


                    filtered_words.addAll(filtered_entries);
                    team++;
                    if(team == team_count){
                        newRound();
                    } else {
                        callFilterActivity(team);
                    }
                }
        }
    }

    @Override
    public void onBackPressed() {
        // do nothing.
    }
}

