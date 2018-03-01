package analysis.code;

import java.util.ArrayList;

public class CodeManipulation {
	public static int min(int idx1, int idx2) {
		if(idx1==-1)
			return idx2;
		if(idx2==-1)
			return idx1;
		return Math.min(idx1, idx2);
	}
	
	public static String removeComments(String code) {
		  return code.replaceAll("(?:/\\*(?:[^*]|(?:\\*+[^*/]))*\\*+/)|(?://.*)", "");
		}
	
	public static int topLevelIndexOf(String text, String str, int startingPosition) {
		char first = str.charAt(0);
		int pos = startingPosition-1;
		while(pos<text.length()) {
			pos = topLevelIndexOf(text, first, pos+1);
			if(pos==-1)
				break;
			boolean found = true;
			for(int i=0;i<str.length() && found;i++)
				if(pos+i>=text.length())
					found = false;
				else if(str.charAt(i)!=text.charAt(pos+i))
					found = false;
			if(found)
				return pos;
		}
		return -1;
	}
	
	public static int topLevelIndexOf(String text, char c, int pos) {
		int level = 0;
		for(int i=pos;i<text.length();i++) {
			if(text.charAt(i)==c && level==0)
				return i;
			if(text.charAt(i)=='(')
				level++;
			else if(text.charAt(i)==')')
				level--;
			if(text.charAt(i)==c && level<=0)
				return i;
			if(level<0)
				break;
		}
		return -1;
	}
	
	public static ArrayList<String> getTopLevelStatements(String text, int pos) {
		ArrayList<String> statements = new ArrayList<String>();
		String currentStatement = "";
		int level = 0;
		for(int i=pos;i<text.length();i++) {
			char c = text.charAt(i);
			if(c=='{')
				level++;
			else if(c=='}')
				level--;
			else if(c=='\n')
				continue;
			else if(c==';' && level==0) {
				statements.add(currentStatement.trim());
				currentStatement = "";
			}
			else if(level==0)
				currentStatement += c;
			if(level<0)
				break;
		}
		currentStatement = currentStatement.trim();
		if(!currentStatement.isEmpty())
			statements.add(currentStatement);
		return statements;
	}
	
	public static ArrayList<String> splitToStatements(String text, int pos) {
		ArrayList<String> statements = new ArrayList<String>();
		String currentStatement = "";
		int level = 0;
		for(int i=pos;i<text.length();i++) {
			char c = text.charAt(i);
			if(c=='\n')
				continue;
			else if(c=='{' || c=='}' || c==';') {
				statements.add(currentStatement.trim());
				currentStatement = "";
			}
			else
				currentStatement += c;
			if(level<0)
				break;
		}
		currentStatement = currentStatement.trim();
		if(!currentStatement.isEmpty())
			statements.add(currentStatement);
		return statements;
	}
	
	public static int topLevelCountOf(String text, char c, int pos) {
		int level = 0;
		int count = 0;
		for(int i=pos;i<text.length();i++) {
			if(text.charAt(i)=='(')
				level++;
			else if(text.charAt(i)==')')
				level--;
			else if(text.charAt(i)==c && level==0)
				count++;
			if(level<0)
				break;
		}
		return count;
	}
}