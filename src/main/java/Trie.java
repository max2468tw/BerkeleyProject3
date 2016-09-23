import java.util.LinkedList;

/**
 * A Trie data structure consisting of 26 TrieNodes.
 * @Author Samuel Shen
 */
public class Trie {

    /* An array of the trienode "heads", i.e. every letter of the alphabet
    *  representing the start of each word*/
    TrieNode[] heads;

    public Trie() {
        //initializes with 26 blank nodes
        heads = new TrieNode[26];
        for (int i = 0; i < 26; i++) {
            TrieNode node = new TrieNode((char) (i + 97), new LinkedList<>(), false);
            heads[i] = node;
        }
    }

    /**
     * Adds a word to the trie.
     * Indexes the word based on the last character of it's first block.
     * i.e. 725 Gilman is indexed based on "725 G" or "G"
     * @param word to be added
     */
    public void addWord(String word) {
        char[] arr = word.toCharArray();
        TrieNode current = new TrieNode();

        //indexes the word by it's starting block
        int ix = 0;
        char letter = arr[ix];
        String toAdd = letter + "";

        while (!(isLetter(letter + ""))) {
            ix++;
            letter = arr[ix];
            toAdd += letter + "";
        }

        char[] arr2 = toAdd.toCharArray();
        int index = arr2.length - 1;

        //gets correct bucket based on last letter of starting block
        if (arr2[index] >= 65) {
            if (arr2[index] >= 97) {     //lower case
                current = heads[(int) arr2[index] - 97];
            } else {
                current = heads[(int) arr2[index] - 65];
            }
        } else {
            current = heads[(int) arr2[index] - 22];
        }
        current = heads[(int) arr[0] - 97];

        //goes through and adds where necessary
        for (int i = 0; i < arr.length; i++) {
            letter = arr[i];
            toAdd = letter + "";

            while (!(isLetter(letter + ""))) {
                i++;
                letter = arr[i];
                toAdd += letter + "";
            }

            //if it already has a path
            if (current.pointsTo(letter)) {
                current = current.getNextNode(letter);
                if (i == arr.length - 1) {
                    current.setLastStatus(true);
                }
            } else {        //if it needs to make a path
                if (i != arr.length - 1) {       //if it's not the last letter
                    current.addNext(new TrieNode(arr[i], new LinkedList<>(), false));
                    current = current.getNextNode(letter);
                } else {                        //if it is the last letter
                    current.addNext(new TrieNode(arr[i], new LinkedList<>(), true));
                }
            }
        }
    }

    /**
     * Determines if word is a letter or not
     * @param word
     * @return
     */
    private boolean isLetter(String word) {
        char[] arr = word.toCharArray();
        char c = arr[0];
        if (c <= 122 && c >= 97 || c <= 90 && c >= 65 || c == ' ') {
            return true;
        }
        return false;
    }
    /**
     * Determines whether the trie contains the word or not independent of case.
     * The word might be part of a larger entry i.e.
     *      a call "Sushi" to a Trie with "Sushi Sono" returns true
     * @param word the word to check for
     * @return the trienodes leading up to that prefix
     */
    public LinkedList<TrieNode> contains(String word) {
        LinkedList<TrieNode> route =  new LinkedList<>();
        if (word.length() != 0) {
            char[] arr = word.toCharArray();
            TrieNode head;
            head = heads[(int) arr[0] - 97];


            //for the remaining letters
            for (int i = 0; i < arr.length; i++) {
                if (head.pointsTo(arr[i])) {        //if it has a valid pointer, increment
                    head = head.getNextNode(arr[i]);
                    route.addFirst(head);
                }
            }
            return route;
        } else {
            return null;
        }
    }

    /**
     * Gets the completes versions of prefix CASE INDEPENDENT
     * @param prefix is the prefix to look up.
     * @return the completed versions of prefix CASE DEPENDENT
     */
    public LinkedList<String> getCompletions(String prefix) {
        try {
            LinkedList<String> completions = new LinkedList<>();
            LinkedList<TrieNode> currentList = contains(prefix); //goes to the final node of prefix
            TrieNode current = currentList.getFirst();
            //stack of the fringe TrieNodes initialized to the starting node "children"
            //TODO IF I THERE ARE SPEED ISSUES, IT'S PROBABLY RIGHT HERE
            LinkedList<TrieNode> fringeStack =
                    (LinkedList<TrieNode>) current.getNextNodes().clone();
            if (current.isLastLetter()) {       //if the prefix is spot on
                completions.add(prefix);
            }
            //goes through each of the initial children of the end of the prefix
            while (!(fringeStack.isEmpty())) {
                completionsHelper(fringeStack, completions, prefix);
            }
            return completions;
        } catch (NullPointerException e) {
            System.out.println("Something went wrong on " + prefix);
            return new LinkedList<>();
        }

    }

    public void completionsHelper(LinkedList<TrieNode> fringe,
            LinkedList<String> list, String soFar) {
        TrieNode node = fringe.pop();

        soFar += node.letter();
        if (node.isLastLetter()) {
            list.add(soFar);
        }

        for (TrieNode tempNode: node.getNextNodes()) {
            fringe.addFirst(tempNode);
            completionsHelper(fringe, list, soFar);
        }

    }
}
