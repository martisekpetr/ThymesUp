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
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;


public class GameActivity extends Activity {
    // string constants
    public final static String TEAM_COUNT = "martisep.thymesup.TEAM_COUNT";
    public final static String TOPICS = "martisep.thymesup.TOPICS";
    public final static String ENTRIES_LIST = "martisep.thymesup.ENTRIES_LIST";
    public final static String SCORE = "martisep.thymesup.SCORE";
    public static final String PLAYER_INDEX = "martisep.thymesup.PLAYER_INDEX";
    public static final String ROUND = "martisep.thymesup.ROUND";
    public static final String TEAM_NAME = "martisep.thymesup.TEAM_NAME";
    private static final int SUMMARY_REQUEST_CODE = 8;
    private static final int FILTER_REQUEST_CODE = 9;
    private static final int NUM_ROUNDS = 3;

    // game variables
    private int team_count;
    private int[] score;
    private String[] team_names;
    private ArrayList<Entry> words;
    private ArrayList<Entry> filtered_words;
    // round variables
    private int round;
    private int remaining_words;
    private ArrayList<Entry> round_words;
    // turn variables
    private int current_team;
    private int current_index;
    private int turn_start_index;
    private int turn_counter;

    //ui elements
    TextView name_text;
    TextView keywords_text;
    ProgressBar progress_bar_countdown;
    CountDownTimer countDownTimer;
    TextView countdown_text;
    Button btn_correct;
    Button btn_skip;
    Button btn_burn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        // get parameters of the game from intent
        Intent intent = getIntent();
        team_count = intent.getIntExtra(TEAM_COUNT,2);
        String[] topics = (intent.getStringArrayListExtra(TOPICS)).toArray(new String[0]);

        // prepare words
        loadWordsFromDB(topics, 20 * team_count);
        // prepare ui elements
        initUIElements();
        //start game
        newGame();
    }

    private void loadWordsFromDB(String[] topics, int word_limit) {
        // get random set of words of given size from database according to selected topics
        DBController dbController = new DBController(this);
        SQLiteDatabase db = dbController.getReadableDatabase();
        String query = "SELECT DISTINCT "   // topics can overlap, filter duplicates
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

    private void initUIElements(){
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
                nextWord();
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
                nextWord();
            }
        });

        progress_bar_countdown =(ProgressBar)findViewById(R.id.progressBar);
        countdown_text = (TextView) findViewById(R.id.coundown);
        if (Build.VERSION.SDK_INT >= 11){
            progress_bar_countdown.setRotation(180); // bar decreases from left to right
        }

        // change every 1000 ms, but check every 500 ms (otherwise the last second will not be updated!)
        countDownTimer = new CountDownTimer(30000,500) {
            @Override
            public void onTick(long millisUntilFinished) {
                progress_bar_countdown.setProgress((int) (millisUntilFinished / 1000 + 1)); //round up
                countdown_text.setText(Integer.toString((int) (millisUntilFinished / 1000 + 1)));
            }
            @Override
            public void onFinish() {
                progress_bar_countdown.setProgress(0);
                countdown_text.setText(Integer.toString(0));
                endTurn();
            }
        };
    }

    private void newGame(){
        score = new int[team_count];
        team_names = new String[team_count];

        // default team names
        for(int i = 0; i < team_count; i++){
            team_names[i] = "Team " + Integer.toString(i);
        }

        current_team = -1;
        round = 0;
        filtered_words = new ArrayList<>();

        // only let players filter if there are at least 6 words per player (12 per team)
        if(words.size() > 12 * team_count){
            callFilterActivity(0); // eventually calls newRound()
        } else{
            filtered_words = words; //TODO stuck with default team names
            newRound();
        }
    }

    private void newRound(){
        round++;
        round_words = new ArrayList<>();
        for(Entry p : filtered_words){
            round_words.add(p.clone());
        }
        Collections.shuffle(round_words);

        // first round it is possible to burn cards (i.e. rules violation), in second and third player can skip cards
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
        newTurn(); // order is important! (turn_counter must be reset to 0 in newTurn())
    }

    private void newTurn(){
        current_team = (current_team + 1) % team_count;
        turn_start_index = current_index;
        turn_counter = 0;

        //reset countdown
        progress_bar_countdown.setProgress(30);
        countdown_text.setText("30");

        //hide texts until countdown started
        name_text.setVisibility(View.INVISIBLE);
        keywords_text.setVisibility(View.INVISIBLE);

        //ready?
        new AlertDialog.Builder(this)
                .setTitle(team_names[current_team])
                .setMessage("Ready?")
                .setCancelable(false)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("Start!", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        name_text.setVisibility(View.VISIBLE);
                        keywords_text.setVisibility(View.VISIBLE);
                        countDownTimer.start();
                    }}).show();
    }

    private void nextWord(){
        if(round_words.size() == 0){ return; }

        turn_counter++; // count how many words the player cycled through
        if (remaining_words == 0){
            endTurn(); // not yet endRound! some words may be returned to play in SummaryActivity
        } else {
            // get next not yet guessed (or burnt) word
            do{
                current_index = (current_index + 1) % round_words.size();
            } while(round_words.get(current_index).getState() != Entry.EntryState.NONE);

            name_text.setText(round_words.get(current_index).getName());
            keywords_text.setText(round_words.get(current_index).getKeywords());
        }
    }

    private void endTurn(){
        countDownTimer.cancel(); //should already be stopped, except in the end of the round

        // play alarm and vibrate
        MediaPlayer mPlayer = MediaPlayer.create(getApplicationContext(), R.raw.fifth_short);
        mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mPlayer.setVolume(1.0f, 1.0f);
        mPlayer.start();

        Vibrator v = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
        long[] pattern = {100,60,110,60,110,60,100, 2000};
        v.vibrate(pattern, -1); // The '-1' here means to vibrate once, as '-1' is out of bounds in the pattern array

        if (remaining_words > 0) {
            new AlertDialog.Builder(this)
                    .setTitle("Discard last word?")
                    .setMessage("Do you want to discard the last word or leave it for the next team?")
                    .setCancelable(false)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setNegativeButton("Discard", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            if(round == 1) {
                                round_words.get(current_index).setState(Entry.EntryState.BURNT);
                                remaining_words--;
                            }
                            nextWord(); //TODO next word can be seen by current player
                            callSummaryActivity();
                        }})
                    .setPositiveButton("Leave", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            callSummaryActivity();
                        }})
                    .show();
        } else {
            callSummaryActivity();
        }
    }

    // let players filter the entries
    private void callFilterActivity(int player){
        Intent filter_intent = new Intent(getApplicationContext(), FilterActivity.class);
        filter_intent.putExtra(PLAYER_INDEX, player);
        filter_intent.putExtra(TEAM_NAME, team_names[player % team_count]);

        ArrayList<Entry> words_to_filter = new ArrayList<>();

        // divide entries among players (2 * team count)
        int num_entries = Math.round((float) words.size() / (2 * team_count));

        for(int i = player*num_entries; i < (player+1)*num_entries && i < words.size(); i++){
            words_to_filter.add(words.get(i));
        }
        filter_intent.putParcelableArrayListExtra(ENTRIES_LIST, words_to_filter);

        try {
            startActivityForResult(filter_intent, FILTER_REQUEST_CODE);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(getBaseContext(), "No filter activity found.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    // let player check which entries he guessed / discarded
    private void callSummaryActivity(){
        Intent summary_intent = new Intent(getApplicationContext(), SummaryActivity.class);
        summary_intent.putExtra(SCORE, score[current_team]);
        summary_intent.putExtra(TEAM_NAME, team_names[current_team]);
        summary_intent.putExtra(ROUND, round);

        // copy only words which player cycled through (without repeat)
        ArrayList<Entry> used_words = new ArrayList<>();
        turn_counter = Math.min(turn_counter,round_words.size());

        for(int i = 0; i < turn_counter; i++){
            used_words.add(round_words.get((turn_start_index + i) % round_words.size()));
        }
        summary_intent.putParcelableArrayListExtra(ENTRIES_LIST, used_words);

        try {
            startActivityForResult(summary_intent, SUMMARY_REQUEST_CODE);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(getBaseContext(), "No summary activity found.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void endRound(){
        // display current score
        StringBuilder stringBuilder = new StringBuilder();
        for(int i = 0; i < team_count; i++){
            stringBuilder.append(team_names[i]).append(": ").append(score[i]).append("\n");
        }
        new AlertDialog.Builder(this)
                .setTitle("End of round " + Integer.toString(round))
                .setMessage("Score:\n" + stringBuilder.toString())
                .setIcon(android.R.drawable.ic_dialog_info)
                .setCancelable(false)
                .setPositiveButton("Next round", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        if (round == NUM_ROUNDS) {
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
                    score[current_team] = data.getIntExtra(GameActivity.SCORE, score[current_team]);

                    ArrayList<Entry> corrected_entries = data.getParcelableArrayListExtra(GameActivity.ENTRIES_LIST);

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
                break;
            case FILTER_REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    ArrayList<Entry> filtered_entries = data.getParcelableArrayListExtra(GameActivity.ENTRIES_LIST);
                    int player = data.getIntExtra(GameActivity.PLAYER_INDEX, 0);
                    team_names[player % team_count] = data.getStringExtra(GameActivity.TEAM_NAME);

                    filtered_words.addAll(filtered_entries);
                    player++;
                    if(player == 2*team_count){
                        newRound();
                    } else {
                        callFilterActivity(player);
                    }
                }
                break;
        }
    }

    @Override
    public void onBackPressed() {
        // do nothing.
    }
}

