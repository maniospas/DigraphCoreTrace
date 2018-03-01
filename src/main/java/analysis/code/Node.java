package analysis.code;

import java.util.ArrayList;

public abstract class Node {
	protected Node parent;
	protected ArrayList<Node> children = new ArrayList<Node>();
	
	/**
	 * <h1>addChild</h1>
	 * Adds a child to the AST node. Also sets the child's parent node.
	 * @param child
	 */
	public void addChild(Node child) {
		if(child.parent!=null)
			throw new RuntimeException("Child already assigned to parent");
		if(child==this)
			throw new RuntimeException("Cannot make self a child");
		child.parent = this;
		children.add(child);
		child.updateComments(child.getComments());
	}
	/**
	 * <h1>removeChild</h1>
	 * Removes a child from the AST node. Also removes the child's parent node.
	 * @param child
	 */
	public void removeChild(Node child) {
		if(child.parent!=this)
			throw new RuntimeException("Child assigned to different parent");
		child.parent = null;
		children.remove(child);
	}
	/**
	 * <h1>getChildren</h1>
	 * @return a list of the node's child nodes (i.e. methods of function or arguments of methods)
	 */
	public ArrayList<Node> getChildren() {
		return children;
	}
	/**
	 * <h1>getParent</h1>
	 * @return the parent node
	 */
	public Node getParent() {
		return parent;
	}
	/**
	 * <h1>collapse</h1>
	 * Implementation is iterative.
	 * @return a list containing this entity and its children.
	 */
	final public ArrayList<Node> collapse() {
		ArrayList<Node> list = new ArrayList<Node>();
		list.add(this);
		for(Node child : children) 
			list.addAll(child.collapse());
		return list;
	}
	/**
	 * <h1>getLevel</h1> 
	 * @return the number of parents until the top parent is reached
	 */
	public final int getLevel() {
		int level = 0;
		if(parent!=null)
			level = parent.getLevel()+1;
		return level;
	}
	public boolean isComparable(ASTEntity to) {
		return getLevel()==to.getLevel();
	}
	/**
	 * <h1>getStrackTrace</h1>
	 * Implementation is iterative.
	 * @return a unique representation of this entity's position within its parents.
	 */
	public abstract String getStackTrace();
	public abstract String getName();
	public abstract String getType();
	public abstract String getComments();
	public abstract void updateComments(String newComments);
	public abstract Node copyWithoutComments();
}
