import java.util.LinkedList;

/**
 * Single node with a Trie structure.  Contains the current letter and a lists of the next letters.
 * @Author Samuel Shen
 */

public class TrieNode {
    private char letter;
    private LinkedList<TrieNode> nextNodes;
    private boolean end;        //whether this node is the end of a word

    public TrieNode() {

    }
    public TrieNode(char c) {
        letter = c;
        nextNodes = new LinkedList<>();
    }
    public TrieNode(char c, LinkedList<TrieNode> next) {
        letter = c;

        this.nextNodes = next;

    }
    public TrieNode(char c, LinkedList<TrieNode> next, boolean end) {
        letter = c;
        this.nextNodes = next;
        this.end = end;
    }

    /**
     * Tells if the current TrieNode is the last letter in a word
     * @return whether this letter is the last one
     */
    public boolean isLastLetter() {
        return (end);
    }

    /**
     * Adds a TrieNode to next nodes
     * @param next is the node to be added
     */
    public void addNext(TrieNode next) {
        nextNodes.add(next);
    }


    public char letter() {
        return letter;
    }
    public LinkedList<TrieNode> nextNodes() {
        return nextNodes;
    }

    /**
     * Checks if this node points to a node with a certain letter.
     * @param c the letter to point to
     * @return whether it points to that letter
     */
    public boolean pointsTo(char c) {
        if (nextNodes == null) {
            return false;
        }
        for (TrieNode node: nextNodes) {
            if (node.letter() == c) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the trienode that has this letter
     * @return the trie node
     */
    public TrieNode getNextNode(char c) {
        for (TrieNode node: nextNodes) {
            if (node.letter() == c) {
                return node;
            }
        }
        return null;
    }

    /**
     * Gets the list of next nodes.
     * @return the list of next nodes.
     */
    public LinkedList<TrieNode> getNextNodes() {
        return (nextNodes);
    }


    public void setLastStatus(boolean set) {
        this.end = set;
    }
    @Override
    public String toString() {
        if (end) {
            return (letter + "t" + " ");
        } else {
            return (letter + "f" + " ");
        }
    }
}
