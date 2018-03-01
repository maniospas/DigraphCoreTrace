package trace;

public class TrivialDigraph extends SimpleDigraph {
	private static final long serialVersionUID = -2110650028977707221L;
	private int singleNode;
	public TrivialDigraph(int numNodes, int singleNode) {
		super(numNodes);
		this.singleNode = singleNode;
	}
	@Override
	public boolean canAddEdgeToJungGraph(int node) {
		return super.canAddEdgeToJungGraph(node) || node==singleNode;
	}
}
