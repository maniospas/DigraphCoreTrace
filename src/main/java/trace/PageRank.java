package trace;


public class PageRank {
	public static double[] obtainRanks(double[][] M) {
		return obtainRanks(M, 0.85);
	}
	public static double[] obtainRanks(double[][] M, double d) {
		double[] R = new double[M.length];
		for(int i=0;i<R.length;i++)
			R[i] = 1;
		double[] outgoingLinks = new double[R.length];
		double[] nextR = new double[R.length];
		for(int iteration=0;iteration<50;iteration++) {
			//calculate outgoing nodes
			for(int i=0;i<outgoingLinks.length;i++) {
				outgoingLinks[i] = 0;
				for(int j=0;j<outgoingLinks.length;j++)
					outgoingLinks[i] += M[i][j];
			}
			//update ranks
			for(int i=0;i<R.length;i++) {
				nextR[i] = 0;
				for(int j=0;j<R.length;j++)
					nextR[i] += M[j][i]*R[j]/outgoingLinks[i];
			}
			for(int i=0;i<R.length;i++) 
				R[i] = nextR[i]*d+(1-d)/R.length;
		}
		return R;
	}
}
