import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapreduce.lib.db.DBWritable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


public class DBOutputWritable implements Writable, DBWritable {
    private String starting_phrase;
    private String follow_word;
    private int count;

    public DBOutputWritable(String starting_phrase, String follow_word, int count){
        this.starting_phrase =starting_phrase;
        this.follow_word = follow_word;
        this.count=count;
    }
    /**
     * Retrieves the value of the designated column in the current row
     */
    public void readFields(ResultSet result) throws SQLException{
        starting_phrase = result.getString(1);
        follow_word = result.getString(2);
        count =result.getInt(3);
    }
    public void write(PreparedStatement ps) throws SQLException{
        ps.setString(1, starting_phrase);
        ps.setString(2, follow_word);
        ps.setInt(3,count);
    }
    public void readFields(DataInput arg0) throws IOException{

    }
    public void write(DataOutput arg0) throws IOException{

    }


}
