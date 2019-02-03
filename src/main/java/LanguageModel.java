import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;



public class LanguageModel {
    public static class Map extends Mapper<LongWritable, Text, Text, Text> {
        int minCount;

        public void setup(Context context) {
            Configuration conf = context.getConfiguration();
            minCount = conf.getInt("minCount", 5);
        }

        //ex input: this is cool \t20
        public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
            if ((value == null || value.toString().trim().length() == 0)) return;
            String thisLine = value.toString().trim();
            String[] wordsAndCount = thisLine.split("\t"); //split by tab
            String[] words = wordsAndCount[0].split("\\s+"); //split each word by space
            int count = Integer.valueOf(wordsAndCount[wordsAndCount.length - 1]);
            if ((wordsAndCount.length < 2 || count <= minCount)) return;

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < words.length - 1; i++) {
                sb.append(words[i]).append(" ");
            }
            String outputKey = sb.toString().trim();
            String outputValue = words[words.length - 1];
            if (!((outputKey == null || outputKey.length() < 1))) {
                context.write(new Text(outputKey), new Text(outputValue + "=" + count)); //key: is, value: smart = 12
            }
        }
    }
    //ex input: key: this is value:cool=20
    public static class Reduce extends Reducer<Text, Text, DBOutputWritable, NullWritable> {
        int numWordsFollowingInput;
        public void setup(Reducer.Context context) {
            Configuration conf = context.getConfiguration();
            numWordsFollowingInput = conf.getInt("numWordFollowingInput", 5);
        }
        public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException{
            //sort key in descending order
            TreeMap <Integer, List<String>> map = new TreeMap<Integer, List<String>>(Collections.reverseOrder());
            for(Text val: values){
                String curVal= val.toString().trim();
                String word = curVal.split("=")[0].trim();

                int count = Integer.parseInt(curVal.split("=")[1].trim());
                if(map.containsKey(count)){
                    map.get(count).add(word);
                }else{
                    List<String> list = new ArrayList<String>();
                    list.add(word);
                    map.put(count, list);
                }
            }
            //<<50, <girl, bird>, 40 <boy, ancle>>
            Iterator<Integer> iter = map.keySet().iterator();
            for(int i=0; iter.hasNext() && i < numWordsFollowingInput; i++){
                int keyCount = iter.next();
                List<String> words = map.get(keyCount);

                //write to database
                for(String curWord: words){
                    context.write(new DBOutputWritable(key.toString(), curWord, keyCount), NullWritable.get());
                    i++;
                }
            }

        }

    }
}
