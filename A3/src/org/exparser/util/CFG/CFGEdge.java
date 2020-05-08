package org.exparser.util.CFG;

public class CFGEdge {

    private int edgeHash;

    private CFGNode fromNode;

    private CFGNode toNode;

    public CFGEdge(CFGNode fromNode, CFGNode toNode){
        this.fromNode = fromNode;
        this.toNode = toNode;
        this.edgeHash = createHash();
    }

    private int createHash(){
        String toHash;
        if(fromNode == null && toNode == null){
            toHash = "null" + "null";
        }
        else if(fromNode == null ){
            toHash = toNode.toString();
        }
        else if(toNode == null){
            toHash = fromNode.toString();
        }
        else {
            toHash = fromNode.toString() + toNode.toString();
        }
        return toHash.hashCode();

    }

    public int getHash() {
        return edgeHash;
    }

    public CFGNode getFromNode() {
        return fromNode;
    }

    public CFGNode getToNode() {
        return toNode;
    }

    public void destroyEdge(){
        this.getFromNode().removeEdgeOut(this);
        this.getToNode().removeEdgeIn(this);
    }

}
