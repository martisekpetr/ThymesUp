package martisep.thymesup;

import android.os.Parcel;
import android.os.Parcelable;

public class Entry implements Parcelable{
    private String name;
    private String keywords;
    private boolean guessed;

    public Entry(String name, String keywords){
        this.name = name;
        this.keywords = keywords;
        this.guessed = false;
    }

    public String getName(){
        return name;
    }

    public String getKeywords() {
        return keywords;
    }

    public boolean isGuessed() {
        return guessed;
    }

    public void setGuessed(boolean guessed) {
        this.guessed = guessed;
    }

    public String toString(){
        return name + " ( " + keywords + " )";
    }

    public Entry clone(){
        Entry p = new Entry(this.name, this.keywords);
        p.guessed = this.guessed;
        return p;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(keywords);
        dest.writeByte((byte) (guessed ? 0x01 : 0x00));
    }

    protected Entry(Parcel in) {
        name = in.readString();
        keywords = in.readString();
        guessed = in.readByte() != 0x00;
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
