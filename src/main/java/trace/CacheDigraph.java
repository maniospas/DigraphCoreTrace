package trace;

import java.util.HashMap;

public class CacheDigraph extends SimpleDigraph {
	private static final long serialVersionUID = -6325985952030163464L;
	private HashMap<Integer, int[]> quickSuccessors = new HashMap<Integer, int[]>();
	private HashMap<Integer, int[]> quickPredecessors = new HashMap<Integer, int[]>();
	private HashMap<Integer, Integer> quickOutDegree = new HashMap<Integer, Integer>();
	private HashMap<Integer, Integer> quickInDegree = new HashMap<Integer, Integer>();

	public CacheDigraph(double[][] A) {
		super(A);
	}
	public CacheDigraph(SparceMatrix A) {
		super(A);
	}
	protected CacheDigraph(int numNodes) {
		super(numNodes);
	}
	@Override
	synchronized public CacheDigraph copy() {
		CacheDigraph ret = new CacheDigraph(A);
		ret.quickSuccessors = new HashMap<Integer, int[]>(quickSuccessors);
		ret.quickPredecessors = new HashMap<Integer, int[]>(quickPredecessors);
		ret.quickOutDegree = new HashMap<Integer, Integer>(quickOutDegree);
		ret.quickInDegree = new HashMap<Integer, Integer>(quickInDegree);
		return ret;
	}
	@Override
	public SimpleDigraph copyEmpty() {
		return new CacheDigraph(getNumNodes());
	}
	
	private void resetMetadata() {
		quickSuccessors.clear();
		quickPredecessors.clear();
		quickOutDegree.clear();
		quickInDegree.clear();
	}
	
	@Override
	synchronized public void setEdge(int from, int to, double value) {
		super.setEdge(from, to, value);
		resetMetadata();
	}
	
	synchronized public int getOutDegree(int from) {
		Integer ret = quickOutDegree.get(from);
		if(ret==null)
			quickOutDegree.put(from, ret = super.getOutDegree(from));
		return ret;
	}
	synchronized public int getInDegree(int to) {
		Integer ret = quickInDegree.get(to);
		if(ret==null)
			quickInDegree.put(to, ret = super.getInDegree(to));
		return ret;
	}
	synchronized public int[] getSuccessors(int from) {
		int [] ret = quickSuccessors.get(from);
		if(ret==null)
			quickSuccessors.put(from, ret = super.getSuccessors(from));
		return ret;
	}
	synchronized public int[] getPredecessors(int to) {
		int [] ret = quickPredecessors.get(to);
		if(ret==null)
			quickPredecessors.put(to, ret = super.getPredecessors(to));
		return ret;
	}
}
