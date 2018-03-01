package trace;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map.Entry;

public class SparceMatrix implements Serializable {
	private static final long serialVersionUID = -7526411834318494492L;
	private int size = 0;
	private HashMap<Integer, Double> contents = new HashMap<Integer, Double>();
	
	public SparceMatrix(int size) {
		this.size = size;
	}
	public SparceMatrix copy() {
		SparceMatrix ret = new SparceMatrix(size);
		for(Entry<Integer, Double> content : contents.entrySet())
			ret.contents.put(content.getKey(), content.getValue());
		return ret;
	}
	public int size() {
		return size;
	}
	protected int convertToIdx(int x, int y) {
		if(x<0 || y<0 || x>=size || y>=size)
			throw new RuntimeException("Invalid matrix index");
		return y*size+x;
	}
	public void set(int x, int y, double val) {
		if(val==0)
			contents.remove(convertToIdx(x, y));
		else
			contents.put(convertToIdx(x, y), val);
	}
	public double get(int x, int y) {
		Double ret = contents.get(convertToIdx(x, y));
		if(ret==null)
			return 0;
		return (double)ret;
	}
	public int countNonZeros() {
		return contents.size();
	}
}
