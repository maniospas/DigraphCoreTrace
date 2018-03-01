import trace.Decomposition;
import trace.SimpleDigraph;
import trace.Trace;

public class Benchmark {
	public static void main(String[] args) {
		int numItterations = 1000;
		double tracePrecision = 0;
		double traceSize = 0;
		double traceSize2 = 0;
		double decompositionPrecision = 0;
		double decompositionSize = 0;
		double decompositionSize2 = 0;
		double traceELOD = 0;
		double decompositionELOD = 0;
		double size = 0;
		SimpleDigraph G = new SimpleDigraph(1);
		SimpleDigraph trace = new SimpleDigraph(1);
		for(int itteration=0;itteration<numItterations;itteration++) {
			int coreNodes = 3+(int)(Math.random()*8);
			int additionalNodes = 47+(int)(Math.random()*141);
			double[] wOut = new double[coreNodes+additionalNodes];
			double[] wIn = new double[coreNodes+additionalNodes];
			double betaOut = 0.5+Math.random()*(3.2-0.5);
			double betaIn = 0.5+Math.random()*(betaOut-0.5);
			double cOut =0;
			double cIn = 0;
			double avgOutDeg = 5+(int)(Math.random()*11);
			double avgInDeg = 5+(int)(Math.random()*11);
			for(int i=0;i<wOut.length;i++) {
				wIn[i] = Math.exp(-betaIn*Math.random());
				wOut[i] = Math.exp(-betaOut*Math.random());
				cOut += wOut[i];
				cIn += wIn[i];
			}
			for(int i=0;i<wOut.length;i++) {
				wOut[i] /= cOut/avgOutDeg;
				wIn[i] /= cIn/avgInDeg;
			}
			G = new SimpleDigraph(wOut.length);
			for(int i=0;i<wOut.length;i++)
				for(int j=0;j<wIn.length;j++)
					if(Math.random()<wOut[i] && Math.random()<wIn[j])
						G.addEdge(i, j);
			G.removeSelfLoops();
			
			for(int node=0;node<coreNodes-1;node++)
				G.addEdge(node, node+1);
			/*for(int link=0;link<links;link++) {
				int from = (int)((1-Math.exp(-Math.random()))*G.getNumNodes());
				int to = (int)(Math.random()*G.getNumNodes());
				G.addEdge(from, to);
			}*/
			/*double recall = 0;
			for(int node=0;node<coreNodes-1;node++)
				if(trace.hasEdge(node, node+1))
					recall += 1;
			totalRecall += recall/(coreNodes-1);*/
			G.keepReachableFrom(0);
			{
				trace = Decomposition.coreDecomposition(G, 0, null, true);
				double precision = 0;
				for(int node=0;node<coreNodes-1;node++)
					if(trace.hasEdge(node, node+1))
						precision += 1;
				decompositionPrecision += precision/(coreNodes-1);
				decompositionSize += trace.countNumNodes()/(double)G.countNumNodes();
				decompositionSize2 += trace.countNumNodes()/(double)coreNodes;
				decompositionELOD += Trace.ELOD(trace, G, 2); 
			}
			{
				trace = Trace.coreTrace(G, 0, null);
				double precision = 0;
				for(int node=0;node<coreNodes-1;node++)
					if(trace.hasEdge(node, node+1))
						precision += 1;
				tracePrecision += precision/(coreNodes-1);
				traceSize += (trace.getNumEdges()+1)/(double)G.countNumNodes();
				traceSize2 += (trace.getNumEdges()+1)/(double)coreNodes;
				traceELOD += Trace.ELOD(trace, G, 2); 
			}
			size += G.getNumEdges();
		}
		tracePrecision /= numItterations;
		decompositionPrecision /= numItterations;
		traceSize /= numItterations;
		decompositionSize /= numItterations;
		traceSize2 /= numItterations;
		decompositionSize2 /= numItterations;
		size /= numItterations;
		traceELOD /= numItterations;
		decompositionELOD /= numItterations;
		System.out.println("Trace Precision: "+Math.round(100*tracePrecision)+"%");
		System.out.println("Decomposition Precision: "+Math.round(100*decompositionPrecision)+"%");
		System.out.println("Trace Size vs Appended: "+Math.round(100*traceSize2)+"%");
		System.out.println("Decomposition Size vs Appended: "+Math.round(100*decompositionSize2)+"%");
		System.out.println("Trace Size: "+Math.round(100*traceSize)+"%");
		System.out.println("Decomposition Size: "+Math.round(100*decompositionSize)+"%");
		System.out.println("Trace ELOD: "+(traceELOD));
		System.out.println("Decomposition ELOD: "+(decompositionELOD));
		System.out.println("Avg Size: "+(int)size);
		//System.out.println("F-measure: "+Math.round(200*totalPrecision*totalRecall/(totalPrecision+totalRecall))+"%");
		
	}
}
