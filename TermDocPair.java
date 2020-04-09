import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableComparable;

public class TermDocPair implements Writable, WritableComparable<TermDocPair>{
	private String term;
	private String docName;
	
	public TermDocPair() {}
	public TermDocPair(String term, String docName) {
		this.term = term;
		this.docName = docName;
	}
	
	@Override
	public int compareTo(TermDocPair pair) {
		int termCompare = this.term.compareTo(pair.term);
		if(termCompare == 0) {
			int docNameCompare = this.docName.compareTo(pair.docName);
			return docNameCompare;
		}
		else
			return termCompare;
	}
	
	public String getTerm() {
		return term;
	}
	
	public String getDocName() {
		return docName;
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		term = in.readUTF();
		docName = in.readUTF();
	}

	@Override
	public void write(DataOutput out) throws IOException {
		out.writeUTF(term);;
		out.writeUTF(docName);
	}
	
	@Override
	public String toString() {
		return term + "\t" + docName;
	}
}
