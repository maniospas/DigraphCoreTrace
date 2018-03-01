package trace;
import java.util.Stack;

public class Trace {
	public static double ELOD(SimpleDigraph subgraph, SimpleDigraph supergraph, double a) {
		double ELOD = 0;
		for(int i=0;i<subgraph.getNumNodes();i++)
			if(subgraph.getInDegree(i)!=0 || subgraph.getOutDegree(i)!=0)
				ELOD += supergraph.getOutDegree(i)-a;
		return ELOD;
	}
	
	public static SimpleDigraph MEPT(SimpleDigraph G, int r, double a, double[] originalOutDegree) {
		G = G.copy();
		SimpleDigraph T = G.copyEmpty();
		G.removeSelfLoops();
		G.keepReachableFrom(r);
		G.removeDirectedLoopsFrom(r);
		double[] D = new double[G.getNumNodes()];
		for(int i=0;i<D.length;i++)
			D[i] = Double.NEGATIVE_INFINITY;
		double[] originalD = new double[D.length];
		for(int i=0;i<D.length;i++)
			originalD[i] = originalOutDegree!=null?originalOutDegree[i]:G.getOutDegree(i);
		D[r] = originalD[r];
		int[] requiresCalc = new int[G.getNumNodes()];
		for(int i=0;i<requiresCalc.length;i++)
			requiresCalc[i] = G.getInDegree(i);
		Stack<Integer> pending = new Stack<Integer>();
		pending.push(r);
		while(!pending.isEmpty()) {
			int v = pending.pop();
			for(int u : G.getSuccessors(v)) {
				if(D[u]<originalD[u]+D[v]-a && requiresCalc[u]>0) {
					D[u] = originalD[u]+D[v]-a;
					T.removeNodePredecessors(u);
					T.addEdge(v, u);
				}
				requiresCalc[u]--;
				if(requiresCalc[u]==0)
					pending.push(u);
			}
		}

		T.metaScore = new Double[T.getNumNodes()];
		for(int i=0;i<T.metaScore.length;i++)
			T.metaScore[i] = D[i];
		return T;
	}
	
	public static SimpleDigraph trace(SimpleDigraph G, int r, double a, double[] additionalOutgoing) {
		G = G.copy();
		G.removeSelfLoops();
		G.keepReachableFrom(r);
		G.removeDirectedLoopsFrom(r);
		double[] D = new double[G.getNumNodes()];
		for(int i=0;i<D.length;i++)
			D[i] = G.getOutDegree(i)+(additionalOutgoing==null?0:additionalOutgoing[i]);
		SimpleDigraph T = MEPT(G, r, a, D);
		Stack<Integer> pending = new Stack<Integer>();
		for(int i=0;i<D.length;i++)
			if(T.getOutDegree(i)==0)
				pending.push(i);
		int[] requiresCalc = new int[G.getNumNodes()];
		for(int i=0;i<requiresCalc.length;i++) 
			requiresCalc[i] = T.getOutDegree(i);
		
		while(!pending.isEmpty()) {
			int v = pending.pop();
			int[] preds = T.getPredecessors(v);
			if(preds.length>1)
				throw new RuntimeException("MEPT should always yield a tree (i.e. each node should have at most one predecessor)");
			else if(preds.length==1) {
				int p = preds[0];
				if(requiresCalc[p]>0) {
					D[p] += Math.max(0, D[v]-a);
					requiresCalc[p]--;
					if(requiresCalc[p]==0)
						pending.add(p);
				}
			}
		}

		SimpleDigraph trace = G.copyEmpty();
		trace.metaScore = new Double[trace.getNumNodes()];
		for(int i=0;i<trace.metaScore.length;i++)
			trace.metaScore[i] = D[i];
		pending.clear();//should be empty already
		pending.add(r);
		
		while(!pending.isEmpty()) {
			int v = pending.pop();
			if(D[v]>a) {
				int[] preds = T.getPredecessors(v);
				if(preds.length==1)
					trace.addEdge(preds[0], v);
				else if(v!=r)
					throw new RuntimeException("MEPT should always yield a tree (i.e. each node should have at most one predecessor)");
				for(int u : T.getSuccessors(v))
					pending.push(u);
			}
		}
		return trace;
	}
	
	public static SimpleDigraph coreTrace(SimpleDigraph G, int r, double[] additionalOutgoing) {
		return coreTrace(G, r, additionalOutgoing, Integer.MAX_VALUE);
	}
	
	public static SimpleDigraph coreTrace(SimpleDigraph G, int r, double[] additionalOutgoing, int a_max) {
		SimpleDigraph trace = G;
		double a = 0;
		double tmp_a = 0;
		while(true) {
			tmp_a = a+1;
			if(a>a_max)
				break;
			SimpleDigraph tmp = Trace.trace(G, r, a, additionalOutgoing);
			if(tmp.getOutDegree(r)==0)
				break;
			trace = tmp;
			a = tmp_a;
		}
		if(trace!=null && trace!=G)
			trace.removeSelfLoops();
		else
			trace = new TrivialDigraph(G.getNumNodes(), r);
		return trace;
	}
}
