import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;

import java.io.IOException;



public class NGramLibraryBuilder {
    public static class NGramMapper extends Mapper<LongWritable, Text, Text, IntWritable> {
        int numGram;

        public void setup(Context context) {
            Configuration conf = context.getConfiguration();
            numGram = conf.getInt("numGram", 3);
        }

        public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
            String thisLine = value.toString().trim().toLowerCase().replaceAll("[^a-z]+", "");
            String[] wordsInLine = thisLine.split("\\s+");//group all whilespace as delimiter
            if (wordsInLine.length < 2) return;

            StringBuilder sb;

            for (int i = 0; i < wordsInLine.length - 1; i++) {
                sb = new StringBuilder();
                for (int j = 0; i + j < wordsInLine.length && j < numGram; j++) {
                    sb.append(" "); //add whitespace before each word
                    sb.append(wordsInLine[i + j]);
                    context.write(new Text(sb.toString().trim()), new IntWritable(1)); //whitespace before first word will be trimmed.
                }
            }

        }
    }
    public static class NGramReducer extends Reducer<Text, IntWritable, Text, IntWritable> {
        public void reduce(Text key, Iterable<IntWritable> values, Context context) throws IOException, InterruptedException{
                int sum = 0;
                for(IntWritable value: values){
                    sum+= value.get();
                }
                context.write(key, new IntWritable((sum)));
        }
    }
}


