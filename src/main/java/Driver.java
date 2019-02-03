import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.lib.db.DBConfiguration;
import org.apache.hadoop.mapred.lib.db.DBOutputFormat;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

import java.io.IOException;


public class Driver {
    public static void main(String[] args) throws ClassNotFoundException, IOException, InterruptedException{
        Configuration conf1= new Configuration();
        //seperate the text input into sentence using "." as delimeter;
        conf1.set("textinputformat.record.delimiter", ".");
        conf1.set("numGram", args[2]);


        //first job - build nGram library
        Job job1 = Job.getInstance(conf1);
        job1.setJobName("BuildNGram");
        job1.setJarByClass(Driver.class);
        job1.setMapperClass(NGramLibraryBuilder.NGramMapper.class);
        job1.setReducerClass(NGramLibraryBuilder.NGramReducer.class);
        job1.setOutputKeyClass(Text.class);
        job1.setOutputValueClass(IntWritable.class);
        job1.setInputFormatClass(TextInputFormat.class);
        job1.setOutputFormatClass(TextOutputFormat.class);

        TextInputFormat.setInputPaths(job1, new Path(args[0]));
        TextOutputFormat.setOutputPath(job1, new Path(args[1]));
        job1.waitForCompletion(true);//second job execute after first job finishes

        //second job = build language model
        Configuration conf2 = new Configuration();
        conf2.set("minCount", args[3]); //words that appear less than minCount will be ignore
        conf2.set("numWordsFollowingInput", args[4]); // ex. numWordsFollowingInput=3, then "I love to eat" will be display as search result
        //DB connection setting
        DBConfiguration.configureDB(conf2, "com.mysql.jdbc.Driver", "jdbc:mysql:", "", "");

        Job job2 = Job.getInstance(conf2);
        job2.setJobName("BuildLanguageModel");
        job2.setJarByClass(Driver.class);

        job2.addArchiveToClassPath(new Path("/mysql/mysql-connector-java-5.1.39-bin.jar"));

        job2.setMapOutputKeyClass(Text.class);
        job2.setMapOutputValueClass(Text.class);
        job2.setOutputKeyClass(Text.class);
        job2.setOutputValueClass(NullWritable.class);

        job2.setMapperClass(LanguageModel.Map.class);
        job2.setReducerClass(LanguageModel.Reduce.class);

        job2.setInputFormatClass(TextInputFormat.class);
        job2.setOutputFormatClass(DBOutputFormat.class);

        DBOutputFormat.setOutput(
                job2,
                "output",
                new String[] {"starting_phrase", "following_word", "count"} //table columns
        );

        TextInputFormat.setInputPaths(job2, new Path(args[1]));//input of job2 is output of job1
        System.exit(job2.waitForCompletion(true)? 0:1);//system exit when job 2 complete






    }
}
