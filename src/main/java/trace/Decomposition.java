package trace;

public class Decomposition {
	
	public static SimpleDigraph decomposition(SimpleDigraph G, int r, int a, double[] additionalOutgoing, boolean undirected) {
		G = G.copy();
		G.removeSelfLoops();
		G.keepReachableFrom(r);
		G.removeDirectedLoopsFrom(r);
		double[] D = new double[G.getNumNodes()];
		for(int i=0;i<D.length;i++)
			D[i] = G.getOutDegree(i)+(additionalOutgoing==null?0:additionalOutgoing[i]) + (undirected?G.getInDegree(i):0);
		
		while(true) {
			int changes = 0;
			for(int i=0;i<D.length;i++) {
				if(D[i]!=0 && D[i]<a) {
					for(int j : G.getPredecessors(i)) {
						G.removeEdge(j, i);
						D[j]--;
						changes++;
					}
				}
				if(D[i]!=0 && D[i]<a) {
					for(int j : G.getSuccessors(i)) {
						G.removeEdge(i, j);
						D[j]--;
						changes++;
					}
				}
			}
			G.keepReachableFrom(r);
			for(int i=0;i<D.length;i++)
				D[i] = G.getOutDegree(i)+(additionalOutgoing==null?0:additionalOutgoing[i]) + (undirected?G.getInDegree(i):0);
			if(changes==0)
				break;
		}
		return G;
	}

	public static SimpleDigraph coreDecomposition(SimpleDigraph G, int r, double[] additionalOutgoing, boolean undirected) {
		SimpleDigraph trace = G;
		int a = 0;
		int tmp_a = 0;
		while(true) {
			tmp_a = a+1;
			SimpleDigraph tmp = decomposition(G, r, a, additionalOutgoing, undirected);
			if(tmp.getOutDegree(r)==0)
				break;
			trace = tmp;
			a = tmp_a;
		}
		return trace;
	}
	
	public static SimpleDigraph iterativeCoreDecomposition(SimpleDigraph G, int r, double[] additionalOutgoing, boolean undirected) {
		int prevDegree = G.getNumEdges();
		SimpleDigraph trace = coreDecomposition(G, r, additionalOutgoing, undirected);
		while(true) {
			int degree = trace.getNumEdges();
			if(prevDegree==degree)
				break;
			prevDegree = degree;
			trace = coreDecomposition(trace, r, additionalOutgoing, undirected);
		}
		return trace;
	}
}
