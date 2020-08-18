package de.tuebingen.sfs.util;

import java.io.FileNotFoundException;
import java.util.*;

public class LanguageTree {
    Map<String, List<String>> paths;
    // virtual root node is marked "ROOT"
    public String root = "ROOT";
    public Map<String, String> parents;
    public Map<String, TreeSet<String>> children;

    public Map<String, Set<String>> annotation;

    public LanguageTree() {
        paths = new TreeMap<String, List<String>>();
        parents = new TreeMap<String, String>();
        children = new TreeMap<String, TreeSet<String>>();
        children.put("ROOT", new TreeSet<String>());

        annotation = new TreeMap<String, Set<String>>();
    }
    public static LanguageTree fromNewickFile(String nwkFile) throws FileNotFoundException {
        // one tree per line, will be connected by virtual ROOT node
        // important assumption: unique leaf names!
        List<String> nwkStrings = ListReader.listFromFile(nwkFile);

        int unnamedNodeID = 0;

        LanguageTree tree = new LanguageTree();
        // stack-based parsing, creating and adding nodes on the fly
        List<List<String>> stack = new LinkedList<List<String>>();
        stack.add(new LinkedList<String>());
        for (String nwkString : nwkStrings) {
            char[] nwk = nwkString.toCharArray();
            boolean parsingNodeName = false;
            boolean parsingBranchLength = false;
            boolean parsingAnnotation = false;
            boolean closingLabel = false;
            StringBuilder currentNodeName = new StringBuilder();
            StringBuilder currentAnnotation = new StringBuilder();
            for (int i = 0; i < nwk.length; i++) {
                if (parsingBranchLength) {
                    if (!(nwk[i] == ',' || nwk[i] == ')' || nwk[i] == '(' || nwk[i] == ';'))
                        continue;
                    parsingBranchLength = false;
                    parsingNodeName = true;
                }
                if (parsingAnnotation) {
                    if (!(nwk[i] == ',' || nwk[i] == ')' || nwk[i] == '(' || nwk[i] == ';')) {
                        currentAnnotation.append(nwk[i]);
                        continue;
                    } else {
                        parsingAnnotation = false;
                        parsingNodeName = true;
                    }
                }
                if (parsingNodeName) {
                    if ((nwk[i] == ',' || nwk[i] == ')' || nwk[i] == ';' || nwk[i] == '(')) {
                        String completeNodeName = currentNodeName.toString();
                        if (completeNodeName.length() == 0) {
                            completeNodeName = "unnamedNode" + (unnamedNodeID++);
                        }
                        String completeAnnotation = currentAnnotation.toString();
                        currentNodeName = new StringBuilder();
                        currentAnnotation = new StringBuilder();
                        parsingNodeName = false;

                        if (closingLabel) {
                            TreeSet<String> children = new TreeSet<String>();
                            for (String child : stack.remove(0)) {
                                tree.parents.put(child, completeNodeName);
                                children.add(child);
                            }
                            tree.children.put(completeNodeName, children);
                            if (completeAnnotation.length() > 0)
                                tree.annotation.put(completeNodeName,
                                        new TreeSet<String>(Arrays.asList(completeAnnotation.split("/"))));
                            closingLabel = false;
                        }

                        switch (nwk[i]) {
                            case ',': {
                                stack.get(0).add(completeNodeName);
                                if (completeAnnotation.length() > 0)
                                    tree.annotation.put(completeNodeName,
                                            new TreeSet<String>(Arrays.asList(completeAnnotation.split("/"))));
                                break;
                            }
                            case ')': {
                                closingLabel = true;
                                stack.get(0).add(completeNodeName);
                                if (completeAnnotation.length() > 0)
                                    tree.annotation.put(completeNodeName,
                                            new TreeSet<String>(Arrays.asList(completeAnnotation.split("/"))));
                                break;
                            }
                            // ; lowest stack layer contains nodes to be connected
                            // by root
                            case ';': {
                                stack.get(0).add(completeNodeName);
                                if (completeAnnotation.length() > 0)
                                    tree.annotation.put(completeNodeName,
                                            new TreeSet<String>(Arrays.asList(completeAnnotation.split("/"))));
                                break;
                            }
                            case '(': {
                                stack.add(0, new LinkedList<String>());
                                break;
                            }
                        }
                    } else {
                        if (nwk[i] == ':') {
                            parsingAnnotation = true;
                            continue;
                        } else if (nwk[i] == '#') {
                            parsingAnnotation = true;
                            continue;
                        } else {
                            currentNodeName.append(nwk[i]);
                        }
                    }
                } else if (nwk[i] == '(') {
                    stack.add(0, new LinkedList<String>());
                } else if (nwk[i] == ':') {
                    // empty node label
                    parsingAnnotation = true;
                } else {
                    parsingNodeName = true;
                    currentNodeName.append(nwk[i]);
                }
            }
        }
        TreeSet<String> roots = new TreeSet<String>();
        if (stack.get(0).size() == 1) {
            if (stack.get(0).get(0).equals("ROOT")) {
                roots.addAll(tree.children.get("ROOT"));
            } else {
                roots.add(stack.get(0).get(0));
            }
            tree.root = stack.get(0).get(0);
        } else {
            for (String lang : stack.get(0)) {
                roots.add(lang);
                tree.parents.put(lang, "ROOT");
            }
            tree.children.put("ROOT", roots);
        }

        for (String root : roots) {
            addPaths(tree, root, new LinkedList<String>());
        }
        return tree;
    }

    public static void addPaths(LanguageTree tree, String node, List<String> pathToNode) {
        tree.paths.put(node, new LinkedList<String>(pathToNode));
        if (tree.children.get(node) != null && tree.children.get(node).size() > 0) {
            List<String> extendedPath = new LinkedList<String>(pathToNode);
            extendedPath.add(node);
            for (String child : tree.children.get(node)) {
                addPaths(tree, child, extendedPath);
            }
        }
    }

    public List<String> pathFromRoot(String lang) {
        LinkedList<String> path = new LinkedList<String>();
        while (parents.get(lang) != null && !lang.equals("ROOT")) {
            lang = parents.get(lang);
            path.add(lang);
        }
        Collections.reverse(path);
        return path;
    }
}
