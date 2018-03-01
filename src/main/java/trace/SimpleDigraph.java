package trace;
import java.awt.Dimension;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Stack;
import javax.swing.JFrame;
import javax.swing.ToolTipManager;

import edu.uci.ics.jung.algorithms.layout.ISOMLayout;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.algorithms.layout.TreeLayout;
import edu.uci.ics.jung.graph.DelegateForest;
import edu.uci.ics.jung.graph.Forest;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.SparseGraph;
import edu.uci.ics.jung.graph.util.EdgeType;
import edu.uci.ics.jung.visualization.BasicVisualizationServer;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;

public class SimpleDigraph implements Serializable {
	private static final long serialVersionUID = 1315984429773126628L;
	protected SparceMatrix A;
	public Double[] metaScore;

	public SimpleDigraph(int numNodes) {
		A = new SparceMatrix(numNodes);
	}
	public SimpleDigraph(double[][] A) {
		if(A.length!=A[0].length)
			throw new RuntimeException("Digraph can only be constructed from a square matrix");
		this.A = new SparceMatrix(A.length);
		for(int i=0;i<A.length;i++)
			for(int j=0;j<A.length;j++)
				this.A.set(i, j, A[i][j]);
	}
	public SimpleDigraph(SparceMatrix A) {
		this.A = A.copy();
	}
	public SimpleDigraph copy() {
		return new SimpleDigraph(A);
	}
	public SimpleDigraph copyEmpty() {
		return new SimpleDigraph(getNumNodes());
	}
	public void removeDirectedLoopsFrom(int root) {
		SimpleDigraph tmp = new SimpleDigraph(A.size());
		Stack<Integer> pending = new Stack<Integer>();
		Integer node = root;
		while(node!=null) {
			//System.out.println("Node "+node);
			for(int succ : getSuccessors(node)) 
				if(!tmp.hasEdge(succ, node)) {
					//System.out.println("Has child "+node);
					pending.push(succ);
					tmp.addEdge(node, succ);
					for(int pred : tmp.getPredecessors(node)) 
						tmp.addEdge(pred, succ);
				}
				else
					removeEdge(node, succ);
			node = pending.isEmpty()?null:pending.pop();
		}
	}
	public void keepReachableFrom(int root) {
		SimpleDigraph tmp = new SimpleDigraph(A.size());
		Stack<Integer> pending = new Stack<Integer>();
		Integer node = root;
		while(node!=null) {
			for(int succ : getSuccessors(node)) 
				if(!tmp.hasEdge(node, succ)) {
					tmp.addEdge(node, succ);
					pending.push(succ);
				}
			node = pending.isEmpty()?null:pending.pop();
		}
		this.A = tmp.A;
	}
	public void removeSelfLoops() {
		for(int i=0;i<A.size();i++)
			A.set(i, i, 0);
	}
	public void removeNode(int node) {
		for(int i=0;i<A.size();i++) {
			A.set(i, node, 0);
			A.set(node, i, 0);
		}
	}
	public int getNumEdges() {
		int ret = 0;
		for(int i=0;i<A.size();i++)
			for(int j=0;j<A.size();j++)
				if(hasEdge(i,j))
					ret += 1;
		return ret;
	}
	public void removeNodePredecessors(int node) {
		for(int i=0;i<A.size();i++) 
			removeEdge(i, node);
	}
	public void setEdge(int from, int to, double value) {
		A.set(from, to, value);
	}
	public void addEdge(int from, int to) {
		setEdge(from, to, 1);
	}
	public void removeEdge(int from, int to) {
		setEdge(from, to, 0);
	}
	public double getEdge(int from, int to) {
		return A.get(from, to);
	}
	public boolean hasEdge(int from, int to) {
		return A.get(from, to)!=0;
	}
	public int getOutDegree(int from) {
		int n = 0;
		for(int i=0;i<A.size();i++)
			if(A.get(from, i)!=0)
				n++;
		return n;
	}
	public int getInDegree(int to) {
		int n = 0;
		for(int i=0;i<A.size();i++)
			if(A.get(i, to)!=0)
				n++;
		return n;
	}
	public int[] getSuccessors(int from) {
		int n = 0;
		for(int i=0;i<A.size();i++)
			if(A.get(from, i)!=0)
				n++;
		int [] ret = new int[n];
		n = 0;
		for(int i=0;i<A.size();i++) {
			if(A.get(from, i)!=0) {
				ret[n] = i;
				n++;
			}
		}
		return ret;
	}
	public int[] getPredecessors(int to) {
		int n = 0;
		for(int i=0;i<A.size();i++)
			if(A.get(i, to)!=0)
				n++;
		int [] ret = new int[n];
		n = 0;
		for(int i=0;i<A.size();i++) {
			if(A.get(i, to)!=0) {
				ret[n] = i;
				n++;
			}
		}
		return ret;
	}
	public int getNumNodes() {
		return A.size();
	}
	@Override
	public String toString() {
		String ret = "";
		for(int i=0;i<A.size();i++) {
			for(int j=0;j<A.size();j++) {
				ret += (int)A.get(i, j)+" ";
			}
			ret += "\n";
		}
		return ret;
	}
	
	public int countNumNodes() {
		int count = 0;
		for(int i=0;i<A.size();i++) 
			if(getOutDegree(i)!=0 || getInDegree(i)!=0)
				count++;
		return count;
	}

	public boolean canAddEdgeToJungGraph(int node) {
		return getOutDegree(node)!=0 || getInDegree(node)!=0;
	}
	
	public Graph<String, String> convertToJungGraph(String[] nodeNames) {
		if(nodeNames==null) {
			nodeNames = new String[A.size()];
			for(int i=0;i<A.size();i++) 
				nodeNames[i] = ""+i;
		}
		else {
			String[] tmp = new String[nodeNames.length];
			for(int i=0;i<tmp.length;i++)
				tmp[i] = nodeNames[i];
			nodeNames = tmp;
		}
		if(metaScore!=null)
			for(int i=0;i<A.size();i++) 
				if(metaScore[i]!=null)
					nodeNames[i] += " score: "+metaScore[i];
		Graph<String, String> ig = new SparseGraph<String, String>();
		for(int i=0;i<A.size();i++) 
			if(canAddEdgeToJungGraph(i))
				ig.addVertex(nodeNames[i]);
		for(int i=0;i<A.size();i++) 
			for(int j : getSuccessors(i))
				ig.addEdge(nodeNames[i]+"->"+nodeNames[j], nodeNames[i], nodeNames[j], EdgeType.DIRECTED);
		return ig;
	}

	public Forest<String, String> convertToJungTree(String[] nodeNames, int root) {
		SimpleDigraph G = this.copy();
		G.keepReachableFrom(root);
		if(nodeNames==null) {
			nodeNames = new String[A.size()];
			for(int i=0;i<A.size();i++) 
				nodeNames[i] = ""+i;
		}
		else {
			String[] tmp = new String[nodeNames.length];
			for(int i=0;i<tmp.length;i++)
				tmp[i] = nodeNames[i];
			nodeNames = tmp;
		}
		Forest<String, String> ig = new DelegateForest<String, String>();
		for(int i=0;i<A.size();i++) 
			if(G.canAddEdgeToJungGraph(i))
				ig.addVertex(nodeNames[i]);
		for(int i=0;i<A.size();i++) 
			for(int j : G.getSuccessors(i))
				ig.addEdge(nodeNames[i]+"->"+nodeNames[j], nodeNames[i], nodeNames[j]);
		return ig;
	}
	
	public JFrame visualize(String name, String[] nodeNames, String[] tooltips, int root) {
		 JFrame frame = new JFrame(name);
		 frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		 frame.getContentPane().add(getJungVisualizationServer(nodeNames, tooltips, 800, 600, root));
		 frame.pack();
		 frame.setVisible(true);
		 return frame;
	}

	public BasicVisualizationServer<String, String> getJungVisualizationServer(String[] nodeNames, String[] tooltips, int width, int height, int root) {
		final HashMap<String, String> tooltip = new HashMap<String, String>();
		if(tooltips!=null && nodeNames!=null)
			for(int i=0;i<nodeNames.length;i++)
				tooltip.put(nodeNames[i], tooltips[i]);
		Graph<String, String> graph = root==-1?convertToJungGraph(nodeNames):convertToJungTree(nodeNames, root);
		if(graph.getEdgeCount()<1)
			return null;
		Layout<String, String> layout = root==-1?new ISOMLayout<String, String>(graph):new TreeLayout<String, String>((Forest<String, String>)graph, width/4, 50);
		VisualizationViewer<String, String> vv = new VisualizationViewer<String, String>(layout, new Dimension(width,height));
		vv.setPreferredSize(new Dimension(width,height));
	    final DefaultModalGraphMouse<String, Number> graphMouse = new DefaultModalGraphMouse<String, Number>();
	    vv.setGraphMouse(graphMouse);
	    graphMouse.setMode(edu.uci.ics.jung.visualization.control.ModalGraphMouse.Mode.PICKING);
	    vv.getRenderContext().setVertexLabelTransformer(new ToStringLabeller());
	    
	    /*vv.getRenderContext().setVertexLabelRenderer(new VertexLabelRenderer() {
			public <V> Component getVertexLabelRendererComponent(JComponent vv, Object value, Font font, boolean isSelected, V vertex) {
				if(!isSelected || tooltip.get(vertex.toString())==null)
					return new JLabel(vertex.toString());
				else {
					return new JLabel("<html>"+vertex.toString()+"<br/>"+stringToHTMLString(tooltip.get(vertex.toString()))+"</html>");
				}
			}
	    	
	    });*/
	    ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);
	    com.google.common.base.Function<? super String, String> fun = new com.google.common.base.Function<String, String>() {
	        public String apply(String v) {
	        	if(tooltip.get(v)==null)
	        		return "";
	        	return "<html><font size=\"5\">"+v+"<br/></font>"+stringToHTMLString(tooltip.get(v))+"</html>";
	        }
	    };
	    
	    vv.setVertexToolTipTransformer(fun);
	    
		return vv;
	}
	
	
	public static String stringToHTMLString(String string) {
	    StringBuffer sb = new StringBuffer(string.length());
	    // true if last char was blank
	    boolean lastWasBlankChar = false;
	    int len = string.length();
	    char c;

	    for (int i = 0; i < len; i++) {
	        c = string.charAt(i);
	        if (c == ' ') {
	            // blank gets extra work,
	            // this solves the problem you get if you replace all
	            // blanks with &nbsp;, if you do that you loss 
	            // word breaking
	            if (lastWasBlankChar) {
	                lastWasBlankChar = false;
	                sb.append("&nbsp;");
	            } else {
	                lastWasBlankChar = true;
	                sb.append(' ');
	            }
	        } else {
	            lastWasBlankChar = false;
	            //
	            // HTML Special Chars
	            if (c == '"')
	                sb.append("&quot;");
	            else if (c == '&')
	                sb.append("&amp;");
	            else if (c == '<')
	                sb.append("&lt;");
	            else if (c == '>')
	                sb.append("&gt;");
	            else if (c == '\n')
	                // Handle Newline
	                sb.append("<br/>");
	            else {
	                int ci = 0xffff & c;
	                if (ci < 160)
	                    // nothing special only 7 Bit
	                    sb.append(c);
	                else {
	                    // Not 7 Bit use the unicode system
	                    sb.append("&#");
	                    sb.append(new Integer(ci).toString());
	                    sb.append(';');
	                }
	            }
	        }
	    }
	    return sb.toString();
	}
	public double linksWithin(SimpleDigraph trace, double outgoingWeight, double incommingWeight) {
		double ret = 0;
		for(int node=0;node<trace.getNumNodes();node++) {
			if(trace.getOutDegree(node)+trace.getInDegree(node)==0)
				continue;
			if(outgoingWeight!=0)
				ret += outgoingWeight*this.getOutDegree(node);
			if(incommingWeight!=0)
				ret += incommingWeight*this.getInDegree(node);
		}
		return ret;
	}
}