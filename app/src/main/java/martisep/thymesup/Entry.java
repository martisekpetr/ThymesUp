package martisep.thymesup;

import android.os.Parcel;
import android.os.Parcelable;

public class Entry implements Parcelable{
    public enum EntryState {GUESSED, BURNT, NONE};

    private String name;
    private String keywords;
    private EntryState state;

    public Entry(String name, String keywords){
        this.name = name;
        this.keywords = keywords;
        this.state = EntryState.NONE;
    }

    public String getName(){
        return name;
    }

    public String getKeywords() {
        return keywords;
    }

    public EntryState getState(){
        return state;
    }
    public void setState(EntryState state){
        this.state = state;
    }
    public boolean isGuessed(){
        return state == EntryState.GUESSED;
    }

    public String toString(){
        return name + " ( " + keywords + " )";
    }

    public Entry clone(){
        Entry p = new Entry(this.name, this.keywords);
        p.state = this.state;
        return p;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(keywords);
        dest.writeSerializable(state);
    }

    protected Entry(Parcel in) {
        name = in.readString();
        keywords = in.readString();
        state = (EntryState) in.readSerializable();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<Entry> CREATOR = new
            Parcelable.Creator<Entry>() {
                public Entry createFromParcel(Parcel in) {
                    return new Entry(in);
                }

                public Entry[] newArray(int size) {
                    return new Entry[size];
                }
            };

    public Parcel createParcel() {
        Parcel parcel = Parcel.obtain();
        writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        return parcel;
    }
}
