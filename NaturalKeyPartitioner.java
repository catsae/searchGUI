import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.mapreduce.Partitioner;

public class NaturalKeyPartitioner extends Partitioner<TermDocPair, IntWritable>{
	@Override
	public int getPartition(TermDocPair key, IntWritable value, int numPartitions) {
		return Math.abs(key.getTerm().hashCode() % numPartitions);
	}
	
}
