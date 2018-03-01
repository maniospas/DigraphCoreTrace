package analysis.code;

/**
 * <h1>ASTEntity</h1>
 * This class is used to represent an AST node of high-level code structures (i.e. classes, methods, arguments)
 * that is abstracted to a name, its type, its comments and its implementation (classes would not have a type, while
 * function arguments should not have a name).
 * @author Manios Krasanakis
 */
public class ASTEntity extends Node {
	private String type;
	private String name;
	private String comments;
	private String implementation;
	private int positionalId;

	/**
	 * A constructor that assigns the desired parameters for the AST node.
	 * @param type node type (empty string for classes)
	 * @param name node name (empty string for arguments) 
	 * @param comments related comments, can also be set with {@link #updateComments(String)}
	 * @param implementation implementation in the source code
	 * @param positionalId position in the source code text
	 */
	public ASTEntity(String type, String name, String comments, String implementation, int positionalId) {
		this.name = name;
		this.type = type.trim();
		this.implementation = implementation;
		this.positionalId = positionalId;
		updateComments(comments);
	}
	protected ASTEntity() {
	}
	/**
	 * <h1>getImplementation</h1>
	 * @return the source code implementation (if available) for this entity
	 */
	public String getImplementation() {
		return implementation;
	}
	/**
	 * <h1>getPositionalId</h1>
	 * @return the position of the entity (if available) in the source code
	 */
	public int getPositionalId() {
		return positionalId;
	}
	/**
	 * <h1>getName</h1>
	 * @return the node's name
	 */
	public String getName() {
		return name;
	}
	/**
	 * <h1>getType</h1>
	 * @return the node's type
	 */
	public String getType() {
		return type;
	}
	/**
	 * <h1>getComments</h1>
	 * @return comments related to the node
	 */
	public String getComments() {
		return comments;
	}
	/**
	 * <h1>updateComments</h1>
	 * @return set new comments related to the node, while omitting parameter javadoc
	 */
	public void updateComments(String newComments) {
		comments = analysis.code.CleanComments.removeParams(newComments);
	}
	public String getTypedStackTrace() {
		String ret = "";
		if(!type.isEmpty())
			ret += type+" ";
		ret += getStackTrace();
		/*if(!children.isEmpty()) {
			ret += " (";
			for(ASTEntity child : children)
				ret += child.getTypedStackTrace()+", ";
			if(ret.endsWith(", "))
				ret = ret.substring(0, ret.length()-2);
			ret += ")";
		}*/
		return ret;
	}
	/**
	 * <h1>copyWithoutComments</h1>
	 * @return an identical copy of the node without comments
	 */
	public ASTEntity copyWithoutComments() {
		ASTEntity ret = new ASTEntity();
		ret.comments = "";
		ret.name = name;
		ret.type = type;
		ret.parent = null;
		ret.positionalId = positionalId;
		ret.implementation = implementation;
		try{
			for(Node child : children)
				ret.addChild(child.copyWithoutComments());
		}
		catch(Exception e){ 
			e.printStackTrace();
		}
		return ret;
	}
	/**
	 * <h1>getStrackTrace</h1>
	 * Implementation is iterative.
	 * @return a unique representation of this entity's position within its parents.
	 */
	public String getStackTrace() {
		String args = "";
		if(isMethod()) {
			for(Node child : children) {
				if(!args.isEmpty())
					args += ", ";
				args += child.getType();
			}
			args = "("+args+")";
		}
		return (parent!=null?parent.getStackTrace()+".":"")+getName()+args;
	}
	/**
	 * <h1>isArgument</h1>
	 * @return whether the node is a method argument
	 */
	public boolean isArgument() {
		return !isMethod() && !isClass();//TODO check for empty name instead
	}
	/**
	 * <h1>isClass</h1>
	 * @return whether the node is a class (i.e. does not have a type)
	 */
	public boolean isClass() {
		return type.isEmpty();
	}
	/**
	 * <h1>isMethod</h1>
	 * @return whether the node is a method
	 */
	public boolean isMethod() {
		return getParent()!=null && parent.getType().isEmpty() && !isClass(); //TODO check if neither argument nor class instead
	}
	@Override
	public String toString() {
		return getStackTrace();
	}
}
