package it.unipi.cc.mapreduce;

import it.unipi.cc.Driver;
import it.unipi.cc.model.IntArrayWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;

import java.io.IOException;

public class ParameterCalibration {
    public static class PCMapper extends Mapper<Object, Text, IntWritable, IntWritable> {

        private static int n_rates;
        private static int[] counter;

        private static final IntWritable outputKey = new IntWritable();
        private static final IntWritable outputVal = new IntWritable();

        @Override
        protected void setup(Context context) {
            n_rates = Integer.parseInt(context.getConfiguration().get("n_rates"));
            counter = new int[n_rates];
        }

        @Override
        public void map(Object key, Text value, Context context) {
            String record = value.toString();
            if (record == null || record.startsWith("tconst"))
                return;
            String[] tokens = record.split("\t");

            int rate = Math.round(Float.parseFloat(tokens[1])); // <title, rating, numVotes>
            counter[rate - 1]++;
        }

        @Override
        protected void cleanup(Context context) throws IOException, InterruptedException {
            for(int i=0; i<n_rates; i++) {
                outputKey.set(i + 1);
                outputVal.set(counter[i]);
                context.write(outputKey, outputVal);
            }
        }
    }

    public static class PCReducer extends Reducer<IntWritable, IntWritable, IntWritable, IntArrayWritable> {

        private static double p;
        private static IntWritable[] arr;    // for m and k
        private static final IntArrayWritable params = new IntArrayWritable();

        @Override
        protected void setup(Context context) {

            p = context.getConfiguration().getDouble("p", 0.01);
            arr = new IntWritable[2];
        }

        @Override
        public void reduce(IntWritable key, Iterable<IntWritable> values, Context context) throws IOException, InterruptedException {
            int n = 0;
            while(values.iterator().hasNext())
                n += values.iterator().next().get();

            int m = (int) (- (n * Math.log(p)) / (Math.pow(Math.log(2),2)));
            int k = (int) ((m/n) * Math.log(2));

            arr[0] = new IntWritable(m);
            arr[1] = new IntWritable(k);
            params.set(arr);
//            Driver.print(String.valueOf(params));
            context.write(key, params);
        }
    }

}
