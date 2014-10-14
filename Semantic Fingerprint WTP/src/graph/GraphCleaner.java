package graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;

import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;

/**
 * Implements an algorithm to remove unused nodes from a given graph. Returns all paths connecting the request nodes that match the given constraints (length).
 * @author Christian Nywelt
 */
public class GraphCleaner {
	protected final Graph graph;
	protected final LinkedHashSet<Node> requestNodes = new LinkedHashSet<Node>();
	
	/**
	 * Constructs a ready to use graph cleaner.
	 * @param graph The graph that will be tidied.
	 * @param requestedNodes The start nodes between which the paths shall be found.
	 */
	public GraphCleaner(Graph graph, List<dbpedia.BreadthFirstSearch.Node> requestedNodes) {
		this.graph = graph;
		
		// put request nodes into hashmap
		for(dbpedia.BreadthFirstSearch.Node temp : requestedNodes) {
			this.requestNodes.add(graph.getNode(temp.resourceName()));
		}
	}
	
	/**
	 * Constructs a ready to use graph cleaner.
	 * @param graph The graph that will be tidied.
	 * @param requestedNodes The start nodes between which the paths shall be found.
	 */
	public GraphCleaner(Graph graph, Collection<Node> requestedNodes) {
		this.graph = graph;
		
		// put request nodes into hashmap
		for(Node temp : requestedNodes) {
			this.requestNodes.add(temp);
		}
	}
	
	/**
	 * 
	 */
	public static class Path extends LinkedList<Node> implements Comparable<Path> {
		/**
		 * 
		 * @param nodes
		 */
		public Path(LinkedList<Node> nodes) {
			this.addAll(nodes);
		}
		
		@Override
		public int compareTo(Path o) {
			int thisSize = size();
			int oSize = o.size();
			if (thisSize < oSize) {
				return -1;
			} else if (thisSize > oSize) {
				return 1;
			} else {
				Iterator<Node> oIt = o.iterator();
				for (Node n : this) {
					Node oNode = oIt.next();
					if(!n.equals(oNode)) {
						return n.getId().compareTo(oNode.toString());
					}
				}
				return 0;
			}
		}
	}
	
	/**
	 * 
	 */
	public static class ExtendedPath extends Path {
		/**
		 * 
		 */
		private static final long serialVersionUID = 7840724985771379176L;
		
		private final int extensionLength;
		
		public final int getExtensionLength() {
			return extensionLength;
		}
		
		public ExtendedPath(LinkedList<Node> nodes, int extensionLen) {
			super(nodes);
			this.extensionLength = extensionLen;
		}
		
		/* Does not work correctly
		@Override
		public int compareTo(Path o) {
			int cmp = super.compareTo(o);
			if (cmp == 0) {
				return cmp;
			} else {
				if (o instanceof ExtendedPath) {
					return Integer.compare(extensionLength, ((ExtendedPath) o).extensionLength);
				} else {
					return cmp;
				}
			}
		}
		*/
	}
	
	/**
	 * 
	 */
	public static class ImplicitPath extends Path {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1853567879609198658L;

		public ImplicitPath(LinkedList<Node> nodes) {
			super(nodes);
		}
	}
	
	/**
	 * Cleans the graph using BreadthFirstSearch.
	 * @param maxPathLength The maximum length a path between to start nodes can have.
	 * @param maxExtensionLength The maximum length a path extension can have.<br>Note that extended paths will also not be longer than maxPathLength.
	 * @return All the paths between the start nodes that match the given constraints.
	 */
	public LinkedList<Path> clean(int maxPathLength, int maxExtensionLength) {
		// this list will contain all the found path that match the given constraints (method parameters)
		LinkedList<Path> allPaths = new LinkedList<Path>();
		
		// caclulate the maximum amount of levels the algorithm should compare for
		int maxDepth = (int) Math.ceil((maxPathLength+1)/(double)2);
		
		// 1) init vars
		int numRequestNodes = requestNodes.size();
		BFSMemory bfsMem = new BFSMemory(numRequestNodes, maxDepth);
		
		// 2) fill the level 0 lists
		//System.out.println("\n--- Start Nodes");
		int k = 0;
		for(Node n : requestNodes) {
			bfsMem.getList(k, 0).add(n);
			bfsMem.seenNodes.put(n, (byte) 1);
			k++;
			//System.out.println("(" + (k-1) + ") " + n);
		}
		//System.out.println("--- Linking Nodes:");
		
		// 3) fill the other lists and serach for connector nodes
		for (int level = 0; level < maxDepth; level++) {
			for (int idxNode = 0; idxNode < numRequestNodes; idxNode++) {
				// get all adjacent node for each node in this list
				HashSet<Node> levelNodes = bfsMem.getList(idxNode, level);
				HashSet<Node> nextLevelNodes = bfsMem.getList(idxNode, level+1);
				
				for(Node n : levelNodes) {
					if ((maxExtensionLength == 0) && level != 0 && bfsMem.seenNodes.get(n) > 0) continue; // just for performance improvements
					
					Iterator<Node> neighborNodes = n.getNeighborNodeIterator();
					while(neighborNodes.hasNext()) {
						Node neighbour = neighborNodes.next();
						// 3.1) check if we are going backwards to were we came from
						if (bfsMem.seenFromNode(idxNode, neighbour)) continue; // make this constraint weaker to get longer paths
						//if (bfsMem.visitedFromNodeAtLevel(idxNode, neighbour, level-1)) continue;
						
						
						// 3.2) add it to this list
						if (!bfsMem.seenNodes.containsKey(neighbour)) {
							bfsMem.seenNodes.put(neighbour, (byte) 0);
						}
						nextLevelNodes.add(neighbour);
						
						// 3.3) check if the neighbor is already in any other list of the other start nodes (than its a linking node)
						LinkedList<LinkedList<Node>> paths = new LinkedList<LinkedList<Node>>();
						for(int otherIdx = 0; otherIdx < numRequestNodes; otherIdx++) { 
							if (otherIdx == idxNode) continue;
							LinkedList<LinkedList<Node>> pathToThisNode = bfsMem.getAllPathsBack(otherIdx, neighbour);
							// remove the first element from all the lists (because its the linking node)
							for (LinkedList<Node> path : pathToThisNode) {
								path.removeFirst();
							}
							paths.addAll(pathToThisNode);
						}
						
						// 3.4) if so mark it as linking node (and all other nodes from this node to the start as well)
						if (!paths.isEmpty()) {
							LinkedList<LinkedList<Node>> thisPaths = bfsMem.getAllPathsBack(idxNode, neighbour);
							// reverse all lists for faster complete path genertion
							for (LinkedList<Node> path : thisPaths) {
								Collections.reverse(path);
							}
							// Search for correct paths
							for (LinkedList<Node> path : paths) {
								for (LinkedList<Node> p : thisPaths) {
									if (Collections.disjoint(path, p)) {
										byte pathType; // 0: kürzester Pfad, 1: erweiterter Pfad, 2: implizieter (erweiterter) Pfad
										// a) generate path
										@SuppressWarnings("unchecked")
										LinkedList<Node> completePath = (LinkedList<Node>) p.clone();
										completePath.addAll(path);
										
										// paths should always start at the node with the smaller index
										if(bfsMem.compareByIndex(p.getFirst(), completePath.getLast()) > 0) {
											Collections.reverse(completePath);
										}
										
										// b) check if we didn't find that path already
										if(allPaths.contains(completePath)) {
											continue;
										}
										
										// c) count the deteur length
										int shortNodes = 0;
										for(Node pathNode : completePath) {
											if (bfsMem.seenNodes.get(pathNode) == 1) {
												shortNodes++;
											}
										}
										
										if (shortNodes == 2) { // only the startNodes are already marked
											pathType = (byte) 0;
										} else if(shortNodes == completePath.size()) {
											pathType = (byte) 2;
										} else {
											pathType = (byte) 1;
										}
										
										int extensionLength = completePath.size() - shortNodes;
										// d) mark all the nodes as linking nodes (if the path is not to long)
										if (completePath.size() <= (maxPathLength+2) && (pathType == 0|| extensionLength <= maxExtensionLength)) {
											// d.1) mark the nodes 
											for(Node pathNode : completePath) {
												if (bfsMem.seenNodes.get(pathNode) == 0) {
													bfsMem.seenNodes.put(pathNode, (byte) (pathType+1)); // will never return 3 because implicit paths do not have a node with value == 0
												}
											}
											// d.2) save the result path
											Path resultPath;
											switch(pathType) {
											case 0:
												resultPath = new Path(completePath);
												break;
											case 1:
												resultPath = new ExtendedPath(completePath, extensionLength);
												break;
											default:
												resultPath = new ImplicitPath(completePath);
												break;
											}
											allPaths.add(resultPath);
											
											//System.out.println(completePath);
										}
									}
								}
							}
						}
					}
				}
			}
		}
		
		// 4) delete all nodes that have not been marked as linking nodes
		List<Node> nToDelete = new LinkedList<Node>();
		for(Node n : graph) {
			if (!bfsMem.seenNodes.containsKey(n) || bfsMem.seenNodes.get(n) == 0) { // bfsMem.visitedNodes.containsKey(n) because: when we delete edges small unconnected groups could remain
				nToDelete.add(n);
			}
		}
		for(Node n : nToDelete) {
			graph.removeNode(n);
		}
		
		return allPaths;
	}
	
	/**
	 * Implements a Matrix
	 *
	 */
	private class BFSMemory {
		// 0: not a linking node, 1: linking node on shortest path, 2: linking node on extension path
		public final HashMap<Node, Byte> seenNodes = new HashMap<Node, Byte>(); // saves visited nodes and whether the node is a linking node
		public final ArrayList<HashSet<Node>> visited; // saves the nodes for each level of the bfs from each startnode
		public final int numRequestNodes;
		public final int numListsEachNode;
		public final int maxDepth;
		
		/**
		 * 
		 * @param numRequestNodes
		 * @param maxDepth
		 */
		public BFSMemory(int numRequestNodes, int maxDepth) {
			this.maxDepth = maxDepth;
			this.numListsEachNode = maxDepth + 1;
			this.numRequestNodes = numRequestNodes;
			int numLists = numRequestNodes * numListsEachNode;
			visited = new ArrayList<HashSet<Node>>(numLists);
			// init empty
			for(int i = 0; i < numLists; i++) {
				visited.add(new HashSet<Node>());
			}
		}
		
		/**
		 * 
		 * @param first
		 * @param other
		 * @return
		 */
		public int compareByIndex(Node first, Node other) {
			for(int idx = 0; idx < numRequestNodes; idx++) {
				HashSet<Node> nodesForNextLevel = getList(idx, 0);
				if (nodesForNextLevel.contains(first)) {
					return -1;
				} else if (nodesForNextLevel.contains(other)) {
					return 1;
				}
			}
			return 0;
		}

		/**
		 * Returns true if the bfs already found this node (starting at the specified node)
		 * @param idx
		 * @param node
		 * @return
		 */
		public boolean seenFromNode(int idx, Node node) {
			for(int level = maxDepth; level >= 0; level--) {
				HashSet<Node> nodesForLevel = getList(idx, level);
				if (nodesForLevel.contains(node)) {
					return true;
				}
			}
			return false;
		}
		
		/**
		 * Returns true if the node was already reached from the given idx start node on the given level
		 * @param idx
		 * @param node
		 * @param level
		 * @return
		 */
		public boolean visitedFromNodeAtLevel(int idx, Node node, int level) {
			if (level < 0) return false;
			else return getList(idx, level).contains(node);
		}
		
		
		/**
		 * If not only the shortest paths shall be returned additional nodes need an extra check. They must not use the same node on their path to the starting nodes.
		 * @param excludedIdx
		 * @param node
		 * @return
		 */
		public LinkedList<LinkedList<Node>> getAllPathsBack(int idx, Node node) {
			LinkedList<LinkedList<Node>> res = new LinkedList<LinkedList<Node>>();
			
			for(int level = maxDepth; level >= 0; level--) {
				HashSet<Node> nodesForLevel = getList(idx, level);
				if (nodesForLevel.contains(node)) {
					// add start node
					LinkedList<Node> tmp = new LinkedList<Node>();
					tmp.add(node);
					
					// extend to paths, TODO: do not recursively
					res.addAll(extendBack(tmp, idx, level-1));
					
					// stop searching for the start node
					break;
				}
			}
			
			return res;
		}

		/**
		 * Tiefensuche zum finden aller Pfade zurück zum Startknoten
		 * @param pathUntilNow
		 * @param idx
		 * @param level
		 * @return
		 */
		private LinkedList<LinkedList<Node>> extendBack(LinkedList<Node> pathUntilNow, int idx, int level) {
			LinkedList<LinkedList<Node>> res = new LinkedList<LinkedList<Node>>();
			
			if (level < 0) {
				@SuppressWarnings("unchecked")
				LinkedList<Node> p = (LinkedList<Node>) pathUntilNow.clone();
				res.add(p);
				return res;
			}
			
			// search deeper
			Node node = pathUntilNow.getLast();
			HashSet<Node> nodesForLevel = getList(idx, level);
			
			// serach
			Iterator<Node> neighborNodes = node.getNeighborNodeIterator();
			while(neighborNodes.hasNext()) {
				Node neighbour = neighborNodes.next();
				if (nodesForLevel.contains(neighbour)) {
					// extend list
					pathUntilNow.addLast(neighbour);
					res.addAll(extendBack(pathUntilNow, idx, level-1));
					pathUntilNow.removeLast();
				}
			}
			// and return
			return res;
		}

		/**
		 * 
		 * @param idx
		 * @param level
		 * @return
		 */
		public HashSet<Node> getList(int idx, int level) {
			return visited.get(idx * numListsEachNode + level);
		}
	}
}
