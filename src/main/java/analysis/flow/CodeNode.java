package analysis.flow;

import java.util.ArrayList;

public class CodeNode {
	private CodeNode parent = null;
	private ArrayList<CodeNode> children = new ArrayList<CodeNode>();
	private ArrayList<CodeNode> parameters = new ArrayList<CodeNode>();
	private String name;
	private CodeNode type;
	
	public CodeNode(String name, CodeNode type) {
		this.name = name;
		this.type = type;
		if(type!=null && !type.isClass())
			throw new RuntimeException("Types must be classes");
	}
	public ArrayList<CodeNode> pack() {
		ArrayList<CodeNode> ret = new ArrayList<CodeNode>();
		ret.add(this);
		for(CodeNode child : children)
			ret.addAll(child.pack());
		if(isClass())
			for(CodeNode param : parameters)
				ret.addAll(param.pack());
		return ret;
	}
	public CodeNode getParent() {
		return parent;
	}
	public ArrayList<CodeNode> getChildren() {
		return children;
	}
	public ArrayList<CodeNode> getParameters() {
		return parameters;
	}
	public void addParameter(CodeNode parameter) {
		if(isClass() != parameter.isClass())
			throw new RuntimeException("Parameters must be classes for classes and non-classes for methods");
		//if(parameter.parent!=null)
			//throw new RuntimeException("Parameter already has a parent");
		//parameter.parent = this;
		parameters.add(parameter);
	}
	public void add(CodeNode child) {
		if(child.parent!=null)
			throw new RuntimeException("Child already has a parent");
		child.parent = this;
		children.add(child);
	}
	public boolean isClass() {
		return type==null;
	}
	public boolean isMethod() {
		return !isClass() && !isVariable();
	}
	public boolean isVariable() {
		return false;
	}
	public String getName() {
		return name;
	}
	public CodeNode getType() {
		return type;
	}
	public void changeType(CodeNode type) {
		if(type!=null && !type.isClass())
			throw new RuntimeException("Types must be classes");
		this.type = type;
	}
	protected String getTrace() {
		String ret = "";
		if(parent!=null)
			ret += parent.getTrace()+".";
		return ret+name;
	}
	@Override
	public String toString() {
		String ret = "";
		if(getType()!=null)
			ret += getType().getTrace()+" ";
		ret += getTrace();
		if(!parameters.isEmpty()) {
			ret += "("+parameters.get(0).toString();
			for(int i=1;i<parameters.size();i++)
				ret += ", "+parameters.get(i).toString();
			ret += ")";
		}
		else if(!isClass())
			ret += "()";
		return ret;
	}
	
	public static final CodeNode UNRESOLVED = new CodeNode("???", null){
		@Override
		public String toString() {
			return "???";
		}
	};
}
