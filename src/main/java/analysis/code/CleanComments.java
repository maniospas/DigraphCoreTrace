package analysis.code;

import java.util.ArrayList;
import java.util.StringJoiner;

/**
 * <h1>CleanComments</h1>
 * This class can be used to clean code comments into easily parsable forms.
 * @author Manios Krasanakis
 */
public class CleanComments {

	/**
	 * <h1>clean</h1>
	 * @param comments the comments to be cleaned
	 * @return comments without foreign elements and no alignment line breaks
	 */
	public static String clean(String comments) {
		return removeHandAlignedLinebreaks(cleanForeignElements(comments));
	}
	
	public static String removeParams(String comment) {
		String[] lines = getTextSentences(comment);
		StringJoiner ret = new StringJoiner("\n");
		for(int i=0;i<lines.length;i++) 
			if(!lines[i].startsWith("@"))
				ret.add(lines[i]);
		return ret.toString();
	}
	protected static String removeHTML(String comment) {
		StringBuilder builder = new StringBuilder();
		int level = 0;
		for(int i=0;i<comment.length();i++) {
			Character c = comment.charAt(i);
			if(c=='<')
				level++;
			else if(c=='>') {
				if(level>0)
					level--;
			}
			else if(level==0)
				builder.append(c);
		}
		return builder.toString();
	}
	protected static String removeCommentAnnotation(String comment) {
		return comment.replaceAll("(?m)^[\\*\\/\\s]+", "").replaceAll("\n+", "\n").replaceAll("\\s+\\*\\/", "");
	}
	protected static String cleanForeignElements(String comment) {
		return removeHTML(removeCommentAnnotation(comment));
	}
	protected static String removeHandAlignedLinebreaks(String comment) {
		String[] lines = getTextSentences(comment);
		if(lines.length==0)
			return comment;
		StringBuilder ret = new StringBuilder();
		String previousSentence;
		ret.append(previousSentence = lines[0]);
		for(int i=1;i<lines.length;i++) {
			lines[i] = lines[i].trim();
			if(lines[i].isEmpty())
				continue;
			if(!isAppendableToPreviousSentence(previousSentence, lines[i])) 
				ret.append("\n");
			else 
				ret.append(" ");
			ret.append(previousSentence=lines[i]);
		}
		return ret.toString();
	}
	
	protected static boolean isAppendableToPreviousSentence(String previousSentence, String sentence) {
		if(sentence.startsWith("It"))
			return true;
		if(sentence.startsWith("In this"))
			return true;
		if(sentence.startsWith("{"))
			return true;
		if(Character.isLowerCase(sentence.charAt(0)))
			return true;
		if(Character.isUpperCase(sentence.charAt(0)) && Character.isUpperCase(sentence.charAt(1)))
			return true;
		return false;
	}
	
	protected static String[] getTextSentences(String text) {
		String[] allSentences = text.split("(\\n|(\\.\\s+($(|\\n|\\s+(?=[A-Z0-9])))))");
		ArrayList<String> sentences = new ArrayList<String>();
		String currentString = "";
		int accumulation = 0;
		for(String sentence : allSentences) {
			int localAccumulation = countOccurrences(sentence, '(')+countOccurrences(sentence, '{')+countOccurrences(sentence, '[')
									-countOccurrences(sentence, ')')-countOccurrences(sentence, '}')-countOccurrences(sentence, ']');
			accumulation += localAccumulation;
			if(accumulation>0) 
				currentString += sentence+" ";
			else {
				currentString += sentence;
				sentences.add(currentString);
				currentString = "";
			}
		}
		if(!currentString.isEmpty()) {
			sentences.add(currentString);
		}
		return (String[])sentences.toArray(new String[sentences.size()]);
	}
	private static int countOccurrences(String haystack, char needle)
	{
	    int count = 0;
	    for (int i=0; i < haystack.length(); i++)
	        if (haystack.charAt(i) == needle)
	             count++;
	    return count;
	}
	
}
