package analysis.flow;

public class VariableNode extends CodeNode {
	public VariableNode(String name, CodeNode type) {
		super(name, type);
		if(type==null)
			throw new RuntimeException("Cannot declare variable without type");
	}
	@Override
	public boolean isVariable() {
		return true;
	}
	@Override
	public String toString() {
		String ret = "";
		if(getType()!=null)
			ret += getType().getTrace()+" ";
		if(getParent()!=null)
			ret += getParent().getTrace()+":";
		return ret+getName();
	}
}
